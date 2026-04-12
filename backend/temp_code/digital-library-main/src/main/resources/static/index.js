/* index.js
   Navigation helpers, admin login, OTP send/verify, header scroll,
   reCAPTCHA render + Enter key behavior, footer-year filler.
*/

/* ---------------------
   reCAPTCHA state
   --------------------- */
let facultyCaptchaId = null;
let studentCaptchaId = null;

// This function will be called by the reCAPTCHA script once it's loaded.
// Make sure the reCAPTCHA <script> includes ?onload=onRecaptchaLoaded&render=explicit
window.onRecaptchaLoaded = function onRecaptchaLoaded() {
  // Replace YOUR_SITE_KEY_HERE with your actual site key from Google reCAPTCHA admin
  const SITE_KEY = '6LftStMrAAAAAETwfdEDOKLnNL6IAnBXS-symvIU';
  try {
    if (window.grecaptcha && typeof grecaptcha.render === 'function') {
      if (document.getElementById('faculty-captcha')) {
        facultyCaptchaId = grecaptcha.render('faculty-captcha', { sitekey: SITE_KEY });
      }
      if (document.getElementById('student-captcha')) {
        studentCaptchaId = grecaptcha.render('student-captcha', { sitekey: SITE_KEY });
      }
    }
  } catch (e) {
    console.warn('reCAPTCHA render error', e);
  }
};

/* ----- Navigation / UI helpers ----- */
function showHome() {
  const home = document.getElementById('homeContent');
  if (home) home.style.display = 'block';

  ['adminPanel', 'facultyPanel', 'studentPanel'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.setAttribute('aria-hidden', 'true');
  });

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function showPanel(role) {
  const home = document.getElementById('homeContent');
  if (home) home.style.display = 'none';

  ['adminPanel', 'facultyPanel', 'studentPanel'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.setAttribute('aria-hidden', 'true');
  });

  if (role === 'admin') {
    const el = document.getElementById('adminPanel');
    if (el) el.setAttribute('aria-hidden', 'false');
    setTimeout(() => document.getElementById('adminUser')?.focus(), 180);
  }
  if (role === 'faculty') {
    const el = document.getElementById('facultyPanel');
    if (el) el.setAttribute('aria-hidden', 'false');
    setTimeout(() => document.getElementById('facultyEmail')?.focus(), 180);
  }
  if (role === 'student') {
    const el = document.getElementById('studentPanel');
    if (el) el.setAttribute('aria-hidden', 'false');
    setTimeout(() => document.getElementById('studentEmail')?.focus(), 180);
  }

  setTimeout(() => {
    const el = document.querySelector('.panel[aria-hidden="false"]');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, 80);
}

/* ----- ADMIN login ----- */
async function adminLogin() {
  const uEl = document.getElementById('adminUser');
  const pEl = document.getElementById('adminPass');
  const errEl = document.getElementById('adminErr');

  const u = (uEl && uEl.value) ? uEl.value.trim() : '';
  const p = (pEl && pEl.value) ? pEl.value.trim() : '';
  if (errEl) errEl.textContent = '';

  if (!u || !p) {
    if (errEl) errEl.textContent = 'Enter username and password';
    return;
  }

  try {
    const res = await fetch('http://localhost:8083/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p }),
    });

    const ct = res.headers.get('content-type') || '';
    let data = null;
    if (ct.includes('application/json')) data = await res.json();
    else {
      const t = await res.text();
      throw new Error('Unexpected response: ' + t);
    }

    if (data && (data.status === 'success' || data.auth === true || data.role === 'admin')) {
      localStorage.setItem('role', 'admin');
      localStorage.setItem('email', data.email || u);
      localStorage.setItem('loggedConfirmed', 'true');
      window.location.href = '/admin';
    } else {
      if (errEl) errEl.textContent = data?.message || 'Invalid credentials';
    }
  } catch (err) {
    console.error(err);
    if (errEl) errEl.textContent = 'Server error. Check backend.';
  }
}

/* ----- OTP helpers & validation ----- */
let facultyTimer = null, facultyTime = 60;
let studentTimer = null, studentTime = 60;

function facultyCountdown(btn) {
  if (!btn) return;
  if (facultyTimer) { clearInterval(facultyTimer); facultyTimer = null; facultyTime = 60; }
  btn.disabled = true;
  btn.textContent = `Resend OTP in ${facultyTime}s`;
  facultyTimer = setInterval(() => {
    facultyTime--;
    btn.textContent = `Resend OTP in ${facultyTime}s`;
    if (facultyTime <= 0) {
      clearInterval(facultyTimer);
      facultyTimer = null;
      facultyTime = 60;
      btn.disabled = false;
      btn.textContent = 'Send OTP';
    }
  }, 1000);
}

function studentCountdown(btn) {
  if (!btn) return;
  if (studentTimer) { clearInterval(studentTimer); studentTimer = null; studentTime = 60; }
  btn.disabled = true;
  btn.textContent = `Resend OTP in ${studentTime}s`;
  studentTimer = setInterval(() => {
    studentTime--;
    btn.textContent = `Resend OTP in ${studentTime}s`;
    if (studentTime <= 0) {
      clearInterval(studentTimer);
      studentTimer = null;
      studentTime = 60;
      btn.disabled = false;
      btn.textContent = 'Send OTP';
    }
  }, 1000);
}

function facultyEmailValid(email) {
  if (!email) return false;
  return email.endsWith('.lingayas@limat.edu.in') && /^[a-zA-Z0-9._%+-]+$/.test(email.split('@')[0]);
}
function studentEmailValid(email) {
  if (!email) return false;
  if (!email.endsWith('.lingayas@limat.edu.in')) return false;
  const local = email.split('@')[0].toLowerCase();
  return /na/.test(local) && /[0-9]/.test(local);
}

/* Shared OTP sender; accepts captchaWidgetId to fetch token */
async function sendOtpFor({ emailElId, btnId, statusElId, validateFn, captchaWidgetId }) {
  const emailEl = document.getElementById(emailElId);
  const email = emailEl ? emailEl.value.trim() : '';
  const status = document.getElementById(statusElId);
  if (status) status.textContent = '';
  if (!validateFn(email)) {
    if (status) status.textContent = 'Invalid email format.';
    return;
  }

  const btn = document.getElementById(btnId);
  const prevText = btn ? btn.textContent : 'Send OTP';
  if (btn) { btn.disabled = true; btn.textContent = 'Sending...'; }

  // get captcha token (if widget id provided)
  let captchaToken = '';
  try {
    if (typeof grecaptcha !== 'undefined' && captchaWidgetId !== null && captchaWidgetId !== undefined) {
      captchaToken = grecaptcha.getResponse(captchaWidgetId);
      if (!captchaToken) {
        if (status) status.textContent = 'Please complete the CAPTCHA';
        if (btn) { btn.disabled = false; btn.textContent = prevText; }
        return;
      }
    }
  } catch (e) {
    console.warn('reCAPTCHA check error', e);
  }

  try {
    const body = new URLSearchParams();
    body.append('email', email);
    if (captchaToken) body.append('g-recaptcha-response', captchaToken);

    const res = await fetch('http://localhost:8083/api/otp/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });

    const ct = res.headers.get('content-type') || '';
    let data = null;
    if (ct.includes('application/json')) data = await res.json();
    else {
      const t = await res.text();
      throw new Error('Unexpected response: ' + t);
    }

    if (res.ok && data && data.status === 'success') {
      if (status) status.textContent = 'OTP sent — check your email.';
      if (btnId === 'facultySend') facultyCountdown(btn);
      if (btnId === 'studentSend') studentCountdown(btn);
      try { if (captchaWidgetId !== null && typeof grecaptcha !== 'undefined') grecaptcha.reset(captchaWidgetId); } catch (e) { /* ignore */ }
      alert('✅ OTP sent to your email.');
    } else {
      const msg = (data && data.message) ? data.message : 'Failed to send OTP';
      if (status) status.textContent = '❌ ' + msg;
      if (btn) { btn.disabled = false; btn.textContent = prevText; }
      try { if (captchaWidgetId !== null && typeof grecaptcha !== 'undefined') grecaptcha.reset(captchaWidgetId); } catch (e) { /* ignore */ }
    }
  } catch (err) {
    console.error('Error sending OTP:', err);
    if (status) status.textContent = 'Server error while sending OTP';
    if (btn) { btn.disabled = false; btn.textContent = prevText; }
    try { if (captchaWidgetId !== null && typeof grecaptcha !== 'undefined') grecaptcha.reset(captchaWidgetId); } catch (e) { /* ignore */ }
  }
}

function sendFacultyOtp() { sendOtpFor({ emailElId: 'facultyEmail', btnId: 'facultySend', statusElId: 'facultyStatus', validateFn: facultyEmailValid, captchaWidgetId: facultyCaptchaId }); }
function sendStudentOtp() { sendOtpFor({ emailElId: 'studentEmail', btnId: 'studentSend', statusElId: 'studentStatus', validateFn: studentEmailValid, captchaWidgetId: studentCaptchaId }); }

async function verifyFacultyOtp() {
  const email = document.getElementById('facultyEmail')?.value.trim();
  const otp = document.getElementById('facultyOtp')?.value.trim();
  const status = document.getElementById('facultyStatus'); if (status) status.textContent = '';
  if (!otp) { if (status) status.textContent = 'Enter OTP'; return; }
  try {
    const res = await fetch('http://localhost:8083/api/otp/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'email=' + encodeURIComponent(email) + '&otp=' + encodeURIComponent(otp),
    });
    const ct = res.headers.get('content-type') || '';
    let data = null;
    if (ct.includes('application/json')) data = await res.json();
    else { const t = await res.text(); throw new Error('Unexpected response: ' + t); }
    if (data && data.status === 'success') {
      if (status) status.textContent = 'Verified ✅';
      localStorage.setItem('role', 'faculty'); localStorage.setItem('email', email); localStorage.setItem('loggedConfirmed', 'true');
      window.location.href = '/tiles';
    } else {
      if (status) status.textContent = 'Invalid OTP ❌';
    }
  } catch (e) {
    console.error(e); if (status) status.textContent = 'Server error';
  }
}

async function verifyStudentOtp() {
  const email = document.getElementById('studentEmail')?.value.trim();
  const otp = document.getElementById('studentOtp')?.value.trim();
  const status = document.getElementById('studentStatus'); if (status) status.textContent = '';
  if (!otp) { if (status) status.textContent = 'Enter OTP'; return; }
  try {
    const res = await fetch('http://localhost:8083/api/otp/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'email=' + encodeURIComponent(email) + '&otp=' + encodeURIComponent(otp),
    });
    const ct = res.headers.get('content-type') || '';
    let data = null;
    if (ct.includes('application/json')) data = await res.json();
    else { const t = await res.text(); throw new Error('Unexpected response: ' + t); }
    if (data && data.status === 'success') {
      if (status) status.textContent = 'Verified ✅';
      localStorage.setItem('role', 'student'); localStorage.setItem('email', email); localStorage.setItem('loggedConfirmed', 'true');
      window.location.href = '/tiles';
    } else {
      if (status) status.textContent = 'Invalid OTP ❌';
    }
  } catch (e) {
    console.error(e); if (status) status.textContent = 'Server error';
  }
}

/* ----- UI setup: header scroll, enter handlers, footer year ----- */
(function initUI() {
  document.addEventListener('DOMContentLoaded', function () {
    const storedRole = localStorage.getItem('role');
    const confirmed = localStorage.getItem('loggedConfirmed') === 'true';
    if (storedRole && confirmed && ['admin', 'faculty', 'student'].includes(storedRole)) {
      document.body.classList.add('logged-in');
    } else {
      document.body.classList.remove('logged-in');
    }

    const header = document.getElementById('siteHeader');
    function onScroll() { if (header) { if (window.scrollY > 30) header.classList.add('scrolled'); else header.classList.remove('scrolled'); } }
    window.addEventListener('scroll', onScroll);
    onScroll();

    window.__toggleTheme = () => document.body.classList.toggle('logged-in');

    // initially show home
    showHome();

    // footer year
    const fy = document.getElementById('footer-year');
    if (fy) fy.textContent = new Date().getFullYear();

    /* --- Enter key bindings --- */
    // faculty email -> send OTP
    const facultyEmail = document.getElementById('facultyEmail');
    if (facultyEmail) facultyEmail.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); sendFacultyOtp(); }
    });

    // faculty OTP -> verify
    const facultyOtp = document.getElementById('facultyOtp');
    if (facultyOtp) facultyOtp.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); verifyFacultyOtp(); }
    });

    // student email -> send OTP
    const studentEmail = document.getElementById('studentEmail');
    if (studentEmail) studentEmail.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); sendStudentOtp(); }
    });

    // student OTP -> verify
    const studentOtp = document.getElementById('studentOtp');
    if (studentOtp) studentOtp.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); verifyStudentOtp(); }
    });

    // admin password: Enter triggers adminLogin
    const adminPass = document.getElementById('adminPass');
    if (adminPass) adminPass.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); adminLogin(); }
    });
  });
})();
