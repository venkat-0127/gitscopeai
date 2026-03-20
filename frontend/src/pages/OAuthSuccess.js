import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function OAuthSuccess() {
  const navigate = useNavigate();

  useEffect(() => {
    try {
      const params = new URLSearchParams(window.location.search);

      const token = params.get("token");
      const username = params.get("username");

      console.log("OAuth Success → Token:", token);
      console.log("OAuth Success → Username:", username);

      // ❌ If no token → go back to login
      if (!token) {
        alert("GitHub login failed. Try again.");
        navigate("/");
        return;
      }

      // ✅ Save token
      localStorage.setItem("token", token);

      // ✅ Save user safely
      localStorage.setItem(
        "user",
        JSON.stringify({
          github_username: username || null,
        })
      );

      // ✅ Small delay (ensures storage is saved before navigation)
      setTimeout(() => {
        navigate("/home");
      }, 500);

    } catch (error) {
      console.error("OAuth Error:", error);
      navigate("/");
    }
  }, [navigate]);

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">
      <h2 className="text-xl">Logging in with GitHub...</h2>
    </div>
  );
}

export default OAuthSuccess;