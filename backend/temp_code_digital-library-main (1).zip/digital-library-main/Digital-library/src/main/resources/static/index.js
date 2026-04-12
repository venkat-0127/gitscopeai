/* index.js (updated)
   Navigation helpers, admin login, OTP send/verify, header scroll,
   reCAPTCHA render + Enter key behavior, mobile menu toggle, footer-year filler.
*/

/* ---------------------
   BACKEND base URL
   --------------------- */
const LIVE_BACKEND = 'https://digital-library-4bne.onrender.com/'; // <-- CHANGE THIS

const BACKEND_BASE = (function () {
  const host = location.hostname;

  // Local development (backend runs on 8083)
  if (host === 'localhost' || host === '127.0.0.1') {
    return 'http://localhost:8083';
  }

  // If a global override is provided (optional)
  if (typeof window !== 'undefined' && window.__BACKEND__) {
    return window.__BACKEND__;
  }

  // Deployed on HTTPS: always call your backend service URL (must be HTTPS)
  return LIVE_BACKEND;
})();

console.log('[LMS] Using BACKEND_BASE =', BACKEND_BASE);

/* ---------------------
   reCAPTCHA state
   --------------------- */
let facultyCaptchaId = null;
let studentCaptchaId = null;

// Called by reCAPTCHA onload script (ensure your script uses ?onload=onRecaptchaLoaded&render=explicit)
window.onRecaptchaLoaded = function onRecaptchaLoaded() {
  const SITE_KEY = '6LftStMrAAAAAETwfdEDOKLnNL6IAnBXS-symvIU'; // replace with your site key
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
    const res = await fetch(`${BACKEND_BASE}/api/admin/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p }),
      credentials: 'include',
    });

    const ct = res.headers.get('content-type') || '';
    let data = null;
    if (ct.includes('application/json')) {
      data = await res.json();
    } else {
      const t = await res.text();
      throw new Error('Unexpected response: ' + t);
    }

    // success branch
    if (data && data.status === 'success') {
      // backend sends: username, name, role, accessModules, mustChangePassword
      const roleFromServer = (data.role || 'ADMIN').toString().toUpperCase();

      // store info for admin dashboard
      localStorage.setItem('role', roleFromServer);              // e.g. SUPER_ADMIN, ADMIN, LIBRARIAN
      localStorage.setItem('email', data.username || u);         // login id
      localStorage.setItem('name', data.name || '');             // optional
      localStorage.setItem('loggedConfirmed', 'true');

      window.location.href = '/admin';
    } else {
      // failed
      if (errEl) {
        errEl.textContent = (data && data.message)
          ? data.message
          : 'Invalid credentials or account inactive';
      }
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

/* Improved validation:
   - Faculty: must be name.lingayas@limat.edu.in (local part should not be a roll-like pattern).
   - Student: retains roll-like checks (local contains 'na' and digits).
*/
function facultyEmailValid(email) {
  if (!email) return false;
  const domain = '.lingayas@limat.edu.in';
  if (!email.endsWith(domain)) return false;
  const local = email.split('@')[0] || '';

  // Reject if local part begins with a digit (common in roll numbers like 22...)
  if (/^\d/.test(local)) return false;

  // Reject if local resembles student roll (contains 'na' and digits)
  if (/na/i.test(local) && /\d/.test(local)) return false;

  // Ensure local contains at least one letter and allowed characters
  if (!/[A-Za-z]/.test(local)) return false;
  if (!/^[A-Za-z0-9._%+-]+$/.test(local)) return false;

  // All checks passed -> valid faculty email (name-like)
  return true;
}

function studentEmailValid(email) {
  if (!email) return false;
  const domain = '.lingayas@limat.edu.in';
  if (!email.endsWith(domain)) return false;
  const local = (email.split('@')[0] || '').toLowerCase();
  // Keep student's pattern: local contains 'na' and some digits (typical rolllike)
  return /na/i.test(local) && /[0-9]/.test(local);
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

    const res = await fetch(`${BACKEND_BASE}/api/otp/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
      credentials: 'include',
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
    const res = await fetch(`${BACKEND_BASE}/api/otp/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'email=' + encodeURIComponent(email) + '&otp=' + encodeURIComponent(otp),
      credentials: 'include',
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
    const res = await fetch(`${BACKEND_BASE}/api/otp/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'email=' + encodeURIComponent(email) + '&otp=' + encodeURIComponent(otp),
      credentials: 'include',
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

/* ----- Mobile menu toggle (integrates with CSS) ----- */
(function initMobileMenu() {
  // runs on DOMContentLoaded below; define here so functions exist earlier
  window._initMobileMenuOnce = function () {
    const toggle = document.querySelector('.menu-toggle');
    const mobileMenu = document.querySelector('.mobile-menu');

    if (!toggle || !mobileMenu) return;

    // set initial ARIA states
    toggle.setAttribute('aria-expanded', 'false');
    mobileMenu.setAttribute('aria-hidden', 'true');

    // open/close handler
    toggle.addEventListener('click', function (e) {
      const expanded = this.getAttribute('aria-expanded') === 'true';
      this.setAttribute('aria-expanded', String(!expanded));
      mobileMenu.setAttribute('aria-hidden', String(expanded)); // note: flip value
      if (!expanded) {
        // open -> focus first item
        const first = mobileMenu.querySelector('.menu-item');
        if (first) first.focus();
      } else {
        // closed -> return focus
        toggle.focus();
      }
    });

    // close on outside click
    document.addEventListener('click', function (e) {
      const isOpen = mobileMenu.getAttribute('aria-hidden') === 'false';
      if (!isOpen) return;
      if (!mobileMenu.contains(e.target) && !toggle.contains(e.target)) {
        toggle.setAttribute('aria-expanded', 'false');
        mobileMenu.setAttribute('aria-hidden', 'true');
      }
    });

    // close on Escape
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && mobileMenu.getAttribute('aria-hidden') === 'false') {
        toggle.setAttribute('aria-expanded', 'false');
        mobileMenu.setAttribute('aria-hidden', 'true');
        toggle.focus();
      }
    });

    // if a menu item is clicked, close menu (and allow navigation)
    mobileMenu.addEventListener('click', function (e) {
      const target = e.target;
      if (target && target.classList && target.classList.contains('menu-item')) {
        toggle.setAttribute('aria-expanded', 'false');
        mobileMenu.setAttribute('aria-hidden', 'true');
        // navigation handled by onclick attributes or hrefs on the button
      }
    });
  };
})();

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

    // header (supports element id 'siteHeader' or class 'header')
    const header = document.getElementById('siteHeader') || document.querySelector('.header');
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

    // initialize mobile menu (if present)
    if (typeof window._initMobileMenuOnce === 'function') {
      try { window._initMobileMenuOnce(); } catch (e) { console.warn('Mobile menu init failed', e); }
    }
  });
})();
