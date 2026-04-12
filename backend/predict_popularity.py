import joblib
import pandas as pd
import numpy as np

model = joblib.load("popularity_model.pkl")

def predict_popularity(stars, forks, watchers, issues, age_days, commits):
    data = pd.DataFrame([{
        "stars": stars,
        "forks": forks,
        "watchers": watchers,
        "issues": issues,
        "age_days": age_days,
        "commits": commits
    }])

    # 🔹 ML prediction
    ml_score = model.predict(data)[0]

    # 🔹 Feature-based scoring (log scaling)
    feature_score = (
        np.log1p(stars) * 4 +
        np.log1p(forks) * 3 +
        np.log1p(watchers) * 2 +
        np.log1p(commits) * 1
    )

    # 🔹 Combine ML + Feature
    score = (ml_score * 0.4) + (feature_score * 0.6)

    # 🔥 FIXED NORMALIZATION (IMPORTANT)
    score = np.log1p(score) * 15

    # 🔹 Clamp to 0–100
    score = min(max(score, 0), 100)

    # 🔹 Optional adjustment for low-activity repos
    if stars < 50 and forks < 20:
        score *= 0.7

    # 🔹 Level classification
    if score < 40:
        level = "Low 🧊"
    elif score < 70:
        level = "Medium ⚡"
    else:
        level = "High 🔥"

    return round(score, 2), level