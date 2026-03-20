import React, { useEffect, useState } from "react";

function Profile() {
  const [profile, setProfile] = useState(null);
  const [showPanel, setShowPanel] = useState(false);
  const [githubInput, setGithubInput] = useState("");
  const [user, setUser] = useState(null);

  // =========================
  // LOAD USER + PROFILE
  // =========================
  useEffect(() => {
    const storedUser = JSON.parse(localStorage.getItem("user"));

    if (!storedUser) {
      alert("Please login first");
      window.location.href = "/";
      return;
    }

    setUser(storedUser);

    if (storedUser.github_username) {
      loadProfile(storedUser.github_username);
    }
  }, []);

  // =========================
  // FETCH PROFILE
  // =========================
  const loadProfile = async (username) => {
    try {
      const res = await fetch(
        `http://localhost:8000/github/profile/${username}`
      );
      const data = await res.json();
      setProfile(data);
    } catch (err) {
      console.error(err);
      alert("Failed to load profile");
    }
  };

  // =========================
  // SAVE GITHUB USERNAME
  // =========================
  const saveGitHub = async () => {
    if (!githubInput) {
      alert("Enter GitHub username");
      return;
    }

    try {
      await fetch("http://localhost:8000/auth/save-github", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          email: user.email,
          github_username: githubInput
        })
      });

      const updatedUser = {
        ...user,
        github_username: githubInput
      };

      localStorage.setItem("user", JSON.stringify(updatedUser));
      setUser(updatedUser);

      loadProfile(githubInput);

    } catch (err) {
      console.error(err);
      alert("Failed to save GitHub username");
    }
  };

  // =========================
  // LOGOUT FUNCTION
  // =========================
  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/";
  };

  // =========================
  // NEW USER (NO GITHUB USERNAME)
  // =========================
  if (user && !user.github_username) {
    return (
      <div style={{ padding: "20px", color: "white", background: "#0f172a", minHeight: "100vh" }}>
        <h2>Enter Your GitHub Username</h2>

        <input
          type="text"
          placeholder="GitHub Username"
          value={githubInput}
          onChange={(e) => setGithubInput(e.target.value)}
          style={{
            padding: "10px",
            marginRight: "10px"
          }}
        />

        <button onClick={saveGitHub}>Continue</button>
      </div>
    );
  }

  // =========================
  // LOADING STATE
  // =========================
  if (!profile) {
    return <h2 style={{ padding: "20px" }}>Loading...</h2>;
  }

  // =========================
  // MAIN UI
  // =========================
  return (
    <div style={{ background: "#0f172a", minHeight: "100vh", color: "white" }}>

      {/* TOP RIGHT PROFILE ICON */}
      <div style={{
        display: "flex",
        justifyContent: "flex-end",
        padding: "20px"
      }}>
        <img
          src={profile.avatar}
          alt="profile"
          width="50"
          style={{
            borderRadius: "50%",
            cursor: "pointer",
            border: "2px solid white"
          }}
          onClick={() => setShowPanel(!showPanel)}
        />
      </div>

      {/* DROPDOWN PANEL */}
      {showPanel && (
        <div style={{
          position: "absolute",
          top: "80px",
          right: "20px",
          width: "350px",
          background: "#1e293b",
          borderRadius: "10px",
          padding: "15px",
          boxShadow: "0 0 10px rgba(0,0,0,0.5)",
          maxHeight: "500px",
          overflowY: "auto"
        }}>

          {/* PROFILE INFO */}
          <div style={{ textAlign: "center", marginBottom: "15px" }}>
            <img
              src={profile.avatar}
              width="70"
              style={{ borderRadius: "50%" }}
            />
            <h3>{profile.name || "No Name"}</h3>
            <p>@{profile.username}</p>
          </div>

          {/* REPOSITORIES */}
          <h4>Repositories</h4>

          {profile.repos.map((repo, index) => (
            <div key={index} style={{
              borderBottom: "1px solid gray",
              padding: "10px 0"
            }}>
              <strong>{repo.name}</strong>
              <div style={{ fontSize: "14px" }}>
                ⭐ {repo.stars} | 🍴 {repo.forks}
              </div>

              <a
                href={repo.url}
                target="_blank"
                rel="noreferrer"
                style={{ color: "#38bdf8", fontSize: "14px" }}
              >
                View →
              </a>
            </div>
          ))}

          {/* LOGOUT BUTTON */}
          <div style={{ marginTop: "15px" }}>
            <button
              onClick={handleLogout}
              style={{
                width: "100%",
                background: "#ef4444",
                color: "white",
                padding: "10px",
                border: "none",
                borderRadius: "6px",
                cursor: "pointer"
              }}
            >
              Logout
            </button>
          </div>

        </div>
      )}

      {/* MAIN PAGE CONTENT */}
      <div style={{ padding: "20px" }}>
        <h1>Welcome to GitScope AI 🚀</h1>
        <p>Click your profile icon (top right) to view your repositories.</p>
      </div>

    </div>
  );
}

export default Profile;