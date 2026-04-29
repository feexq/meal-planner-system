import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { recipesAPI, productsAPI, cartAPI } from '../api/api';
import './RecipeDetailPage.css';

const COOK_TIME_LABELS = {
  MIN_15: '15 хв', MIN_30: '30 хв', MIN_60: '1 год',
  FIFTEEN_MIN: '15 хв', THIRTY_MIN: '30 хв', SIXTY_MIN: '1 год',
  HOURS_4: '4 год', FOUR_HOURS: '4 год',
  DAYS_1_PLUS: '1+ день', ONE_PLUS_DAYS: '1+ день',
};

const MEAL_TYPE_LABELS = {
  BREAKFAST: 'Сніданок', LUNCH: 'Обід', DINNER: 'Вечеря',
  DESSERT: 'Десерт', DRINK: 'Напій', SNACK: 'Перекус',
  SAUCE_OR_CONDIMENT: 'Соус', UNCLASSIFIED: 'Інше',
};

const MEAL_TYPE_EMOJIS = {
  BREAKFAST: '🥞', LUNCH: '🥗', DINNER: '🐟',
  DESSERT: '🍰', DRINK: '🥤', SNACK: '🥑',
};

const COMPLEXITY_LABELS = {
  EASY: '🟢 Легко', MEDIUM: '🟡 Середньо', HARD: '🔴 Складно',
};

export default function RecipeDetailPage() {
  const { slug } = useParams();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [addingToCart, setAddingToCart] = useState(false);
  const [cartSuccess, setCartSuccess] = useState(false);
  const [productsMap, setProductsMap] = useState({});
  const [completedSteps, setCompletedSteps] = useState(new Set());

  useEffect(() => { fetchRecipe(); }, [slug]);

  const fetchRecipe = async () => {
    setLoading(true);
    try {
      const { data } = /^\d+$/.test(slug)
        ? await recipesAPI.getById(slug)
        : await recipesAPI.getBySlug(slug);
      setRecipe(data);


      if (data.ingredients?.length > 0) {
        const ingredientIds = data.ingredients.map(ing => ing.ingredientId);
        try {
          const productsData = await productsAPI.findAllByIngredients(ingredientIds);
          const map = {};
          productsData.forEach(p => map[p.id] = p);
          setProductsMap(map);
        } catch (err) {
          console.error('Failed to fetch products:', err);
        }
      }
    } catch (error) {
      console.error('Error fetching recipe:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    console.log("Оновлений productsMap:", productsMap);
  }, [productsMap]);

  const toggleStep = (index) => {
    setCompletedSteps(prev => {
      const next = new Set(prev);
      next.has(index) ? next.delete(index) : next.add(index);
      return next;
    });
  };

  const handleAddToCart = async () => {
    if (!recipe) return;
    setAddingToCart(true);
    try {
      await cartAPI.addRecipeIngredients(recipe.id);
      setCartSuccess(true);
      setTimeout(() => setCartSuccess(false), 2500);
    } catch (error) {
      console.error('Cart error:', error);
    } finally {
      setAddingToCart(false);
    }
  };

  if (loading) return (
    <div className="recipe-detail-page">
      <Navbar />
      <div className="page-loader"><div className="spinner" /></div>
    </div>
  );

  if (!recipe) return (
    <div className="recipe-detail-page">
      <Navbar />
      <div className="container" style={{ paddingTop: 100, textAlign: 'center' }}>
        <h2>Рецепт не знайдено</h2>
        <Link to="/recipes" style={{ marginTop: 20, display: 'inline-block', padding: '12px 24px', background: 'var(--primary)', color: '#fff', borderRadius: 8, fontWeight: 600, textDecoration: 'none' }}>
          Всі рецепти
        </Link>
      </div>
    </div>
  );

  const { nutrition, ingredients } = recipe;

  const productsArray = Object.values(productsMap);

  const availableCount = productsArray.filter(p => p.available).length || 0;
  const totalCount = productsArray.length || 0;

  const totalCost = productsArray
    .filter(p => p.available)
    .reduce((sum, p) => sum + (p.price || 0), 0) || 0;

  const availabilityPct = totalCount ? Math.round((availableCount / totalCount) * 100) : 0;

  const sortedSteps = recipe.steps
    ? [...recipe.steps].sort((a, b) => {
      const n = s => parseInt(s.match(/^\d+/)?.[0] || '0');
      return n(a) - n(b);
    })
    : [];

  const completedCount = completedSteps.size;
  const stepProgress = sortedSteps.length ? Math.round((completedCount / sortedSteps.length) * 100) : 0;
  let ingredientsRawList = [];

  try {
    ingredientsRawList = recipe.ingredientsRawStr
      ? JSON.parse(recipe.ingredientsRawStr)
      : [];
  } catch (e) {
    console.error("Invalid ingredients JSON:", e);
    ingredientsRawList = [];
  }

  return (
    <div className="recipe-detail-page">
      <Navbar />

      <main className="container">

        {}
        <div className="breadcrumbs">
          <Link to="/">Головна</Link> /
          <Link to="/recipes">Рецепти</Link>
          {recipe.mealType && (
            <> / <Link to={`/recipes/category/${recipe.mealType}`}>
              {MEAL_TYPE_LABELS[recipe.mealType.toUpperCase()] || recipe.mealType}
            </Link></>
          )}
          / <span>{recipe.name}</span>
        </div>

        {}
        <div className="recipe-header">
          <h1>{recipe.name}</h1>
          <div className="recipe-meta">
            {recipe.mealType && (
              <Link
                to={`/recipes/category/${recipe.mealType.toUpperCase()}`}
                className="meta-highlight"
              >
                {MEAL_TYPE_LABELS[recipe.mealType.toUpperCase()] || recipe.mealType}
              </Link>
            )}
            {recipe.cookComplexity.toUpperCase() && (
              <span className="meta-item">
                {COMPLEXITY_LABELS[recipe.cookComplexity.toUpperCase()] || recipe.cookComplexity.toUpperCase()}
              </span>
            )}
            {recipe.cookTime && (
              <span className="meta-item">
                ⏱️ {COOK_TIME_LABELS[recipe.cookTime] || recipe.cookTime}
              </span>
            )}
            {recipe.servings && (
              <span className="meta-item">🍽️ {recipe.servings} порції</span>
            )}
            {nutrition?.calories && (
              <span className="meta-item">🔥 {Math.round(nutrition.calories)} ккал</span>
            )}
          </div>
        </div>

        <div className="recipe-layout">

          {}
          <div className="main-content">

            {}
            <div className="recipe-image" style={recipe.imageUrl ? { backgroundImage: `url(${recipe.imageUrl})` } : {}}>
              {!recipe.imageUrl && (MEAL_TYPE_EMOJIS[recipe.mealType] || '🥑')}
            </div>

            {}
            {nutrition && (
              <div className="macros-grid">
                <div className="macro-card">
                  <div className="macro-val" style={{ color: 'var(--accent)' }}>
                    {Math.round(nutrition.calories || 0)}
                  </div>
                  <div className="macro-lbl">Ккал</div>
                </div>
                <div className="macro-card">
                  <div className="macro-val" style={{ color: '#3B82F6' }}>
                    {Math.round(nutrition.proteinG || 0)}г
                  </div>
                  <div className="macro-lbl">Білки</div>
                </div>
                <div className="macro-card">
                  <div className="macro-val" style={{ color: '#F59C0A' }}>
                    {Math.round(nutrition.totalFatG || 0)}г
                  </div>
                  <div className="macro-lbl">Жири</div>
                </div>
                <div className="macro-card">
                  <div className="macro-val" style={{ color: '#10B981' }}>
                    {Math.round(nutrition.totalCarbsG || 0)}г
                  </div>
                  <div className="macro-lbl">Вуглеводи</div>
                </div>
              </div>
            )}

            {}
            {recipe.description && (
              <div className="recipe-description">{recipe.description}</div>
            )}

            {}
            {recipe.tags?.length > 0 && (
              <div className="recipe-tag-list">
                {recipe.tags.map(tag => (
                  <Link key={tag} to={`/recipes/category/${tag}`} className="recipe-tag-chip">
                    {tag}
                  </Link>
                ))}
              </div>
            )}

            {}
            {sortedSteps.length > 0 && (
              <div className="steps-section">
                <div className="steps-header">
                  <h2>Спосіб приготування</h2>
                  <span className="steps-hint">
                    {completedCount}/{sortedSteps.length} виконано
                  </span>
                </div>

                {}
                <div className="steps-progress">
                  <div className="steps-progress-fill" style={{ width: `${stepProgress}%` }} />
                </div>

                <div className="step-list">
                  {sortedSteps.map((step, index) => (
                    <div
                      key={index}
                      className={`step-item ${completedSteps.has(index) ? 'completed' : ''}`}
                      onClick={() => toggleStep(index)}
                    >
                      <div className="step-number">
                        {completedSteps.has(index) ? '✓' : index + 1}
                      </div>
                      <div className="step-text">{step.includes("|||") ? step.split("|||")[1] : step}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {}
          <aside>
            <div className="sidebar-sticky-wrap">
              <div className="shop-widget">

                {}
                <div className="widget-header">
                  <div className="widget-title">Продукти</div>
                  <div className="widget-availability">
                    <div className="availability-count">{availableCount}/{totalCount}</div>
                    <div className="availability-label">в наявності</div>
                  </div>
                </div>

                {}
                <div className="availability-bar">
                  <div className="availability-bar-fill" style={{ width: `${availabilityPct}%` }} />
                </div>

                {}
                {}
                <div className="ing-list">
                  {Object.values(productsMap).map((product, i) => (
                    <div
                      key={product.id || i}
                      className={`ing-row ${product.available ? 'ing-available' : 'ing-missing'}`}
                    >
                      <div className="ing-main">
                        <div className={`ing-dot ${product.available ? 'dot-in' : 'dot-out'}`} />
                        <span className="ing-name">
                          {product.nameUk || 'Невідомий продукт'}
                        </span>
                      </div>

                      {}
                      <span className="ing-amount">{product.unit.replace(/(\d+)([^\d]+)/, '$1 $2')}</span>

                      {product.price > 0 && (
                        <span className="ing-price">₴{product.price}</span>
                      )}
                    </div>
                  ))}
                </div>

                <div className="package-warning-alert" style={{ marginBottom: '16px' }}>
                  <strong>⚠️ Зверніть увагу:</strong> Ціни вказані за кількість товару, цієї кількості може не вистачити для приготування страви.
                  При натисканні "Додати у кошик" система автоматично розрахує та додасть <b>необхідну кількість цілих упаковок. Тому ціна може відрізнятися</b>.
                </div>

                {}
                {totalCount > 0 && (
                  <button
                    className={`btn-buy ${cartSuccess ? 'success' : ''}`}
                    onClick={handleAddToCart}
                    disabled={addingToCart || cartSuccess || availableCount === 0}
                  >
                    {cartSuccess
                      ? '✓ Додано в кошик'
                      : addingToCart
                        ? 'Додаємо...'
                        : `🛒 Додати у кошик (${availableCount}/${totalCount})`}
                  </button>
                )}
                {availableCount < totalCount && totalCount > 0 && (
                  <div className="missing-alert">
                    ⚠️ {totalCount - availableCount} інгредієнти відсутні в магазині — буде додано лише наявні товари.
                  </div>
                )}
              </div>

              {}
              {ingredientsRawList.length > 0 && (
                <div className="ingredients-raw-section">
                  <h3 className="section-title">Склад за рецептом</h3>
                  <div className="ingredients-raw-list">
                    {ingredientsRawList.map((item, index) => (
                      <div key={index} className="ingredient-raw-item">
                        <div className="raw-item-main">
                          <span className="raw-item-name">{item.name_uk || item.name}</span>
                          {item.note && <span className="raw-item-note">({item.note})</span>}
                        </div>
                        <div className="raw-item-amount">
                          {item.amount} {item.unit}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </aside>

        </div>
      </main>
    </div>
  );
}