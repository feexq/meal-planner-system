import { useState, useEffect } from 'react';
import { categoriesAPI } from '../api/api';

export default function AdminCategoryFormModal({ category, onClose, onSave }) {
  const isEdit = !!category;

  const [form, setForm] = useState({
    name: '',
    parentId: '',
  });
  
  const [allCategories, setAllCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {

    categoriesAPI.getAll()
      .then(({ data }) => {

        let filtered = data;
        if (isEdit) {
          filtered = data.filter((c) => c.id !== category.id);
        }
        setAllCategories(filtered);
      })
      .catch(() => {});
  }, [isEdit, category]);

  useEffect(() => {
    if (category) {
      setForm({
        name: category.name || '',
        parentId: category.parentId || '',
      });
    }
  }, [category]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        parentId: form.parentId !== '' ? Number(form.parentId) : null,
      };
      await onSave(payload);
    } catch (err) {
      setError(err.response?.data?.message || 'Помилка збереження');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal admin-modal-sm" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h2>{isEdit ? 'Редагувати категорію' : 'Створити категорію'}</h2>
          <button className="admin-modal-close" onClick={onClose}>✕</button>
        </div>

        <form className="admin-modal-form" onSubmit={handleSubmit}>
          {error && <div className="admin-form-error">{error}</div>}

          <div className="admin-form-group">
            <label>Назва категорії *</label>
            <input 
              name="name" 
              value={form.name} 
              onChange={handleChange} 
              required 
            />
          </div>

          <div className="admin-form-group">
            <label>Батьківська категорія</label>
            <select name="parentId" value={form.parentId} onChange={handleChange}>
              <option value="">— Коренева категорія —</option>
              {allCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div className="admin-modal-actions">
            <button type="button" className="admin-btn admin-btn-ghost" onClick={onClose}>
              Скасувати
            </button>
            <button type="submit" className="admin-btn admin-btn-primary" disabled={saving}>
              {saving ? 'Збереження…' : isEdit ? 'Зберегти' : 'Створити'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
