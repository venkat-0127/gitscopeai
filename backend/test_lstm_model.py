import pandas as pd
import numpy as np
import joblib
from sklearn.model_selection import train_test_split
from tensorflow.keras.models import load_model

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Load scaler
scaler = joblib.load("lstm_scaler.pkl")
X = scaler.transform(X)

# Reshape
X = np.reshape(X, (X.shape[0], X.shape[1], 1))

# Split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# Load model
model = load_model("lstm_model.h5")

# Evaluate
loss, accuracy = model.evaluate(X_test, y_test)

print("\n🔥 LSTM Accuracy:", accuracy)