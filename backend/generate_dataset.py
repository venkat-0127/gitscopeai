import requests
import pandas as pd
from datetime import datetime
import time

repos = [
    "tensorflow/tensorflow",
    "facebook/react",
    "torvalds/linux",
    "microsoft/vscode",
    "keras-team/keras",
    "pallets/flask",
    "django/django",
    "scikit-learn/scikit-learn"
]

data_list = []

for repo in repos:
    url = f"https://api.github.com/repos/{repo}"
    res = requests.get(url).json()

    if "message" in res:
        continue

    created_at = datetime.strptime(res["created_at"], "%Y-%m-%dT%H:%M:%SZ")
    age_days = (datetime.now() - created_at).days

    data_list.append({
        "stars": res["stargazers_count"],
        "forks": res["forks_count"],
        "watchers": res["watchers_count"],
        "issues": res["open_issues_count"],
        "age_days": age_days,
        "commits": 100,  # keep for now
        "popularity_score": res["stargazers_count"] / 1000
    })

    time.sleep(1)  # avoid rate limit

df = pd.DataFrame(data_list)
df.to_csv("github_dataset.csv", index=False)

print("Dataset created!")