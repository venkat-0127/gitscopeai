import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function Home() {
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [showPanel, setShowPanel] = useState(false);
  const [githubInput, setGithubInput] = useState("");
  const [needsUsername, setNeedsUsername] = useState(false);

  // =========================
  // EXTRACT USERNAME FROM INPUT
  // =========================
  const extractUsername = (input) => {
    if (input.includes("github.com")) {
      const parts = input.split("github.com/");
      return parts[1]?.replace("/", "");
    }
    return input;
  };

  // =========================
  // LOAD USER
  // =========================
  useEffect(() => {
    const token = localStorage.getItem("token");
    const user = JSON.parse(localStorage.getItem("user"));

    if (!token) {
      navigate("/");
      return;
    }

    if (!user || !user.github_username) {
      setNeedsUsername(true);
      return;
    }

    loadProfile(user.github_username);
  }, [navigate]);

  // =========================
  // FETCH PROFILE
  // =========================
  const loadProfile = (username) => {
    fetch(`http://localhost:8000/github/profile/${username}`)
      .then(res => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then(data => setProfile(data))
      .catch(() => alert("Invalid GitHub username"));
  };

  // =========================
  // SAVE USERNAME / LINK
  // =========================
  const saveUsername = () => {
    if (!githubInput) {
      alert("Enter GitHub username or profile link");
      return;
    }

    const username = extractUsername(githubInput);

    const user = JSON.parse(localStorage.getItem("user")) || {};

    const updatedUser = {
      ...user,
      github_username: username
    };

    localStorage.setItem("user", JSON.stringify(updatedUser));

    setNeedsUsername(false);
    loadProfile(username);
  };

  // =========================
  // LOGOUT
  // =========================
  const logout = () => {
    localStorage.clear();
    navigate("/");
  };

  const handleClick = (feature) => {
    alert(`${feature} module coming soon 🚀`);
  };

  // =========================
  // USERNAME INPUT SCREEN
  // =========================
  if (needsUsername) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-900 text-white">
        <h2 className="text-2xl mb-4">Enter GitHub Username or Profile Link</h2>

        <input
          type="text"
          placeholder="e.g. venkat-0127 OR https://github.com/venkat-0127"
          value={githubInput}
          onChange={(e) => setGithubInput(e.target.value)}
          className="px-4 py-2 rounded text-black w-80"
        />

        <button
          onClick={saveUsername}
          className="mt-4 bg-blue-600 px-4 py-2 rounded"
        >
          Continue
        </button>
      </div>
    );
  }

  // =========================
  // LOADING
  // =========================
  if (!profile) {
    return <h2 className="text-white p-10">Loading...</h2>;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">

      {/* TOP BAR */}
      <div className="flex justify-between items-center px-10 py-6">
        <h1 className="text-2xl font-bold text-blue-400">
          GitScope AI
        </h1>

        {/* PROFILE ICON */}
        <div className="relative">
          <img
            src={profile.avatar}
            alt="profile"
            className="w-12 h-12 rounded-full cursor-pointer border-2 border-white"
            onClick={() => setShowPanel(!showPanel)}
          />

          {/* PROFILE PANEL */}
          {showPanel && (
            <div className="absolute right-0 mt-3 w-80 bg-gray-800 rounded-lg shadow-lg p-4 max-h-[500px] overflow-y-auto">

              <div className="text-center">
                <img
                  src={profile.avatar}
                  className="w-16 h-16 rounded-full mx-auto"
                  alt="profile"
                />
                <h3 className="mt-2 font-bold">{profile.name}</h3>
                <p className="text-sm text-gray-400">@{profile.username}</p>
              </div>

              <hr className="my-3 border-gray-600" />

              <h4 className="text-sm font-semibold mb-2">Repositories</h4>

              {profile.repos.map((repo, i) => (
                <div key={i} className="bg-gray-700 p-3 rounded mb-3">

                  <h5 className="font-semibold">{repo.name}</h5>

                  <div className="text-sm text-gray-300">
                    ⭐ {repo.stars} | 🍴 {repo.forks} | 🐞 {repo.issues}
                  </div>

                  <div className="text-xs text-green-400">
                    Popularity: {repo.popularity}
                  </div>

                  <div className="text-xs text-yellow-400">
                    Quality: {repo.quality}
                  </div>

                  <a
                    href={repo.url}
                    target="_blank"
                    rel="noreferrer"
                    className="text-blue-400 text-sm"
                  >
                    View on GitHub →
                  </a>

                </div>
              ))}

              <button
                onClick={logout}
                className="mt-3 w-full bg-red-600 py-2 rounded hover:bg-red-700"
              >
                Logout
              </button>
            </div>
          )}
        </div>
      </div>

      {/* DASHBOARD */}
      <div className="flex flex-col items-center justify-center mt-20">

        <h2 className="text-3xl font-bold mb-12">
          Welcome to GitScope AI 🚀
        </h2>

        <div className="grid grid-cols-2 gap-10">

          <div onClick={() => handleClick("Code Quality")} className="bg-gray-800 w-64 h-32 flex items-center justify-center rounded-lg cursor-pointer hover:bg-gray-700">
            Check Code Quality
          </div>

          <div onClick={() => handleClick("Popularity")} className="bg-gray-800 w-64 h-32 flex items-center justify-center rounded-lg cursor-pointer hover:bg-gray-700">
            Predict Popularity
          </div>

          <div onClick={() => handleClick("Bug Risks")} className="bg-gray-800 w-64 h-32 flex items-center justify-center rounded-lg cursor-pointer hover:bg-gray-700">
            Detect Bug Risks
          </div>

          <div onClick={() => handleClick("Bounties")} className="bg-gray-800 w-64 h-32 flex items-center justify-center rounded-lg cursor-pointer hover:bg-gray-700">
            Find Bounties
          </div>

        </div>
      </div>
    </div>
  );
}

export default Home;