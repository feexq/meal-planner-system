import { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function AuthPage() {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Pick up redirect path from state or query parameter
  const queryParams = new URLSearchParams(location.search);
  const redirectParam = queryParams.get('redirect') || location.state?.redirect || '/';

  // Form state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');

  const resetForm = () => {
    setEmail('');
    setPassword('');
    setFirstName('');
    setLastName('');
    setError('');
  };

  const switchMode = (newMode) => {
    setMode(newMode);
    resetForm();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register({ email, password, firstName, lastName });
      }
      navigate(redirectParam);
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        (mode === 'login'
          ? 'Невірний email або пароль'
          : 'Помилка реєстрації. Спробуйте ще раз.');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    if (redirectParam !== '/') localStorage.setItem('postLoginRedirect', redirectParam);
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleGithubLogin = () => {
    if (redirectParam !== '/') localStorage.setItem('postLoginRedirect', redirectParam);
    window.location.href = 'http://localhost:8080/oauth2/authorization/github';
  };

  return (
    <div className="auth-page">
      <div className="auth-container">

        {/* Banner Section */}
        <div className="auth-banner">
          <Link to="/" className="brand-logo">FoodMart</Link>

          <div className="banner-content">
            <h1>Твій розумний підхід до їжі</h1>
            <p>Створюй персоналізовані раціони, відстежуй макронутрієнти та купуй необхідні продукти в одному місці.</p>

            <ul className="feature-list">
              <li>ШІ-генерація раціону під твої цілі</li>
              <li>Врахування дієтичних потреб (Vegan, Keto тощо)</li>
              <li>Автоматичне формування кошика для покупок</li>
            </ul>
          </div>
        </div>

        {/* Form Section */}
        <div className="auth-form-wrapper">
          <div className="auth-header">
            <h2>{mode === 'login' ? 'З поверненням!' : 'Створити акаунт'}</h2>
            <p>
              {mode === 'login'
                ? 'Увійдіть, щоб продовжити роботу з раціоном'
                : 'Почніть свій шлях до здорового харчування'}
            </p>
          </div>

          <div className="toggle-container">
            <div
              className="toggle-slider"
              style={{ transform: mode === 'register' ? 'translateX(100%)' : 'translateX(0)' }}
            ></div>
            <button
              className={`toggle-btn ${mode === 'login' ? 'active' : ''}`}
              onClick={() => switchMode('login')}
              type="button"
            >
              Вхід
            </button>
            <button
              className={`toggle-btn ${mode === 'register' ? 'active' : ''}`}
              onClick={() => switchMode('register')}
              type="button"
            >
              Реєстрація
            </button>
          </div>

          {error && <div className="auth-error">{error}</div>}

          {/* Login Form */}
          <form
            className={`auth-form ${mode === 'login' ? 'active' : ''}`}
            onSubmit={handleSubmit}
          >
            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                placeholder="mail@example.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Пароль</label>
              <input
                type="password"
                placeholder="••••••••"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="form-options">
              <label className="checkbox-label">
                <input type="checkbox" /> Запам'ятати мене
              </label>
              <Link to="/forgot-password" className="forgot-password">Забули пароль?</Link>
            </div>

            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? 'Зачекайте...' : 'Увійти'}
            </button>
          </form>

          {/* Register Form */}
          <form
            className={`auth-form ${mode === 'register' ? 'active' : ''}`}
            onSubmit={handleSubmit}
          >
            <div className="form-group" style={{ display: 'flex', gap: '16px' }}>
              <div style={{ flex: 1 }}>
                <label>Ім'я</label>
                <input
                  type="text"
                  placeholder="Іван"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label>Прізвище</label>
                <input
                  type="text"
                  placeholder="Франко"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                placeholder="mail@example.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Пароль</label>
              <input
                type="password"
                placeholder="Мінімум 8 символів"
                required
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="form-options">
              <label className="checkbox-label">
                <input type="checkbox" required />
                <span>Я погоджуюсь з <Link to="/terms" style={{ color: 'var(--primary)' }}>правилами</Link></span>
              </label>
            </div>

            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? 'Зачекайте...' : 'Створити акаунт'}
            </button>
          </form>

          <div className="social-login">
            <p>Або продовжити через</p>
            <div className="social-btns">
              <button className="btn-social" type="button" onClick={handleGoogleLogin}>
                <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" /><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" /><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" /><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" /><path d="M1 1h22v22H1z" fill="none" /></svg>
                Google
              </button>
              <button className="btn-social" type="button" onClick={handleGithubLogin}>
                <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.43 9.79 8.2 11.38.6.11.82-.26.82-.58 0-.28-.01-1.02-.01-2.01-3.35.73-4.05-1.62-4.05-1.62-.55-1.38-1.35-1.75-1.35-1.75-1.1-.75.08-.74.08-.74 1.22.08 1.87 1.25 1.87 1.25 1.09 1.86 2.86 1.33 3.56.99.11-.77.42-1.33.76-1.64-2.67-.3-5.47-1.33-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.3-.54-1.52.12-3.18 0 0 1-.32 3.34 1.23.97-.27 2.01-.4 3.05-.4.99 0 2.03.13 3.05.4 2.34-1.55 3.34-1.23 3.34-1.23.66 1.66.24 2.88.12 3.18.77.84 1.23 1.91 1.23 3.22 0 4.61-2.8 5.62-5.48 5.92.43.37.81 1.09.81 2.22 0 1.61-.01 2.91-.01 3.31 0 .32.22.69.82.58C20.56 21.79 24 17.31 24 12c0-6.63-5.37-12-12-12z" fill="#24292F" /></svg>
                GitHub
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
