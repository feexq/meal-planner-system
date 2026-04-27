import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

export default function ScrollToTop() {
    const { pathname } = useLocation();

    useEffect(() => {
        // При кожній зміні шляху (pathname) браузер буде скролити на координати (0, 0)
        window.scrollTo(0, 0);
    }, [pathname]);

    // Цей компонент нічого не рендерить на екран
    return null;
}