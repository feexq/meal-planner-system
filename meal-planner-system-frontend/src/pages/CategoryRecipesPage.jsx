import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { recipesAPI, recipeTagsAPI } from '../api/api';
import './CategoryRecipesPage.css';

const COOK_TIME_LABELS = {
  MIN_15: '⏱️ 15 хв', MIN_30: '⏱️ 30 хв', MIN_60: '⏱️ 1 год',
  HOURS_4: '⏱️ 4 год', DAYS_1_PLUS: '⏱️ 1+ день',
};

const BUDGET_LABELS = { LOW: 'Економ', MEDIUM: 'Середній', HIGH: 'Преміум' };

const MEAL_TYPE_EMOJIS = {
  BREAKFAST: '🥞', LUNCH: '🥗', DINNER: '🐟',
  DESSERT: '🍰', DRINK: '🥤', SNACK: '🥑',
};

export default function CategoryRecipesPage() {
  const { slug } = useParams();
  const navigate = useNavigate();

  const [recipes, setRecipes] = useState([]);
  const [tagInfo, setTagInfo] = useState(null);
  const [availableTags, setAvailableTags] = useState([]);
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [isFetching, setIsFetching] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(24);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [isTagsExpanded, setIsTagsExpanded] = useState(false);
  const [tagSearchQuery, setTagSearchQuery] = useState('');
  const [filters, setFilters] = useState({ budget: [], cookTime: [], mealType: [], tags: [] });

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchQuery), 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    recipeTagsAPI.getAll()
      .then(res => {
        const tags = res.data || [];
        setAvailableTags(tags);
        if (slug) {
          const found = tags.find(t => t.slug?.toLowerCase() === slug.toLowerCase());
          setTagInfo(found ?? { name: slug });
        } else {
          setTagInfo({ name: 'Каталог рецептів', description: 'Обирайте з найкращих страв нашого сервісу.' });
        }
      })
      .catch(() => { });
  }, [slug]);

  useEffect(() => { setCurrentPage(0); }, [slug, filters, debouncedSearch, pageSize]);

  useEffect(() => { fetchRecipes(); }, [slug, filters, debouncedSearch, currentPage, pageSize]);

  const fetchRecipes = async () => {
    if (!isInitialLoad) setIsFetching(true);
    try {
      const params = { size: pageSize, page: currentPage };
      const allTagsSet = new Set([...filters.tags]);
      if (slug) allTagsSet.add(slug);
      const tagsArray = Array.from(allTagsSet);
      if (tagsArray.length > 0) {
        params.tags = tagsArray.join(',');
        params.tagsCount = tagsArray.length;
      }
      if (filters.mealType.length > 0) params.mealTypes = filters.mealType.join(',');
      if (filters.cookTime.length > 0) params.cookTimes = filters.cookTime.join(',');
      if (filters.budget.length > 0) params.cookBudgets = filters.budget.join(',');
      if (debouncedSearch?.trim().length >= 3) params.search = debouncedSearch.trim();

      const { data } = await recipesAPI.getAllWithFilters(params);
      setRecipes(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setTotalElements(data.page?.totalElements || 0);
    } catch (err) {
      console.error(err);
      setRecipes([]);
    } finally {
      setIsFetching(false);
      setIsInitialLoad(false);
    }
  };

  const toggleTag = (tag) => {
    const id = tag.slug || tag.name;
    if (slug && id.toLowerCase() === slug.toLowerCase()) {
      navigate('/recipes/catalog');
      return;
    }
    setFilters(prev => ({
      ...prev,
      tags: prev.tags.includes(id) ? prev.tags.filter(t => t !== id) : [...prev.tags, id],
    }));
  };

  const toggleFilter = (group, value) => {
    setFilters(prev => ({
      ...prev,
      [group]: prev[group].includes(value)
        ? prev[group].filter(i => i !== value)
        : [...prev[group], value],
    }));
  };

  const resetFilters = () => {
    setFilters({ budget: [], cookTime: [], mealType: [], tags: [] });
    setSearchQuery('');
    if (slug) navigate('/recipes/catalog');
  };

  const hasActiveFilters = filters.budget.length || filters.cookTime.length || filters.mealType.length || filters.tags.length || searchQuery || slug;

  return (
    <div className="category-recipes-page">
      <Navbar />

      <main className="container">

        {}
        <div className="category-header">
          <div className="breadcrumbs">
            <Link to="/recipes">Рецепти</Link>
            {' / '}<span>{tagInfo?.name || '...'}</span>
          </div>
          <h1>{tagInfo?.name || '...'}</h1>
          {tagInfo?.description && <p>{tagInfo.description}</p>}
          {!isInitialLoad && (
            <div className="header-stats">
              <div className="header-stat">
                <span className="header-stat-val">{totalElements}</span>
                <span className="header-stat-lbl">рецептів</span>
              </div>
              {filters.budget.length + filters.mealType.length + filters.cookTime.length + filters.tags.length > 0 && (
                <div className="header-stat">
                  <span className="header-stat-val">
                    {filters.budget.length + filters.mealType.length + filters.cookTime.length + filters.tags.length}
                  </span>
                  <span className="header-stat-lbl">фільтрів</span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="main-layout">

          {}
          <aside className="sidebar">
            <div className="sidebar-header">
              <h2>Фільтри</h2>
              {hasActiveFilters && (
                <button className="btn-reset-text" onClick={resetFilters}>Скинути</button>
              )}
            </div>

            {}
            <div className="filter-group search-group">
              <div className="search-bar-local">
                <span className="search-icon">🔍</span>
                <input
                  type="text"
                  placeholder="Назва страви..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            {}
            {availableTags.length > 0 && (
              <div className="filter-group">
                <span className="filter-label">Категорії та Теги</span>
                <div className="tag-search-container">
                  <input
                    type="text"
                    placeholder="Пошук тегів..."
                    value={tagSearchQuery}
                    onChange={e => setTagSearchQuery(e.target.value)}
                  />
                </div>
                <ul className="filter-list tag-list-scrollable">
                  {availableTags
                    .filter(tag => tag.name?.toLowerCase().includes(tagSearchQuery.toLowerCase()))
                    .sort((a, b) => {
                      const aId = a.slug || a.name;
                      const bId = b.slug || b.name;
                      const aChecked = filters.tags.includes(aId) || (slug && aId === slug);
                      const bChecked = filters.tags.includes(bId) || (slug && bId === slug);
                      if (aChecked && !bChecked) return -1;
                      if (!aChecked && bChecked) return 1;
                      return (b.recipeCount || 0) - (a.recipeCount || 0);
                    })
                    .slice(0, isTagsExpanded || tagSearchQuery ? availableTags.length : 10)
                    .map(tag => {
                      const id = tag.slug || tag.name;
                      const isChecked = filters.tags.includes(id) || (slug && id === slug);
                      return (
                        <label key={tag.id || id} className="filter-item">
                          <input type="checkbox" checked={isChecked} onChange={() => toggleTag(tag)} />
                          <span className="tag-name">{tag.name}</span>
                          {tag.recipeCount > 0 && <span className="tag-count">{tag.recipeCount}</span>}
                        </label>
                      );
                    })}
                </ul>
                {!tagSearchQuery && availableTags.length > 10 && (
                  <button className="btn-toggle-tags" onClick={() => setIsTagsExpanded(!isTagsExpanded)}>
                    {isTagsExpanded ? 'Згорнути ↑' : `Показати ще (${availableTags.length - 10}) ↓`}
                  </button>
                )}
              </div>
            )}

            {}
            <div className="filter-group">
              <span className="filter-label">Прийом їжі</span>
              <ul className="filter-list">
                {[
                  ['BREAKFAST', '🍳 Сніданок'],
                  ['LUNCH', '☀️ Обід'],
                  ['DINNER', '🌙 Вечеря'],
                  ['SNACK', '🍎 Перекус'],
                  ['DESSERT', '🍰 Десерт'],
                ].map(([val, label]) => (
                  <label key={val} className="filter-item">
                    <input type="checkbox" checked={filters.mealType.includes(val)} onChange={() => toggleFilter('mealType', val)} />
                    <span>{label}</span>
                  </label>
                ))}
              </ul>
            </div>

            {}
            <div className="filter-group">
              <span className="filter-label">Бюджет</span>
              <ul className="filter-list">
                {[['LOW', '💚 Економ'], ['MEDIUM', '💛 Середній'], ['HIGH', '💜 Преміум']].map(([val, label]) => (
                  <label key={val} className="filter-item">
                    <input type="checkbox" checked={filters.budget.includes(val)} onChange={() => toggleFilter('budget', val)} />
                    <span>{label}</span>
                  </label>
                ))}
              </ul>
            </div>

            {}
            <div className="filter-group">
              <span className="filter-label">Час приготування</span>
              <ul className="filter-list">
                {[['MIN_15', '⚡ До 15 хв'], ['MIN_30', '🕐 До 30 хв'], ['MIN_60', '🕑 До 1 год'], ['HOURS_4', '🕓 До 4 год']].map(([val, label]) => (
                  <label key={val} className="filter-item">
                    <input type="checkbox" checked={filters.cookTime.includes(val)} onChange={() => toggleFilter('cookTime', val)} />
                    <span>{label}</span>
                  </label>
                ))}
              </ul>
            </div>

            <div className="filter-group">
              <span className="filter-label">На сторінці</span>
              <select className="page-size-select" value={pageSize} onChange={e => setPageSize(Number(e.target.value))}>
                <option value={12}>12 рецептів</option>
                <option value={24}>24 рецепти</option>
                <option value={48}>48 рецептів</option>
              </select>
            </div>
          </aside>

          {}
          <section className="catalog-content">
            <div className="catalog-toolbar">
              <span className="results-count">
                Знайдено: <strong>{totalElements || recipes.length}</strong>
                {hasActiveFilters && (
                  <button className="btn-reset-inline" onClick={resetFilters}>Скинути фільтри</button>
                )}
              </span>
            </div>

            {isInitialLoad ? (
              <div className="grid-loading"><div className="spinner" /></div>
            ) : recipes.length === 0 && !isFetching ? (
              <div className="grid-empty">
                <h3>Нічого не знайдено 😕</h3>
                <p style={{ marginBottom: 16 }}>Спробуйте змінити або скинути фільтри</p>
                <button className="btn-primary" onClick={resetFilters}>Скинути фільтри</button>
              </div>
            ) : (
              <div className={`catalog-grid-wrapper ${isFetching ? 'fetching-mask' : ''}`}>
                <div className="recipe-grid">
                  {recipes.map(recipe => (
                    <Link to={`/recipe/${recipe.slug}`} key={recipe.id} className="recipe-card">
                      <div className="recipe-img" style={recipe.imageUrl ? { backgroundImage: `url(${recipe.imageUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}}>
                        {!recipe.imageUrl && (
                          <span className="recipe-emoji">{MEAL_TYPE_EMOJIS[recipe.mealType] || '🍽️'}</span>
                        )}
                        <div className="recipe-badges">
                          {recipe.cookTime && (
                            <div className="badge-time">{COOK_TIME_LABELS[recipe.cookTime] || recipe.cookTime}</div>
                          )}
                          {recipe.cookBudget && (
                            <div className="badge-budget">{BUDGET_LABELS[recipe.cookBudget]}</div>
                          )}
                        </div>
                        {}
                        {recipe.matchPercent != null && (
                          <div className="match-pct">✓ {Math.round(recipe.matchPercent)}%</div>
                        )}
                      </div>

                      <div className="recipe-info">
                        <h3 className="recipe-title">{recipe.name}</h3>

                        {}
                        <div className="macro-mini">
                          {recipe.calories != null && (
                            <span className="macro-mini-pill m-kcal">🔥 {Math.round(recipe.calories)} ккал</span>
                          )}
                          {recipe.proteinG != null && (
                            <span className="macro-mini-pill m-p">Б {Math.round(recipe.proteinG)}г</span>
                          )}
                          {recipe.totalFatG != null && (
                            <span className="macro-mini-pill m-f">Ж {Math.round(recipe.totalFatG)}г</span>
                          )}
                          {recipe.totalCarbsG != null && (
                            <span className="macro-mini-pill m-c">В {Math.round(recipe.totalCarbsG)}г</span>
                          )}
                        </div>

                        {}
                        {recipe.tags?.length > 0 && (
                          <div className="recipe-tags">
                            {recipe.tags.slice(0, 2).map(tag => (
                              <span key={tag} className="r-tag">{tag}</span>
                            ))}
                          </div>
                        )}

                        <div className="recipe-meta">
                          <span className="recipe-cals">
                            {recipe.servings ? `${recipe.servings} порц.` : ''}
                          </span>
                          <span className="btn-view">Рецепт →</span>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            )}

            {}
            {totalPages > 1 && (
              <div className="pagination">
                <button disabled={currentPage === 0} onClick={() => setCurrentPage(p => p - 1)} className="page-btn">← Назад</button>
                <div className="page-numbers">
                  {[...Array(totalPages)].map((_, i) => (
                    (i === 0 || i === totalPages - 1 || (i >= currentPage - 1 && i <= currentPage + 1)) ? (
                      <button key={i} onClick={() => setCurrentPage(i)} className={`page-num ${currentPage === i ? 'active' : ''}`}>{i + 1}</button>
                    ) : (
                      (i === 1 || i === totalPages - 2) ? <span key={i}>...</span> : null
                    )
                  ))}
                </div>
                <button disabled={currentPage >= totalPages - 1} onClick={() => setCurrentPage(p => p + 1)} className="page-btn">Далі →</button>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}