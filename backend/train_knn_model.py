import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neighbors import KNeighborsClassifier
import joblib

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] * 0.6 + df["forks"] * 0.4) > 30000
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Scaling (VERY IMPORTANT for KNN)
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# KNN model (intentionally weak config)
model = KNeighborsClassifier(
    n_neighbors=15   # larger value → lower accuracy
)

model.fit(X_train, y_train)

joblib.dump(model, "knn_model.pkl")
joblib.dump(scaler, "knn_scaler.pkl")

print("✅ KNN model trained!")