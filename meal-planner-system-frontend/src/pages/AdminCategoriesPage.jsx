import { useState, useEffect, useCallback } from 'react';
import { categoriesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import AdminCategoryFormModal from '../components/AdminCategoryFormModal';
import '../pages/AdminIngredientsPage.css';

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);


  const [formModal, setFormModal] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {



      const { data } = await categoriesAPI.getAll();
      setCategories(data);
    } catch (err) {
      setError('Помилка завантаження');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);


  const filteredCategories = categories.filter((c) => 
    c.name.toLowerCase().includes(search.toLowerCase()) || 
    c.slug.toLowerCase().includes(search.toLowerCase())
  );


  const handleCreateSave = async (payload) => {
    await categoriesAPI.create(payload);
    setFormModal(null);
    fetchData();
  };

  const handleEditSave = async (payload) => {
    await categoriesAPI.update(formModal.category.id, payload);
    setFormModal(null);
    fetchData();
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await categoriesAPI.remove(deleteConfirm.id);
      setDeleteConfirm(null);
      fetchData();
    } catch (err) {
      console.error('Delete error:', err);
    }
  };

  const openEdit = async (cat) => {
    try {

      const { data } = await categoriesAPI.getById(cat.id);
      setFormModal({ category: data });
    } catch {
      setFormModal({ category: cat });
    }
  };

  const getParentName = (parentId) => {
    if (!parentId) return '—';
    const parent = categories.find((c) => c.id === parentId);
    return parent ? parent.name : parentId;
  };

  return (
    <div className="admin-page">
      <Navbar />

      <div className="admin-container animate-fade-in">
        <div className="admin-header">
          <div>
            <h1 className="admin-title">
              <span className="gradient-text">Управління категоріями</span>
            </h1>
            <p className="admin-subtitle">
              Всього: {categories.length} категорій
            </p>
          </div>
          <button className="admin-btn admin-btn-primary" onClick={() => setFormModal({})}>
            + Створити категорію
          </button>
        </div>

        {}
        <div className="admin-filters glass-card">
          <div className="admin-filter-item admin-search-box">
            <span className="admin-search-icon">🔍</span>
            <input
              type="text"
              placeholder="Пошук за назвою або слагом…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {}
        <div className="admin-table-wrap glass-card">
          {loading ? (
            <div className="admin-loading"><div className="spinner"></div></div>
          ) : error ? (
            <div className="admin-error">
              <p>{error}</p>
              <button className="admin-btn admin-btn-ghost" onClick={fetchData}>Спробувати знову</button>
            </div>
          ) : filteredCategories.length === 0 ? (
            <div className="admin-empty">Категорій не знайдено</div>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Назва</th>
                  <th>Слаг</th>
                  <th>Батьківська категорія</th>
                  <th>Дії</th>
                </tr>
              </thead>
              <tbody>
                {filteredCategories.map((cat) => (
                  <tr key={cat.id}>
                    <td className="td-id">{cat.id}</td>
                    <td className="td-name">
                      <span style={{ fontSize: '0.95rem' }}>{cat.name}</span>
                    </td>
                    <td>
                      <span className="admin-badge type-diet">{cat.slug}</span>
                    </td>
                    <td>
                      <span style={{ color: 'var(--text-secondary)' }}>
                        {getParentName(cat.parentId)}
                      </span>
                    </td>
                    <td className="td-actions">
                      <button className="admin-action-btn" title="Редагувати" onClick={() => openEdit(cat)}>✏️</button>
                      <button className="admin-action-btn admin-action-danger" title="Видалити" onClick={() => setDeleteConfirm(cat)}>🗑️</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {}
      {formModal && (
        <AdminCategoryFormModal
          category={formModal.category || null}
          onSave={formModal.category ? handleEditSave : handleCreateSave}
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
              <p className="admin-delete-warn">
                Якщо категорія має підкатегорії чи інгредієнти, видалення може бути заблоковано бекендом.
              </p>
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
