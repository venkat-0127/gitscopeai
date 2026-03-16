from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import RedirectResponse
from database import db
from utils import generate_otp, send_otp_email
from datetime import datetime, timedelta
import jwt
import requests
import httpx

from config import (
    JWT_SECRET,
    JWT_ALGORITHM,
    JWT_EXPIRE_MINUTES,
    OTP_EXPIRE_MINUTES,
    RECAPTCHA_SECRET,
    GITHUB_CLIENT_ID,
    GITHUB_CLIENT_SECRET,
    FRONTEND_URL
)

from dependencies import get_current_user

router = APIRouter()


# =========================
# SEND OTP (Protected by reCAPTCHA)
# =========================
@router.post("/send-otp")
async def send_otp(data: dict):
    email = data.get("email")
    recaptcha_token = data.get("recaptcha_token")

    if not email:
        raise HTTPException(status_code=400, detail="Email required")

    if not recaptcha_token:
        raise HTTPException(status_code=400, detail="reCAPTCHA required")

    # Verify reCAPTCHA
    recaptcha_response = requests.post(
        "https://www.google.com/recaptcha/api/siteverify",
        data={
            "secret": RECAPTCHA_SECRET,
            "response": recaptcha_token
        }
    )

    result = recaptcha_response.json()

    if not result.get("success"):
        raise HTTPException(status_code=400, detail="reCAPTCHA verification failed")

    otp = generate_otp()

    await db.otp_sessions.delete_many({"email": email})

    await db.otp_sessions.insert_one({
        "email": email,
        "otp": otp,
        "expires_at": datetime.utcnow() + timedelta(minutes=OTP_EXPIRE_MINUTES)
    })

    await send_otp_email(email, otp)

    return {"message": "OTP sent successfully"}


# =========================
# VERIFY OTP
# =========================
@router.post("/verify-otp")
async def verify_otp(data: dict):
    email = data.get("email")
    otp = data.get("otp")

    if not email or not otp:
        raise HTTPException(status_code=400, detail="Email and OTP required")

    record = await db.otp_sessions.find_one({"email": email})

    if not record:
        raise HTTPException(status_code=400, detail="OTP not found")

    if record["otp"] != otp:
        raise HTTPException(status_code=400, detail="Invalid OTP")

    if record["expires_at"] < datetime.utcnow():
        raise HTTPException(status_code=400, detail="OTP expired")

    await db.otp_sessions.delete_many({"email": email})

    await db.users.update_one(
        {"email": email},
        {
            "$set": {"last_login": datetime.utcnow()},
            "$setOnInsert": {"created_at": datetime.utcnow()}
        },
        upsert=True
    )

    token = jwt.encode(
        {
            "email": email,
            "exp": datetime.utcnow() + timedelta(minutes=JWT_EXPIRE_MINUTES)
        },
        JWT_SECRET,
        algorithm=JWT_ALGORITHM
    )

    return {"token": token}


# =========================
# GITHUB LOGIN
# =========================
@router.get("/github/login")
async def github_login():
    github_auth_url = (
        "https://github.com/login/oauth/authorize"
        f"?client_id={GITHUB_CLIENT_ID}"
        "&scope=user:email"
    )
    return RedirectResponse(github_auth_url)


# =========================
# GITHUB CALLBACK
# =========================
@router.get("/github/callback")
async def github_callback(code: str):
    async with httpx.AsyncClient() as client:

        # Exchange code for access token
        token_response = await client.post(
            "https://github.com/login/oauth/access_token",
            headers={"Accept": "application/json"},
            data={
                "client_id": GITHUB_CLIENT_ID,
                "client_secret": GITHUB_CLIENT_SECRET,
                "code": code,
            },
        )

        token_json = token_response.json()
        access_token = token_json.get("access_token")

        if not access_token:
            raise HTTPException(status_code=400, detail="Failed to get access token")

        # Fetch GitHub user profile
        user_response = await client.get(
            "https://api.github.com/user",
            headers={"Authorization": f"Bearer {access_token}"}
        )

        user_data = user_response.json()

    email = user_data.get("email")

    # If email is private, fetch from emails API
    if not email:
        async with httpx.AsyncClient() as client:
            emails_response = await client.get(
                "https://api.github.com/user/emails",
                headers={"Authorization": f"Bearer {access_token}"}
            )
            emails = emails_response.json()
            for e in emails:
                if e.get("primary"):
                    email = e.get("email")
                    break

    if not email:
        raise HTTPException(status_code=400, detail="Email not found from GitHub")

    await db.users.update_one(
        {"email": email},
        {
            "$set": {"last_login": datetime.utcnow()},
            "$setOnInsert": {"created_at": datetime.utcnow()}
        },
        upsert=True
    )

    token = jwt.encode(
        {
            "email": email,
            "exp": datetime.utcnow() + timedelta(minutes=JWT_EXPIRE_MINUTES)
        },
        JWT_SECRET,
        algorithm=JWT_ALGORITHM
    )

    return RedirectResponse(f"{FRONTEND_URL}/oauth-success?token={token}")


# =========================
# PROTECTED ROUTE
# =========================
@router.get("/me")
async def get_me(current_user: str = Depends(get_current_user)):
    return {"email": current_user}