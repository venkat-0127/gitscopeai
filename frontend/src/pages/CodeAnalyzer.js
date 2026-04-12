import { useState } from "react";
import { useNavigate } from "react-router-dom";

function CodeAnalyzer() {
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);

  const [score, setScore] = useState(null);
  const [pylint, setPylint] = useState(null);
  const [flake8, setFlake8] = useState(null);
  const [javaOutput, setJavaOutput] = useState(null);
  const [language, setLanguage] = useState(null);

  // ✅ SAVE HISTORY
  const saveHistory = (type, repo, score) => {
    const newItem = {
      type,
      repo,
      score,
      date: new Date().toLocaleString(),
    };

    const existing = JSON.parse(localStorage.getItem("history")) || [];
    localStorage.setItem("history", JSON.stringify([newItem, ...existing]));
  };

  const handleUpload = async () => {
    if (!file) {
      alert("Please upload a ZIP file");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      setLoading(true);

      const res = await fetch("http://localhost:8000/analyze-code", {
        method: "POST",
        body: formData,
      });

      const data = await res.json();

      // reset
      setScore(null);
      setPylint(null);
      setFlake8(null);
      setJavaOutput(null);

      setLanguage(data.language);

      if (data.language === "python") {
        const finalScore = data.result?.score;
        setScore(finalScore);
        setPylint(data.result?.pylint_output);
        setFlake8(data.result?.flake8_output);

        // 🔥 SAVE HISTORY
        saveHistory("Code Quality", file.name, finalScore);

      } else if (data.language === "java") {
        setJavaOutput(data.output);

        // 🔥 SAVE HISTORY (no score case)
        saveHistory("Code Quality", file.name, "N/A");
      }

    } catch (error) {
      console.error(error);
      alert("Analysis failed");
    } finally {
      setLoading(false);
    }
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
              className="bg-gray-200 px-4 py-2 rounded-lg hover:bg-gray-300 transition"
            >
              ⬅ Back
            </button>

            <h2 className="text-2xl font-bold text-gray-900">
               Code Quality Analyzer
            </h2>

            <div></div>
          </div>

          {/* UPLOAD CARD */}
          <div className="max-w-xl mx-auto bg-white border p-8 rounded-xl shadow-md text-center">

            <input
              type="file"
              onChange={(e) => setFile(e.target.files[0])}
              className="mb-4"
            />

            <button
              onClick={handleUpload}
              className="w-full bg-black text-white py-3 rounded-lg mt-2 hover:scale-[1.02] active:scale-[0.98] transition"
            >
              {loading ? "Analyzing..." : "Analyze Code"}
            </button>

            {file && (
              <p className="mt-3 text-gray-500 text-sm">
                Selected: {file.name}
              </p>
            )}
          </div>

          {/* RESULT */}
          {language && (
            <div className="mt-10 max-w-4xl mx-auto bg-white border p-6 rounded-xl shadow-md">

              <h3 className="text-center text-lg font-semibold mb-6">
                Detected Language: {language.toUpperCase()}
              </h3>

              {/* SCORE */}
              {score !== null && (
                <div className="text-center mb-6">

                  <p className="text-xl font-semibold">
                    📊 Score: {score}/10
                  </p>

                  <div className="w-full bg-gray-200 rounded mt-3">
                    <div
                      className="bg-green-600 text-white text-xs py-1 rounded"
                      style={{ width: `${score * 10}%` }}
                    >
                      {score * 10}%
                    </div>
                  </div>
                </div>
              )}

              {/* PYLINT */}
              {pylint && (
                <div className="mt-6">
                  <h4 className="font-semibold mb-2 text-gray-800">
                    Pylint Output:
                  </h4>
                  <pre className="text-sm whitespace-pre-wrap bg-gray-100 p-3 rounded">
                    {pylint}
                  </pre>
                </div>
              )}

              {/* FLAKE8 */}
              {flake8 && (
                <div className="mt-6">
                  <h4 className="font-semibold mb-2 text-gray-800">
                    Flake8 Issues:
                  </h4>
                  <pre className="text-sm whitespace-pre-wrap bg-gray-100 p-3 rounded">
                    {flake8}
                  </pre>
                </div>
              )}

              {/* JAVA */}
              {javaOutput && (
                <div className="mt-6">
                  <h4 className="font-semibold mb-2 text-gray-800">
                    Java Analysis:
                  </h4>
                  <pre className="text-sm whitespace-pre-wrap bg-gray-100 p-3 rounded">
                    {javaOutput}
                  </pre>
                </div>
              )}

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

export default CodeAnalyzer;