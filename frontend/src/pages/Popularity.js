import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Line, Bar } from "react-chartjs-2";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend
} from "chart.js";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend
);

function Popularity() {
  const navigate = useNavigate();

  const [repoUrl, setRepoUrl] = useState("");
  const [result, setResult] = useState(null);
  const [trend, setTrend] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);

  const saveHistory = (repo, score) => {
    const newItem = {
      type: "Popularity",
      repo,
      score,
      date: new Date().toLocaleString(),
    };

    const existing = JSON.parse(localStorage.getItem("history")) || [];
    localStorage.setItem("history", JSON.stringify([newItem, ...existing]));
  };

  const predictPopularity = async () => {
    if (!repoUrl) return alert("Enter repository URL");

    setLoading(true);
    setResult(null);

    try {
      const res = await fetch("http://localhost:8000/predict-popularity", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repo: repoUrl })
      });

      const data = await res.json();

      if (data.error) return alert(data.error);

      setResult(data);
      saveHistory(repoUrl, data.score);

      const trendRes = await fetch("http://localhost:8000/repo-trend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repo: repoUrl })
      });

      const trendData = await trendRes.json();
      setTrend(trendData.trend || []);

      const recRes = await fetch("http://localhost:8000/recommend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repo: repoUrl })
      });

      const recData = await recRes.json();
      setRecommendations(recData.recommendations || []);

    } catch {
      alert("Server error");
    }

    setLoading(false);
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-gray-50 to-gray-200">

      <div className="flex flex-1 m-6 rounded-2xl overflow-hidden shadow-xl bg-white">
        <div className="w-full p-10">

          {/* HEADER */}
          <div className="flex justify-between items-center mb-10">
            <button
              onClick={() => navigate("/home")}
              className="bg-gray-200 px-4 py-2 rounded-lg hover:bg-gray-300"
            >
              ⬅ Back
            </button>

            <h2 className="text-2xl font-bold">
              Predict Repository Popularity
            </h2>

            <div></div>
          </div>

          {/* INPUT */}
          <div className="max-w-xl mx-auto bg-white border p-8 rounded-xl shadow-md text-center">
            <input
              type="text"
              placeholder="Enter GitHub Repo URL"
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              className="w-full px-4 py-3 border rounded-lg mb-4"
            />

            <button
              onClick={predictPopularity}
              className="w-full bg-black text-white py-3 rounded-lg"
            >
              {loading ? "Predicting..." : "Predict"}
            </button>
          </div>

          {/* RESULT */}
          {result && (
            <div className="mt-10 max-w-6xl mx-auto">

              {/* SCORE */}
              <div className="bg-white border p-6 rounded-xl shadow text-center mb-6">
                <h2 className="text-xl font-bold">
                  Score: {result.score}
                </h2>
                <p className="text-gray-600">
                  Level: {result.level}
                </p>
              </div>

              {/* GRAPHS */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

                {/* BAR CHART */}
                <div className="bg-white border p-6 rounded-xl shadow h-[350px]">
                  <h3 className="text-center mb-2 font-semibold">
                    Repository Metrics
                  </h3>

                  <Bar
                    options={{ responsive: true, maintainAspectRatio: false }}
                    data={{
                      labels: ["Stars", "Forks", "Watchers", "Issues"],
                      datasets: [
                        {
                          label: "Metrics",
                          data: [
                            result.data.stars ?? 0,
                            result.data.forks ?? 0,
                            result.data.watchers ?? 0,
                            result.data.issues ?? 0
                          ],
                          backgroundColor: [
                            "#facc15", // yellow
                            "#3b82f6", // blue
                            "#22c55e", // green
                            "#ef4444"  // red
                          ]
                        }
                      ]
                    }}
                  />
                </div>

                {/* LINE CHART */}
                <div className="bg-white border p-6 rounded-xl shadow h-[350px]">
                  <h3 className="text-center mb-2 font-semibold">
                    Growth Trend
                  </h3>

                  <Line
                    options={{ responsive: true, maintainAspectRatio: false }}
                    data={{
                      labels: ["Past", "Growth", "Mid", "Recent", "Now"],
                      datasets: [
                        {
                          label: "Trend",
                          data: trend,
                          borderColor: "#3b82f6",
                          backgroundColor: "rgba(59,130,246,0.2)",
                          tension: 0.4,
                          fill: true
                        }
                      ]
                    }}
                  />
                </div>

              </div>

              {/* RECOMMEND */}
              {recommendations.length > 0 && (
                <div className="mt-6 bg-white border p-6 rounded-xl shadow text-center">
                  <h3 className="font-semibold mb-2">
                    Recommended Repositories
                  </h3>
                  {recommendations.map((repo, i) => (
                    <p key={i}>{repo}</p>
                  ))}
                </div>
              )}

              {/* STATS */}
              <div className="mt-4 text-center text-gray-600">
                ⭐ {result.data.stars} | 🍴 {result.data.forks} | 👀 {result.data.watchers} | 🐛 {result.data.issues}
              </div>

            </div>
          )}
        </div>
      </div>

      {/* FOOTER */}
      <div className="bg-black text-white text-center py-3 text-sm">
        © 2026 GitScope AI. All rights reserved.
      </div>
    </div>
  );
}

export default Popularity;