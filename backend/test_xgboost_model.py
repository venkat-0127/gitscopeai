import pandas as pd
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score, classification_report

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Load scaler
scaler = joblib.load("xgb_scaler.pkl")
X = scaler.transform(X)

# Split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# Load model
model = joblib.load("xgboost_model.pkl")

# Predict
y_pred = model.predict(X_test)

# Accuracy
print("\n🔥 XGBoost Accuracy:", accuracy_score(y_test, y_pred))

print("\n📋 Classification Report:")
print(classification_report(y_test, y_pred))

# Cross-validation
scores = cross_val_score(model, X, y, cv=5)
print("\n📊 Cross-validation Accuracy:", scores.mean())