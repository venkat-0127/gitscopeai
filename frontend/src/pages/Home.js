import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function Home() {
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [showPanel, setShowPanel] = useState(false);
  const [activeTab, setActiveTab] = useState("repos");
  const [history, setHistory] = useState([]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const user = JSON.parse(localStorage.getItem("user"));

    if (!token) {
      navigate("/");
      return;
    }

    if (!user || !user.github_username) {
      alert("GitHub username missing. Please login again.");
      navigate("/");
      return;
    }

    loadProfile(user.github_username);

    const savedHistory = JSON.parse(localStorage.getItem("history")) || [];
    setHistory(savedHistory);

  }, [navigate]);

  const loadProfile = (username) => {
    fetch(`http://localhost:8000/github/profile/${username}`)
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((data) => setProfile(data))
      .catch(() => alert("Invalid GitHub username"));
  };

  const logout = () => {
    localStorage.clear();
    navigate("/");
  };

  if (!profile) return <h2 className="p-10">Loading...</h2>;

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-gray-50 to-gray-200">

      <div className="flex flex-1 m-6 rounded-2xl overflow-hidden shadow-xl bg-white">

        <div className="w-full p-10">

          {/* HEADER */}
          <div className="flex justify-between items-center mb-12">

            <div className="flex items-center gap-3">
              <img
                src="https://avatars.githubusercontent.com/oa/3409429?s=120&u=e558ce441ef24b0b5fc2f04643bed64d60c4a905&v=4"
                className="w-10 h-10 rounded-full"
                alt="logo"
              />
              <h1 className="text-2xl font-bold text-gray-900">
                GitScope AI
              </h1>
            </div>

            {/* PROFILE */}
            <div className="relative">
              <img
                src={profile.avatar}
                className="w-12 h-12 rounded-full cursor-pointer border"
                onClick={() => setShowPanel(!showPanel)}
                alt="profile"
              />

              {showPanel && (
                <div className="absolute right-0 mt-3 w-80 bg-white border rounded-lg shadow-lg p-4 max-h-[450px] overflow-y-auto">

                  <div className="text-center">
                    <img
                      src={profile.avatar}
                      className="w-16 h-16 rounded-full mx-auto"
                      alt=""
                    />
                    <h3 className="mt-2 font-bold">{profile.name}</h3>
                    <p className="text-sm text-gray-500">@{profile.username}</p>
                  </div>

                  {/* TABS */}
                  <div className="flex justify-around mt-4 mb-2">
                    <button
                      onClick={() => setActiveTab("repos")}
                      className={`text-sm font-semibold ${
                        activeTab === "repos"
                          ? "text-black border-b-2 border-black"
                          : "text-gray-500"
                      }`}
                    >
                      Repositories
                    </button>

                    <button
                      onClick={() => setActiveTab("history")}
                      className={`text-sm font-semibold ${
                        activeTab === "history"
                          ? "text-black border-b-2 border-black"
                          : "text-gray-500"
                      }`}
                    >
                      History
                    </button>
                  </div>

                  <hr className="mb-3" />

                  {/* REPOS */}
                  {activeTab === "repos" && (
                    <>
                      {profile.repos.map((repo, i) => (
                        <div key={i} className="bg-gray-100 p-3 rounded mb-2">

                          <h5 className="font-semibold">{repo.name}</h5>

                          <div className="text-sm text-gray-600 mb-1">
                            ⭐ {repo.stars} | 🍴 {repo.forks} | 👀 {repo.watchers}
                          </div>

                          <a
                            href={repo.url}
                            target="_blank"
                            rel="noreferrer"
                            className="text-sm text-blue-600 hover:underline"
                          >
                            View Repo →
                          </a>
                        </div>
                      ))}
                    </>
                  )}

                  {/* HISTORY */}
                  {activeTab === "history" && (
                    <>
                      {history.length === 0 ? (
                        <p className="text-sm text-gray-500">
                          No analysis history
                        </p>
                      ) : (
                        history.map((item, i) => (
                          <div key={i} className="bg-gray-100 p-3 rounded mb-2">

                            <p className="text-sm font-semibold">
                              🔍 {item.type}
                            </p>

                            <p className="text-xs text-gray-500">
                              Repo: {item.repo}
                            </p>

                            <p className="text-xs text-gray-500">
                              📅 {item.date}
                            </p>

                            <p className="text-sm font-bold text-green-600">
                              Score: {item.score}
                            </p>
                          </div>
                        ))
                      )}
                    </>
                  )}

                  <button
                    onClick={logout}
                    className="mt-3 w-full bg-red-600 text-white py-2 rounded hover:bg-red-700"
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* TITLE */}
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900">
              Welcome to GitScope AI 
            </h2>
          </div>

          {/* CARDS */}
          <div className="grid grid-cols-2 gap-8 max-w-2xl mx-auto">

            <div
              onClick={() => navigate("/code-analyzer")}
              className="bg-white border p-6 rounded-xl shadow-md hover:shadow-xl hover:scale-[1.03] transition cursor-pointer text-center"
            >
              💻 <p className="mt-2 font-semibold">Check Code Quality</p>
            </div>

            <div
              onClick={() => navigate("/popularity")}
              className="bg-white border p-6 rounded-xl shadow-md hover:shadow-xl hover:scale-[1.03] transition cursor-pointer text-center"
            >
              📈 <p className="mt-2 font-semibold">Predict Popularity</p>
            </div>

            <div
              onClick={() => navigate("/bug-risk")}
              className="bg-white border p-6 rounded-xl shadow-md hover:shadow-xl hover:scale-[1.03] transition cursor-pointer text-center"
            >
              🐛 <p className="mt-2 font-semibold">Detect Bug Risks</p>
            </div>

            <div
              onClick={() => navigate("/bounties")}
              className="bg-white border p-6 rounded-xl shadow-md hover:shadow-xl hover:scale-[1.03] transition cursor-pointer text-center"
            >
              💰 <p className="mt-2 font-semibold">Find Bounties</p>
            </div>

          </div>
        </div>
      </div>

      {/* FOOTER */}
      <div className="bg-black text-white text-center py-3 text-sm">
        © 2026 GitScope AI. All rights reserved.
      </div>
    </div>
  );
}

export default Home;