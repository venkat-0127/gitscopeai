import numpy as np
import requests


# -------------------------------
# ✅ SAFE LOG FUNCTION (IMPORTANT)
# -------------------------------
def safe_log(x):
    try:
        if x <= 0 or np.isnan(x) or np.isinf(x):
            return 0
        return np.log1p(x)
    except:
        return 0


# 🔥 Issue-based bug detection
def get_bug_issue_count(repo_name, token=None):
    url = f"https://api.github.com/repos/{repo_name}/issues?state=all&per_page=100"

    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    try:
        res = requests.get(url, headers=headers)
        issues = res.json()

        bug_count = 0
        total_issues = 0

        for issue in issues:
            if "pull_request" in issue:
                continue

            total_issues += 1

            title = issue.get("title", "").lower()
            labels = [l["name"].lower() for l in issue.get("labels", [])]

            if (
                "bug" in title
                or "error" in title
                or "fix" in title
                or any("bug" in label for label in labels)
            ):
                bug_count += 1

        return bug_count, total_issues

    except:
        return 0, 0


# 🔥 MAIN FUNCTION
def predict_bug_risk(
    stars, forks, watchers, issues, age_days, commits, repo_name=None, token=None
):

    # -------------------------------
    # ✅ SAFE VALUES
    # -------------------------------
    commits = max(commits, 1)
    stars_forks = max(stars + forks, 1)
    age_days = max(age_days, 1)

    # -------------------------------
    # 🚨 LOW DATA HANDLING
    # -------------------------------
    if stars == 0 and forks == 0 and commits < 20:
        return {
            "score": 30,
            "level": "Medium ⚠️",
            "health_score": 40,
            "health_level": "Average 🟡",
            "metrics": {
                "issue_density": 0,
                "issue_ratio": 0,
                "activity": 0,
                "bug_ratio": 0
            },
            "suggestions": [
                "Repository has very little data",
                "Project may be incomplete or inactive",
                "Cannot accurately assess risk"
            ]
        }

    # -------------------------------
    # 📊 BASIC METRICS
    # -------------------------------
    issue_density = issues / commits
    issue_ratio = issues / stars_forks
    activity = commits / age_days

    # -------------------------------
    # 🐛 BUG DETECTION
    # -------------------------------
    bug_ratio = 0
    bug_count = 0
    total_issues = 0

    if repo_name:
        bug_count, total_issues = get_bug_issue_count(repo_name, token)
        if total_issues > 0:
            bug_ratio = bug_count / total_issues

    # -------------------------------
    # 🔥 BUG RISK SCORE (SAFE)
    # -------------------------------
    risk_score = (
        safe_log(issue_density) * 30 +
        safe_log(issue_ratio) * 20 +
        safe_log(bug_ratio) * 30 -
        safe_log(activity) * 15
    )

    # extra transform safely
    risk_score = safe_log(risk_score + 1) * 20

    # final clamp
    if np.isnan(risk_score) or np.isinf(risk_score):
        risk_score = 0

    risk_score = min(max(risk_score, 0), 100)

    # -------------------------------
    # 🎯 RISK LEVEL
    # -------------------------------
    if risk_score < 35:
        risk_level = "Low 🟢"
    elif risk_score < 70:
        risk_level = "Medium 🟡"
    else:
        risk_level = "High 🔴"

    # -------------------------------
    # 📊 ACTIVITY SCORE
    # -------------------------------
    activity_score = safe_log(activity) * 20
    activity_score = min(activity_score, 100)

    # -------------------------------
    # ⭐ POPULARITY SCORE
    # -------------------------------
    popularity_score = (
        safe_log(stars) * 4 +
        safe_log(forks) * 3 +
        safe_log(watchers) * 2
    )

    popularity_score = min(popularity_score, 100)

    # -------------------------------
    # 💚 HEALTH SCORE
    # -------------------------------
    health_score = (
        (popularity_score * 0.5) +
        ((100 - risk_score) * 0.3) +
        (activity_score * 0.2)
    )

    if np.isnan(health_score) or np.isinf(health_score):
        health_score = 0

    health_score = min(max(health_score, 0), 100)

    if health_score < 40:
        health_level = "Poor 🔴"
    elif health_score < 70:
        health_level = "Average 🟡"
    else:
        health_level = "Excellent 🟢"

    # -------------------------------
    # 💡 SUGGESTIONS
    # -------------------------------
    suggestions = []

    if issues == 0:
        suggestions.append("No issues reported — may lack testing")

    if issues > 50:
        suggestions.append("Too many issues — needs maintenance")

    if issue_density > 0.5:
        suggestions.append("High issue density — unstable code")

    if activity < 0.5:
        suggestions.append("Low development activity")

    if commits < 50:
        suggestions.append("Very few commits — immature project")

    if bug_ratio > 0.3:
        suggestions.append("Many issues are bug-related")

    if not suggestions:
        suggestions.append("Repository looks healthy")

    # -------------------------------
    # 📦 FINAL RETURN (JSON SAFE)
    # -------------------------------
    return {
        "score": round(float(risk_score), 2),
        "level": risk_level,
        "health_score": round(float(health_score), 2),
        "health_level": health_level,
        "metrics": {
            "issue_density": float(round(issue_density, 3)),
            "issue_ratio": float(round(issue_ratio, 3)),
            "activity": float(round(activity, 3)),
            "bug_ratio": float(round(bug_ratio, 3)),
            "bug_count": int(bug_count),
            "total_issues": int(total_issues)
        },
        "suggestions": suggestions
    }