import numpy as np


def calculate_repo_health(popularity_score, bug_risk_score, commits, age_days):

    # -------------------------------
    # 📊 Activity Score
    # -------------------------------
    activity = commits / max(age_days, 1)
    activity_score = np.log1p(activity) * 20
    activity_score = min(activity_score, 100)

    # -------------------------------
    # ⚖️ Combine Scores
    # -------------------------------
    # Higher popularity → good
    # Higher bug risk → bad

    health_score = (
        (popularity_score * 0.5) +          # 50% weight
        ((100 - bug_risk_score) * 0.3) +    # 30% weight (inverse)
        (activity_score * 0.2)              # 20% weight
    )

    health_score = min(max(health_score, 0), 100)

    # -------------------------------
    # 🎯 Level
    # -------------------------------
    if health_score < 40:
        level = "Poor 🔴"
    elif health_score < 70:
        level = "Average 🟡"
    else:
        level = "Excellent 🟢"

    # -------------------------------
    # 💡 Suggestions
    # -------------------------------
    suggestions = []

    if popularity_score < 40:
        suggestions.append("Low popularity — improve visibility")

    if bug_risk_score > 60:
        suggestions.append("High bug risk — fix issues")

    if activity_score < 10:
        suggestions.append("Low development activity")

    if not suggestions:
        suggestions.append("Repository is in great condition")

    return {
        "health_score": round(health_score, 2),
        "level": level,
        "activity_score": round(activity_score, 2),
        "suggestions": suggestions
    }