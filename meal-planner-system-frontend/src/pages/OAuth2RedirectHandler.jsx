import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cartAPI } from '../api/api';

export default function OAuth2RedirectHandler() {
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const token = params.get('token');
        const refreshToken = params.get('refreshToken');
        const error = params.get('error');

        if (!token) {
            console.error("Помилка OAuth2:", error);
            navigate('/auth?error=oauth2_failed');
            return;
        }


        localStorage.setItem('accessToken', token);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);


        cartAPI.mergeCart()
            .catch(err => console.error('Cart merge failed:', err))
            .finally(() => {

                localStorage.removeItem('cartSessionId');

                const redirect = localStorage.getItem('postLoginRedirect') || '/';
                localStorage.removeItem('postLoginRedirect');
                window.location.href = redirect;
            });

    }, [location, navigate]);

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
            <h2>Завершення авторизації... ⏳</h2>
        </div>
    );
}