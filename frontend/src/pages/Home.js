import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function Home() {
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/");
    }
  }, [navigate]);

  const logout = () => {
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <div className="flex justify-between items-center px-10 py-6 bg-gray-800">
        <h1 className="text-2xl font-bold text-blue-400">
          GitScope AI Dashboard
        </h1>
        <button
          onClick={logout}
          className="bg-red-600 px-4 py-2 rounded hover:bg-red-700"
        >
          Logout
        </button>
      </div>

      <div className="flex items-center justify-center mt-40">
        <h2 className="text-4xl font-bold">
          Welcome to GitScope AI
        </h2>
      </div>
    </div>
  );
}

export default Home;