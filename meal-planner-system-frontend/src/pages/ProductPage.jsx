import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { productsAPI, cartAPI, recipesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import './ProductPage.css';

const UNIT_LABELS = {
  KG: 'кг', G: 'г', L: 'л', ML: 'мл', PCS: 'шт', BUNCH: 'пучок',
};

const STATUS_CLASSES = {
  ALLOWED: 'allowed',
  SOFT_FORBIDDEN: 'soft_forbidden',
  HARD_FORBIDDEN: 'hard_forbidden',
};

const MEAL_TYPE_EMOJIS = {
  BREAKFAST: '🥞', LUNCH: '🥗', DINNER: '🐟',
  DESSERT: '🍰', DRINK: '🥤', SNACK: '🥑',
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
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const { data: ing } = await productsAPI.getBySlug(slug);
        setIngredient(ing);
        try {
          const { data: tags } = await productsAPI.getDietaryTags(ing.id);
          setDietaryTags(tags || []);
        } catch { }
        try {
          const { data: recipesRes } = await recipesAPI.getByIngredient(ing.id, { page: 0, size: 3 });
          setRelatedRecipes(recipesRes.content || []);
        } catch { }
      } catch {
        setError('Продукт не знайдено або сталася помилка.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [slug]);

  const handleAddToCart = async () => {
    if (adding || !ingredient?.available) return;
    setAdding(true);
    try {
      const { data } = await cartAPI.addItem(ingredient.id, quantity);
      setAdded(true);
      if (setCartCount) setCartCount(data.totalItems);
      setTimeout(() => setAdded(false), 2000);
    } catch { }
    finally { setAdding(false); }
  };

  if (loading) return (
    <div className="product-page">
      <Navbar cartCount={cartCount} />
      <div className="product-page-container" style={{ textAlign: 'center', marginTop: 100 }}>
        <div className="spinner-large" />
      </div>
    </div>
  );

  if (error || !ingredient) return (
    <div className="product-page">
      <Navbar cartCount={cartCount} />
      <div className="product-page-container" style={{ textAlign: 'center', marginTop: 100 }}>
        <h2>Ой, щось пішло не так 😔</h2>
        <p style={{ marginBottom: 20 }}>{error}</p>
        <Link to="/" className="btn-add-cart" style={{ display: 'inline-flex', width: 'auto', textDecoration: 'none' }}>
          Повернутись до магазину
        </Link>
      </div>
    </div>
  );

  const galleryImages = ingredient.imageUrls || (ingredient.imageUrl ? [ingredient.imageUrl] : []);


  const nutrition = ingredient.nutrition;

  return (
    <div className="product-page">
      <Navbar cartCount={cartCount} />

      <main className="product-page-container">

        {}
        <div className="breadcrumbs">
          <Link to="/">Головна</Link> /
          <Link to="/catalog">Магазин</Link>
          {ingredient.categoryName && (
            <> / <Link to={`/catalog?category=${ingredient.categoryId}`}>{ingredient.categoryName}</Link></>
          )}
          / <span>{ingredient.nameUk}</span>
        </div>

        <div className="product-layout">

          {}
          <div className="product-gallery">
            <div className="main-image">
              {!ingredient.available && (
                <div className="product-badge unavailable">Немає в наявності</div>
              )}
              {ingredient.available && ingredient.price && (
                <div className="product-badge">В наявності ✓</div>
              )}
              <img
                src={galleryImages.length > 0 ? galleryImages[activeImageIndex] : '/image-placeholder.png'}
                alt={ingredient.nameUk}
                onError={(e) => { e.target.onerror = null; e.target.src = '/image-placeholder.png'; }}
              />
            </div>

            {galleryImages.length > 0 && (
              <div className="thumbnails">
                {galleryImages.map((img, idx) => (
                  <div
                    key={idx}
                    className={`thumb ${activeImageIndex === idx ? 'active' : ''}`}
                    onClick={() => setActiveImageIndex(idx)}
                  >
                    <img
                      src={img || '/image-placeholder.png'}
                      alt={`preview-${idx}`}
                      onError={(e) => { e.target.onerror = null; e.target.src = '/image-placeholder.png'; }}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          {}
          <div className="product-details">

            {}
            {dietaryTags.length > 0 && (
              <div className="dietary-tags">
                {dietaryTags.map(tag => (
                  <span key={tag.conditionId} className={`tag ${STATUS_CLASSES[tag.status] || 'neutral'}`}>
                    {tag.conditionName}
                  </span>
                ))}
              </div>
            )}

            <h1>{ingredient.nameUk}</h1>

            <div className="weight">
              <span>{ingredient.unit}</span>
              {ingredient.categoryName && (
                <span className="category-chip">{ingredient.categoryName}</span>
              )}
            </div>

            {}
            <div className="price-block">
              <div className="price-current">
                {ingredient.price != null ? `${ingredient.price} ₴` : '—'}
              </div>
              {ingredient.unit && (
                <div className="price-unit">{UNIT_LABELS[ingredient.unit] || ingredient.unit}</div>
              )}
            </div>

            {}
            <div className="action-block">
              <div className="quantity-selector">
                <button
                  className="btn-qty"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || !ingredient.available}
                >−</button>
                <input type="text" className="qty-input" value={quantity} readOnly />
                <button
                  className="btn-qty"
                  onClick={() => setQuantity(quantity + 1)}
                  disabled={!ingredient.available}
                >+</button>
              </div>
              <button
                className={`btn-add-cart ${added ? 'added' : ''}`}
                onClick={handleAddToCart}
                disabled={!ingredient.available || adding}
              >
                {added ? '✓ Додано в кошик' : adding ? 'Додаємо...' : '🛒 Додати в кошик'}
              </button>
            </div>

            {}
            {ingredient.calories && (
              <div className="nutrition-block">
                <div className="nutrition-block-title">Харчова цінність (на 100г). Впевненість ШІ: {Math.round(ingredient.calorieConfidence * 100)}%.</div>
                <div className="nutrition-grid">
                  <div className="nutrition-cell">
                    <div className="nutrition-cell-val">{Math.round(ingredient.calories || 0)}</div>
                    <div className="nutrition-cell-lbl">Ккал</div>
                  </div>
                  <div className="nutrition-cell">
                    <div className="nutrition-cell-val">{Math.round(ingredient.proteinG || 0)}г</div>
                    <div className="nutrition-cell-lbl">Білки</div>
                  </div>
                  <div className="nutrition-cell">
                    <div className="nutrition-cell-val">{Math.round(ingredient.fatG || 0)}г</div>
                    <div className="nutrition-cell-lbl">Жири</div>
                  </div>
                  <div className="nutrition-cell">
                    <div className="nutrition-cell-val">{Math.round(ingredient.carbsG || 0)}г</div>
                    <div className="nutrition-cell-lbl">Вуглеводи</div>
                  </div>
                </div>
              </div>
            )}

            {}
            <div className="description">
              <strong>Про товар</strong>
              <p>
                {ingredient.description ||
                  `Свіжий продукт "${ingredient.nameUk}" — ідеально підходить для улюблених страв або як доповнення до щоденного раціону.`}
              </p>
              {ingredient.aliases?.length > 0 && (
                <div className="aliases-row">
                  <strong style={{ display: 'inline', fontSize: 12 }}>Також відомий як:</strong>{' '}
                  {ingredient.aliases.join(', ')}
                </div>
              )}
            </div>

          </div>
        </div>

        {}
        {relatedRecipes.length > 0 && (
          <div className="product-recipes-section">
            <h2>В яких рецептах використовується</h2>
            <div className="related-recipes-grid">
              {relatedRecipes.map(recipe => (
                <Link to={`/recipe/${recipe.slug}`} key={recipe.id} className="related-recipe-card">
                  <div className="recipe-image">
                    {recipe.imageUrl ? (
                      <img src={recipe.imageUrl} alt={recipe.name} />
                    ) : (
                      <span>{MEAL_TYPE_EMOJIS[recipe.mealType?.toUpperCase()] || '🍲'}</span>
                    )}
                  </div>
                  <div className="recipe-info">
                    <h4>{recipe.name}</h4>
                    <p>{recipe.mealTypeDetailed || recipe.mealType}</p>
                    <span className="btn-outline">Відкрити рецепт →</span>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}

      </main>
    </div>
  );
}