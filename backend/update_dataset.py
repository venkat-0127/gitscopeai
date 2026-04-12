import pandas as pd

df = pd.read_csv("github_dataset.csv")

# Create 'popular' column using stars & forks
df["popular"] = ((df["stars"] > 50000) & (df["forks"] > 10000)).astype(int)

# Save dataset
df.to_csv("github_dataset.csv", index=False)

print("✅ 'popular' column created using stars & forks!")
print(df.head())