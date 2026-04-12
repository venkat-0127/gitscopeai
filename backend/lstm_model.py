import numpy as np
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense

# Dummy sequence data (you can improve later)
X = np.array([
    [10, 20, 30],
    [50, 60, 70],
    [100, 150, 200]
])

y = np.array([20, 60, 150])

X = X.reshape((X.shape[0], X.shape[1], 1))

model = Sequential()
model.add(LSTM(50, activation='relu', input_shape=(3, 1)))
model.add(Dense(1))

model.compile(optimizer='adam', loss='mse')

model.fit(X, y, epochs=200, verbose=0)

model.save("lstm_model.h5")

print("LSTM model ready!")