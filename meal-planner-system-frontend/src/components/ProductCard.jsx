import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { cartAPI, ingredientsAPI } from '../api/api';
import './ProductCard.css';

const UNIT_LABELS = {
  KG: 'кг',
  G: 'г',
  L: 'л',
  ML: 'мл',
  PCS: 'шт',
  BUNCH: 'пучок',
};

// Expanded mapping for fallback icons based on UA/EN words
const getFallbackIcon = (name) => {
  if (!name) return '🥗';
  const lower = name.toLowerCase();

  if (lower.includes('авокадо') || lower.includes('avocado')) return '🥑';
  if (lower.includes('кур') || lower.includes('м\'яс') || lower.includes('філе') || lower.includes('chicken') || lower.includes('meat') || lower.includes('beef') || lower.includes('pork')) return '🥩';
  if (lower.includes('молок') || lower.includes('сир') || lower.includes('йогурт') || lower.includes('milk') || lower.includes('cheese') || lower.includes('yogurt') || lower.includes('butter')) return '🥛';
  if (lower.includes('брок') || lower.includes('овоч') || lower.includes('капуст') || lower.includes('зелень') || lower.includes('broccoli') || lower.includes('vegetable') || lower.includes('cabbage') || lower.includes('greens') || lower.includes('lettuce')) return '🥦';
  if (lower.includes('яблук') || lower.includes('фрукт') || lower.includes('банан') || lower.includes('apple') || lower.includes('fruit') || lower.includes('banana') || lower.includes('orange')) return '🍎';
  if (lower.includes('хліб') || lower.includes('булк') || lower.includes('батон') || lower.includes('bread') || lower.includes('bun') || lower.includes('baguette') || lower.includes('toast')) return '🍞';
  if (lower.includes('риб') || lower.includes('лосос') || lower.includes('fish') || lower.includes('salmon') || lower.includes('tuna')) return '🐟';
  if (lower.includes('potato') || lower.includes('картоп')) return '🥔';
  if (lower.includes('tomato') || lower.includes('помідор') || lower.includes('томат')) return '🍅';
  if (lower.includes('egg') || lower.includes('яйц')) return '🥚';

  return '🥗';
};

export default function ProductCard({ product, onCartUpdate }) {
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const [imgError, setImgError] = useState(false);
  const [tags, setTags] = useState([]);

  useEffect(() => {
    // Fetch tags for this product
    let isMounted = true;
    ingredientsAPI.getTags(product.id)
      .then(res => {
        if (isMounted) setTags(res.data || []);
      })
      .catch(err => {
        console.error('Failed to fetch tags for', product.id, err);
      });

    return () => { isMounted = false; };
  }, [product.id]);

  const handleAddToCart = async (e) => {
    e.preventDefault(); // Prevent link click if added
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
    <Link to={`/product/${product.slug}`} className="product-card">
      <div className="product-img">
        {product.imageUrl && !imgError ? (
          <img
            src={product.imageUrl}
            alt={product.nameUk}
            loading="lazy"
            onError={() => setImgError(true)}
          />
        ) : (
          <span>{getFallbackIcon(product.nameUk)}</span>
        )}
      </div>

      <div className="product-name">{product.nameUk}</div>

      <div className="product-tags">
        {tags.slice(0, 3).map(t => (
          <span
            key={t.id}
            className="product-tag"
            style={{ background: t.color ? `${t.color}20` : '#eee', color: t.color || '#333' }}
          >
            {t.name}
          </span>
        ))}
        {tags.length > 3 && (
          <span className="product-tag" style={{ background: '#eee', color: '#666' }}>
            +{tags.length - 3}
          </span>
        )}
      </div>

      <div className="product-weight">
        {product.unit ? `${UNIT_LABELS[product.unit] || product.unit}` : '1 шт'}
      </div>

      <div className="product-footer">
        <div className="product-price">
          {product.price != null ? `${product.price} ₴` : '—'}
        </div>
        <button
          className={`btn-add ${added ? 'added' : ''}`}
          onClick={handleAddToCart}
          disabled={!product.available || adding}
        >
          {added ? '✓' : '+'}
        </button>
      </div>
    </Link>
  );
}
