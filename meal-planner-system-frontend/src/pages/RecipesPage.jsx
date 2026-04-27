import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { recipesAPI, recipeTagsAPI } from '../api/api';
import './RecipesPage.css';

// ─── Константи ────────────────────────────────────────────────────────────────

const MEAL_TYPE_LABELS = {
  BREAKFAST: 'Сніданок', LUNCH: 'Обід', DINNER: 'Вечеря',
  DESSERT: 'Десерт', DRINK: 'Напій', SNACK: 'Перекус',
  SAUCE_OR_CONDIMENT: 'Соус', UNCLASSIFIED: 'Інше',
};

const MEAL_TYPE_COLORS = {
  BREAKFAST: '#7C3AED', LUNCH: '#059669', DINNER: '#4338CA',
  DESSERT: '#9C27B0', DRINK: '#0284C7', SNACK: '#D97706',
};

const COOK_TIME_LABELS = {
  MIN_15: '15 хв', MIN_30: '30 хв', MIN_60: '1 год',
  FIFTEEN_MIN: '15 хв', THIRTY_MIN: '30 хв', SIXTY_MIN: '1 год',
  HOURS_4: '4 год', FOUR_HOURS: '4 год',
  DAYS_1_PLUS: '1+ день', ONE_PLUS_DAYS: '1+ день',
};

const MEAL_TYPE_EMOJIS = {
  BREAKFAST: '🥞', LUNCH: '🥗', DINNER: '🐟',
  DESSERT: '🍰', DRINK: '🥤', SNACK: '🥑',
};

const FIXED_FILTERS = [
  { key: 'ALL', label: 'Всі рецепти' },
  { key: 'BREAKFAST', label: '🍳 Сніданки' },
  { key: 'LUNCH', label: '🍲 Обіди' },
  { key: 'DINNER', label: '🥗 Вечері' },
  { key: 'QUICK', label: '⚡ До 15 хвилин' },
  { key: 'high-protein', label: '💪 Високий білок' },
  { key: 'vegan', label: '🌱 Веганські' },
  { key: 'keto', label: '🥑 Кето' },
];

const FIXED_TAG_KEYS = new Set(['high-protein', 'vegan', 'keto']);
const MAX_EXTRA_TAGS = 30;

// ─── Drag scroll hook ────────────────────────────────────────────────────────
function useDragScroll() {
  const ref = useRef(null);
  const isDragging = useRef(false);
  const startX = useRef(0);
  const scrollLeft = useRef(0);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const onMouseDown = (e) => {
      isDragging.current = true;
      startX.current = e.pageX - el.offsetLeft;
      scrollLeft.current = el.scrollLeft;
      el.style.cursor = 'grabbing';
    };
    const stop = () => { isDragging.current = false; el.style.cursor = 'grab'; };
    const onMouseMove = (e) => {
      if (!isDragging.current) return;
      e.preventDefault();
      el.scrollLeft = scrollLeft.current - (e.pageX - el.offsetLeft - startX.current) * 1.2;
    };
    el.addEventListener('mousedown', onMouseDown);
    el.addEventListener('mouseleave', stop);
    el.addEventListener('mouseup', stop);
    el.addEventListener('mousemove', onMouseMove);
    return () => {
      el.removeEventListener('mousedown', onMouseDown);
      el.removeEventListener('mouseleave', stop);
      el.removeEventListener('mouseup', stop);
      el.removeEventListener('mousemove', onMouseMove);
    };
  }, []);
  return ref;
}

function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  return debouncedValue;
}

// ─── Component ───────────────────────────────────────────────────────────────
export default function RecipesPage() {
  const navigate = useNavigate();
  const [recipes, setRecipes] = useState([]);
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [isFetching, setIsFetching] = useState(false);
  const [search, setSearch] = useState('');
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [allPills, setAllPills] = useState(FIXED_FILTERS);
  const pillsRef = useDragScroll();
  const debouncedSearch = useDebounce(search, 500);

  useEffect(() => {
    recipeTagsAPI.getAll()
      .then(res => {
        const extra = (res.data || [])
          .filter(t => !FIXED_TAG_KEYS.has(t.name))
          .slice(0, MAX_EXTRA_TAGS)
          .map(t => ({ key: t.name, label: t.name, tagId: t.id }));
        setAllPills([...FIXED_FILTERS, ...extra]);
      })
      .catch(() => { });
  }, []);

  useEffect(() => {
    fetchRecipes(debouncedSearch);
  }, [activeFilter, debouncedSearch]);

  const fetchRecipes = async (searchQuery = '') => {
    if (!isInitialLoad) setIsFetching(true);
    try {
      const params = { size: 12 };
      if (searchQuery) params.search = searchQuery;
      if (activeFilter !== 'ALL') {
        if (['BREAKFAST', 'LUNCH', 'DINNER', 'DESSERT', 'DRINK', 'SNACK'].includes(activeFilter)) {
          params.mealType = activeFilter;
        } else if (activeFilter === 'QUICK') {
          params.cookTime = 'MIN_15';
        } else {
          params.tag = activeFilter;
        }
      }
      const { data } = await recipesAPI.getAll(params);
      setRecipes(data.content || []);
    } catch (err) {
      console.error(err);
    } finally {
      setIsFetching(false);
      setIsInitialLoad(false);
    }
  };

  const handlePillClick = (pill) => {
    const isFixed = FIXED_FILTERS.some(f => f.key === pill.key);
    if (!isFixed) {
      navigate(`/recipes/category/${encodeURIComponent(pill.key)}`);
      return;
    }
    setActiveFilter(pill.key);
  };

  const activePillLabel = allPills.find(p => p.key === activeFilter)?.label || 'Рецепти';

  return (
    <div className="recipes-page">
      <Navbar />

      <main className="container">

        {/* ── Hero ── */}
        <section className="recipe-hero">
          <h1>Що приготуємо сьогодні?</h1>
          <p>Знайдіть ідеальний рецепт за інгредієнтами, часом приготування або дієтичними вподобаннями.</p>

          {/* Hero stats */}
          <div className="hero-stats">
            <div className="hero-stat">
              <span className="hero-stat-val">500+</span>
              <span className="hero-stat-lbl">Рецептів</span>
            </div>
            <div className="hero-stat">
              <span className="hero-stat-val">12</span>
              <span className="hero-stat-lbl">Категорій</span>
            </div>
            <div className="hero-stat">
              <span className="hero-stat-val">15 хв</span>
              <span className="hero-stat-lbl">Найшвидший рецепт</span>
            </div>
          </div>

          <form className="hero-search" onSubmit={(e) => { e.preventDefault(); fetchRecipes(debouncedSearch); }}>
            <input
              type="text"
              placeholder="Наприклад: салат з авокадо, кето, без глютену..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                if (!isInitialLoad) setIsFetching(true);
              }}
            />
            <button type="submit">Шукати 🔍</button>
          </form>
        </section>

        {/* ── Filter Pills ── */}
        <section className="filters-section">
          <div className="filter-pills" ref={pillsRef}>
            {allPills.map((pill) => {
              const isFixed = FIXED_FILTERS.some(f => f.key === pill.key);
              const isActive = isFixed && activeFilter === pill.key;
              return (
                <div
                  key={pill.key}
                  className={`filter-pill ${isActive ? 'active' : ''} ${!isFixed ? 'filter-pill--tag' : ''}`}
                  onClick={() => handlePillClick(pill)}
                >
                  {pill.label}
                  {!isFixed && <span className="pill-arrow">›</span>}
                </div>
              );
            })}
          </div>
        </section>

        {/* ── Recipe Grid ── */}
        <section>
          <div className="section-header">
            <h2 className="section-title">
              {activeFilter === 'ALL' ? 'Вибір дня' : activePillLabel}
            </h2>
            {!isInitialLoad && (
              <span className="section-count">{recipes.length} рецептів</span>
            )}
          </div>

          {isInitialLoad ? (
            <div className="recipes-loading"><div className="spinner" /></div>
          ) : recipes.length === 0 && !isFetching ? (
            <div className="recipes-empty">
              <h3>Рецептів не знайдено</h3>
              <p>Спробуйте змінити умови пошуку або фільтрації</p>
            </div>
          ) : (
            <div className={`recipes-grid-wrapper ${isFetching ? 'fetching-mask' : ''}`}>
              <div className="recipes-grid">
                {recipes.map(recipe => {
                  const mealKey = recipe.mealType?.toUpperCase();
                  return (
                    <Link to={`/recipe/${recipe.slug}`} key={recipe.id} className="recipe-card">
                      <div className="recipe-img">
                        {recipe.imageUrl ? (
                          <img src={recipe.imageUrl} alt={recipe.name} />
                        ) : (
                          <div className="recipe-emoji-wrap">
                            <span className="recipe-emoji">{MEAL_TYPE_EMOJIS[mealKey] || '🍽️'}</span>
                            <span className="recipe-emoji-sub">{MEAL_TYPE_LABELS[mealKey] || 'Рецепт'}</span>
                          </div>
                        )}

                        {/* Category badge (shown only when image exists to avoid overlap with emoji-sub) */}
                        {recipe.imageUrl && (
                          <div
                            className="recipe-category-badge"
                            style={{ background: MEAL_TYPE_COLORS[mealKey] || 'var(--primary)' }}
                          >
                            {MEAL_TYPE_LABELS[mealKey] || recipe.mealType}
                          </div>
                        )}

                        {/* Time badge */}
                        {recipe.cookTime && (
                          <div className="recipe-time-badge">
                            ⏱️ {COOK_TIME_LABELS[recipe.cookTime] || recipe.cookTime}
                          </div>
                        )}
                      </div>

                      <div className="recipe-content">
                        <h3 className="recipe-title">{recipe.name}</h3>

                        {/* Macro pills */}
                        <div className="macro-pills">
                          {recipe.calories != null && (
                            <span className="macro-pill" style={{ background: '#FEF3C7', color: '#92400E', borderColor: '#FDE68A' }}>
                              🔥 {Math.round(recipe.calories)} ккал
                            </span>
                          )}
                          {recipe.proteinG != null && (
                            <span className="macro-pill protein">Б {Math.round(recipe.proteinG)}г</span>
                          )}
                          {recipe.totalFatG != null && (
                            <span className="macro-pill fat">Ж {Math.round(recipe.totalFatG)}г</span>
                          )}
                          {recipe.totalCarbsG != null && (
                            <span className="macro-pill carbs">В {Math.round(recipe.totalCarbsG)}г</span>
                          )}
                        </div>

                        {/* Tags */}
                        {recipe.tags?.length > 0 && (
                          <div className="recipe-tags" style={{ marginTop: '8px' }}>
                            {recipe.tags.slice(0, 2).map(tag => (
                              <span key={tag} className="r-tag">{tag}</span>
                            ))}
                          </div>
                        )}

                        {/* Bottom row */}
                        <div className="recipe-nutrition">
                          <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 500 }}>
                            {recipe.cookComplexity ? `📊 ${recipe.cookComplexity}` : ''}
                            {recipe.servings ? ` · ${recipe.servings} порції` : ''}
                          </span>
                          <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--primary)' }}>
                            Рецепт →
                          </span>
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>

              <div className="see-more-row">
                <Link to="/recipes/catalog" className="btn-see-more">
                  Переглянути всі рецепти з фільтрами →
                </Link>
              </div>
            </div>
          )}
        </section>

      </main>
    </div>
  );
}