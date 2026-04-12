from github_api import get_repo_data
from predict_popularity import predict_popularity

import pandas as pd
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score, classification_report


# ==============================
# 🔹 PART 1: SINGLE REPO TEST
# ==============================

repo = "https://github.com/tensorflow/tensorflow"

data = get_repo_data(repo)

score, level = predict_popularity(
    data["stars"],
    data["forks"],
    data["watchers"],
    data["issues"],
    data["age_days"],
    data["commits"]
)

print("\n==============================")
print("📊 SINGLE REPO PREDICTION")
print("==============================")
print("📊 Popularity Score:", score)
print("🚀 Level:", level)


# ==============================
# 🔹 PART 2: MODEL EVALUATION
# ==============================

print("\n==============================")
print("📈 MODEL EVALUATION")
print("==============================")

try:
    # Load dataset
    df = pd.read_csv("large_github_dataset.csv")

    # Same logic as training
    df["popular"] = (
        (df["stars"] > 50000) | 
        (df["forks"] > 10000)
    ).astype(int)

    # Features & target
    X = df.drop(["popular", "popularity_score"], axis=1)
    y = df["popular"]

    # Load scaler
    scaler = joblib.load("scaler.pkl")
    X = scaler.transform(X)

    # ✅ FIX: Store split values
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # Load model
    model = joblib.load("popularity_model.pkl")

    # Predict
    y_pred = model.predict(X_test)

    # Accuracy
    acc = accuracy_score(y_test, y_pred)
    print(f"\n🔥 Accuracy: {acc:.4f}")

    # Report
    print("\n📋 Classification Report:")
    print(classification_report(y_test, y_pred))

    # ✅ Cross-validation (after model & data ready)
    scores = cross_val_score(model, X, y, cv=5)
    print("\n📊 Cross-validation Accuracy:", scores.mean())

except Exception as e:
    print("⚠️ Error while calculating accuracy:", str(e))