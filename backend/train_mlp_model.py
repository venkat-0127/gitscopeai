import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
import joblib

df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

model = MLPClassifier(hidden_layer_sizes=(64, 32), max_iter=300)

model.fit(X_train, y_train)

joblib.dump(model, "mlp_model.pkl")
joblib.dump(scaler, "mlp_scaler.pkl")

print("✅ MLP model trained!")