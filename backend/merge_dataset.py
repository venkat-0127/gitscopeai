import pandas as pd

files = [
    "dataset1.csv",
    "dataset2.csv",
    "dataset3.csv"
]

df_list = [pd.read_csv(file) for file in files]

df = pd.concat(df_list)

# 🔥 Remove duplicates
df = df.drop_duplicates()

df.to_csv("final_dataset.csv", index=False)

print("✅ Final dataset ready:", len(df))