import pandas as pd
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score, classification_report

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

# Same target logic (VERY IMPORTANT)
df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

# Features & target
X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Load scaler
scaler = joblib.load("mlp_scaler.pkl")
X = scaler.transform(X)

# Train-test split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# Load model
model = joblib.load("mlp_model.pkl")

# Predict
y_pred = model.predict(X_test)

# Accuracy
acc = accuracy_score(y_test, y_pred)
print("\n🔥 MLP Accuracy:", acc)

# Classification report
print("\n📋 Classification Report:")
print(classification_report(y_test, y_pred))

# Cross-validation
scores = cross_val_score(model, X, y, cv=5)
print("\n📊 Cross-validation Accuracy:", scores.mean())