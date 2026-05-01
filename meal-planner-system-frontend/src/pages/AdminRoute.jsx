import { Navigate } from 'react-router-dom';

function getRole() {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        // Spring Security кладе ролі в "roles" або "authorities"
        const authorities = payload.role || payload.authorities || [];
        return authorities.includes('ADMIN') ? 'ADMIN' : 'USER';
    } catch {
        return null;
    }
}

export default function AdminRoute({ children }) {
    const role = getRole();
    if (!role) return <Navigate to="/auth" replace />;
    if (role !== 'ADMIN') return <Navigate to="/" replace />;
    return children;
}