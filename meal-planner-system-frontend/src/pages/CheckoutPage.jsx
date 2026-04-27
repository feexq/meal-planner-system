import { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { useAuth } from '../context/AuthContext';
import { cartAPI, ordersAPI } from '../api/api';
import './CheckoutPage.css';

// Initialize Stripe with the public key from env, or fallback to test key
const stripePublicKey = import.meta.env.VITE_STRIPE_PUBLIC_KEY || 'pk_test_TYooMQauvdEDq54NiTphI7jx';
const stripePromise = loadStripe(stripePublicKey);

const CheckoutForm = ({ clientSecret, city, warehouse, cart, orderId }) => {
  const stripe = useStripe();
  const elements = useElements();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [error, setError] = useState(null);
  const [processing, setProcessing] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    setProcessing(true);

    const cardElement = elements.getElement(CardElement);

    const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
      payment_method: {
        card: cardElement,
        billing_details: {
          name: user?.firstName + ' ' + user?.lastName || 'Гість',
          email: user?.email || '',
        },
      },
    });

    if (stripeError) {
      setError(stripeError.message);
      setProcessing(false);
    } else {
      if (paymentIntent.status === 'succeeded') {
        navigate(`/order-success?orderId=${orderId}`);
      }
    }
  };

  const CARD_ELEMENT_OPTIONS = {
    style: {
      base: {
        color: '#1A1030',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        fontSmoothing: 'antialiased',
        fontSize: '15px',
        '::placeholder': {
          color: '#7A6A90',
        },
      },
      invalid: {
        color: '#D14343',
        iconColor: '#D14343',
      },
    },
    hidePostalCode: true,
  };

  return (
    <div className="checkout-layout">
      <div className="checkout-form-container">
        <form id="payment-form" onSubmit={handleSubmit}>

          <div className="checkout-section">
            <div className="section-title"><span className="step-number">1</span> Контактні дані</div>
            <div className="form-row">
              <div className="form-group">
                <label>Ім'я та Прізвище</label>
                <input
                  type="text"
                  className="form-control"
                  value={user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : 'Іван Франко'}
                  readOnly
                />
              </div>
              <div className="form-group">
                <label>Телефон</label>
                <input type="tel" className="form-control" value="+380 50 123 45 67" required />
              </div>
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label>Email</label>
              <input type="email" className="form-control" value={user?.email || 'ivan.franko@example.com'} readOnly />
            </div>
          </div>

          <div className="checkout-section">
            <div className="section-title"><span className="step-number">2</span> Доставка (Нова Пошта)</div>
            <div className="form-row" style={{ marginBottom: 0 }}>
              <div className="form-group">
                <label>Місто</label>
                <input
                  type="text"
                  className="form-control"
                  value={city?.name || ''}
                  readOnly
                  style={{ background: 'var(--neutral)' }}
                />
              </div>
              <div className="form-group">
                <label>Відділення</label>
                <input
                  type="text"
                  className="form-control"
                  value={warehouse?.name || ''}
                  readOnly
                  style={{ background: 'var(--neutral)' }}
                />
              </div>
            </div>
          </div>

          <div className="checkout-section">
            <div className="section-title"><span className="step-number">3</span> Оплата карткою</div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label>Дані картки</label>
              <div className="stripe-card-wrapper">
                <CardElement options={CARD_ELEMENT_OPTIONS} />
              </div>
              {error && <div className="card-errors" role="alert">{error}</div>}
            </div>
          </div>

        </form>
      </div>

      <div>
        <div className="summary-card">
          <h2 className="summary-title">Підсумок</h2>

          <div className="order-items">
            {cart?.items?.map(item => (
              <div className="order-item" key={item.id || item.ingredientId}>
                <span className="order-item-name">{item.normalizedName} ({item.quantity} шт)</span>
                <span className="order-item-price">{item.price?.toFixed(2)} ₴</span>
              </div>
            ))}
          </div>

          <div className="summary-row">
            <span>Сума</span>
            <span>{cart?.totalPrice?.toFixed(2) || '0'} ₴</span>
          </div>
          <div className="summary-row">
            <span>Доставка</span>
            <span>За тарифами перевізника</span>
          </div>

          <div className="summary-total">
            <span>До сплати</span>
            <span>{cart?.totalPrice?.toFixed(2) || '0'} ₴</span>
          </div>

          <button
            type="submit"
            form="payment-form"
            className="btn-pay"
            disabled={!stripe || processing}
          >
            {processing ? '🔄 Обробка...' : `💳 Оплатити ${cart?.totalPrice?.toFixed(2) || '0'} ₴`}
          </button>

          <div className="secure-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            Безпечний платіж через Stripe
          </div>
        </div>
      </div>
    </div>
  );
};

export default function CheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { city, warehouse } = location.state || {};

  const [clientSecret, setClientSecret] = useState(null);
  const [orderId, setOrderId] = useState(null);
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!city || !warehouse) {
      const pending = sessionStorage.getItem('pendingDelivery');
      if (pending) {
        const { city: savedCity, warehouse: savedWarehouse } = JSON.parse(pending);
        sessionStorage.removeItem('pendingDelivery');

        navigate('/checkout', {
          replace: true,
          state: { city: savedCity, warehouse: savedWarehouse }
        });
        return;
      }

      navigate('/cart');
      return;
    }

    // 3. Якщо дані є, ініціалізуємо оплату
    const initCheckout = async () => {
      try {
        const cartRes = await cartAPI.getCart();
        setCart(cartRes.data);

        const intentRes = await ordersAPI.checkoutIntent(city.ref, warehouse.ref);
        const secret = intentRes.data.checkoutUrl || intentRes.data.clientSecret;
        setClientSecret(secret);
        setOrderId(intentRes.data.orderId || intentRes.data.id || 'NEW');

      } catch (err) {
        console.error('Failed to init checkout:', err);
        alert(err.response?.data?.message || err.message || 'Помилка завантаження');
        navigate('/cart');
      } finally {
        setLoading(false);
      }
    };

    initCheckout();
  }, [city, warehouse, navigate]);

  if (loading) {
    return (
      <div className="page-loader">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <>
      <header className="checkout-header">
        <div className="container">
          <Link to="/" className="logo">FoodMart</Link>
        </div>
      </header>

      <main className="container checkout-container">
        <div className="page-header">
          <h1>Оформлення замовлення</h1>
        </div>

        {clientSecret && (
          <Elements stripe={stripePromise} options={{ clientSecret }}>
            <CheckoutForm
              clientSecret={clientSecret}
              city={city}
              warehouse={warehouse}
              cart={cart}
              orderId={orderId}
            />
          </Elements>
        )}
      </main>
    </>
  );
}
