import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import joblib

# 🔥 Load large dataset
df = pd.read_csv("large_github_dataset.csv")

# Features and target
X = df.drop("popularity_score", axis=1)
y = df["popularity_score"]

# Train model
model = RandomForestRegressor(
    n_estimators=200,
    max_depth=15,
    random_state=42
)

model.fit(X, y)

# Save model
joblib.dump(model, "popularity_model.pkl")

print("✅ Model trained on large dataset!")