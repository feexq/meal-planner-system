import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { authAPI } from '../api/api';
import './ProfileSidebar.css';

const ProfileSidebar = () => {
  const navigate = useNavigate();

  const handleLogout = async (e) => {
    e.preventDefault();
    try {
      await authAPI.logout();
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/auth');
    } catch (error) {
      console.error('Logout failed:', error);

      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/auth');
    }
  };

  return (
    <aside className="profile-sidebar">
      <ul className="profile-nav-menu">
        <li className="profile-nav-item">
          <NavLink to="/profile" end className={({ isActive }) => isActive ? 'active' : ''}>
            <span className="profile-nav-icon">👤</span> Особисті дані
          </NavLink>
        </li>
        <li className="profile-nav-item">
          <NavLink to="/profile/orders" className={({ isActive }) => isActive ? 'active' : ''}>
            <span className="profile-nav-icon">🛍️</span> Мої замовлення
          </NavLink>
        </li>
        <li className="profile-nav-item">
          <NavLink to="/profile/preferences" className={({ isActive }) => isActive ? 'active' : ''}>
            <span className="profile-nav-icon">📝</span> Параметри раціону
          </NavLink>
        </li>
        <li className="profile-nav-item">
          <NavLink to="/profile/statistics" className={({ isActive }) => isActive ? 'active' : ''}>
            <span className="profile-nav-icon">📊</span> Статистика
          </NavLink>
        </li>
        <li className="profile-nav-item logout-item">
          <a href="#" onClick={handleLogout} className="logout-link">
            <span className="profile-nav-icon">🚪</span> Вийти
          </a>
        </li>
      </ul>
    </aside>
  );
};

export default ProfileSidebar;
