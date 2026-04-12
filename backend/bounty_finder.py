import requests
import re
import os

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

headers = {}
if GITHUB_TOKEN:
    headers["Authorization"] = f"token {GITHUB_TOKEN}"


# 🔍 Extract reward from text
def extract_reward(text):
    if not text:
        return None

    patterns = [r"\$\s?\d+", r"\d+\s?usd", r"₹\s?\d+"]
    for p in patterns:
        m = re.search(p, text.lower())
        if m:
            return m.group()

    return None


POPULAR_REPOS = [
    "microsoft/vscode",
    "facebook/react",
    "tensorflow/tensorflow",
    "pallets/flask",
    "fastapi/fastapi"
]


def fetch_all_bounties():
    results = []

    try:
        # 🔹 STEP 1: Search GitHub Issues (REAL DATA)
        search_url = "https://api.github.com/search/issues?q=bounty+OR+reward+is:issue+is:open&per_page=20"
        res = requests.get(search_url, headers=headers)

        if res.status_code == 200:
            data = res.json()

            for item in data.get("items", []):

                # ✅ SAFETY CHECK
                if not item or not item.get("html_url"):
                    continue

                description = (item.get("body") or "")[:200]

                results.append({
                    "repo": item.get("repository_url", "").replace("https://api.github.com/repos/", ""),
                    "title": item.get("title", "No title"),
                    "description": description + "..." if description else "No description",
                    "issue_url": item.get("html_url"),
                    "reward": extract_reward(item.get("body")) or "Not specified",
                    "verified": True
                })

        # 🔥 STEP 2: Fallback (if search gives less data)
        if len(results) < 10:
            print("Using fallback data...")

            for repo in POPULAR_REPOS:
                url = f"https://api.github.com/repos/{repo}/issues?state=open&per_page=10"
                res = requests.get(url, headers=headers)

                if res.status_code != 200:
                    continue

                issues = res.json()

                for issue in issues:

                    if not issue or not issue.get("html_url"):
                        continue

                    labels = [l.get("name", "").lower() for l in issue.get("labels", [])]

                    # classify type
                    if "good first issue" in labels:
                        issue_type = "Opportunity 🟡"
                    elif "help wanted" in labels:
                        issue_type = "Opportunity 🚀"
                    else:
                        issue_type = "General Issue"

                    description = (issue.get("body") or "")[:200]

                    results.append({
                        "repo": repo,
                        "title": issue.get("title", "No title"),
                        "description": description + "..." if description else "No description",
                        "issue_url": issue.get("html_url"),
                        "reward": "Not specified",
                        "verified": False,
                        "type": issue_type
                    })

        # ✅ REMOVE DUPLICATES
        unique = {}
        for item in results:
            if item["issue_url"]:
                unique[item["issue_url"]] = item

        return list(unique.values())

    except Exception as e:
        print("Error:", e)

        return [{
            "repo": "Error",
            "title": "Something went wrong",
            "description": str(e),
            "issue_url": "#",
            "reward": "N/A",
            "verified": False
        }]