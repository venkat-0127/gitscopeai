import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
import joblib

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Scaling (important for LR)
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

model = LogisticRegression(max_iter=1000)

model.fit(X_train, y_train)

joblib.dump(model, "logistic_model.pkl")
joblib.dump(scaler, "lr_scaler.pkl")

print("✅ Logistic Regression trained!")