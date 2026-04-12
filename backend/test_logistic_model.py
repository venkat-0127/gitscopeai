import pandas as pd
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score

df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

scaler = joblib.load("lr_scaler.pkl")
X = scaler.transform(X)

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

model = joblib.load("logistic_model.pkl")

y_pred = model.predict(X_test)

print("\n🔥 Logistic Accuracy:", accuracy_score(y_test, y_pred))

scores = cross_val_score(model, X, y, cv=5)
print("\n📊 Cross-validation Accuracy:", scores.mean())