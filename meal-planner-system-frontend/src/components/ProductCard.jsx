import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cartAPI } from '../api/api';
import './ProductCard.css';

const UNIT_LABELS = {
  KG: 'кг',
  G: 'г',
  L: 'л',
  ML: 'мл',
  PCS: 'шт',
  BUNCH: 'пучок',
};

export default function ProductCard({ product, onCartUpdate }) {
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);

  const handleAddToCart = async () => {
    if (adding || !product.available) return;
    setAdding(true);
    try {
      const { data } = await cartAPI.addItem(product.id, 1);
      setAdded(true);
      if (onCartUpdate) onCartUpdate(data);
      setTimeout(() => setAdded(false), 1500);
    } catch (err) {
      console.error('Cart error:', err);
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="product-card">
      <Link to={`/product/${product.slug}`} className="product-card-link-wrapper">
        <div className="product-card-image">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.normalizedName} loading="lazy" />
          ) : (
            <span className="product-card-placeholder">🥬</span>
          )}
        </div>

        <div className="product-card-body">
          <span className="product-card-name" style={{ cursor: 'pointer' }}>{product.normalizedName}</span>
          <div className="product-card-meta">
            <span className="product-card-price">
              {product.price != null ? `${product.price} ₴` : '—'}
              {product.unit && (
                <span className="product-card-unit">
                  /{UNIT_LABELS[product.unit] || product.unit}
                </span>
              )}
            </span>
            <span
              className={`product-card-badge ${product.available ? 'badge-available' : 'badge-unavailable'}`}
            >
              {product.available ? 'В наявності' : 'Немає'}
            </span>
          </div>
        </div>
      </Link>

      <div className="product-card-action">
        <button
          className={`add-to-cart-btn ${added ? 'added' : ''}`}
          onClick={handleAddToCart}
          disabled={!product.available || adding}
        >
          <span>{added ? '✓ Додано' : adding ? '...' : '🛒 У кошик'}</span>
        </button>
      </div>
    </div>
  );
}
