from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware

from auth import router as auth_router
import github_profile

import shutil
import os
import subprocess
import zipfile

from code_analyzer import analyze_code

from github_api import get_repo_data
from predict_popularity import predict_popularity
from bug_risk import predict_bug_risk
from bounty_finder import fetch_all_bounties

app = FastAPI(title="GitScope AI Backend")

# =========================
# CORS
# =========================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =========================
# ROUTES
# =========================
app.include_router(auth_router, prefix="/auth")
app.include_router(github_profile.router, prefix="/github")

# =========================
# ROOT
# =========================
@app.get("/")
def root():
    return {"message": "Backend running 🚀"}


# =========================
# LANGUAGE DETECTION
# =========================
def detect_language(folder):
    for root, dirs, files in os.walk(folder):
        for file in files:
            if file.endswith(".py"):
                return "python"
            if file.endswith(".java"):
                return "java"
    return "unknown"


# =========================
# EXTRACT ZIP
# =========================
def extract_zip(file_path, extract_to):
    with zipfile.ZipFile(file_path, 'r') as zip_ref:
        zip_ref.extractall(extract_to)


# =========================
# CLEANUP
# =========================
def cleanup(path):
    if os.path.exists(path):
        if os.path.isfile(path):
            os.remove(path)
        else:
            shutil.rmtree(path)


# =========================
# ERROR EXPLANATION
# =========================
def explain_error(msg):
    msg = msg.lower()

    if "';' expected" in msg:
        return "Missing semicolon (;)"
    if "cannot find symbol" in msg:
        return "Variable or method not defined"
    if "class not found" in msg:
        return "Class not found"
    if "incompatible types" in msg:
        return "Type mismatch error"

    return msg


# =========================
# ANALYZE CODE API
# =========================
@app.post("/analyze-code")
async def analyze_code_api(file: UploadFile = File(...)):
    file_path = f"temp_{file.filename}"
    extract_folder = "temp_code"

    cleanup(file_path)
    cleanup(extract_folder)

    # save file
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # extract or copy
    if file.filename.endswith(".zip"):
        extract_zip(file_path, extract_folder)
    else:
        os.makedirs(extract_folder, exist_ok=True)
        shutil.copy(file_path, os.path.join(extract_folder, file.filename))

    language = detect_language(extract_folder)

    # =========================
    # PYTHON
    # =========================
    if language == "python":
        result = analyze_code(extract_folder)
        return {
            "language": "python",
            "result": result
        }

    # =========================
    # JAVA (FINAL VERSION 🔥)
    # =========================
    elif language == "java":
        try:
            java_files = []

            for root, dirs, files in os.walk(extract_folder):
                for file in files:
                    if file.endswith(".java"):
                        java_files.append(os.path.join(root, file))

            if not java_files:
                return {
                    "language": "java",
                    "output": "❌ No Java files found"
                }

            final_output = ""
            total_errors = 0

            for java_file in java_files:
                file_block = f"\n📄 {os.path.basename(java_file)}\n"

                # =========================
                # 1. COMPILATION (JAVAC)
                # =========================
                compile_result = subprocess.run(
                    ["javac", java_file],
                    capture_output=True,
                    text=True
                )

                raw = compile_result.stderr.strip()

                compile_lines = []
                for line in raw.split("\n"):
                    if ".java:" in line:
                        parts = line.split(":")
                        if len(parts) >= 4:
                            line_no = parts[1]
                            msg = parts[3].strip()
                            readable = explain_error(msg)

                            compile_lines.append(f"❌ Line {line_no} → {readable}")
                            total_errors += 1

                if compile_lines:
                    file_block += "\n".join(compile_lines) + "\n"

                # =========================
                # 2. CHECKSTYLE (QUALITY)
                # =========================
                try:
                    checkstyle_result = subprocess.run(
                        [
                            "java",
                            "-jar",
                            os.path.join(os.getcwd(), "checkstyle.jar"),
                            "-c",
                            os.path.join(os.getcwd(), "google_checks.xml"),
                            java_file
                        ],
                        capture_output=True,
                        text=True,
                        timeout=5
                    )

                    style_output = checkstyle_result.stdout

                    style_lines = []
                    for line in style_output.split("\n"):
                        if ".java:" in line:
                            style_lines.append("⚠️ " + line)

                    if style_lines:
                        file_block += "\n".join(style_lines) + "\n"

                except:
                    file_block += "⚠️ Checkstyle not configured properly\n"

                final_output += file_block

            score = max(0, 10 - total_errors)

            if not final_output.strip():
                final_output = "✅ No issues found (Clean Code)"

            return {
                "language": "java",
                "score": score,
                "output": final_output
            }

        except Exception as e:
            return {
                "error": f"Java analysis failed: {str(e)}"
            }

    # =========================
    # UNKNOWN
    # =========================
    else:
        return {
            "error": "Unsupported project type (Only Python & Java supported)"
        }
# =========================
# POPULARITY PREDICTION API
# =========================
@app.post("/predict-popularity")
async def predict_popularity_api(data: dict):
    try:
        repo_url = data.get("repo")

        if not repo_url:
            return {"error": "Repository URL is required"}

        repo_data = get_repo_data(repo_url)

        score, level = predict_popularity(
            repo_data["stars"],
            repo_data["forks"],
            repo_data["watchers"],
            repo_data["issues"],
            repo_data["age_days"],
            repo_data["commits"]
        )

        return {
            "repo": repo_url,
            "score": score,
            "level": level,
            "data": repo_data
        }

    except Exception as e:
        return {"error": str(e)}


# =========================
# TREND API
# =========================
@app.post("/repo-trend")
def repo_trend(data: dict):
    repo_url = data["repo"]

    repo = get_repo_data(repo_url)

    trend = [
        repo["stars"] * 0.2,
        repo["stars"] * 0.4,
        repo["stars"] * 0.6,
        repo["stars"] * 0.8,
        repo["stars"]
    ]

    return {
        "trend": trend
    }


# =========================
# RECOMMENDATION API
# =========================
@app.post("/recommend")
def recommend(data: dict):
    repo_url = data["repo"]

    repo = get_repo_data(repo_url)

    stars = repo["stars"]

    if stars > 100000:
        recs = [
            "facebook/react",
            "microsoft/vscode",
            "tensorflow/tensorflow"
        ]
    elif stars > 10000:
        recs = [
            "pallets/flask",
            "fastapi/fastapi",
            "django/django"
        ]
    else:
        recs = [
            "small beginner repos",
            "student projects",
            "open issues repos"
        ]

    return {"recommendations": recs}
# =========================
# BUG RISK API
# =========================
@app.post("/predict-bug-risk")
def bug_risk_api(data: dict):
    repo_url = data["repo"]

    repo = get_repo_data(repo_url)

    repo_name = repo_url.replace("https://github.com/", "").replace(".git", "")

    result = predict_bug_risk(
        repo["stars"],
        repo["forks"],
        repo["watchers"],
        repo["issues"],
        repo["age_days"],
        repo["commits"],
        repo_name=repo_name
    )

    return {
        "repo": repo_url,
        "risk_score": result["score"],
        "risk_level": result["level"],
        "health_score": result["health_score"],
        "health_level": result["health_level"],
        "metrics": result["metrics"],
        "suggestions": result["suggestions"]
    }
@app.get("/bounties")
def get_bounties():
    return fetch_all_bounties()
