import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { cartAPI } from '../api/api';
import './CartPage.css';

const UNIT_LABELS = {
  KG: 'кг',
  G: 'г',
  L: 'л',
  ML: 'мл',
  PCS: 'шт',
  BUNCH: 'пучок',
};

export default function CartPage() {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  const fetchCart = async () => {
    try {
      const { data } = await cartAPI.getCart();
      setCart(data);
    } catch (err) {
      setError('Не вдалося завантажити кошик 😔');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCart();
  }, []);

  const handleUpdateQuantity = async (ingredientId, quantity) => {
    if (quantity < 1) return;
    setUpdatingId(ingredientId);
    try {
      await cartAPI.updateItem(ingredientId, quantity);
      await fetchCart();
    } catch (err) {
      console.error('Update quantity error:', err);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRemoveItem = async (ingredientId) => {
    setUpdatingId(ingredientId);
    try {
      await cartAPI.removeItem(ingredientId);
      await fetchCart();
    } catch (err) {
      console.error('Remove item error:', err);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Ви впевнені, що хочете очистити кошик?')) return;
    setLoading(true);
    try {
      await cartAPI.clearCart();
      await fetchCart();
    } catch (err) {
      console.error('Clear cart error:', err);
      setLoading(false);
    }
  };

  if (loading && !cart) {
    return (
      <div className="cart-page">
        <Navbar cartCount={0} />
        <div className="cart-container">
          <div className="spinner-large"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <Navbar cartCount={cart?.totalItems || 0} />

      <div className="cart-container animate-fade-in">
        <div className="cart-header">
          <h1 className="cart-title gradient-text">Ваш кошик</h1>
          {cart?.items?.length > 0 && (
            <button className="btn-ghost-danger" onClick={handleClearCart}>
              🗑 Очистити кошик
            </button>
          )}
        </div>

        {error ? (
          <div className="cart-error glass-card">
            <p>{error}</p>
            <button className="btn-primary mt-4" onClick={fetchCart}>Спробувати знову</button>
          </div>
        ) : !cart || !cart.items || cart.items.length === 0 ? (
          <div className="cart-empty glass-card">
            <div className="cart-empty-icon">🛒</div>
            <h2>Ваш кошик порожній</h2>
            <p className="text-secondary">Час додати трохи смачних інгредієнтів!</p>
            <Link to="/" className="btn-primary mt-4">Перейти до магазину</Link>
          </div>
        ) : (
          <div className="cart-layout">
            <div className="cart-items">
              {cart.items.map((item) => (
                <div key={item.ingredientId} className={`cart-item glass-card ${updatingId === item.ingredientId ? 'updating' : ''}`}>
                  <Link to={`/product/${item.slug}`} className="cart-item-image">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.normalizedName} />
                    ) : (
                      <span className="cart-item-placeholder">🥬</span>
                    )}
                  </Link>

                  <div className="cart-item-details">
                    <Link to={`/product/${item.slug}`} className="cart-item-name">
                      {item.normalizedName}
                    </Link>
                    <div className="cart-item-price-unit">
                      {item.price != null ? `${item.price} ₴` : '—'} / {UNIT_LABELS[item.unit] || item.unit}
                    </div>
                  </div>

                  <div className="cart-item-controls">
                    <div className="quantity-control-sm">
                      <button 
                        onClick={() => handleUpdateQuantity(item.ingredientId, item.quantity - 1)}
                        disabled={item.quantity <= 1 || updatingId === item.ingredientId}
                      >-</button>
                      <span>{item.quantity}</span>
                      <button 
                        onClick={() => handleUpdateQuantity(item.ingredientId, item.quantity + 1)}
                        disabled={updatingId === item.ingredientId}
                      >+</button>
                    </div>
                    
                    <div className="cart-item-total">
                      {item.totalPrice != null ? `${item.totalPrice.toFixed(2)} ₴` : '—'}
                    </div>

                    <button 
                      className="cart-item-remove"
                      onClick={() => handleRemoveItem(item.ingredientId)}
                      disabled={updatingId === item.ingredientId}
                      title="Видалити"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="cart-summary glass-card">
              <h2>Замовлення</h2>
              <div className="summary-row">
                <span>Товари ({cart.totalItems})</span>
                <span>{cart.totalPrice ? `${cart.totalPrice.toFixed(2)} ₴` : '—'}</span>
              </div>
              <div className="summary-row summary-total">
                <span>До сплати</span>
                <span className="gradient-text">{cart.totalPrice ? `${cart.totalPrice.toFixed(2)} ₴` : '—'}</span>
              </div>
              
              <button 
                className="btn-primary checkout-btn" 
                onClick={() => alert("Функціонал оформлення замовлення у розробці! 🚀")}
              >
                Оформити замовлення
              </button>
              
              <Link to="/" className="btn-ghost back-to-shop-btn">
                Продовжити покупки
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
