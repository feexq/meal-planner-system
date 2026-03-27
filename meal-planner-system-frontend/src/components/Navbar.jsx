import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import './Navbar.css';

export default function Navbar({ cartCount = 0 }) {
  const { user, isAuthenticated, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClick(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleLogout = async () => {
    setDropdownOpen(false);
    await logout();
  };

  const initials = user
    ? `${(user.firstName || user.email || '?')[0]}`.toUpperCase()
    : '?';

  return (
    <nav className="navbar" id="main-navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="navbar-brand-icon">🥗</span>
          <span className="gradient-text">NutriStore</span>
        </Link>

        <div className="navbar-actions">
          <Link to="/cart" className="navbar-cart" id="cart-button">
            <span className="navbar-cart-icon">🛒</span>
            {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
          </Link>

          {isAuthenticated ? (
            <div ref={dropdownRef} style={{ position: 'relative' }}>
              <button
                className="navbar-user"
                id="user-button"
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                <div className="user-avatar">{initials}</div>
                <span>{user?.firstName || user?.email}</span>
              </button>

              {dropdownOpen && (
                <div className="user-dropdown">
                  <Link
                    to="/admin/ingredients"
                    className="user-dropdown-btn"
                    onClick={() => setDropdownOpen(false)}
                  >
                    ⚙️ Інгредієнти
                  </Link>
                  <Link
                    to="/admin/categories"
                    className="user-dropdown-btn"
                    onClick={() => setDropdownOpen(false)}
                  >
                    📂 Категорії
                  </Link>
                  <button className="user-dropdown-btn danger" onClick={handleLogout}>
                    Вийти
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/auth" className="navbar-user" id="login-link">
              Увійти
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}
