import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
import joblib

# Load dataset
df = pd.read_csv("large_github_dataset.csv")

# Target (same as others)
df["popular"] = (
    (df["stars"] > 50000) | 
    (df["forks"] > 10000)
).astype(int)

# Features
X = df.drop(["popular", "popularity_score"], axis=1)
y = df["popular"]

# Scale
scaler = StandardScaler()
X = scaler.fit_transform(X)

# 👉 LSTM expects 3D input → reshape
X = np.reshape(X, (X.shape[0], X.shape[1], 1))

# Split
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# Build model
model = Sequential()
model.add(LSTM(50, input_shape=(X.shape[1], 1)))
model.add(Dense(1, activation='sigmoid'))

model.compile(
    loss='binary_crossentropy',
    optimizer='adam',
    metrics=['accuracy']
)

# Train
model.fit(X_train, y_train, epochs=5, batch_size=32)

# Save
model.save("lstm_model.h5")
joblib.dump(scaler, "lstm_scaler.pkl")

print("✅ LSTM model trained!")