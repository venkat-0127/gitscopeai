import pandas as pd
import numpy as np
import pickle

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

# -----------------------------
# 1. Load Dataset
# -----------------------------
df = pd.read_csv("startup.csv")

# Keep only acquired and closed
df = df[df['status'].isin(['acquired', 'closed'])].copy()

# Create target
df['target'] = df['status'].map({'acquired': 1, 'closed': 0})

# Drop unnecessary columns
df.drop(['Unnamed: 0', 'id', 'name', 'closed_at'], axis=1, inplace=True, errors='ignore')

# -----------------------------
# 2. Convert Dates to Year
# -----------------------------
date_cols = ['founded_at', 'first_funding_at', 'last_funding_at']

for col in date_cols:
    if col in df.columns:
        df[col] = pd.to_datetime(df[col], errors='coerce')
        df[col + "_year"] = df[col].dt.year
        df.drop(columns=[col], inplace=True)

# -----------------------------
# 3. Split Features & Target
# -----------------------------
X = df.drop(['status', 'target'], axis=1)
y = df['target']

# Save feature columns for Flask
feature_columns = X.columns.tolist()
pickle.dump(feature_columns, open("columns.pkl", "wb"))

# Identify column types
numeric_features = X.select_dtypes(include=['int64', 'float64']).columns
categorical_features = X.select_dtypes(include=['object']).columns

# -----------------------------
# 4. Preprocessing Pipelines
# -----------------------------

numeric_transformer = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="median"))
])

categorical_transformer = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="constant", fill_value="Unknown")),
    ("onehot", OneHotEncoder(handle_unknown="ignore"))
])

preprocessor = ColumnTransformer(
    transformers=[
        ("num", numeric_transformer, numeric_features),
        ("cat", categorical_transformer, categorical_features)
    ]
)

# -----------------------------
# 5. Full Model Pipeline
# -----------------------------
model = Pipeline(steps=[
    ("preprocessor", preprocessor),
    ("classifier", RandomForestClassifier(
        n_estimators=300,
        random_state=42
    ))
])

# -----------------------------
# 6. Train Test Split
# -----------------------------
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# -----------------------------
# 7. Train
# -----------------------------
model.fit(X_train, y_train)

# -----------------------------
# 8. Evaluate
# -----------------------------
y_pred = model.predict(X_test)

print("\nModel Evaluation")
print("----------------------------")
print("Accuracy:", accuracy_score(y_test, y_pred))
print("\nClassification Report:\n")
print(classification_report(y_test, y_pred))
print("\nConfusion Matrix:\n")
print(confusion_matrix(y_test, y_pred))

# -----------------------------
# 9. Save Model
# -----------------------------
pickle.dump(model, open("model.pkl", "wb"))

print("\nModel and columns saved successfully!")
