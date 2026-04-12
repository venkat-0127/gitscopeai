from flask import Flask, render_template, request
import pickle
import pandas as pd

app = Flask(__name__)

# Load trained model
model = pickle.load(open("model.pkl", "rb"))

# Load saved feature columns
feature_columns = pickle.load(open("columns.pkl", "rb"))

@app.route('/')
def home():
    return render_template("home.html")


@app.route('/predict', methods=['POST'])
def predict():

    form_data = request.form.to_dict()
    input_df = pd.DataFrame([form_data])

    # Convert numeric safely
    for col in input_df.columns:
        input_df[col] = pd.to_numeric(input_df[col], errors='coerce')

    # Fill any NaN created
    input_df = input_df.fillna(0)

    # Add missing columns
    for col in feature_columns:
        if col not in input_df.columns:
            input_df[col] = 0

    # Arrange correct order
    input_df = input_df[feature_columns]

    prediction = model.predict(input_df)[0]
    probability = model.predict_proba(input_df)[0][1] * 100

    if prediction == 1:
        result = "Startup Likely To Succeed"
        risk_level = "Low Risk"
    else:
        result = "Startup Likely To Fail"
        risk_level = "High Risk"

    return render_template(
        "result.html",
        outcome=result,
        probability=round(probability, 2),
        risk=risk_level
    )

if __name__ == "__main__":
    app.run(debug=True)
