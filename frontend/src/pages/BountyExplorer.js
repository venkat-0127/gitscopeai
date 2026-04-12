import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function BountyExplorer() {
  const navigate = useNavigate();

  const [bounties, setBounties] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("all");

  // ✅ FETCH DATA (FIXED)
  useEffect(() => {
    const loadBounties = async () => {
      try {
        const res = await fetch("http://localhost:8000/bounties");
        const data = await res.json();

        console.log("API:", data);

        let bountyData = [];

        if (Array.isArray(data)) {
          bountyData = data;
        } else if (data.bounties) {
          bountyData = data.bounties;
        }

        // ❗ CLEAN INVALID DATA
        bountyData = bountyData.filter(b => b && b.title);

        setBounties(bountyData);
        setFiltered(bountyData);

      } catch (err) {
        console.error(err);

        // 🔥 FALLBACK (so UI never breaks)
        setBounties([]);
        setFiltered([]);
      }

      setLoading(false);
    };

    loadBounties();
  }, []);

  // 🔍 FILTER
  useEffect(() => {
    let data = [...bounties];

    if (filter === "real") {
      data = data.filter(b => b.reward && b.reward !== "Not specified");
    } else if (filter === "opportunity") {
      data = data.filter(b => !b.reward || b.reward === "Not specified");
    } else if (filter === "verified") {
      data = data.filter(b => b.verified);
    }

    setFiltered(data);
  }, [filter, bounties]);

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
              💰 Bounty Opportunities
            </h2>

            <div></div>
          </div>

          {/* FILTERS */}
          <div className="flex justify-center gap-3 mb-8 flex-wrap">

            <button onClick={() => setFilter("all")}
              className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300">
              All
            </button>

            <button onClick={() => setFilter("real")}
              className="px-4 py-2 bg-green-500 text-white rounded-lg">
              💰 Real
            </button>

            <button onClick={() => setFilter("verified")}
              className="px-4 py-2 bg-blue-500 text-white rounded-lg">
              ✅ Verified
            </button>

            <button onClick={() => setFilter("opportunity")}
              className="px-4 py-2 bg-yellow-500 text-white rounded-lg">
              ⚠️ Opportunity
            </button>

          </div>

          {/* LOADING */}
          {loading && (
            <p className="text-center text-gray-500">
              Loading bounty issues...
            </p>
          )}

          {/* EMPTY */}
          {!loading && filtered.length === 0 && (
            <p className="text-center text-gray-500">
              No bounty issues found
            </p>
          )}

          {/* LIST */}
          <div className="grid md:grid-cols-2 gap-6">

            {filtered.map((item, index) => {

              // ✅ FIX URL BUG
              const issueUrl =
                item.issue_url ||
                item.url ||
                item.html_url ||
                "#";

              return (
                <div
                  key={index}
                  className="bg-white border p-5 rounded-xl shadow hover:shadow-lg transition"
                >

                  {/* REPO */}
                  <h2 className="text-lg font-bold text-blue-600">
                    {item.repo || "Unknown Repo"}
                  </h2>

                  {/* TITLE */}
                  <p className="mt-2 font-semibold">
                    {item.title}
                  </p>

                  {/* DESCRIPTION */}
                  <p className="text-gray-500 mt-2 text-sm">
                    {item.description || "No description"}
                  </p>

                  {/* BADGE */}
                  <div className="mt-3">
                    {item.reward && item.reward !== "Not specified" ? (
                      <span className="bg-green-500 text-white px-2 py-1 rounded text-sm">
                        💰 Real Bounty
                      </span>
                    ) : (
                      <span className="bg-yellow-500 text-white px-2 py-1 rounded text-sm">
                        ⚠️ Opportunity
                      </span>
                    )}
                  </div>

                  {/* REWARD */}
                  <p className="text-green-600 mt-2">
                    Reward: {item.reward || "N/A"}
                  </p>

                  {/* VERIFIED */}
                  {item.verified && (
                    <p className="text-blue-500 mt-1">
                      ✅ Verified
                    </p>
                  )}

                  {/* LINKS */}
                  <div className="mt-4 flex flex-col gap-2">

                    <a
                      href={issueUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="bg-blue-600 text-white px-3 py-2 rounded text-center hover:bg-blue-700"
                    >
                      🔗 Open GitHub Issue
                    </a>

                    {/* OPTIONAL BOUNTY LINKS */}
                    {item.bounty_links?.map((link, i) => (
                      <a
                        key={i}
                        href={link}
                        target="_blank"
                        rel="noreferrer"
                        className="bg-green-600 text-white px-3 py-2 rounded text-center hover:bg-green-700"
                      >
                        💸 View Bounty
                      </a>
                    ))}

                  </div>
                </div>
              );
            })}
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

export default BountyExplorer;