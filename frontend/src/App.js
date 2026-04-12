import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Home from "./pages/Home";
import OAuthSuccess from "./pages/OAuthSuccess";
import Profile from "./pages/Profile";
import CodeAnalyzer from "./pages/CodeAnalyzer";
import Popularity from "./pages/Popularity";
import BugRisk from "./pages/BugRisk";
import BountyExplorer from "./pages/BountyExplorer";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/home" element={<Home />} />
        <Route path="/oauth-success" element={<OAuthSuccess />} />   
        <Route path="/profile" element={<Profile />} /> 
        <Route path="/code-analyzer" element={<CodeAnalyzer />} />
        <Route path="/popularity" element={<Popularity />} />
        <Route path="/bug-risk" element={<BugRisk />} />
        <Route path="/bounties" element={<BountyExplorer />} />
      </Routes>
    </Router>
  );
}

export default App;