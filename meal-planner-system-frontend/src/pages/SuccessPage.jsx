import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ordersAPI } from '../api/api';
import './SuccessPage.css';

export default function SuccessPage() {
  const [searchParams] = useSearchParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const orderId = searchParams.get('orderId');
    if (orderId) {
      ordersAPI.getOrderById(orderId)
        .then(({ data }) => setOrder(data))
        .catch(err => console.error('Failed to load order:', err))
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [searchParams]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('uk-UA', {
      day: 'numeric', month: 'long', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  return (
    <div className="success-page">
      <header className="success-header">
        <div className="container">
          <Link to="/" className="logo">FoodMart</Link>
        </div>
      </header>

      <main className="success-main">
        <div className="container" style={{ display: 'flex', justifyContent: 'center' }}>
          {loading ? (
            <div className="spinner" style={{ marginTop: '80px' }}></div>
          ) : (
            <div className="success-card">
              <div className="icon-wrapper">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>

              <h1>Оплата успішна!</h1>
              <p>Дякуємо за замовлення. Продукти вже готуються до відправки.</p>

              <div className="order-details">
                <div className="detail-row">
                  <span className="detail-label">Номер замовлення</span>
                  <span className="detail-value">#{order?.id || searchParams.get('orderId') || '—'}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Дата</span>
                  <span className="detail-value">
                    {order?.createdAt ? formatDate(order.createdAt) : new Date().toLocaleDateString('uk-UA', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Статус</span>
                  <span className="detail-value">{order?.status === 'PAID' ? 'Оплачено' : order?.status || 'Обробка'}</span>
                </div>
                {order?.trackingNumber && (
                  <div className="detail-row">
                    <span className="detail-label">Трек-номер</span>
                    <span className="detail-value">{order.trackingNumber}</span>
                  </div>
                )}
                <div className="detail-row total-row">
                  <span className="detail-label">Сума оплати</span>
                  <span className="detail-value">{(order?.totalPrice || order?.totalAmount) ? `${(order.totalPrice || order.totalAmount).toFixed(2)} ₴` : '—'}</span>
                </div>
              </div>

              <div className="actions">
                <Link to="/profile/orders" className="btn-primary">Перейти до замовлень</Link>
                <Link to="/" className="btn-secondary">Повернутися до магазину</Link>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
