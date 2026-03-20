import subprocess
import sys

print("🔍 Running Advanced Code Quality Checks...")

TARGET_FOLDER = "backend"

# ---------------------------
# RUN PYLINT (FAST + CLEAN)
# ---------------------------
print("\n➡️ Running Pylint...")

try:
    pylint_result = subprocess.run(
    [
        "python", "-m", "pylint", TARGET_FOLDER,
        "--ignore=venv,__pycache__",
        "--disable=C0114,C0115,C0116,C0304,C0411,E0401,W0613"
    ],
    capture_output=True,
    text=True
    )
except Exception as e:
    print("❌ Error running pylint:", e)
    sys.exit(1)

print(pylint_result.stdout)

# ---------------------------
# EXTRACT SCORE
# ---------------------------
score = 0.0

for line in pylint_result.stdout.split("\n"):
    if "rated at" in line:
        try:
            score_part = line.split("rated at")[1].strip()
            score = float(score_part.split("/")[0])
        except:
            score = 0.0

print(f"\n📊 Pylint Score: {score}/10")

# ---------------------------
# RUN FLAKE8 (STYLE CHECK)
# ---------------------------
print("\n➡️ Running Flake8...")

try:
    flake_result = subprocess.run(
        ["python", "-m", "flake8", TARGET_FOLDER, "--exclude=venv,__pycache__"],
        capture_output=True,
        text=True
    )
except Exception as e:
    print("❌ Error running flake8:", e)
    sys.exit(1)

print(flake_result.stdout)

# ---------------------------
# DECISION LOGIC
# ---------------------------

# ❌ Block only for serious issues
if score < 4:
    print("\n❌ Poor code quality (score < 4). Commit blocked.")
    sys.exit(1)

# ⚠️ Style issues → allow
if flake_result.stdout.strip():
    print("\n⚠️ Style issues found (not blocking commit):")
    print(flake_result.stdout)

# ✅ Success
print("\n✅ Code quality passed. Commit allowed.")
sys.exit(0)