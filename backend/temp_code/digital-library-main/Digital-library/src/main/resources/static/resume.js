// app.js (cleaned & fixed)
// Replace your existing JS with this file. It fixes the nested parseTextToModel and other issues.

(() => {
  // basic model
  const resume = {
    name: '', email: '', phone: '', location: '', roll: '', links: [], experience: [], education: [], skills: [], summary: '', certifications: [], additional: '', template: 'clean', color: '#0b66c3'
  };

  // helpers
  const $ = id => document.getElementById(id);
  function escapeHtml(s) { return (s || '').toString().replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

  // load/save local
  function saveLocal() { try { localStorage.setItem('resume_builder_v2', JSON.stringify(resume)); alert('Saved locally'); } catch (e) { console.error(e); alert('Save failed'); } }
  function loadLocal() { const raw = localStorage.getItem('resume_builder_v2'); if (!raw) return false; try { Object.assign(resume, JSON.parse(raw)); return true; } catch (e) { return false; } }
  function clearLocal() { localStorage.removeItem('resume_builder_v2'); alert('Cleared local save'); }

  // UI wiring
  const steps = Array.from(document.querySelectorAll('.step'));
  let curStep = 1;
  if ($('curStep')) $('curStep').innerText = curStep;

  function showStep(n) {
    steps.forEach(s => s.classList.toggle('active', Number(s.dataset.step) === n));
    for (let i = 1; i <= 7; i++) {
      const p = $('panel-' + i);
      if (p) p.classList.toggle('hidden', i !== n);
    }
    curStep = n;
    if ($('curStep')) $('curStep').innerText = curStep;
  }
  if ($('nextStep')) $('nextStep').addEventListener('click', () => { if (curStep < 7) showStep(curStep + 1); });
  if ($('prevStep')) $('prevStep').addEventListener('click', () => { if (curStep > 1) showStep(curStep - 1); });
  steps.forEach(s => s.addEventListener('click', () => showStep(Number(s.dataset.step))));

  // template & color selection
  document.querySelectorAll('.template-thumb').forEach(t => {
    t.addEventListener('click', () => {
      document.querySelectorAll('.template-thumb').forEach(x => x.style.borderColor = '#ddd');
      t.style.borderColor = 'var(--accent)';
      resume.template = t.dataset.tmpl;
      renderPreview();
    });
  });
  document.querySelectorAll('.color-dot').forEach(c => {
    c.addEventListener('click', () => {
      document.querySelectorAll('.color-dot').forEach(x => x.style.outline = 'none');
      c.style.outline = '3px solid rgba(0,0,0,0.06)';
      resume.color = c.dataset.color;
      document.documentElement.style.setProperty('--accent', resume.color);
      renderPreview();
    });
  });

  // header fields - guard if elements exist
  if ($('name')) $('name').addEventListener('input', () => { resume.name = $('name').value; renderPreview(); });
  if ($('email')) $('email').addEventListener('input', () => { resume.email = $('email').value; renderPreview(); });
  if ($('phone')) $('phone').addEventListener('input', () => { resume.phone = $('phone').value; renderPreview(); });
  if ($('location')) $('location').addEventListener('input', () => { resume.location = $('location').value; renderPreview(); });
  if ($('studentRoll')) $('studentRoll').addEventListener('input', () => { resume.roll = $('studentRoll').value; });

  // skills/summary/additional/certs
  if ($('skillsTxt')) $('skillsTxt').addEventListener('input', () => { resume.skills = $('skillsTxt').value.split(',').map(s => s.trim()).filter(Boolean); renderPreview(); });
  if ($('summaryTxt')) $('summaryTxt').addEventListener('input', () => { resume.summary = $('summaryTxt').value; renderPreview(); });
  if ($('additionalTxt')) $('additionalTxt').addEventListener('input', () => { resume.additional = $('additionalTxt').value; renderPreview(); });
  if ($('certTxt')) $('certTxt').addEventListener('input', () => { resume.certifications = $('certTxt').value.split(/\r?\n/).map(s => s.trim()).filter(Boolean); renderPreview(); });

  // links UI
  const linksList = $('linksList');
  function renderLinksUI() {
    if (!linksList) return;
    linksList.innerHTML = '';
    resume.links.forEach((l, idx) => {
      const row = document.createElement('div'); row.className = 'link-row'; row.style.display = 'flex'; row.style.gap = '8px'; row.style.marginTop = '6px';
      const label = document.createElement('input'); label.placeholder = 'Label'; label.value = l.label || ''; label.style.flex = '1';
      label.addEventListener('input', () => { l.label = label.value; renderPreview(); });
      const url = document.createElement('input'); url.placeholder = 'URL'; url.value = l.url || ''; url.style.flex = '2';
      url.addEventListener('input', () => { l.url = url.value; renderPreview(); });
      const del = document.createElement('button'); del.className = 'btn ghost'; del.textContent = 'Remove'; del.addEventListener('click', () => { resume.links.splice(idx, 1); renderLinksUI(); renderPreview(); });
      row.appendChild(label); row.appendChild(url); row.appendChild(del);
      linksList.appendChild(row);
    });
  }
  if ($('addLinkBtn')) $('addLinkBtn').addEventListener('click', () => { resume.links.push({ label: '', url: '' }); renderLinksUI(); });

  // experience / education UI builders
  function makeExpItem(item, idx) {
    const wrap = document.createElement('div'); wrap.className = 'exp-item';
    wrap.style.border = '1px dashed #e6eef9'; wrap.style.padding = '8px'; wrap.style.borderRadius = '8px'; wrap.style.marginTop = '8px';
    const t = document.createElement('input'); t.placeholder = 'Job title'; t.value = item.title || ''; t.style.width = '100%';
    t.addEventListener('input', () => { item.title = t.value; renderPreview(); });
    const c = document.createElement('input'); c.placeholder = 'Company'; c.value = item.company || ''; c.style.width = '100%';
    c.addEventListener('input', () => { item.company = c.value; renderPreview(); });
    const dates = document.createElement('div'); dates.style.display = 'flex'; dates.style.gap = '8px'; dates.style.marginTop = '6px';
    const s = document.createElement('input'); s.placeholder = 'Start (e.g., 2022)'; s.value = item.start || ''; s.style.flex = '1';
    s.addEventListener('input', () => { item.start = s.value; renderPreview(); });
    const e = document.createElement('input'); e.placeholder = 'End (e.g., Present)'; e.value = item.end || ''; e.style.flex = '1';
    e.addEventListener('input', () => { item.end = e.value; renderPreview(); });
    const bullets = document.createElement('textarea'); bullets.placeholder = 'Bullets (one per line)'; bullets.value = (item.bullets || []).join('\n'); bullets.style.width = '100%'; bullets.style.marginTop = '6px';
    bullets.addEventListener('input', () => { item.bullets = bullets.value.split(/\r?\n/).map(x => x.trim()).filter(Boolean); renderPreview(); });
    const del = document.createElement('button'); del.className = 'btn ghost'; del.textContent = 'Remove'; del.style.marginTop = '6px';
    del.addEventListener('click', () => { resume.experience.splice(idx, 1); renderExperience(); renderPreview(); });
    dates.appendChild(s); dates.appendChild(e);
    wrap.appendChild(t); wrap.appendChild(c); wrap.appendChild(dates); wrap.appendChild(bullets); wrap.appendChild(del);
    return wrap;
  }
  function renderExperience() { const c = $('expList'); if (!c) return; c.innerHTML = ''; resume.experience.forEach((it, idx) => c.appendChild(makeExpItem(it, idx))); }
  if ($('addExp')) $('addExp').addEventListener('click', () => { resume.experience.push({ title: '', company: '', start: '', end: '', bullets: [] }); renderExperience(); });

  // education items (adds cgpa/grade field)
  function makeEduItem(item, idx) {
    const w = document.createElement('div'); w.style.border = '1px dashed #f0f0f0'; w.style.padding = '8px'; w.style.borderRadius = '8px'; w.style.marginTop = '8px';
    const deg = document.createElement('input'); deg.placeholder = 'Degree (B.Tech, etc.)'; deg.value = item.degree || ''; deg.style.width = '100%'; deg.addEventListener('input', () => { item.degree = deg.value; renderPreview(); });
    const school = document.createElement('input'); school.placeholder = 'School / College'; school.value = item.school || ''; school.style.width = '100%'; school.addEventListener('input', () => { item.school = school.value; renderPreview(); });
    const year = document.createElement('input'); year.placeholder = 'Year (e.g., 2022)'; year.value = item.year || ''; year.style.width = '100%'; year.style.marginTop = '6px'; year.addEventListener('input', () => { item.year = year.value; renderPreview(); });
    const grade = document.createElement('input'); grade.placeholder = 'CGPA / Grade (e.g., 7.4 / 70%)'; grade.value = item.grade || ''; grade.style.width = '100%'; grade.style.marginTop = '6px'; grade.addEventListener('input', () => { item.grade = grade.value; renderPreview(); });
    const del = document.createElement('button'); del.className = 'btn ghost'; del.textContent = 'Remove'; del.style.marginTop = '6px'; del.addEventListener('click', () => { resume.education.splice(idx, 1); renderEducation(); renderPreview(); });
    w.appendChild(deg); w.appendChild(school); w.appendChild(year); w.appendChild(grade); w.appendChild(del);
    return w;
  }
  function renderEducation() { const c = $('eduList'); if (!c) return; c.innerHTML = ''; resume.education.forEach((it, idx) => c.appendChild(makeEduItem(it, idx))); }
  if ($('addEdu')) $('addEdu').addEventListener('click', () => { resume.education.push({ degree: '', school: '', year: '', grade: '' }); renderEducation(); });

  // Render preview
  function renderPreview() {
    if ($('pr_name')) $('pr_name').innerText = resume.name || 'Candidate Name';
    // links
    const linksDiv = $('pr_links');
    if (linksDiv) {
      linksDiv.innerHTML = ''; resume.links.forEach(l => {
        if (l.label && l.url) {
          const a = document.createElement('a'); a.href = l.url; a.target = '_blank'; a.rel = 'noopener noreferrer'; a.textContent = l.label;
          a.style.marginRight = '8px'; linksDiv.appendChild(a);
        } else if (l.label) {
          const s = document.createElement('span'); s.textContent = l.label; s.style.marginRight = '8px'; linksDiv.appendChild(s);
        }
      });
    }

    const contactArr = [];
    if (resume.email) contactArr.push(resume.email);
    if (resume.phone) contactArr.push(resume.phone);
    if (resume.location) contactArr.push(resume.location);
    if ($('pr_contact')) $('pr_contact').innerText = contactArr.join(' • ') || 'email • phone • location';
    if ($('pr_summary')) $('pr_summary').innerText = resume.summary || 'Short summary will appear here';

    // Experience HTML
    const expHtml = resume.experience.length ? resume.experience.map(e => {
      const bullets = (e.bullets || []).map(b => `<li>${escapeHtml(b)}</li>`).slice(0, 5).join('');
      return `<div class="item"><div class="title">${escapeHtml(e.title || '')}</div><div class="meta">${escapeHtml(e.company || '')} ${escapeHtml(e.start || '')} ${escapeHtml(e.end || '')}</div>${bullets ? `<ul>${bullets}</ul>` : ''}</div>`;
    }).join('') : `<div class="item"><div class="title">No experience</div></div>`;

    const eduHtml = resume.education.length ? resume.education.map(ed => `<div class="item"><div class="title">${escapeHtml(ed.degree || '')}</div><div class="meta">${escapeHtml(ed.school || '')} ${escapeHtml(ed.year || '')} ${ed.grade ? ('• ' + escapeHtml(ed.grade)) : ''}</div></div>`).join('') : `<div class="item"><div class="title">No education added</div></div>`;

    const certHtml = resume.certifications && resume.certifications.length ? resume.certifications.map(c => `<div class="meta">${escapeHtml(c)}</div>`).join('') : `<div class="meta">No certifications</div>`;

    const skillsHtml = resume.skills.length ? resume.skills.map(s => `<span class="skill-pill">${escapeHtml(s)}</span>`).join('') : `<span class="skill-pill">Add skills</span>`;

    const addHtml = resume.additional ? `<div class="meta">${escapeHtml(resume.additional)}</div>` : `<div class="meta">Add projects or achievements</div>`;

    if ($('pr_body')) $('pr_body').innerHTML = `
      <div class="section"><h4>Career Objective</h4><div class="meta">${escapeHtml(resume.summary || 'Add a concise career objective or summary')}</div></div>
      <div class="section"><h4>Experience</h4>${expHtml}</div>
      <div class="section"><h4>Education</h4>${eduHtml}</div>
      <div class="section"><h4>Certifications</h4>${certHtml}</div>
      <div class="section"><h4>Skills</h4><div class="skills-list">${skillsHtml}</div></div>
      <div class="section"><h4>Additional</h4>${addHtml}</div>
    `;

    // template classes & color
    const pr = $('resumePreview');
    if (pr) pr.className = 'resume ' + (resume.template || 'clean');
    document.documentElement.style.setProperty('--accent', resume.color || '#0b66c3');

    // scale slightly if taller than page
    requestAnimationFrame(() => {
      if (!pr) return;
      pr.style.transform = '';
      const r = pr.getBoundingClientRect();
      if (r.height > 1120) pr.style.transform = 'scale(0.92)';
    });
  }

  // Export PDF
  function exportPDF() {
    const resumeEl = $('resumePreview');
    if (!resumeEl) return alert('No preview');
    const clone = resumeEl.cloneNode(true);
    clone.style.width = '210mm';
    clone.style.minHeight = '297mm';
    clone.style.padding = '12mm';
    clone.style.boxSizing = 'border-box';
    clone.style.background = '#fff';
    clone.style.color = '#111';
    const wrapper = document.createElement('div');
    wrapper.style.width = '210mm';
    wrapper.style.minHeight = '297mm';
    wrapper.style.boxSizing = 'border-box';
    wrapper.appendChild(clone);
    document.body.appendChild(wrapper);
    const opt = {
      margin: 0.4,
      filename: (resume.name || 'resume') + '.pdf',
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: { scale: 1.6, useCORS: true, letterRendering: true },
      jsPDF: { unit: 'in', format: 'a4', orientation: 'portrait' }
    };
    // html2pdf global required
    if (typeof html2pdf === 'undefined') { alert('html2pdf not loaded'); document.body.removeChild(wrapper); return; }
    html2pdf().set(opt).from(wrapper).save().then(() => document.body.removeChild(wrapper)).catch(err => { document.body.removeChild(wrapper); alert('PDF export failed: ' + err.message); });
  }

  if ($('downloadPdf')) $('downloadPdf').addEventListener('click', exportPDF);

  // Analysis & ATS scoring + suggestions (local heuristics)
  function runAnalysis() {
    const sugEl = $('sugList'); if (!sugEl) return;
    const suggestions = [];
    if (!resume.email) suggestions.push('Add an email address in the header.');
    if (!resume.phone) suggestions.push('Add a phone number for recruiters to call.');
    if (resume.experience.length === 0) suggestions.push('No experience detected — add internships, projects, or volunteer work.');

    const skillCount = resume.skills.length;
    if (skillCount < 4) suggestions.push('Add more relevant technical skills (aim for 6–12).');

    // bullets action verbs
    const actionVerbs = ['Led', 'Developed', 'Designed', 'Implemented', 'Built', 'Improved', 'Managed', 'Optimized', 'Automated', 'Created', 'Analyzed', 'Deployed', 'Tested', 'Refactored', 'Reduced', 'Increased', 'Spearheaded'];
    let verbsOk = 0, bulletsTotal = 0;
    resume.experience.forEach(e => (e.bullets || []).forEach(b => {
      bulletsTotal++;
      const first = (b || '').trim().split(/\s+/)[0].replace(/[.,:]/g, '');
      if (actionVerbs.map(v => v.toLowerCase()).includes(first.toLowerCase())) verbsOk++;
    }));
    if (bulletsTotal > 0 && (verbsOk / bulletsTotal) < 0.6) suggestions.push('Start bullets with strong action verbs (e.g., "Implemented", "Led").');

    // quantification
    let quantified = 0;
    resume.experience.forEach(e => (e.bullets || []).forEach(b => { if (/\d+%|₹\d+|\d+k|\d{2,}/.test(b)) quantified++; }));
    if (bulletsTotal > 0 && (quantified / bulletsTotal) < 0.3) suggestions.push('Add metrics where possible (e.g., "Reduced X by 40%").');

    // JD keyword match
    const jd = ($('jdKeywords') ? $('jdKeywords').value : '') || '';
    const jdArr = jd.toLowerCase().split(',').map(x => x.trim()).filter(Boolean);
    let jdMatches = 0;
    if (jdArr.length) {
      const content = (resume.summary + ' ' + resume.skills.join(' ') + ' ' + resume.additional + ' ' + resume.experience.map(e => e.title + ' ' + (e.bullets || []).join(' ')).join(' ')).toLowerCase();
      jdArr.forEach(k => { if (k && content.includes(k)) jdMatches++; });
      if (jdMatches < jdArr.length) suggestions.push('Not all JD keywords matched — consider adding missing keywords.');
    }

    // score simple model
    let score = 50;
    if (resume.email) score += 10;
    if (resume.phone) score += 10;
    score += Math.min(15, skillCount * 2);
    score += Math.min(15, verbsOk * 3);
    score += Math.min(10, quantified * 2);
    if (jdArr.length) score += Math.min(10, Math.round((jdMatches / jdArr.length) * 10));
    score = Math.max(0, Math.min(100, Math.round(score)));

    sugEl.innerHTML = suggestions.length ? '<ul>' + suggestions.map(s => `<li>${escapeHtml(s)}</li>`).join('') + '</ul>' : '<div class="small muted">Good — no major suggestions.</div>';
    if ($('scoreBadge')) $('scoreBadge').innerText = score + '%';
    if ($('meterBar')) $('meterBar').style.width = score + '%';
    if ($('scoreBreakdown')) $('scoreBreakdown').innerText = `Skills: ${skillCount}, Bullets: ${bulletsTotal}, ActionVerb%: ${bulletsTotal ? Math.round(verbsOk / bulletsTotal * 100) + '%' : '—'}`;

    // small "AI suggestion" — career objective
    if (!resume.summary && (resume.skills.length || ($('roleHint') && $('roleHint').value))) {
      const objective = suggestObjectiveText(($('roleHint') ? $('roleHint').value : '') || resume.skills.join(' '));
      const box = document.createElement('div'); box.style.marginTop = '8px';
      box.innerHTML = `<strong>AI suggestion (career objective)</strong><div style="margin-top:6px">${escapeHtml(objective)}</div><div style="margin-top:6px"><button id="applyObj" class="btn ghost">Apply</button></div>`;
      sugEl.appendChild(box);
      const btn = document.getElementById('applyObj');
      if (btn) btn.addEventListener('click', () => { resume.summary = objective; if ($('summaryTxt')) $('summaryTxt').value = objective; renderPreview(); runAnalysis(); });
    }
  }
  if ($('analyzeBtn')) $('analyzeBtn').addEventListener('click', runAnalysis);

  // simple objective generator based on skills/role
  function suggestObjectiveText(hint) {
    const words = (hint || '').split(/[\s,]+/).filter(Boolean);
    const primary = words[0] || 'software';
    const sk = resume.skills.slice(0, 5).join(', ') || 'software development';
    return `Motivated ${primary} professional with hands-on experience in ${sk}. Seeking a challenging role to contribute technical skills, deliver results, and grow within a collaborative team environment.`;
  }

  // parsing uploaded files - uses parseTextToModel (defined below)
  if ($('btnImport')) $('btnImport').addEventListener('click', () => { if ($('fileUpload')) $('fileUpload').click(); });
  if ($('fileUpload')) $('fileUpload').addEventListener('change', async (ev) => {
    const f = ev.target.files && ev.target.files[0]; if (!f) return;
    try {
      const name = (f.name || '').toLowerCase();
      if (name.endsWith('.txt')) {
        const txt = await f.text(); parseTextToModel(txt);
      } else if (name.endsWith('.pdf')) {
        if (typeof pdfjsLib === 'undefined') { alert('pdf.js not loaded'); return; }
        const ab = await f.arrayBuffer();
        const loading = pdfjsLib.getDocument({ data: ab });
        const pdf = await loading.promise;
        let text = '';
        for (let p = 1; p <= pdf.numPages; p++) {
          const page = await pdf.getPage(p);
          const content = await page.getTextContent();
          // join items - keep newline separators between items to improve heading detection
          const strings = content.items.map(i => i.str || '').join(' ');
          text += strings + '\n\n';
        }
        parseTextToModel(text);
      } else if (name.endsWith('.docx')) {
        if (typeof mammoth === 'undefined') { alert('mammoth not loaded'); return; }
        const ab = await f.arrayBuffer();
        const res = await mammoth.extractRawText({ arrayBuffer: ab });
        parseTextToModel(res.value);
      } else {
        alert('Unsupported file. Use .txt, .pdf, .docx');
      }
    } catch (err) {
      console.error(err);
      alert('File parse failed: ' + (err && err.message ? err.message : err));
    } finally {
      ev.target.value = ''; // reset
    }
  });

  // Improved parseTextToModel (frontend)
  function parseTextToModel(text) {
    if (!text) { alert('No text found'); return; }

    // 1) If text has few newlines and is very long, attempt to split into sentences for heuristics
    //    PDFs sometimes render as one long line. Insert breaks after periods followed by capital letter.
    if ((text.match(/\n/g) || []).length < 6) {
      text = text.replace(/([.?!])\s+(?=[A-Z0-9"']{1,})/g, "$1\n");
    }

    // normalize lines and remove excessive whitespace
    const rawLines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean);

    // helper regexes
    const emailRx = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i;
    const phoneRx = /(?:\+?\d{1,3}[\s-])?(?:\d{2,4}[\s-])?\d{6,12}/;
    const headingKeywords = [
      'experience', 'work experience', 'professional experience', 'education', 'skills',
      'certifications', 'certification', 'projects', 'summary', 'objective', 'career objective',
      'awards', 'achievements', 'additional', 'publications', 'internships'
    ];

    // 2) Extract immediate contact info from first 8 lines (name/email/phone)
    let name = '', email = null, phone = null, location = null;
    for (let i = 0; i < Math.min(10, rawLines.length); i++) {
      const ln = rawLines[i];
      if (!email && emailRx.test(ln)) email = (ln.match(emailRx) || [])[0];
      if (!phone && phoneRx.test(ln)) phone = (ln.match(phoneRx) || [])[0];
      // Heuristic: if line has comma-separated city/state or contains "District" or "City" treat as location
      if (!location && /city|district|province|state|ru|ap|india/i.test(ln) && ln.length < 80) location = ln;
      // candidate name: a short line with letters and maybe middle name but no '@' and no long punctuation
      if (!name && ln.length > 2 && ln.length < 60 && !/@/.test(ln) && /^[A-Za-z ,.'-]+$/.test(ln)) {
        name = ln;
      }
    }

    // 3) Build sections map using heading detection
    const sections = {};
    let cur = 'header'; sections[cur] = [];
    for (const line of rawLines) {
      const low = line.toLowerCase();

      // direct heading detection if line equals or starts with a keyword or line ends with ':' or all-caps short line
      const isColonHeading = /:\s*$/.test(line);
      const isAllCaps = /^[A-Z0-9\W]{3,}$/.test(line) && line.split(' ').length <= 6;
      let matchedKeyword = null;
      for (const kw of headingKeywords) {
        // matches "Skills", "Skills:" "Technical Skills" (if short)
        if (low === kw || low.startsWith(kw + ':') || (low.includes(kw) && low.length < 40)) { matchedKeyword = kw; break; }
      }

      if (matchedKeyword || isColonHeading || isAllCaps) {
        // normalize keyword -> canonical section key
        let key = 'other';
        if (matchedKeyword) {
          if (matchedKeyword.includes('experience')) key = 'experience';
          else if (matchedKeyword.includes('education')) key = 'education';
          else if (matchedKeyword.includes('skill')) key = 'skills';
          else if (matchedKeyword.includes('cert')) key = 'certifications';
          else if (matchedKeyword.includes('project')) key = 'projects';
          else if (matchedKeyword.includes('summary') || matchedKeyword.includes('objective')) key = 'summary';
          else key = matchedKeyword;
        } else if (isAllCaps) {
          const lw = line.toLowerCase();
          if (lw.includes('experience')) key = 'experience';
          else if (lw.includes('education')) key = 'education';
          else if (lw.includes('skills')) key = 'skills';
          else if (lw.includes('cert')) key = 'certifications';
          else key = 'other';
        } else {
          // colon-case: check the word before colon
          const t = line.replace(/:$/, '').toLowerCase();
          if (t.includes('experience')) key = 'experience';
          else if (t.includes('education')) key = 'education';
          else if (t.includes('skill')) key = 'skills';
          else if (t.includes('cert')) key = 'certifications';
          else if (t.includes('summary') || t.includes('objective')) key = 'summary';
          else key = 'other';
        }
        cur = key;
        sections[cur] = sections[cur] || [];
        continue; // skip heading itself
      }

      sections[cur] = sections[cur] || [];
      sections[cur].push(line);
    }

    // 4) Post-process sections into model fields
    resume.name = resume.name || name || '';
    if (email) resume.email = email;
    if (phone) resume.phone = phone;
    if (location) resume.location = location;

    // summary / career objective: prefer explicit summary section, else use header 2-3 lines
    const summaryBlock = (sections.summary && sections.summary.join(' ')) || '';
    resume.summary = summaryBlock || (sections.header ? (sections.header.slice(0, 3).join(' ')) : resume.summary);

    // skills: split by common separators
    const skillsText = (sections.skills && sections.skills.join(' ')) || '';
    let skillsArr = skillsText ? skillsText.split(/[,;•\|\n\/]+/).map(s => s.trim()).filter(Boolean) : [];
    if (skillsArr.length === 0) {
      // fallback: scan header for tech tokens
      const headerText = (sections.header || []).join(' ').toLowerCase();
      const techTokens = ['java', 'spring', 'react', 'python', 'sql', 'javascript', 'html', 'css', 'mysql', 'mongodb', 'aws', 'docker', 'nlp', 'machine learning', 'sap', 'streamlit'];
      techTokens.forEach(t => { if (headerText.includes(t) && !skillsArr.includes(t)) skillsArr.push(t); });
    }
    resume.skills = skillsArr;

    // certifications
    resume.certifications = (sections.certifications ? sections.certifications.join('\n').split(/[\n;,-]+/).map(s => s.trim()).filter(Boolean) : []);

    // education: keep each line as degree entry; try detect CGPA
    resume.education = (sections.education || []).map(line => {
      // parse possible "B.Tech ... CGPA: 7.4 /10" small heuristic
      const gradeMatch = line.match(/(cgpa[:\s]*\d+(\.\d+)?|gpa[:\s]*\d+(\.\d+)?|\d+\.\d+\/10|\d+%)/i);
      const obj = { degree: line, school: '', year: '', grade: (gradeMatch ? gradeMatch[0] : '') };
      return obj;
    });

    // experience: build title/company/bullets by splitting paragraph blocks
    const expArr = (sections.experience && sections.experience.join('\n\n').split(/\n{1,2}/).map(s => s.trim()).filter(Boolean)) || [];
    resume.experience = expArr.map(block => {
      const lines = block.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
      let title = lines[0] || '';
      let company = '';
      // split title by " at " or " - "
      const p = title.split(/\s+at\s+|\s+-\s+|\s+\|\s+/i);
      if (p.length > 1) { title = p[0].trim(); company = p.slice(1).join(' | ').trim(); }
      const bullets = lines.slice(1).map(l => l.replace(/^[-•\s]+/, '').trim()).filter(Boolean);
      return { title, company, start: '', end: '', bullets };
    });

    // projects/additional
    resume.additional = (sections.projects ? sections.projects.join('\n') : '') + '\n' + (sections.other ? sections.other.join('\n') : '');
    resume.additional = (resume.additional || '').trim();

    // push to UI
    pushToUI();
    renderExperience();
    renderEducation();
    renderPreview();
    showStep(1);
    alert('Parsed file — review fields and click Analyze for suggestions.');
  }

  // push model -> UI
  function pushToUI() {
    if ($('name')) $('name').value = resume.name || '';
    if ($('email')) $('email').value = resume.email || '';
    if ($('phone')) $('phone').value = resume.phone || '';
    if ($('location')) $('location').value = resume.location || '';
    if ($('studentRoll')) $('studentRoll').value = resume.roll || '';
    if ($('skillsTxt')) $('skillsTxt').value = (resume.skills || []).join(', ');
    if ($('summaryTxt')) $('summaryTxt').value = resume.summary || '';
    if ($('additionalTxt')) $('additionalTxt').value = resume.additional || '';
    if ($('certTxt')) $('certTxt').value = (resume.certifications || []).join('\n');
    renderLinksUI();
    document.querySelectorAll('.template-thumb').forEach(t => t.style.borderColor = (t.dataset.tmpl === resume.template ? 'var(--accent)' : '#ddd'));
    document.querySelectorAll('.color-dot').forEach(c => c.style.outline = (c.dataset.color === resume.color ? '3px solid rgba(0,0,0,0.06)' : 'none'));
  }

  // initial wiring
  if (loadLocal()) { pushToUI(); renderExperience(); renderEducation(); renderPreview(); }
  renderPreview();

  // keyboard shortcut
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && e.ctrlKey) { runAnalysis(); }
  });

  // expose for debug
  window.resumeBuilder = { resume, renderPreview, runAnalysis, parseTextToModel, saveLocal, loadLocal };

})();
