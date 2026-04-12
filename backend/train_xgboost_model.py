import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from xgboost import XGBClassifier
import joblib

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

# Same target logic
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

# Split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# XGBoost model
model = XGBClassifier(
    n_estimators=300,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    use_label_encoder=False,
    eval_metric='logloss'
)

# Train
model.fit(X_train, y_train)

# Save
joblib.dump(model, "xgboost_model.pkl")
joblib.dump(scaler, "xgb_scaler.pkl")

print("✅ XGBoost model trained successfully!")