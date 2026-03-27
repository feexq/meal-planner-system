import { useState, useEffect } from 'react';
import { categoriesAPI } from '../api/api';

const UNITS = ['KG', 'G', 'L', 'ML', 'PCS', 'BUNCH'];
const UNIT_LABELS = { KG: 'кг', G: 'г', L: 'л', ML: 'мл', PCS: 'шт', BUNCH: 'пучок' };

export default function AdminIngredientFormModal({ ingredient, onSave, onClose }) {
  const isEdit = !!ingredient;

  const [form, setForm] = useState({
    normalizedName: '',
    imageUrl: '',
    price: '',
    unit: 'KG',
    stock: '',
    available: true,
    categoryId: '',
    aliases: [],
  });
  const [aliasInput, setAliasInput] = useState('');
  const [categories, setCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    categoriesAPI.getAll().then(({ data }) => setCategories(data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (ingredient) {
      setForm({
        normalizedName: ingredient.normalizedName || '',
        imageUrl: ingredient.imageUrl || '',
        price: ingredient.price ?? '',
        unit: ingredient.unit || 'KG',
        stock: ingredient.stock ?? '',
        available: ingredient.available ?? true,
        categoryId: ingredient.categoryId ?? '',
        aliases: ingredient.aliases || [],
      });
    }
  }, [ingredient]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const addAlias = () => {
    const val = aliasInput.trim().toLowerCase();
    if (val && !form.aliases.includes(val)) {
      setForm((prev) => ({ ...prev, aliases: [...prev.aliases, val] }));
    }
    setAliasInput('');
  };

  const removeAlias = (a) => {
    setForm((prev) => ({ ...prev, aliases: prev.aliases.filter((x) => x !== a) }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const payload = {
        ...form,
        price: form.price !== '' ? Number(form.price) : null,
        stock: form.stock !== '' ? Number(form.stock) : null,
        categoryId: form.categoryId !== '' ? Number(form.categoryId) : null,
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
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h2>{isEdit ? 'Редагувати інгредієнт' : 'Створити інгредієнт'}</h2>
          <button className="admin-modal-close" onClick={onClose}>✕</button>
        </div>

        <form className="admin-modal-form" onSubmit={handleSubmit}>
          {error && <div className="admin-form-error">{error}</div>}

          <div className="admin-form-group">
            <label>Назва *</label>
            <input name="normalizedName" value={form.normalizedName} onChange={handleChange} required />
          </div>

          <div className="admin-form-row">
            <div className="admin-form-group">
              <label>Ціна</label>
              <input name="price" type="number" step="0.01" min="0" value={form.price} onChange={handleChange} />
            </div>
            <div className="admin-form-group">
              <label>Одиниця</label>
              <select name="unit" value={form.unit} onChange={handleChange}>
                {UNITS.map((u) => <option key={u} value={u}>{UNIT_LABELS[u]} ({u})</option>)}
              </select>
            </div>
            <div className="admin-form-group">
              <label>Запас</label>
              <input name="stock" type="number" min="0" value={form.stock} onChange={handleChange} />
            </div>
          </div>

          <div className="admin-form-group">
            <label>URL зображення</label>
            <input name="imageUrl" value={form.imageUrl} onChange={handleChange} placeholder="https://..." />
          </div>

          <div className="admin-form-group">
            <label>Категорія</label>
            <select name="categoryId" value={form.categoryId} onChange={handleChange}>
              <option value="">— Без категорії —</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>

          <div className="admin-form-group admin-form-check">
            <label>
              <input type="checkbox" name="available" checked={form.available} onChange={handleChange} />
              В наявності
            </label>
          </div>

          <div className="admin-form-group">
            <label>Аліаси</label>
            <div className="admin-alias-input-row">
              <input
                value={aliasInput}
                onChange={(e) => setAliasInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addAlias(); } }}
                placeholder="Додати аліас…"
              />
              <button type="button" className="admin-btn admin-btn-sm" onClick={addAlias}>+</button>
            </div>
            {form.aliases.length > 0 && (
              <div className="admin-alias-tags">
                {form.aliases.map((a) => (
                  <span key={a} className="admin-alias-tag">
                    {a}
                    <button type="button" onClick={() => removeAlias(a)}>×</button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="admin-modal-actions">
            <button type="button" className="admin-btn admin-btn-ghost" onClick={onClose}>Скасувати</button>
            <button type="submit" className="admin-btn admin-btn-primary" disabled={saving}>
              {saving ? 'Збереження…' : isEdit ? 'Зберегти' : 'Створити'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
