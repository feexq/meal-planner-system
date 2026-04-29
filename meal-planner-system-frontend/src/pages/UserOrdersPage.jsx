import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ProfileSidebar from '../components/ProfileSidebar';
import { ordersAPI, cartAPI } from '../api/api';
import './UserOrdersPage.css';

const UserOrdersPage = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [detailsMap, setDetailsMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  const [selectedOrder, setSelectedOrder] = useState(null);

  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

  useEffect(() => {
    fetchOrders();
  }, []);

  const showToast = (message, type = 'success') => {
    setToast({ show: true, message, type });
    setTimeout(() => setToast({ show: false, message: '', type: 'success' }), 3000);
  };

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const response = await ordersAPI.getOrders();
      const fetchedOrders = response.data;
      setOrders(fetchedOrders);
      const map = {};
      await Promise.all(
        fetchedOrders.map(async (order) => {
          try {
            const detailsRes = await ordersAPI.getOrderById(order.id);
            map[order.id] = detailsRes.data;
          } catch (e) {
            console.error(`Error fetching details for order ${order.id}`);
          }
        })
      );
      setDetailsMap(map);
    } catch (error) {
      console.error('Error fetching orders:', error);
      showToast('Помилка завантаження замовлень', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleRepeatOrder = async (orderId) => {
    const details = detailsMap[orderId];
    if (!details || !details.items) return;
    try {
      for (const item of details.items) {
        await cartAPI.addItem(item.ingredientId, item.quantity);
      }
      showToast('Товари успішно додано до кошика! 🛒', 'success');
    } catch (error) {
      console.error('Error repeating order:', error);
      showToast('Помилка при додаванні до кошика', 'error');
    }
  };

  const handlePayNow = async (orderId) => {
    await handleRepeatOrder(orderId);
    showToast('Перенаправлення до кошика...', 'success');
    setTimeout(() => {
      navigate('/cart');
    }, 1000);
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    showToast('ТТН успішно скопійовано! 📋', 'success');
  };

  const filteredOrders = orders.filter(order => {
    if (filter === 'TRANSIT') return ['IN_TRANSIT', 'COURIER_ASSIGNED', 'PAID'].includes(order.status);
    if (filter === 'DELIVERED') return order.status === 'DELIVERED';
    return true;
  });

  const getStatusClass = (status) => {
    switch (status) {
      case 'DELIVERED': return 'status-delivered';
      case 'IN_TRANSIT':
      case 'COURIER_ASSIGNED': return 'status-transit';
      case 'PENDING': return 'status-failed';
      case 'FAILED': return 'status-failed';
      default: return 'status-transit';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'DELIVERED': return 'Доставлено';
      case 'IN_TRANSIT': return 'В дорозі (In Transit)';
      case 'COURIER_ASSIGNED': return 'Кур\'єра призначено';
      case 'PAID': return 'Оплачено';
      case 'PENDING': return 'Скасовано';
      case 'FAILED': return 'Помилка';
      default: return status;
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'DELIVERED': return '📦';
      case 'IN_TRANSIT': return '🚚';
      case 'PENDING': return '❌';
      case 'FAILED': return '❌';
      default: return '🛒';
    }
  };

  const getOrderStatusTrackerElements = (status) => {
    const isPaid = ['PAID', 'COURIER_ASSIGNED', 'IN_TRANSIT', 'DELIVERED'].includes(status);
    const isAssigned = ['COURIER_ASSIGNED', 'IN_TRANSIT', 'DELIVERED'].includes(status);
    const isTransit = ['IN_TRANSIT', 'DELIVERED'].includes(status);
    const isDelivered = status === 'DELIVERED';

    let progress = '0%';
    if (isDelivered) progress = '100%';
    else if (isTransit) progress = '66.66%';
    else if (isAssigned) progress = '33.33%';

    return { isPaid, isAssigned, isTransit, isDelivered, progress };
  };

  if (loading) {
    return (
      <div className="page-loader">
        <div className="spinner"></div>
      </div>
    );
  }

  const renderOrderCard = (order) => {
    const { isPaid, isAssigned, isTransit, isDelivered, progress } = getOrderStatusTrackerElements(order.status);
    const details = detailsMap[order.id];
    const items = details?.items || [];


    const previewItems = items.slice(0, 4);
    const remainingCount = items.length - 4;

    return (
      <div key={order.id} className="order-card" style={order.status === 'IN_TRANSIT' ? { borderColor: '#BAE6FD', boxShadow: '0 4px 16px rgba(2, 132, 199, 0.05)' } : {}}>
        <div className="oc-header">
          <div className="oc-meta">
            <div className="oc-number">Замовлення #ORD-{order.id}</div>
            <div className="oc-date">Створено: {new Date(order.createdAt).toLocaleDateString('uk-UA', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}</div>
          </div>
          <div className={`oc-status ${getStatusClass(order.status)}`}>
            <span>{getStatusIcon(order.status)}</span> {getStatusText(order.status)}
          </div>
        </div>

        {['PAID', 'COURIER_ASSIGNED', 'IN_TRANSIT', 'DELIVERED'].includes(order.status) && (
          <div className="delivery-tracker">
            <div className="tracker-line">
              <div className="tracker-line-fill" style={{ width: progress }}></div>
            </div>

            <div className={`tracker-step ${isPaid ? 'completed' : ''}`}>
              <div className="step-icon">✓</div>
              <div className="step-lbl">Сплачено</div>
            </div>
            <div className={`tracker-step ${isAssigned ? 'completed' : (status === 'PAID' ? 'current' : '')}`}>
              <div className="step-icon">{isAssigned ? '✓' : ''}</div>
              <div className="step-lbl">Зібрано</div>
            </div>
            <div className={`tracker-step ${isTransit ? 'completed' : (order.status === 'COURIER_ASSIGNED' ? 'current' : '')}`}>
              <div className="step-icon">{isTransit ? '✓' : (order.status === 'IN_TRANSIT' ? '🚚' : '')}</div>
              <div className="step-lbl">В дорозі</div>
            </div>
            <div className={`tracker-step ${isDelivered ? 'completed' : (order.status === 'IN_TRANSIT' ? '' : '')}`}>
              <div className="step-icon">{isDelivered ? '✓' : '4'}</div>
              <div className="step-lbl">{isDelivered ? 'Доставлено' : 'У відділенні'}</div>
            </div>
          </div>
        )}

        {order.trackingNumber && (
          <div className="np-card">
            <div className="np-logo">
              <img src="/np-logomark.svg" alt="Нова Пошта" />
            </div>
            <div className="np-info-text">
              <span className="np-ttn">ТТН: {order.trackingNumber}</span>
              <span className="np-branch">Відділення {order.npWarehouseRef || 'доставки'}</span>
            </div>
            <button
              className="np-copy"
              title="Скопіювати ТТН"
              onClick={() => copyToClipboard(order.trackingNumber)}
            >
              📋
            </button>
          </div>
        )}

        {}
        {items.length > 0 && (
          <div className="oc-items-preview">
            {previewItems.map((item, idx) => (
              <div key={idx} className="item-thumb" title={item.name}>
                {item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : '🛒'}
              </div>
            ))}
            {remainingCount > 0 && <div className="item-more">+{remainingCount}</div>}
            <div className="items-text">Товари в замовленні ({items.length} шт)</div>
          </div>
        )}

        <div className="oc-footer">
          <div className="oc-price">
            <span className="price-lbl">{order.status === 'PENDING' ? 'До сплати' : 'Сума замовлення'}</span>
            <span className="price-val">{order.totalAmount?.toLocaleString('uk-UA', { minimumFractionDigits: 2 })} ₴</span>
          </div>
          <div className="oc-actions">
            {order.status === 'IN_TRANSIT' && <button className="btn-outline">Відстежити посилку</button>}

            {}
            {(order.status === 'DELIVERED' || order.status === 'FAILED' || order.status === 'PENDING') && (
              <button className="btn-outline" onClick={() => handleRepeatOrder(order.id)}>Повторити замовлення</button>
            )}

            <button className="btn-primary" onClick={() => setSelectedOrder(details)}>Деталі замовлення</button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="orders-page">
      <Navbar />
      <main className="container">
        <div className="profile-layout">
          <ProfileSidebar />

          <div className="content-area">
            <div className="page-header">
              <h1 className="page-title">Мої замовлення</h1>

              <div className="order-filters">
                <button className={`filter-btn ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>
                  Всі ({orders.length})
                </button>
                <button className={`filter-btn ${filter === 'TRANSIT' ? 'active' : ''}`} onClick={() => setFilter('TRANSIT')}>
                  В дорозі ({orders.filter(o => ['IN_TRANSIT', 'COURIER_ASSIGNED', 'PAID'].includes(o.status)).length})
                </button>
                <button className={`filter-btn ${filter === 'DELIVERED' ? 'active' : ''}`} onClick={() => setFilter('DELIVERED')}>
                  Доставлені ({orders.filter(o => o.status === 'DELIVERED').length})
                </button>
              </div>
            </div>

            {filteredOrders.length === 0 ? (
              <div className="order-card" style={{ textAlign: 'center', padding: '48px' }}>
                <h3>Замовлень не знайдено</h3>
                <p>Ваша історія замовлень поки порожня.</p>
              </div>
            ) : (
              filteredOrders.map(renderOrderCard)
            )}
          </div>
        </div>
      </main>

      {}
      {selectedOrder && (
        <div className="modal-backdrop" onClick={() => setSelectedOrder(null)}>
          <div className="custom-modal details-modal" onClick={e => e.stopPropagation()}>
            <div className="details-header">
              <h3>📦 Деталі замовлення #ORD-{selectedOrder.id}</h3>
              <button className="close-btn" onClick={() => setSelectedOrder(null)}>×</button>
            </div>

            <div className="details-items-list">
              {selectedOrder.items?.map((item, idx) => (
                <div key={idx} className="details-item">
                  <div className="di-thumb">
                    {item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : '🛒'}
                  </div>
                  <div className="di-info">
                    <div className="di-name">{item.name}</div>
                    <div className="di-meta">{item.quantity} шт × {item.priceAtPurchase} ₴</div>
                  </div>
                  <div className="di-total">
                    {(item.quantity * item.priceAtPurchase).toLocaleString('uk-UA', { minimumFractionDigits: 2 })} ₴
                  </div>
                </div>
              ))}
            </div>

            <div className="details-footer">
              <div className="df-total-lbl">Разом до сплати:</div>
              <div className="df-total-val">{selectedOrder.totalAmount?.toLocaleString('uk-UA', { minimumFractionDigits: 2 })} ₴</div>
            </div>

            <button className="btn-primary" style={{ width: '100%', marginTop: '16px' }} onClick={() => setSelectedOrder(null)}>Закрити</button>
          </div>
        </div>
      )}

      {}
      <div className={`toast-notification ${toast.show ? 'show' : ''} ${toast.type}`}>
        {toast.message}
      </div>

    </div>
  );
};

export default UserOrdersPage;