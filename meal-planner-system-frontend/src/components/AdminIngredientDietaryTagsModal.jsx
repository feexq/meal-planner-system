import { useState, useEffect } from 'react';
import { ingredientsAPI, dietaryAPI } from '../api/api';

const STATUS_OPTIONS = [
  { value: 'ALLOWED', label: 'Дозволено', cls: 'status-allowed' },
  { value: 'SOFT_FORBIDDEN', label: "М'яко заборонено", cls: 'status-soft' },
  { value: 'HARD_FORBIDDEN', label: 'Заборонено', cls: 'status-hard' },
];

export default function AdminIngredientDietaryTagsModal({ ingredient, onClose }) {
  const [conditions, setConditions] = useState([]);
  const [tags, setTags] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!ingredient) return;
    setLoading(true);
    Promise.all([
      dietaryAPI.getConditions(),
      ingredientsAPI.getDietaryTags(ingredient.id),
    ])
      .then(([condRes, tagRes]) => {
        setConditions(condRes.data);
        const map = {};
        tagRes.data.forEach((t) => { map[t.conditionId] = t.status; });
        setTags(map);
      })
      .catch(() => setError('Не вдалось завантажити дані'))
      .finally(() => setLoading(false));
  }, [ingredient]);

  const handleChange = (conditionId, status) => {
    setTags((prev) => ({ ...prev, [conditionId]: status }));
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSuccess(false);
    try {
      await ingredientsAPI.updateDietaryTags(ingredient.id, tags);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Помилка збереження');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal admin-modal-wide" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h2>Дієтичні теги — {ingredient?.normalizedName}</h2>
          <button className="admin-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="admin-modal-body">
          {loading ? (
            <div className="admin-loading"><div className="spinner"></div></div>
          ) : error && conditions.length === 0 ? (
            <div className="admin-form-error">{error}</div>
          ) : (
            <>
              {error && <div className="admin-form-error">{error}</div>}
              {success && <div className="admin-form-success">✓ Збережено</div>}

              <table className="admin-table admin-dietary-table">
                <thead>
                  <tr>
                    <th>Стан</th>
                    <th>Тип</th>
                    <th>Статус</th>
                  </tr>
                </thead>
                <tbody>
                  {conditions.map((cond) => (
                    <tr key={cond.id}>
                      <td>{cond.name}</td>
                      <td>
                        <span className={`admin-type-badge ${cond.type === 'DIET' ? 'type-diet' : 'type-contra'}`}>
                          {cond.type === 'DIET' ? 'Дієта' : 'Протипоказання'}
                        </span>
                      </td>
                      <td>
                        <select
                          className={`admin-status-select ${STATUS_OPTIONS.find((s) => s.value === (tags[cond.id] || 'ALLOWED'))?.cls || ''}`}
                          value={tags[cond.id] || 'ALLOWED'}
                          onChange={(e) => handleChange(cond.id, e.target.value)}
                        >
                          {STATUS_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="admin-modal-actions">
                <button className="admin-btn admin-btn-ghost" onClick={onClose}>Закрити</button>
                <button className="admin-btn admin-btn-primary" onClick={handleSave} disabled={saving}>
                  {saving ? 'Збереження…' : 'Зберегти теги'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
