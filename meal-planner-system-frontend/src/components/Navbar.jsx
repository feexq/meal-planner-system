import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { categoriesAPI, preferencesAPI, cartAPI } from '../api/api';
import GlobalSearch from './GlobalSearch';
import './Navbar.css';

export default function Navbar({ cartCount = 0 }) {
  const { user, isAuthenticated, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const navigate = useNavigate();

  // Читаємо роль з JWT
  const isAdmin = (() => {
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) return false;
      const payload = JSON.parse(atob(token.split('.')[1]));
      const authorities = payload.role || payload.authorities || [];
      return Array.isArray(authorities)
        ? authorities.includes('ADMIN')
        : authorities === 'ADMIN';
    } catch { return false; }
  })();

  const [internalCart, setInternalCart] = useState(cartCount);
  const [hasSurvey, setHasSurvey] = useState(false);

  useEffect(() => {
    setInternalCart(cartCount);
  }, [cartCount]);

  useEffect(() => {
    const fetchCartCount = async () => {
      try {
        const res = await cartAPI.getCart();
        if (res && res.data) {
          setInternalCart(res.data.totalItems || 0);
        }
      } catch (e) {

      }
    };

    fetchCartCount();
    window.addEventListener('cartUpdated', fetchCartCount);
    return () => window.removeEventListener('cartUpdated', fetchCartCount);
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      preferencesAPI.get().then(res => {
        if (res.data) setHasSurvey(true);
      }).catch(e => {
        if (e.response?.status !== 404) console.error("Pref fetch error", e);
      });
    } else {
      setHasSurvey(false);
    }
  }, [isAuthenticated]);


  const [categories, setCategories] = useState([]);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [hoveredRoot, setHoveredRoot] = useState(null);

  useEffect(() => {
    let mounted = true;
    categoriesAPI.getAll().then(res => {
      if (mounted && res.data) {
        setCategories(res.data);
      }
    }).catch(e => console.error("Failed to load catalog", e));
    return () => { mounted = false; };
  }, []);

  const roots = categories.filter(c => !c.parentId);
  const getChildren = (parentId) => categories.filter(c => c.parentId === parentId);

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
    : 'О';

  return (
    <header className="navbar">
      <div className="container header-inner">
        <Link to="/" className="logo">
          FoodMart
        </Link>

        <div
          className="catalog-wrapper"
          onMouseEnter={() => setCatalogOpen(true)}
          onMouseLeave={() => { setCatalogOpen(false); setHoveredRoot(null); }}
        >
          <button className="btn-catalog">
            ☰ Каталог
          </button>

          {catalogOpen && roots.length > 0 && (
            <div className="mega-menu">
              <div className="mega-menu-roots">
                {roots.map(root => (
                  <div
                    key={root.id}
                    className={`mega-menu-item ${hoveredRoot === root.id ? 'active' : ''}`}
                    onMouseEnter={() => setHoveredRoot(root.id)}
                  >
                    {root.name} <span style={{ opacity: 0.5, float: 'right' }}>❯</span>
                  </div>
                ))}
              </div>
              <div className="mega-menu-children">
                {hoveredRoot ? (
                  getChildren(hoveredRoot).length > 0 ? (
                    getChildren(hoveredRoot).map(child => (
                      <Link
                        key={child.id}
                        to={`/catalog/category/${child.slug}`}
                        className="mega-menu-child"
                        onClick={() => setCatalogOpen(false)}
                      >
                        {child.name}
                      </Link>
                    ))
                  ) : (
                    <div className="mega-menu-placeholder">Немає підкатегорій</div>
                  )
                ) : (
                  <div className="mega-menu-placeholder">Оберіть категорію ліворуч</div>
                )}
              </div>
            </div>
          )}
        </div>

        <div className="search-bar">
          <GlobalSearch />
        </div>

        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => isActive ? "active" : ""}>Магазин</NavLink>
          <NavLink to="/recipes" className={({ isActive }) => isActive ? "active" : ""}>Рецепти</NavLink>
          {isAuthenticated && hasSurvey ? (
            <NavLink to="/tracker" className={({ isActive }) => isActive ? "active" : ""}>Мій раціон</NavLink>
          ) : (
            <NavLink to="/survey" className={({ isActive }) => isActive ? "active" : ""}>Пройти опитування</NavLink>
          )}
        </nav>

        <div className="header-actions">
          <Link to="/cart" className="cart-btn">
            🛒 {internalCart > 0 && <span className="cart-badge">{internalCart}</span>}
          </Link>

          {isAuthenticated ? (
            <div ref={dropdownRef} style={{ position: 'relative' }}>
              <div
                className="profile-avatar"
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt="avatar" />
                ) : (
                  initials
                )}
              </div>

              {dropdownOpen && (
                <div className="user-dropdown">
                  <Link
                    to="/profile"
                    className="user-dropdown-btn"
                    onClick={() => setDropdownOpen(false)}
                  >
                    👤 Мій профіль
                  </Link>
                  {isAdmin && (
                    <Link
                      to="/admin"
                      className="user-dropdown-btn"
                      onClick={() => setDropdownOpen(false)}
                    >
                      🛡️ Адмін панель
                    </Link>
                  )}
                  <button className="user-dropdown-btn danger" onClick={handleLogout}>
                    Вийти
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/auth" style={{ textDecoration: 'none', color: 'var(--primary)', fontWeight: '600' }}>
              Увійти
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
