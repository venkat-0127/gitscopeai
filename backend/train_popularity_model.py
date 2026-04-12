import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
import joblib

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

# Create target column
df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

# Features & target
X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Scaling
scaler = StandardScaler()
X = scaler.fit_transform(X)

# ✅ FIX: Store split values
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# Model
model = RandomForestClassifier(
    n_estimators=300,
    max_depth=15,
    class_weight='balanced',
    random_state=42
)

# Train
model.fit(X_train, y_train)

# Save model + scaler
joblib.dump(model, "popularity_model.pkl")
joblib.dump(scaler, "scaler.pkl")

print("✅ Improved model trained and saved successfully!")