import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function AuthPage() {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login, register } = useAuth();
  const navigate = useNavigate();

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
      navigate('/');
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

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <h1 className="auth-logo">
              <span className="gradient-text">NutriStore</span>
            </h1>
            <p className="auth-subtitle">
              {mode === 'login'
                ? 'Увійдіть до свого акаунту'
                : 'Створіть новий акаунт'}
            </p>
          </div>

          <div className="auth-toggle">
            <button
              id="toggle-login"
              className={`auth-toggle-btn ${mode === 'login' ? 'active' : ''}`}
              onClick={() => switchMode('login')}
            >
              Вхід
            </button>
            <button
              id="toggle-register"
              className={`auth-toggle-btn ${mode === 'register' ? 'active' : ''}`}
              onClick={() => switchMode('register')}
            >
              Реєстрація
            </button>
          </div>

          {error && <div className="auth-error">{error}</div>}

          <form className="auth-form" onSubmit={handleSubmit}>
            {mode === 'register' && (
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="firstName">
                    Ім'я
                  </label>
                  <input
                    id="firstName"
                    className="form-input"
                    type="text"
                    placeholder="Іван"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="lastName">
                    Прізвище
                  </label>
                  <input
                    id="lastName"
                    className="form-input"
                    type="text"
                    placeholder="Петренко"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                  />
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                className="form-input"
                type="email"
                placeholder="name@example.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">
                Пароль
              </label>
              <input
                id="password"
                className="form-input"
                type="password"
                placeholder="Мінімум 6 символів"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              id="submit-auth"
              type="submit"
              className="auth-submit"
              disabled={loading}
            >
              <span>
                {loading
                  ? 'Зачекайте...'
                  : mode === 'login'
                  ? 'Увійти'
                  : 'Зареєструватись'}
              </span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
