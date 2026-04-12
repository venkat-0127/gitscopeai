import React, { useEffect, useState } from "react";

function Profile() {
  const [profile, setProfile] = useState(null);
  const [repos, setRepos] = useState([]);
  const [showPanel, setShowPanel] = useState(false);
  const [view, setView] = useState("repos");

  useEffect(() => {
    const loadUser = async () => {
      try {
        // ✅ First try localStorage (OTP login)
        const storedUser = JSON.parse(localStorage.getItem("user"));

        if (storedUser && storedUser.github_username) {
          const res = await fetch(
            `https://api.github.com/users/${storedUser.github_username}`
          );
          const data = await res.json();

          setProfile(data);

          const repoRes = await fetch(data.repos_url);
          const repoData = await repoRes.json();
          setRepos(repoData.slice(0, 10));

          return;
        }

        // ✅ Otherwise OAuth user
        const res = await fetch("http://localhost:8000/auth/github/user");
        const data = await res.json();

        setProfile(data);

        const repoRes = await fetch(data.repos_url);
        const repoData = await repoRes.json();
        setRepos(repoData.slice(0, 10));

      } catch (err) {
        console.error(err);
        alert("Failed to load profile");
      }
    };

    loadUser();
  }, []);

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/";
  };

  if (!profile) return <h2 style={{ padding: "20px" }}>Loading...</h2>;

  const history = JSON.parse(localStorage.getItem("history")) || [];

  return (
    <div style={{ background: "#f1f5f9", minHeight: "100vh" }}>

      {/* PROFILE ICON */}
      <div style={{ display: "flex", justifyContent: "flex-end", padding: "20px" }}>
        <img
          src={profile.avatar_url}
          width="50"
          style={{ borderRadius: "50%", cursor: "pointer", border: "2px solid black" }}
          onClick={() => setShowPanel(!showPanel)}
        />
      </div>

      {/* PANEL */}
      {showPanel && (
        <div style={{
          position: "absolute",
          top: "80px",
          right: "20px",
          width: "350px",
          background: "white",
          borderRadius: "10px",
          padding: "15px",
          boxShadow: "0 0 10px rgba(0,0,0,0.2)"
        }}>

          <div style={{ textAlign: "center" }}>
            <img src={profile.avatar_url} width="70" style={{ borderRadius: "50%" }} />
            <h3>{profile.name || "No Name"}</h3>
            <p>@{profile.login}</p>
          </div>

          {/* SWITCH */}
          <div style={{ display: "flex", gap: "10px", margin: "10px 0" }}>
            <button onClick={() => setView("repos")}>Repos</button>
            <button onClick={() => setView("history")}>History</button>
          </div>

          {/* REPOS */}
          {view === "repos" &&
            repos.map((repo, i) => (
              <div key={i} style={{ borderBottom: "1px solid #ddd", padding: "10px 0" }}>
                <strong>{repo.name}</strong>
                <div>⭐ {repo.stargazers_count} | 🍴 {repo.forks_count}</div>
                <a href={repo.html_url} target="_blank">View →</a>
              </div>
            ))}

          {/* HISTORY */}
          {view === "history" &&
            history.map((item, i) => (
              <div key={i} style={{ borderBottom: "1px solid #ddd", padding: "10px 0" }}>
                <strong>{item.type}</strong>
                <div>Score: {item.score}</div>
                <div style={{ fontSize: "12px", color: "gray" }}>{item.date}</div>
              </div>
            ))}

          <button
            onClick={handleLogout}
            style={{
              marginTop: "10px",
              width: "100%",
              background: "black",
              color: "white",
              padding: "10px"
            }}
          >
            Logout
          </button>

        </div>
      )}

      <div style={{ padding: "20px" }}>
        <h1>Welcome to GitScope AI 🚀</h1>
      </div>
    </div>
  );
}

export default Profile;