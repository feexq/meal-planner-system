import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ingredientsAPI, cartAPI, recipesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import './ProductPage.css';

const UNIT_LABELS = {
  KG: 'кг',
  G: 'г',
  L: 'л',
  ML: 'мл',
  PCS: 'шт',
  BUNCH: 'пучок',
};

// Map dietary statuses to colors for badges
const DIETARY_LABELS = {
  ALLOWED: { text: 'Дозволено', color: 'var(--success-color)' },
  SOFT_FORBIDDEN: { text: 'Помірне споживання', color: 'var(--warning-color)' },
  HARD_FORBIDDEN: { text: 'Заборонено', color: 'var(--danger-color)' },
};

export default function ProductPage({ cartCount, setCartCount }) {
  const { slug } = useParams();
  const [ingredient, setIngredient] = useState(null);
  const [dietaryTags, setDietaryTags] = useState([]);
  const [relatedRecipes, setRelatedRecipes] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const { data: ing } = await ingredientsAPI.getBySlug(slug);
        setIngredient(ing);

        // Fetch dietary tags
        try {
          const { data: tags } = await ingredientsAPI.getDietaryTags(ing.id);
          setDietaryTags(tags || []);
        } catch (e) {
          console.warn('Could not fetch dietary tags', e);
        }

        // Fetch related recipes
        try {
          const { data: recipesRes } = await recipesAPI.getByIngredient(ing.id, { page: 0, size: 5 });
          setRelatedRecipes(recipesRes.content || []);
        } catch (e) {
          console.warn('Could not fetch related recipes', e);
        }
      } catch (err) {
        console.error('Failed to load ingredient:', err);
        setError('Продукт не знайдено або сталася помилка зʼєднання.');
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [slug]);

  const handleAddToCart = async () => {
    if (adding || !ingredient.available) return;
    setAdding(true);
    try {
      const { data } = await cartAPI.addItem(ingredient.id, quantity);
      setAdded(true);
      if (setCartCount) setCartCount(data.totalItems);
      setTimeout(() => setAdded(false), 2000);
    } catch (err) {
      console.error('Cart error:', err);
    } finally {
      setAdding(false);
    }
  };

  if (loading) {
    return (
      <div className="product-page">
        <Navbar cartCount={cartCount} />
        <div className="product-page-container">
          <div className="spinner-large"></div>
        </div>
      </div>
    );
  }

  if (error || !ingredient) {
    return (
      <div className="product-page">
        <Navbar cartCount={cartCount} />
        <div className="product-page-container">
          <div className="product-error glass-card">
            <h2>Ой, щось пішло не так 😔</h2>
            <p>{error}</p>
            <Link to="/" className="btn-primary">Повернутись до магазину</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="product-page">
      <Navbar cartCount={cartCount} />

      <div className="product-page-container animate-fade-in">
        <Link to="/" className="back-link">
          ← В магазин
        </Link>

        <div className="product-main-card glass-card">
          <div className="product-image-container">
            {ingredient.imageUrl ? (
              <img src={ingredient.imageUrl} alt={ingredient.normalizedName} />
            ) : (
              <span className="product-placeholder">🥬</span>
            )}
          </div>

          <div className="product-details">
            <h1 className="product-title gradient-text">{ingredient.normalizedName}</h1>
            
            <div className="product-badges">
              {ingredient.categoryName && (
                <span className="product-badge category-badge">
                  {ingredient.categoryName}
                </span>
              )}
              {ingredient.available ? (
                <span className="product-badge available-badge">✔ В наявності</span>
              ) : (
                <span className="product-badge unavailable-badge">✖ Немає в наявності</span>
              )}
            </div>

            <div className="product-price-block">
              <span className="product-price">
                {ingredient.price != null ? `${ingredient.price} ₴` : '—'}
              </span>
              {ingredient.unit && (
                <span className="product-unit">
                  /{UNIT_LABELS[ingredient.unit] || ingredient.unit}
                </span>
              )}
            </div>

            <div className="product-actions">
              <div className="quantity-control">
                <button 
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || !ingredient.available}
                >-</button>
                <span>{quantity}</span>
                <button 
                  onClick={() => setQuantity(quantity + 1)}
                  disabled={!ingredient.available}
                >+</button>
              </div>

              <button
                className={`add-to-cart-btn-large ${added ? 'added' : ''}`}
                onClick={handleAddToCart}
                disabled={!ingredient.available || adding}
              >
                <span>{added ? '✓ Додано в кошик' : adding ? 'Додаємо...' : '🛒 Додати в кошик'}</span>
              </button>
            </div>

            {dietaryTags.length > 0 && (
              <div className="product-dietary-section">
                <h3>Дієтичні особливості</h3>
                <div className="dietary-tags">
                  {dietaryTags.map(tag => (
                    <div key={tag.conditionId} className="dietary-tag">
                      <span className="dietary-tag-name">{tag.conditionName}</span>
                      <span className="dietary-tag-status" style={{ color: DIETARY_LABELS[tag.status]?.color }}>
                        {DIETARY_LABELS[tag.status]?.text || tag.status}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
            
            {ingredient.aliases && ingredient.aliases.length > 0 && (
              <div className="product-aliases">
                <p>Також відомий як: <span className="text-secondary">{ingredient.aliases.join(', ')}</span></p>
              </div>
            )}
          </div>
        </div>

        {relatedRecipes.length > 0 && (
          <div className="product-recipes-section">
            <h2>В яких рецептах використовується</h2>
            <div className="related-recipes-grid">
              {relatedRecipes.map(recipe => (
                <div key={recipe.id} className="related-recipe-card glass-card">
                  <div className="recipe-image">
                    {recipe.imageUrl ? (
                      <img src={recipe.imageUrl} alt={recipe.name} loading="lazy" />
                    ) : (
                      <span className="recipe-placeholder">🍲</span>
                    )}
                  </div>
                  <div className="recipe-info">
                    <h4>{recipe.name}</h4>
                    <p>{recipe.mealType}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
