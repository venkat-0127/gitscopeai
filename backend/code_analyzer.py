import subprocess
import os


def analyze_code(folder_path):
    # =========================
    # VALIDATE FOLDER
    # =========================
    if not os.path.exists(folder_path):
        return {
            "score": 0,
            "pylint_output": "❌ Folder not found",
            "flake8_output": ""
        }

    # =========================
    # RUN PYLINT
    # =========================
    pylint_result = subprocess.run(
        [
            "python",
            "-m",
            "pylint",
            folder_path,
            "--ignore=venv,__pycache__",
            "--disable=E0401"  # ignore import errors
        ],
        capture_output=True,
        text=True
    )

    pylint_output = pylint_result.stdout.strip()

    # =========================
    # EXTRACT SCORE
    # =========================
    score = 0

    for line in pylint_output.split("\n"):
        if "rated at" in line:
            try:
                score = float(line.split("rated at")[1].split("/")[0])
            except:
                score = 0

    # =========================
    # RUN FLAKE8
    # =========================
    flake_result = subprocess.run(
        [
            "python",
            "-m",
            "flake8",
            folder_path,
            "--exclude=venv,__pycache__"
        ],
        capture_output=True,
        text=True
    )

    flake_output = flake_result.stdout.strip()

    # =========================
    # HANDLE EMPTY OUTPUTS
    # =========================
    if not pylint_output:
        pylint_output = "✅ No major pylint issues found"

    if not flake_output:
        flake_output = "✅ No flake8 issues found"

    # =========================
    # FINAL RESPONSE
    # =========================
    return {
        "score": score,
        "pylint_output": pylint_output,
        "flake8_output": flake_output
    }