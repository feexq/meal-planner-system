import { useState, useEffect, useCallback } from 'react';
import { ingredientsAPI, categoriesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import AdminIngredientFormModal from '../components/AdminIngredientFormModal';
import AdminIngredientDietaryTagsModal from '../components/AdminIngredientDietaryTagsModal';
import './AdminIngredientsPage.css';

const UNIT_LABELS = { KG: 'кг', G: 'г', L: 'л', ML: 'мл', PCS: 'шт', BUNCH: 'пучок' };

export default function AdminIngredientsPage() {
  const [ingredients, setIngredients] = useState([]);
  const [page, setPage] = useState(0);
  const [pageMeta, setPageMeta] = useState({ totalPages: 0, totalElements: 0 });
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [availability, setAvailability] = useState('');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // modals
  const [formModal, setFormModal] = useState(null); // null | { ingredient? }
  const [tagsModal, setTagsModal] = useState(null); // null | ingredient
  const [deleteConfirm, setDeleteConfirm] = useState(null); // null | ingredient

  const pageSize = 15;

  useEffect(() => {
    categoriesAPI.getAll().then(({ data }) => setCategories(data)).catch(() => {});
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size: pageSize };
      if (search.trim()) params.search = search.trim();
      if (categoryId) params.categoryId = Number(categoryId);
      if (availability !== '') params.available = availability === 'true';

      const { data } = await ingredientsAPI.getAll(params);
      setIngredients(data.content || []);
      setPageMeta(data.page || { totalPages: 0, totalElements: 0 });
    } catch (err) {
      setError('Помилка завантаження');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, search, categoryId, availability]);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Reset page on filter change
  const handleSearch = (value) => { setSearch(value); setPage(0); };
  const handleCategoryFilter = (value) => { setCategoryId(value); setPage(0); };
  const handleAvailabilityFilter = (value) => { setAvailability(value); setPage(0); };

  // CRUD handlers
  const handleCreateSave = async (payload) => {
    await ingredientsAPI.create(payload);
    setFormModal(null);
    fetchData();
  };

  const handleEditSave = async (payload) => {
    await ingredientsAPI.update(formModal.ingredient.id, payload);
    setFormModal(null);
    fetchData();
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await ingredientsAPI.remove(deleteConfirm.id);
      setDeleteConfirm(null);
      fetchData();
    } catch (err) {
      console.error('Delete error:', err);
    }
  };

  const openEdit = async (ing) => {
    try {
      const { data } = await ingredientsAPI.getById(ing.id);
      setFormModal({ ingredient: data });
    } catch {
      setFormModal({ ingredient: ing });
    }
  };

  // Smart pagination: show max 10 pages around current page
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
              <span className="gradient-text">Управління інгредієнтами</span>
            </h1>
            <p className="admin-subtitle">
              Всього: {pageMeta.totalElements} інгредієнтів
            </p>
          </div>
          <button className="admin-btn admin-btn-primary" onClick={() => setFormModal({})}>
            + Створити
          </button>
        </div>

        {/* ─── Filters ──────────────────────────── */}
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
          <select className="admin-filter-select" value={categoryId} onChange={(e) => handleCategoryFilter(e.target.value)}>
            <option value="">Всі категорії</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select className="admin-filter-select" value={availability} onChange={(e) => handleAvailabilityFilter(e.target.value)}>
            <option value="">Всі</option>
            <option value="true">В наявності</option>
            <option value="false">Немає</option>
          </select>
        </div>

        {/* ─── Table ────────────────────────────── */}
        <div className="admin-table-wrap glass-card">
          {loading ? (
            <div className="admin-loading"><div className="spinner"></div></div>
          ) : error ? (
            <div className="admin-error">
              <p>{error}</p>
              <button className="admin-btn admin-btn-ghost" onClick={fetchData}>Спробувати знову</button>
            </div>
          ) : ingredients.length === 0 ? (
            <div className="admin-empty">Інгредієнтів не знайдено</div>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Фото</th>
                  <th>Назва</th>
                  <th>Ціна</th>
                  <th>Од.</th>
                  <th>Категорія</th>
                  <th>Наявність</th>
                  <th>Дії</th>
                </tr>
              </thead>
              <tbody>
                {ingredients.map((ing) => (
                  <tr key={ing.id}>
                    <td className="td-id">{ing.id}</td>
                    <td className="td-img">
                      {ing.imageUrl ? (
                        <img src={ing.imageUrl} alt="" />
                      ) : (
                        <span className="td-img-placeholder">🥬</span>
                      )}
                    </td>
                    <td className="td-name">
                      <span>{ing.normalizedName}</span>
                      <small className="td-slug">{ing.slug}</small>
                    </td>
                    <td>{ing.price != null ? `${ing.price} ₴` : '—'}</td>
                    <td>{UNIT_LABELS[ing.unit] || ing.unit}</td>
                    <td>{ing.categoryName || '—'}</td>
                    <td>
                      <span className={`admin-badge ${ing.available ? 'badge-available' : 'badge-unavailable'}`}>
                        {ing.available ? 'Так' : 'Ні'}
                      </span>
                    </td>
                    <td className="td-actions">
                      <button className="admin-action-btn" title="Редагувати" onClick={() => openEdit(ing)}>✏️</button>
                      <button className="admin-action-btn" title="Дієтичні теги" onClick={() => setTagsModal(ing)}>🏷️</button>
                      <button className="admin-action-btn admin-action-danger" title="Видалити" onClick={() => setDeleteConfirm(ing)}>🗑️</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* ─── Pagination ────────────────────────── */}
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

      {/* ─── Modals ────────────────────────────── */}
      {formModal && (
        <AdminIngredientFormModal
          ingredient={formModal.ingredient || null}
          onSave={formModal.ingredient ? handleEditSave : handleCreateSave}
          onClose={() => setFormModal(null)}
        />
      )}

      {tagsModal && (
        <AdminIngredientDietaryTagsModal
          ingredient={tagsModal}
          onClose={() => setTagsModal(null)}
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
              <p>Видалити <strong>{deleteConfirm.normalizedName}</strong>?</p>
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
