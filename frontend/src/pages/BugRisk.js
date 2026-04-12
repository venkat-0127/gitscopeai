import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bar } from "react-chartjs-2";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
} from "chart.js";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

function BugRisk() {
  const navigate = useNavigate();

  const [repoUrl, setRepoUrl] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  // ✅ SAVE HISTORY
  const saveHistory = (repo, score) => {
    const newItem = {
      type: "Bug Risk",
      repo,
      score,
      date: new Date().toLocaleString(),
    };

    const existing = JSON.parse(localStorage.getItem("history")) || [];
    localStorage.setItem("history", JSON.stringify([newItem, ...existing]));
  };

  const predictBugRisk = async () => {
    if (!repoUrl) return alert("Enter repository URL");

    setLoading(true);
    setResult(null);

    try {
      const res = await fetch("http://localhost:8000/predict-bug-risk", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repo: repoUrl })
      });

      const data = await res.json();

      if (data.error) {
        alert(data.error);
      } else {
        setResult(data);

        // 🔥 SAVE HISTORY
        saveHistory(repoUrl, data.risk_score);
      }

    } catch {
      alert("Server error");
    }

    setLoading(false);
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-gray-50 to-gray-200">

      {/* MAIN */}
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
              Detect Bug Risk ⚠️
            </h2>

            <div></div>
          </div>

          {/* INPUT CARD */}
          <div className="max-w-xl mx-auto bg-white border p-8 rounded-xl shadow-md text-center">

            <input
              type="text"
              placeholder="Enter GitHub Repo URL"
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              className="w-full px-4 py-3 border rounded-lg mb-4"
            />

            <button
              onClick={predictBugRisk}
              className="w-full bg-red-600 text-white py-3 rounded-lg hover:scale-[1.02] transition"
            >
              {loading ? "Analyzing..." : "Analyze"}
            </button>
          </div>

          {/* RESULT */}
          {result && (
            <div className="mt-10 max-w-5xl mx-auto">

              {/* RESULT CARD */}
              <div className="bg-white border p-6 rounded-xl shadow text-center mb-6">

                <h2 className="text-xl font-bold text-red-500">
                  Risk Score: {result.risk_score}
                </h2>

                <p className="mt-2 text-gray-700">
                  Level: {result.risk_level}
                </p>

                <p className="text-gray-700">
                  Health Score: {result.health_score}
                </p>

                <p className="text-gray-700">
                  Health Level: {result.health_level}
                </p>
              </div>

              {/* GRAPH */}
              <div className="bg-white border p-6 rounded-xl shadow h-[350px]">

                <h3 className="text-center font-semibold mb-3">
                  Risk Metrics
                </h3>

                <Bar
                  options={{ responsive: true, maintainAspectRatio: false }}
                  data={{
                    labels: ["Issue Density", "Issue Ratio", "Activity"],
                    datasets: [
                      {
                        label: "Metrics",
                        data: [
                          result.metrics.issue_density,
                          result.metrics.issue_ratio,
                          result.metrics.activity
                        ],
                        backgroundColor: [
                          "#ef4444", // red
                          "#f59e0b", // amber
                          "#22c55e"  // green
                        ]
                      }
                    ]
                  }}
                />
              </div>

              {/* SUGGESTIONS */}
              <div className="mt-6 bg-white border p-6 rounded-xl shadow text-center">

                <h3 className="font-semibold mb-2">
                  Suggestions
                </h3>

                {result.suggestions.map((s, i) => (
                  <p key={i} className="text-yellow-500">
                    ⚠️ {s}
                  </p>
                ))}
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

export default BugRisk;