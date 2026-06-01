import { useState, useEffect, useCallback } from 'react';
import { recipesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import AdminRecipeFormModal from '../components/AdminRecipeFormModal';
import './AdminRecipesPage.css';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'DESSERT', 'DRINK', 'SNACK', 'SAUCE_OR_CONDIMENT', 'UNCLASSIFIED'];
const COMPLEXITIES = ['EASY', 'MEDIUM', 'HARD'];

export default function AdminRecipesPage() {
  const [recipes, setRecipes] = useState([]);
  const [page, setPage] = useState(0);
  const [pageMeta, setPageMeta] = useState({ totalPages: 0, totalElements: 0 });
  const [search, setSearch] = useState('');
  const [mealType, setMealType] = useState('');
  const [complexity, setComplexity] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [formModal, setFormModal] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const pageSize = 15;

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size: pageSize };
      if (search.trim()) params.search = search.trim();
      if (mealType) params.mealType = mealType;
      if (complexity) params.cookComplexity = complexity;

      const { data } = await recipesAPI.getAll(params);
      setRecipes(data.content || []);
      setPageMeta(data.page || { totalPages: 0, totalElements: 0 });
    } catch (err) {
      setError('Помилка завантаження рецептів');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, search, mealType, complexity]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSearch = (value) => { setSearch(value); setPage(0); };
  const handleMealTypeFilter = (value) => { setMealType(value); setPage(0); };
  const handleComplexityFilter = (value) => { setComplexity(value); setPage(0); };

  const handleCreateSave = async (payload) => {
    await recipesAPI.create(payload);
    setFormModal(null);
    fetchData();
  };

  const handleEditSave = async (payload) => {
    await recipesAPI.update(formModal.recipe.id, payload);
    setFormModal(null);
    fetchData();
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await recipesAPI.remove(deleteConfirm.id);
      setDeleteConfirm(null);
      fetchData();
    } catch (err) {
      console.error('Delete error:', err);
    }
  };

  const getVisiblePages = () => {
    const total = pageMeta.totalPages;
    if (total <= 10) return Array.from({ length: total }, (_, i) => i);
    const pages = [];
    let start = Math.max(0, page - 4);
    let end = Math.min(total - 1, start + 9);
    if (end - start < 9) start = Math.max(0, end - 9);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  const lastPage = pageMeta.totalPages - 1;

  return (
    <div className="admin-page">
      <Navbar />

      <div className="admin-container animate-fade-in">
        <div className="admin-header">
          <div>
            <h1 className="admin-title">
              <span className="gradient-text">Управління рецептами</span>
            </h1>
            <p className="admin-subtitle">
              Всього: {pageMeta.totalElements} рецептів
            </p>
          </div>
          <button className="admin-btn admin-btn-primary" onClick={() => setFormModal({})}>
            + Створити
          </button>
        </div>

        <div className="admin-filters glass-card">
          <div className="admin-filter-item admin-search-box">
            <span className="admin-search-icon">🔍</span>
            <input
              type="text"
              placeholder="Пошук за назвою…"
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
            />
          </div>
          <select className="admin-filter-select" value={mealType} onChange={(e) => handleMealTypeFilter(e.target.value)}>
            <option value="">Всі прийоми їжі</option>
            {MEAL_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <select className="admin-filter-select" value={complexity} onChange={(e) => handleComplexityFilter(e.target.value)}>
            <option value="">Будь-яка складність</option>
            {COMPLEXITIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        <div className="admin-table-wrap glass-card">
          {loading ? (
            <div className="admin-loading"><div className="spinner"></div></div>
          ) : error ? (
            <div className="admin-error">
              <p>{error}</p>
              <button className="admin-btn admin-btn-ghost" onClick={fetchData}>Спробувати знову</button>
            </div>
          ) : recipes.length === 0 ? (
            <div className="admin-empty">Рецептів не знайдено</div>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Фото</th>
                  <th>Назва</th>
                  <th>Прийом їжі</th>
                  <th>Час</th>
                  <th>Складність</th>
                  <th>Калорії</th>
                  <th>Дії</th>
                </tr>
              </thead>
              <tbody>
                {recipes.map((rec) => (
                  <tr key={rec.id}>
                    <td className="td-id">{rec.id}</td>
                    <td className="td-img">
                      {rec.imageUrl ? (
                        <img src={rec.imageUrl} alt="" />
                      ) : (
                        <span className="td-img-placeholder">🍲</span>
                      )}
                    </td>
                    <td className="td-name">
                      <span>{rec.name}</span>
                      <small className="td-slug">{rec.slug}</small>
                    </td>
                    <td>{rec.mealType}</td>
                    <td>{rec.cookTime?.replace('MIN_', '') + ' хв'}</td>
                    <td>{rec.cookComplexity}</td>
                    <td>{rec.calories || rec.nutrition?.calories || '—'} ккал</td>
                    <td className="td-actions">
                      <button className="admin-action-btn" title="Редагувати" onClick={() => setFormModal({ recipe: rec })}>✏️</button>
                      <button className="admin-action-btn admin-action-danger" title="Видалити" onClick={() => setDeleteConfirm(rec)}>🗑️</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {pageMeta.totalPages > 1 && (
          <div className="admin-pagination">
            <button className="admin-btn admin-btn-ghost admin-btn-icon" disabled={page === 0} onClick={() => setPage(0)} title="На початок">
              ⟪
            </button>
            <button className="admin-btn admin-btn-ghost admin-btn-icon" disabled={page === 0} onClick={() => setPage(page - 1)} title="Назад">
              ←
            </button>
            <div className="admin-page-numbers">
              {getVisiblePages()[0] > 0 && <span className="admin-page-ellipsis">…</span>}
              {getVisiblePages().map((p) => (
                <button
                  key={p}
                  className={`admin-page-btn ${p === page ? 'active' : ''}`}
                  onClick={() => setPage(p)}
                >
                  {p + 1}
                </button>
              ))}
              {getVisiblePages()[getVisiblePages().length - 1] < lastPage && <span className="admin-page-ellipsis">…</span>}
            </div>
            <button className="admin-btn admin-btn-ghost admin-btn-icon" disabled={page >= lastPage} onClick={() => setPage(page + 1)} title="Далі">
              →
            </button>
            <button className="admin-btn admin-btn-ghost admin-btn-icon" disabled={page >= lastPage} onClick={() => setPage(lastPage)} title="В кінець">
              ⟫
            </button>
          </div>
        )}
      </div>

      {formModal && (
        <AdminRecipeFormModal
          recipe={formModal.recipe || null}
          onSave={formModal.recipe ? handleEditSave : handleCreateSave}
          onClose={() => setFormModal(null)}
        />
      )}

      {deleteConfirm && (
        <div className="admin-modal-overlay" onClick={() => setDeleteConfirm(null)}>
          <div className="admin-modal admin-modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>Підтвердити видалення</h2>
              <button className="admin-modal-close" onClick={() => setDeleteConfirm(null)}>✕</button>
            </div>
            <div className="admin-modal-body">
              <p>Видалити <strong>{deleteConfirm.name}</strong>?</p>
              <p className="admin-delete-warn">Цю дію неможливо скасувати.</p>
            </div>
            <div className="admin-modal-actions">
              <button className="admin-btn admin-btn-ghost" onClick={() => setDeleteConfirm(null)}>Скасувати</button>
              <button className="admin-btn admin-btn-danger" onClick={handleDelete}>Видалити</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
