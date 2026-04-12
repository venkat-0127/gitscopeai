from fastapi import APIRouter, HTTPException
import httpx

router = APIRouter()


# =========================
# GET USER PROFILE
# =========================
@router.get("/profile/{username}")
async def get_profile(username: str):

    try:
        async with httpx.AsyncClient() as client:
            user_res = await client.get(f"https://api.github.com/users/{username}")
            repo_res = await client.get(f"https://api.github.com/users/{username}/repos")

        # ❌ Invalid user
        if user_res.status_code != 200:
            raise HTTPException(status_code=404, detail="GitHub user not found")

        user_data = user_res.json()
        repos_data = repo_res.json()

        # 🔥 FIX: ensure repos_data is list
        if not isinstance(repos_data, list):
            repos_data = []

        return {
            "username": user_data.get("login"),
            "name": user_data.get("name"),
            "avatar": user_data.get("avatar_url"),
            "followers": user_data.get("followers"),
            "following": user_data.get("following"),
            "public_repos": user_data.get("public_repos"),

            "repos": [
                {
                    "name": r.get("name"),
                    "stars": r.get("stargazers_count"),
                    "forks": r.get("forks_count"),
                    "url": r.get("html_url")
                }
                for r in repos_data[:10]  # safe now
            ]
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))