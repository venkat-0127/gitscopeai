from fastapi import APIRouter, HTTPException
import httpx

router = APIRouter()


# =========================
# GET USER PROFILE
# =========================
@router.get("/profile/{username}")
async def get_profile(username: str):

    async with httpx.AsyncClient() as client:
        user_res = await client.get(f"https://api.github.com/users/{username}")
        repo_res = await client.get(f"https://api.github.com/users/{username}/repos")

    if user_res.status_code != 200:
        raise HTTPException(status_code=404, detail="GitHub user not found")

    user_data = user_res.json()
    repos = repo_res.json()

    return {
        "username": user_data.get("login"),
        "name": user_data.get("name"),
        "avatar": user_data.get("avatar_url"),
        "followers": user_data.get("followers"),
        "following": user_data.get("following"),
        "public_repos": user_data.get("public_repos"),

        "repos": [
            {
                "name": r["name"],
                "stars": r["stargazers_count"],
                "forks": r["forks_count"],
                "url": r["html_url"]
            }
            for r in repos[:10]  # limit to 10
        ]
    }