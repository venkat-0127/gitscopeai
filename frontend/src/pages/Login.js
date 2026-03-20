import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import ReCAPTCHA from "react-google-recaptcha";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [recaptchaToken, setRecaptchaToken] = useState(null);

  // =========================
  // SEND OTP
  // =========================
  const sendOtp = async () => {
    if (!email) {
      alert("Please enter email");
      return;
    }

    if (!recaptchaToken) {
      alert("Please complete reCAPTCHA");
      return;
    }

    try {
      setLoading(true);

      await axios.post("http://127.0.0.1:8000/auth/send-otp", {
        email,
        recaptcha_token: recaptchaToken,
      });

      alert("OTP sent to your email");
      setOtpSent(true);
    } catch (error) {
      alert(error.response?.data?.detail || "Failed to send OTP");
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // VERIFY OTP
  // =========================
  const verifyOtp = async () => {
    if (!otp) {
      alert("Please enter OTP");
      return;
    }

    try {
      setLoading(true);

      const response = await axios.post(
        "http://127.0.0.1:8000/auth/verify-otp",
        {
          email,
          otp,
        }
      );

      localStorage.setItem("token", response.data.token);

      localStorage.setItem(
        "user",
        JSON.stringify({
          email: response.data.email,
          github_username: response.data.github_username,
        })
      );

      navigate("/home");
    } catch (error) {
      alert(error.response?.data?.detail || "Invalid OTP");
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // ✅ FIXED GITHUB LOGIN
  // =========================
  const handleGitHubLogin = (e) => {
    e.preventDefault(); // 🔥 prevents reload
    window.location.replace("http://127.0.0.1:8000/auth/github/login");
  };

  // =========================
  // UI
  // =========================
  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center">
      <div className="bg-gray-800 p-10 rounded-lg shadow-lg w-96">
        <h2 className="text-3xl text-white font-bold text-center mb-8">
          Login to GitScope AI
        </h2>

        {/* EMAIL */}
        <input
          type="email"
          placeholder="Enter Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full mb-4 px-4 py-3 rounded bg-gray-700 text-white"
        />

        {/* RECAPTCHA */}
        {!otpSent && (
          <div className="mb-4 flex justify-center">
            <ReCAPTCHA
              sitekey="6LcaInMsAAAAANiaNT8akvx0Mnc41jmUEgRAPllu"
              onChange={(token) => setRecaptchaToken(token)}
            />
          </div>
        )}

        {/* SEND OTP */}
        {!otpSent ? (
          <button
            type="button"
            onClick={sendOtp}
            className="w-full bg-blue-600 hover:bg-blue-700 py-3 rounded text-white"
            disabled={loading}
          >
            {loading ? "Sending..." : "Send OTP"}
          </button>
        ) : (
          <>
            <input
              type="text"
              placeholder="Enter OTP"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              className="w-full mt-4 mb-4 px-4 py-3 rounded bg-gray-700 text-white"
            />

            <button
              type="button"
              onClick={verifyOtp}
              className="w-full bg-green-600 hover:bg-green-700 py-3 rounded text-white"
              disabled={loading}
            >
              {loading ? "Verifying..." : "Verify OTP"}
            </button>
          </>
        )}

        {/* ✅ FIXED GITHUB BUTTON */}
        <button
          type="button" // 🔥 VERY IMPORTANT
          onClick={handleGitHubLogin}
          className="w-full bg-gray-700 hover:bg-gray-600 py-3 rounded text-white mt-4"
        >
          Login with GitHub
        </button>
      </div>
    </div>
  );
}

export default Login;