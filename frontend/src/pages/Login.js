import React, { useState, useRef, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import ReCAPTCHA from "react-google-recaptcha";

function Login() {
  const navigate = useNavigate();
  const recaptchaRef = useRef();

  const [githubUsername, setGithubUsername] = useState("");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [recaptchaToken, setRecaptchaToken] = useState(null);
  const [message, setMessage] = useState("");
  const [showCaptcha, setShowCaptcha] = useState(true);
  const [timer, setTimer] = useState(0);

  // =========================
  // 🔥 FIX: HANDLE GITHUB LOGIN AUTO REDIRECT
  // =========================
  useEffect(() => {
    const fetchGitHubUser = async () => {
      try {
        const res = await fetch("http://127.0.0.1:8000/auth/github/user");

        if (!res.ok) return; // no session → ignore

        const data = await res.json();

        if (data.login) {
          localStorage.setItem(
            "user",
            JSON.stringify({
              github_username: data.login,
              email: data.email || "",
            })
          );

          navigate("/home");
        }
      } catch {
        // silently ignore
      }
    };

    fetchGitHubUser();
  }, [navigate]);

  // =========================
  // TIMER
  // =========================
  useEffect(() => {
    let interval;
    if (timer > 0) {
      interval = setInterval(() => {
        setTimer((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [timer]);

  const extractUsername = (input) => {
    if (input.includes("github.com")) {
      return input.split("github.com/")[1].replace("/", "");
    }
    return input;
  };

  // =========================
  // SEND OTP
  // =========================
  const sendOtp = async () => {
    if (!githubUsername) {
      setMessage("⚠️ Enter GitHub username");
      return;
    }

    if (!email) return alert("Enter email");

    if (!recaptchaToken) {
      setMessage("⚠️ Please complete reCAPTCHA");
      return;
    }

    try {
      setLoading(true);

      await axios.post("http://127.0.0.1:8000/auth/send-otp", {
        email,
        recaptcha_token: recaptchaToken,
      });

      setOtpSent(true);
      setMessage("✅ OTP sent to your email");
      setTimeout(() => setMessage(""), 3000);

      setTimer(60);

      recaptchaRef.current.reset();
      setRecaptchaToken(null);
      setShowCaptcha(false);

    } catch {
      setMessage("❌ Failed to send OTP");
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // VERIFY OTP
  // =========================
  const verifyOtp = async () => {
    if (!otp) return alert("Enter OTP");

    try {
      setLoading(true);

      const res = await axios.post(
        "http://127.0.0.1:8000/auth/verify-otp",
        {
          email,
          otp,
          github_username: extractUsername(githubUsername),
        }
      );

      localStorage.setItem("token", res.data.token);

      localStorage.setItem(
        "user",
        JSON.stringify({
          email: res.data.email,
          github_username: extractUsername(githubUsername),
        })
      );

      navigate("/home");

    } catch {
      setMessage("❌ Invalid OTP");
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // RESEND OTP
  // =========================
  const resendOtp = async () => {
    if (timer > 0) return;

    if (!recaptchaToken) {
      setShowCaptcha(true);
      setMessage("🔄 Complete reCAPTCHA to resend OTP");
      return;
    }

    await sendOtp();
  };

  // =========================
  // GITHUB LOGIN
  // =========================
  const handleGitHubLogin = () => {
    window.location.replace("http://127.0.0.1:8000/auth/github/login");
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-gray-50 to-gray-200">

      <div className="flex flex-1 m-6 rounded-2xl overflow-hidden shadow-xl bg-white">

         {/* LEFT */}
        <div className="w-1/2 p-12 flex flex-col justify-center border-r border-gray-200">

          <div className="flex items-center gap-4 mb-8">
            <img
              src="https://avatars.githubusercontent.com/oa/3409429?s=120&u=e558ce441ef24b0b5fc2f04643bed64d60c4a905&v=4"
              className="w-14 h-14 rounded-full shadow-sm"
              alt="logo"
            />
            <h1 className="text-4xl font-bold text-gray-900">
              GitScope AI
            </h1>
          </div>

          <p className="text-gray-600 mb-6 leading-relaxed max-w-md">
            GitScope AI analyzes GitHub repositories using AI to predict defects, evaluate quality, and measure project health.
          </p>

          <div className="space-y-3 text-gray-700">
            <p>✔ Pre-commit code quality analysis</p>
            <p>✔ LSTM-based popularity prediction</p>
            <p>✔ Random Forest defect detection</p>
            <p>✔ AI-powered health score</p>
          </div>
        </div>

        {/* RIGHT */}
        <div className="w-1/2 flex items-center justify-center bg-gray-50">

          <div className="w-96 bg-white p-8 rounded-xl shadow-lg border">

            <h2 className="text-2xl font-semibold mb-4">
              Sign in to GitScope
            </h2>

            {message && (
              <div className="mb-3 text-sm text-center bg-gray-100 p-2 rounded">
                {message}
              </div>
            )}

            <input
              type="text"
              placeholder="GitHub Username"
              value={githubUsername}
              onChange={(e) => setGithubUsername(e.target.value)}
              className="w-full mb-3 px-4 py-3 border rounded"
            />

            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full mb-3 px-4 py-3 border rounded"
            />

            {showCaptcha && (
              <ReCAPTCHA
                ref={recaptchaRef}
                sitekey="6LcaInMsAAAAANiaNT8akvx0Mnc41jmUEgRAPllu"
                onChange={(token) => setRecaptchaToken(token)}
              />
            )}

            {!otpSent ? (
              <button
                onClick={sendOtp}
                className="w-full mt-3 bg-black text-white py-3 rounded"
              >
                Send OTP
              </button>
            ) : (
              <>
                <input
                  type="text"
                  placeholder="Enter OTP"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  className="w-full mt-3 mb-3 px-4 py-3 border rounded"
                />

                <button
                  onClick={verifyOtp}
                  className="w-full bg-green-600 text-white py-3 rounded"
                >
                  Verify OTP
                </button>

                <button
                  onClick={resendOtp}
                  disabled={timer > 0}
                  className="w-full mt-2 text-sm"
                >
                  {timer > 0
                    ? `Resend OTP in ${timer}s`
                    : "Resend OTP"}
                </button>
              </>
            )}

            <div className="text-center my-4">or</div>

             <button
              onClick={handleGitHubLogin}
              className="w-full flex items-center justify-center gap-2 border border-gray-300 py-3 rounded-lg hover:bg-gray-100 transition"
            >
              <img
                src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
                className="w-5"
                alt="github"
              />
              Continue with GitHub
            </button>

          </div>
        </div>
      </div>

      <div className="bg-black text-white text-center py-3">
        © 2026 GitScope AI
      </div>
    </div>
  );
}

export default Login;