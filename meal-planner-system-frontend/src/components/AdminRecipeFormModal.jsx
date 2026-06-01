import { useState, useEffect } from 'react';
import { recipesAPI, recipeTagsAPI, ingredientsAPI } from '../api/api';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'DESSERT', 'DRINK', 'SNACK', 'SAUCE_OR_CONDIMENT', 'UNCLASSIFIED'];
const COOK_TIMES = ['MIN_15', 'MIN_30', 'MIN_60', 'HOURS_4', 'DAYS_1_PLUS'];
const COMPLEXITIES = ['EASY', 'MEDIUM', 'HARD'];
const BUDGETS = ['LOW', 'MEDIUM', 'HIGH'];

export default function AdminRecipeFormModal({ recipe, onSave, onClose }) {
  const isEdit = !!recipe;

  const [activeTab, setActiveTab] = useState('basic');

  const [form, setForm] = useState({
    name: '',
    slug: '',
    description: '',
    imageUrl: '',
    mealType: 'UNCLASSIFIED',
    mealTypeDetailed: '',
    cookTime: 'MIN_30',
    cookComplexity: 'MEDIUM',
    cookBudget: 'MEDIUM',
    servings: 1,
    servingSize: '',
    ingredientsRawStr: '',
    steps: [''],
    ingredients: [],
    tagIds: [],
    nutrition: { calories: 0, proteinG: 0, totalFatG: 0, totalCarbsG: 0 },
    translationUk: {
      name: '',
      description: '',
      servingSize: '',
      steps: [{ step_number: 1, description: '' }],
      ingredients: [{ name_uk: '', amount: 0, unit: 'G', note: '' }]
    }
  });

  const [tags, setTags] = useState([]);
  const [allIngredients, setAllIngredients] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    recipeTagsAPI.getAll().then(({ data }) => setTags(data || [])).catch(() => { });
    // Fetch a large list of ingredients for the dropdown
    ingredientsAPI.getAll({ page: 0, size: 1000 }).then(({ data }) => {
      setAllIngredients(data.content || []);
    }).catch(() => { });
  }, []);

  useEffect(() => {
    if (recipe) {
      recipesAPI.getById(recipe.id).then(({ data }) => {
        setForm({
          name: data.name || '',
          slug: data.slug || '',
          description: data.description || '',
          imageUrl: data.imageUrl || '',
          mealType: data.mealType || 'UNCLASSIFIED',
          mealTypeDetailed: data.mealTypeDetailed || '',
          cookTime: data.cookTime || 'MIN_30',
          cookComplexity: data.cookComplexity || 'MEDIUM',
          cookBudget: data.cookBudget || 'MEDIUM',
          servings: data.servings || 1,
          servingSize: data.servingSize || '',
          ingredientsRawStr: data.ingredientsRawStr || '',
          steps: data.steps?.length ? data.steps : [''],
          ingredients: data.ingredients?.length ? data.ingredients.map(i => ({
            ingredientId: i.ingredientId,
            rawName: i.rawName || '',
            rawAmount: i.rawAmount || ''
          })) : [],
          tagIds: data.tags || [], // You may need to map string tags to IDs if API returns strings
          nutrition: data.nutrition || { calories: 0, proteinG: 0, totalFatG: 0, totalCarbsG: 0 },
          translationUk: data.translationUk || {
            name: data.name || '',
            description: data.description || '',
            servingSize: data.servingSize || '',
            steps: data.steps?.length ? data.steps.map((s, idx) => ({ step_number: idx + 1, description: s })) : [{ step_number: 1, description: '' }],
            ingredients: data.ingredients?.length ? data.ingredients.map(i => ({ name_uk: i.rawName || '', amount: 0, unit: 'G', note: '' })) : [{ name_uk: '', amount: 0, unit: 'G', note: '' }]
          }
        });
      });
    }
  }, [recipe]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleNutritionChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, nutrition: { ...prev.nutrition, [name]: Number(value) } }));
  };

  const handleTranslationChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, translationUk: { ...prev.translationUk, [name]: value } }));
  };

  const handleStepChange = (index, value) => {
    const newSteps = [...form.steps];
    newSteps[index] = value;
    setForm(prev => ({ ...prev, steps: newSteps }));
  };
  const addStep = () => setForm(prev => ({ ...prev, steps: [...prev.steps, ''] }));
  const removeStep = (index) => setForm(prev => ({ ...prev, steps: prev.steps.filter((_, i) => i !== index) }));

  const handleIngredientChange = (index, field, value) => {
    const newIngs = [...form.ingredients];
    newIngs[index][field] = field === 'ingredientId' ? Number(value) : value;
    setForm(prev => ({ ...prev, ingredients: newIngs }));
  };
  const addIngredient = () => setForm(prev => ({ ...prev, ingredients: [...prev.ingredients, { ingredientId: '', rawName: '', rawAmount: '' }] }));
  const removeIngredient = (index) => setForm(prev => ({ ...prev, ingredients: prev.ingredients.filter((_, i) => i !== index) }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const payload = {
        ...form,
        servings: Number(form.servings),
        steps: form.steps.filter(s => s.trim()),
        ingredients: form.ingredients.filter(i => i.ingredientId && i.rawName && i.rawAmount),
        // Simplification for translation if needed
      };
      // Ensure translation steps are mapped properly if missing
      if (!payload.translationUk.steps || payload.translationUk.steps.length === 0) {
        payload.translationUk.steps = payload.steps.map((s, i) => ({ step_number: i + 1, description: s }));
      }
      if (!payload.translationUk.ingredients || payload.translationUk.ingredients.length === 0) {
        payload.translationUk.ingredients = payload.ingredients.map(i => ({ name_uk: i.rawName, amount: parseFloat(i.rawAmount) || 0, unit: 'G', note: '' }));
      }

      await onSave(payload);
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
          <h2>{isEdit ? 'Редагувати рецепт' : 'Створити рецепт'}</h2>
          <button className="admin-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="admin-modal-tabs" style={{ display: 'flex', borderBottom: '1px solid var(--border-color)', padding: '0 1.5rem', gap: '1rem' }}>
          {[
            { id: 'basic', label: 'Основне' },
            { id: 'ingredients', label: 'Інгредієнти' },
            { id: 'steps', label: 'Кроки' },
            { id: 'nutrition', label: 'БЖВ' },
            { id: 'translation', label: 'Переклад' }
          ].map(t => (
            <button
              key={t.id}
              type="button"
              onClick={() => setActiveTab(t.id)}
              style={{
                background: 'none', border: 'none', padding: '1rem 0',
                color: activeTab === t.id ? 'var(--accent-primary)' : 'var(--text-secondary)',
                borderBottom: activeTab === t.id ? '2px solid var(--accent-primary)' : '2px solid transparent',
                cursor: 'pointer', fontWeight: 600
              }}
            >
              {t.label}
            </button>
          ))}
        </div>

        <form className="admin-modal-form" onSubmit={handleSubmit}>
          {error && <div className="admin-form-error">{error}</div>}

          <div style={{ display: activeTab === 'basic' ? 'block' : 'none' }}>
            <div className="admin-form-group">
              <label>Назва *</label>
              <input name="name" value={form.name} onChange={handleChange} required />
            </div>
            <div className="admin-form-group">
              <label>Slug *</label>
              <input name="slug" value={form.slug} onChange={handleChange} required />
            </div>
            <div className="admin-form-group">
              <label>Опис</label>
              <textarea name="description" value={form.description} onChange={handleChange} rows={3} style={{ width: '100%', padding: '0.6rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }} />
            </div>
            <div className="admin-form-row">
              <div className="admin-form-group">
                <label>Тип прийому їжі *</label>
                <select name="mealType" value={form.mealType} onChange={handleChange}>
                  {MEAL_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="admin-form-group">
                <label>Час *</label>
                <select name="cookTime" value={form.cookTime} onChange={handleChange}>
                  {COOK_TIMES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <div className="admin-form-row">
              <div className="admin-form-group">
                <label>Складність *</label>
                <select name="cookComplexity" value={form.cookComplexity} onChange={handleChange}>
                  {COMPLEXITIES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="admin-form-group">
                <label>Бюджет *</label>
                <select name="cookBudget" value={form.cookBudget} onChange={handleChange}>
                  {BUDGETS.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <div className="admin-form-row">
              <div className="admin-form-group">
                <label>Порцій</label>
                <input type="number" min="1" name="servings" value={form.servings} onChange={handleChange} />
              </div>
              <div className="admin-form-group">
                <label>Розмір порції</label>
                <input name="servingSize" value={form.servingSize} onChange={handleChange} />
              </div>
            </div>
            <div className="admin-form-group">
              <label>URL зображення</label>
              <input name="imageUrl" value={form.imageUrl} onChange={handleChange} />
            </div>
          </div>

          <div style={{ display: activeTab === 'ingredients' ? 'block' : 'none' }}>
            <div className="admin-form-group">
              <label>Інгредієнти</label>
              {form.ingredients.map((ing, idx) => (
                <div key={idx} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                  <select
                    value={ing.ingredientId}
                    onChange={(e) => handleIngredientChange(idx, 'ingredientId', e.target.value)}
                    style={{ flex: 2, padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }}
                    required
                  >
                    <option value="">Оберіть інгредієнт</option>
                    {allIngredients.map(i => <option key={i.id} value={i.id}>{i.normalizedName}</option>)}
                  </select>
                  <input
                    placeholder="Назва (як у рецепті)"
                    value={ing.rawName}
                    onChange={(e) => handleIngredientChange(idx, 'rawName', e.target.value)}
                    style={{ flex: 2, padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }}
                    required
                  />
                  <input
                    placeholder="Кількість (напр. '2 шт')"
                    value={ing.rawAmount}
                    onChange={(e) => handleIngredientChange(idx, 'rawAmount', e.target.value)}
                    style={{ flex: 1, padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }}
                    required
                  />
                  <button type="button" className="admin-btn admin-btn-danger" onClick={() => removeIngredient(idx)}>✕</button>
                </div>
              ))}
              <button type="button" className="admin-btn admin-btn-ghost" onClick={addIngredient} style={{ alignSelf: 'flex-start' }}>+ Додати інгредієнт</button>
            </div>
            <div className="admin-form-group" style={{ marginTop: '1rem' }}>
              <label>Сирий текст інгредієнтів (для парсингу)</label>
              <textarea name="ingredientsRawStr" value={form.ingredientsRawStr} onChange={handleChange} rows={4} style={{ width: '100%', padding: '0.6rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }} />
            </div>
          </div>

          <div style={{ display: activeTab === 'steps' ? 'block' : 'none' }}>
            <div className="admin-form-group">
              <label>Кроки приготування</label>
              {form.steps.map((step, idx) => (
                <div key={idx} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                  <span style={{ padding: '0.5rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>{idx + 1}</span>
                  <textarea
                    value={step}
                    onChange={(e) => handleStepChange(idx, e.target.value)}
                    rows={2}
                    style={{ flex: 1, padding: '0.5rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }}
                    required
                  />
                  <button type="button" className="admin-btn admin-btn-danger" onClick={() => removeStep(idx)}>✕</button>
                </div>
              ))}
              <button type="button" className="admin-btn admin-btn-ghost" onClick={addStep} style={{ alignSelf: 'flex-start' }}>+ Додати крок</button>
            </div>
          </div>

          <div style={{ display: activeTab === 'nutrition' ? 'block' : 'none' }}>
            <div className="admin-form-row">
              <div className="admin-form-group">
                <label>Калорії</label>
                <input type="number" step="0.1" name="calories" value={form.nutrition.calories} onChange={handleNutritionChange} />
              </div>
              <div className="admin-form-group">
                <label>Білки (г)</label>
                <input type="number" step="0.1" name="proteinG" value={form.nutrition.proteinG} onChange={handleNutritionChange} />
              </div>
            </div>
            <div className="admin-form-row">
              <div className="admin-form-group">
                <label>Жири (г)</label>
                <input type="number" step="0.1" name="totalFatG" value={form.nutrition.totalFatG} onChange={handleNutritionChange} />
              </div>
              <div className="admin-form-group">
                <label>Вуглеводи (г)</label>
                <input type="number" step="0.1" name="totalCarbsG" value={form.nutrition.totalCarbsG} onChange={handleNutritionChange} />
              </div>
            </div>
          </div>

          <div style={{ display: activeTab === 'translation' ? 'block' : 'none' }}>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>Переклад українською мовою (для показу клієнтам)</p>
            <div className="admin-form-group">
              <label>Назва (Укр) *</label>
              <input name="name" value={form.translationUk.name} onChange={handleTranslationChange} required />
            </div>
            <div className="admin-form-group">
              <label>Опис (Укр)</label>
              <textarea name="description" value={form.translationUk.description} onChange={handleTranslationChange} rows={3} style={{ width: '100%', padding: '0.6rem', background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', color: 'var(--text-primary)' }} />
            </div>
            <div className="admin-form-group">
              <label>Розмір порції (Укр)</label>
              <input name="servingSize" value={form.translationUk.servingSize} onChange={handleTranslationChange} />
            </div>
          </div>

          <div className="admin-modal-actions" style={{ marginTop: '1.5rem' }}>
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
