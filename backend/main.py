from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from auth import router as auth_router
import github_profile  # ✅ renamed file

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