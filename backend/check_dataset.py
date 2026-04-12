import pandas as pd

df = pd.read_csv("large_github_dataset.csv")
df["popular"] = (df["popularity_score"] > 100).astype(int)

print(df["popular"].value_counts())