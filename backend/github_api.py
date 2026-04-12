import requests
import os
from datetime import datetime
from dotenv import load_dotenv

# 🔐 Load .env
load_dotenv()

# 🔑 Get GitHub Token
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

# 🔥 Headers with authentication
headers = {
    "Authorization": f"token {GITHUB_TOKEN}" if GITHUB_TOKEN else None
}


def get_repo_data(repo_url):
    repo_name = repo_url.replace("https://github.com/", "").replace(".git", "")

    # -------------------------------
    # 📦 REPO DETAILS
    # -------------------------------
    repo_api = f"https://api.github.com/repos/{repo_name}"
    repo_res = requests.get(repo_api, headers=headers).json()

    # ❌ Error handling
    if "message" in repo_res:
        raise Exception(repo_res["message"])

    # -------------------------------
    # 📊 COMMITS COUNT (approx)
    # -------------------------------
    commits_api = f"https://api.github.com/repos/{repo_name}/commits?per_page=1"
    commits_res = requests.get(commits_api, headers=headers)

    if "link" in commits_res.headers:
        link = commits_res.headers["link"]
        last_page = link.split(",")[-1]
        commits = int(last_page.split("page=")[-1].split(">")[0])
    else:
        commits = 1

    # -------------------------------
    # 📅 AGE CALCULATION
    # -------------------------------
    created_at = datetime.strptime(repo_res["created_at"], "%Y-%m-%dT%H:%M:%SZ")
    age_days = (datetime.now() - created_at).days

    # -------------------------------
    # ✅ FINAL DATA
    # -------------------------------
    return {
        "stars": repo_res.get("stargazers_count", 0),
        "forks": repo_res.get("forks_count", 0),
        "watchers": repo_res.get("watchers_count", 0),
        "issues": repo_res.get("open_issues_count", 0),
        "age_days": age_days,
        "commits": commits
    }