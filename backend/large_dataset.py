import requests
import pandas as pd
from datetime import datetime
import time
import os
from dotenv import load_dotenv

load_dotenv()
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

headers = {
    "Authorization": f"token {GITHUB_TOKEN}"
}

data = []

# 🔥 MULTIPLE QUERIES
queries = [
    "stars:10..50",
    "stars:50..100",
    "stars:100..500",
    "stars:500..1000",
    "stars:1000..5000",
    "stars:>5000"
]

for query in queries:
    print(f"\n🔍 Processing query: {query}")

    for page in range(1, 11):  # 10 pages = 1000 per query
        print(f"Fetching page {page}...")

        url = f"https://api.github.com/search/repositories?q={query}&sort=stars&per_page=100&page={page}"
        response = requests.get(url, headers=headers)

        if response.status_code != 200:
            print("Error:", response.json())
            break

        repos = response.json().get("items", [])

        for repo in repos:
            try:
                created_at = datetime.strptime(repo["created_at"], "%Y-%m-%dT%H:%M:%SZ")
                age_days = (datetime.now() - created_at).days

                data.append({
                    "stars": repo["stargazers_count"],
                    "forks": repo["forks_count"],
                    "watchers": repo["watchers_count"],
                    "issues": repo["open_issues_count"],
                    "age_days": age_days,
                    "commits": 100,
                    "popularity_score": (
                        repo["stargazers_count"] * 0.6 +
                        repo["forks_count"] * 0.2 +
                        repo["watchers_count"] * 0.1 -
                        repo["open_issues_count"] * 0.1
                    )
                })

            except Exception as e:
                print("Skipping:", e)

        time.sleep(1)

df = pd.DataFrame(data)
df = df.drop_duplicates()

df.to_csv("large_github_dataset.csv", index=False)

print(f"\n✅ FINAL DATASET SIZE: {len(df)}")