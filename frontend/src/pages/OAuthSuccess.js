import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

function OAuthSuccess() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");
    if (token) {
      localStorage.setItem("token", token);
      navigate("/home");
    } else {
      navigate("/");
    }
  }, [navigate, searchParams]);

  return <div>Logging in...</div>;
}

export default OAuthSuccess;