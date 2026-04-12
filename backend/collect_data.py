import pandas as pd
from github_api import get_repo_data

repos = [
    "tensorflow/tensorflow",
    "facebook/react",
    "microsoft/vscode",
    "pytorch/pytorch",
    "keras-team/keras",
    "numpy/numpy",
    "django/django",
    "spring-projects/spring-boot",
    "angular/angular",
    "flutter/flutter"
]

data_list = []

for repo in repos:
    url = f"https://github.com/{repo}"
    data = get_repo_data(url)
    data_list.append(data)

df = pd.DataFrame(data_list)

df.to_csv("github_dataset.csv", index=False)

print("✅ Dataset created with", len(df), "rows")