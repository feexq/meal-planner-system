import React, { useState, useEffect, useRef, useCallback } from 'react';
import Navbar from '../components/Navbar';
import ProfileSidebar from '../components/ProfileSidebar';
import { preferencesAPI, profileAPI } from '../api/api';
import './PreferencesPage.css';

const translationCache = {
  'mushroom': 'гриб', 'mushrooms': 'гриби', 'milk': 'молоко', 'cheese': 'сир',
  'pork': 'свинина', 'garlic': 'часник', 'onion': 'цибуля', 'pepper': 'перець',
  'tomato': 'помідор', 'chicken': 'курка', 'beef': 'яловичина', 'fish': 'риба',
  'egg': 'яйце', 'eggs': 'яйця', 'peanut': 'арахіс', 'nut': 'горіх', 'butter': 'масло',
  'strawberry': 'полуниця', 'apple': 'яблуко', 'potato': 'картопля', 'carrot': 'морква'
};

const QUICK_ADDS = [
  { en: 'mushroom', uk: 'Гриб' },
  { en: 'milk', uk: 'Молоко' },
  { en: 'cheese', uk: 'Сир' },
  { en: 'pork', uk: 'Свинина' },
  { en: 'garlic', uk: 'Часник' },
  { en: 'onion', uk: 'Цибуля' }
];

const getDisplayName = (enName) => {
  return UKR_DICTIONARY[enName.toLowerCase()] || enName;
};


function TranslatedName({ enName }) {
  const lowerName = enName ? enName.toLowerCase() : '';
  const [ukName, setUkName] = useState(translationCache[lowerName] || lowerName);

  useEffect(() => {
    if (!translationCache[lowerName] && lowerName) {
      fetch(`https://api.mymemory.translated.net/get?q=${encodeURIComponent(lowerName)}&langpair=en|uk`)
        .then(res => res.json())
        .then(data => {
          if (data.responseData?.translatedText) {
            const translated = data.responseData.translatedText.toLowerCase();
            translationCache[lowerName] = translated;
            setUkName(translated);
          }
        })
        .catch(() => { });
    }
  }, [lowerName]);

  return <>{ukName}</>;
}



function StepperInput({ id, value, onChange, min, max, label, unit }) {
  const decrement = () => onChange(Math.max(min, value - 1));
  const increment = () => onChange(Math.min(max, value + 1));
  return (
    <div className="smart-input-group">
      <span className="smart-input-label">{label}</span>
      <div className="smart-input-control">
        <button type="button" className="stepper-btn" onClick={decrement}>−</button>
        <input
          type="number" id={id} value={value} min={min} max={max}
          onChange={e => {
            const v = parseInt(e.target.value);
            if (!isNaN(v) && v >= min && v <= max) onChange(v);
          }}
          readOnly
        />
        <button type="button" className="stepper-btn" onClick={increment}>+</button>
      </div>
      <span className="input-unit">{unit}</span>
    </div>
  );
}

function RadioCard({ name, id, value, checked, onChange, icon, title, desc }) {
  return (
    <div className="radio-card">
      <input type="radio" name={name} id={id} value={value} checked={checked} onChange={() => onChange(value)} />
      <label htmlFor={id}>
        {icon && <span className="rc-icon">{icon}</span>}
        <span className="rc-title">{title}</span>
        {desc && <span className="rc-desc">{desc}</span>}
      </label>
    </div>
  );
}

function ListCard({ name, id, value, checked, onChange, icon, title, desc }) {
  return (
    <div className="list-card">
      <input type="radio" name={name} id={id} value={value} checked={checked} onChange={() => onChange(value)} />
      <label htmlFor={id}>
        <span className="lc-icon">{icon}</span>
        <div className="lc-text">
          <span className="lc-title">{title}</span>
          {desc && <span className="lc-desc">{desc}</span>}
        </div>
      </label>
    </div>
  );
}

function TagCheck({ name, id, value, checked, onChange, label, type = 'radio' }) {
  return (
    <div className="tag-check">
      <input
        type={type} name={name} id={id} value={value}
        checked={checked}
        onChange={() => onChange(value)}
      />
      <label htmlFor={id}>{label}</label>
    </div>
  );
}

function AiInsight({ icon, children }) {
  return (
    <div className="ai-insight">
      <span className="ai-icon">{icon}</span>
      <div className="ai-text">{children}</div>
    </div>
  );
}



function Step1({ data, onChange }) {
  return (
    <div className="card">
      <h2 className="step-title">👤 Ваші фізичні дані</h2>
      <p className="step-subtitle">Ці дані необхідні для точного розрахунку базового метаболізму (BMR) та денної норми калорій.</p>

      <div className="section-lbl">Стать:</div>
      <div className="grid-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <RadioCard name="gender" id="g-male" value="MALE" checked={data.gender === 'MALE'} onChange={v => onChange('gender', v)} icon="👨" title="Чоловік" />
        <RadioCard name="gender" id="g-female" value="FEMALE" checked={data.gender === 'FEMALE'} onChange={v => onChange('gender', v)} icon="👩" title="Жінка" />
      </div>

      <div className="input-grid" style={{ marginTop: 24 }}>
        <StepperInput id="age" label="Вік" unit="років" value={data.age} min={14} max={100} onChange={v => onChange('age', v)} />
        <StepperInput id="heightCm" label="Зріст" unit="см" value={data.heightCm} min={140} max={220} onChange={v => onChange('heightCm', v)} />
        <StepperInput id="weightKg" label="Вага" unit="кг" value={data.weightKg} min={40} max={150} onChange={v => onChange('weightKg', v)} />
      </div>
    </div>
  );
}

function Step2({ data, onChange }) {
  const activities = [
    { value: 'SEDENTARY', icon: '💻', title: 'Сидяча (Sedentary)', desc: 'Мінімум рухів, офісна робота.' },
    { value: 'LIGHT', icon: '🚶‍♂️', title: 'Легка (Light)', desc: 'Прогулянки, 1-2 легкі тренування.' },
    { value: 'MODERATE', icon: '🏃‍♀️', title: 'Помірна (Moderate)', desc: '3-4 інтенсивні тренування.' },
    { value: 'ACTIVE', icon: '🏋️‍♂️', title: 'Активна (Active)', desc: 'Спорт майже щодня.' },
    { value: 'VERY_ACTIVE', icon: '🏗️', title: 'Дуже активна (Very Active)', desc: 'Фізична праця або 2 тренування.' },
  ];

  const goals = [
    { value: 'WEIGHT_LOSS', icon: '📉', title: 'Схуднення', desc: 'Дефіцит калорій' },
    { value: 'MAINTENANCE', icon: '⚖️', title: 'Підтримка', desc: 'Поточна вага' },
    { value: 'WEIGHT_GAIN', icon: '💪', title: 'Набір маси', desc: 'Профіцит калорій' },
  ];

  const intensities = [
    { value: 'SLOW', icon: '🐢', title: 'Поступово', desc: 'Мінімальний стрес' },
    { value: 'NORMAL', icon: '🚗', title: 'Нормально', desc: 'Збалансовано' },
    { value: 'FAST', icon: '🚀', title: 'Швидко', desc: 'Максимум' },
  ];

  return (
    <div className="card">
      <h2 className="step-title">⚡ Активність та Мета</h2>

      <div className="section-lbl">Рівень щоденної активності:</div>
      <div className="list-cards">
        {activities.map(a => (
          <ListCard key={a.value} name="activity" id={`act-${a.value}`} value={a.value}
            checked={data.activityLevel === a.value} onChange={v => onChange('activityLevel', v)}
            icon={a.icon} title={a.title} desc={a.desc} />
        ))}
      </div>

      <div className="section-lbl" style={{ marginTop: 24 }}>Ваша мета:</div>
      <div className="grid-cards">
        {goals.map(g => (
          <RadioCard key={g.value} name="goal" id={`goal-${g.value}`} value={g.value}
            checked={data.goal === g.value} onChange={v => onChange('goal', v)}
            icon={g.icon} title={g.title} desc={g.desc} />
        ))}
      </div>

      {data.goal !== 'MAINTENANCE' && (
        <>
          <div className="section-lbl" style={{ marginTop: 24 }}>Інтенсивність:</div>
          <div className="grid-cards">
            {intensities.map(i => (
              <RadioCard key={i.value} name="intensity" id={`int-${i.value}`} value={i.value}
                checked={data.goalIntensity === i.value} onChange={v => onChange('goalIntensity', v)}
                icon={i.icon} title={i.title} desc={i.desc} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function Step3({ data, onChange }) {
  const diets = [
    { value: 'OMNIVORE', label: '🍗 Omnivore' },
    { value: 'VEGETARIAN', label: '🥚 Vegetarian' },
    { value: 'VEGAN', label: '🌱 Vegan' },
    { value: 'KETO', label: '🥑 Keto' },
    { value: 'PALEO', label: '🥩 Paleo' },
    { value: 'MEDITERRANEAN', label: '🫒 Mediterranean' },
    { value: 'GLUTEN_FREE', label: '🌾 Gluten-Free' },
  ];

  const healthConditions = [
    { value: 'GASTRITIS', label: 'Гастрит' },
    { value: 'DIABETES', label: 'Діабет' },
    { value: 'HYPERTENSION', label: 'Гіпертонія' },
    { value: 'HIGH_CHOLESTEROL', label: 'Високий холестерин' },
    { value: 'CELIAC_DISEASE', label: 'Целіакія' },
    { value: 'LACTOSE_INTOLERANCE', label: 'Непереносимість лактози' },
    { value: 'NUT_ALLERGY', label: 'Алергія на горіхи' },
    { value: 'SHELLFISH_ALLERGY', label: 'Алергія на молюсків' },
    { value: 'FISH_ALLERGY', label: 'Алергія на рибу' },
    { value: 'GERD', label: 'ГЕРХ (Рефлюкс)' },
    { value: 'IBS', label: 'IBS (СПК)' },
    { value: 'KIDNEY_DISEASE', label: 'Захворювання нирок' },
    { value: 'GOUT', label: 'Подагра' },
    { value: 'PANCREATITIS', label: 'Панкреатит' },
  ];

  const toggleMedical = (val) => {
    const arr = data.healthConditions || [];
    const next = arr.includes(val) ? arr.filter(x => x !== val) : [...arr, val];
    onChange('healthConditions', next);
  };

  return (
    <div className="card">
      <h2 className="step-title">🛡️ Медичний профіль та Дієти</h2>
      <AiInsight icon="🛡️">
        <strong>Медичні фільтри:</strong> Алгоритм жорстко відсіює рецепти, якщо вони суперечать обраним станам або дієтам.
      </AiInsight>

      <div className="section-lbl">Тип дієти (Оберіть один):</div>
      <div className="tag-cloud">
        {diets.map(d => (
          <TagCheck key={d.value} type="radio" name="diet" id={`d-${d.value}`} value={d.value}
            checked={data.dietType === d.value} onChange={v => onChange('dietType', v)} label={d.label} />
        ))}
      </div>

      <div className="section-lbl" style={{ marginTop: 24 }}>Медичні обмеження (Можна обрати декілька):</div>
      <div className="tag-cloud">
        {healthConditions.map(m => (
          <TagCheck key={m.value} type="checkbox" name="medical" id={`m-${m.value}`} value={m.value}
            checked={(data.healthConditions || []).includes(m.value)}
            onChange={toggleMedical} label={m.label} />
        ))}
      </div>
    </div>
  );
}

function Step4({ data, onChange }) {
  const [inputVal, setInputVal] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const debounceTimeout = useRef(null);

  const translateToEn = async (text) => {
    try {
      const res = await fetch(`https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=uk|en`);
      const data = await res.json();
      return data.responseData.translatedText.toLowerCase();
    } catch (e) {
      return text;
    }
  };

  const searchIngredients = async (query) => {
    if (!query || query.length < 2) {
      setSuggestions([]);
      return;
    }

    setLoading(true);
    try {
      let searchQuery = query;
      if (/[а-яА-ЯЄєІіЇїҐґ]/.test(query)) {
        searchQuery = await translateToEn(query);
      }

      const res = await fetch(`/api/ingredients?search=${encodeURIComponent(searchQuery)}&page=0&size=50`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
      });
      if (res.ok) {
        const result = await res.json();
        setSuggestions(result.content || []);
      }
    } catch (err) {
      console.error("Помилка пошуку інгредієнтів:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (debounceTimeout.current) clearTimeout(debounceTimeout.current);
    debounceTimeout.current = setTimeout(() => {
      searchIngredients(inputVal.trim());
    }, 500);

    return () => clearTimeout(debounceTimeout.current);
  }, [inputVal]);

  const addTag = useCallback((enName) => {
    const tag = enName.trim().toLowerCase();
    if (!tag) return;
    const arr = data.dislikedIngredients || [];
    if (!arr.includes(tag)) {
      onChange('dislikedIngredients', [...arr, tag]);
    }
    setInputVal('');
    setSuggestions([]);
  }, [data.dislikedIngredients, onChange]);

  const removeTag = (tag) => {
    const arr = (data.dislikedIngredients || []).filter(t => t !== tag);
    onChange('dislikedIngredients', arr);
  };

  return (
    <div className="card" style={{ position: 'relative' }}>
      <h2 className="step-title">🔍 Виключення продуктів</h2>

      <AiInsight icon="🔍">
        <strong>Точний пошук:</strong> Введіть назву продукту українською (наприклад, "гриб"). Ми перекладемо і знайдемо його в базі.
      </AiInsight>

      <div className="section-lbl">Що слід повністю виключити з раціону?</div>
      <div className="exclusion-wrapper">
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: inputVal ? 8 : 0 }}>
          {(data.dislikedIngredients || []).map(tag => (
            <div key={tag} className="excl-tag">
              <TranslatedName enName={tag} />
              <span onClick={() => removeTag(tag)} style={{ cursor: 'pointer', marginLeft: 8 }}>&times;</span>
            </div>
          ))}
        </div>

        <input
          type="text"
          className="excl-input"
          placeholder="Почніть вводити назву..."
          value={inputVal}
          onChange={e => setInputVal(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') e.preventDefault(); }}
        />

        {inputVal.length >= 2 && (
          <div className="search-preview-box" style={{ width: '100%', marginTop: '12px' }}>
            {loading ? (
              <span style={{ fontSize: 13, color: '#666' }}>Перекладаємо та шукаємо...</span>
            ) : suggestions.length > 0 ? (
              <div className="preview-list" style={{ maxHeight: '200px', overflowY: 'auto', border: 'none', background: 'transparent' }}>
                {suggestions.map(ing => (
                  <div
                    key={ing.id}
                    className="preview-item"
                    style={{ cursor: 'pointer', borderRadius: '8px', marginBottom: '4px' }}
                    onClick={() => addTag(ing.normalizedName)}
                  >
                    {ing.imageUrl && <img src={ing.imageUrl} alt="" className="preview-img" />}
                    <span style={{ textTransform: 'capitalize' }}>
                      <TranslatedName enName={ing.normalizedName} />{' '}
                      <span style={{ color: '#9ca3af', fontSize: 12, textTransform: 'lowercase' }}>({ing.normalizedName})</span>
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <span style={{ fontSize: 13, color: '#b91c1c' }}>Нічого не знайдено. Спробуйте інше слово.</span>
            )}
          </div>
        )}
      </div>

      <div className="quick-adds" style={{ marginTop: 12 }}>
        {QUICK_ADDS.map(q => (
          <span key={q.en} className="quick-add" onClick={() => addTag(q.en)}>+ {q.uk}</span>
        ))}
      </div>
    </div>
  );
}

function Step5({ data, onChange }) {
  const ctimes = [
    { value: 15, id: 'ct-15', icon: '⚡', title: 'До 15 хв' },
    { value: 30, id: 'ct-30', icon: '⏱️', title: 'До 30 хв' },
    { value: 60, id: 'ct-60', icon: '⏳', title: 'До 1 год' },
    { value: 240, id: 'ct-120', icon: '👨‍🍳', title: 'Безліміт' },
  ];

  const complexities = [
    { value: 'EASY', id: 'cc-easy', icon: '🟢', title: 'Легко' },
    { value: 'MEDIUM', id: 'cc-med', icon: '🟡', title: 'Середньо' },
    { value: 'HARD', id: 'cc-hard', icon: '🔴', title: 'Складно' },
  ];

  const budgets = [
    { value: 'LOW', id: 'cb-low', icon: '🪙', title: 'Економ' },
    { value: 'MEDIUM', id: 'cb-med', icon: '💵', title: 'Середній' },
    { value: 'HIGH', id: 'cb-high', icon: '💎', title: 'Преміум' },
  ];

  return (
    <div className="card">
      <h2 className="step-title">🍳 Параметри готування</h2>
      <AiInsight icon="😌">
        <strong>Не хвилюйтесь:</strong> Ці параметри є лише "м'якими" побажаннями. ШІ запропонує найкращі варіанти, навіть якщо вони трохи вийдуть за ці рамки.
      </AiInsight>

      {}
      <div className="section-lbl">Бажаний час готування:</div>
      <div className="grid-cards" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', marginBottom: '24px' }}>
        {ctimes.map(c => (
          <RadioCard key={c.value} name="ctime" id={c.id} value={c.value}
            checked={data.cookingTimeMin === c.value} onChange={v => onChange('cookingTimeMin', Number(v))}
            icon={c.icon} title={c.title} />
        ))}
      </div>

      {}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div>
          <div className="section-lbl">Складність рецептів:</div>
          <div className="grid-cards" style={{ gridTemplateColumns: '1fr', marginBottom: 0 }}>
            {complexities.map(c => (
              <div key={c.value} className="radio-card">
                <input type="radio" name="ccomp" id={c.id} value={c.value}
                  checked={data.cookingComplexity === c.value}
                  onChange={() => onChange('cookingComplexity', c.value)} />
                <label htmlFor={c.id} style={{ flexDirection: 'row', gap: 12, padding: 12, justifyContent: 'flex-start' }}>
                  <span className="rc-icon" style={{ fontSize: 20, margin: 0 }}>{c.icon}</span>
                  <span className="rc-title">{c.title}</span>
                </label>
              </div>
            ))}
          </div>
        </div>

        <div>
          <div className="section-lbl">Бюджет покупок:</div>
          <div className="grid-cards" style={{ gridTemplateColumns: '1fr', marginBottom: 0 }}>
            {budgets.map(b => (
              <div key={b.value} className="radio-card">
                <input type="radio" name="cbudget" id={b.id} value={b.value}
                  checked={data.budgetLevel === b.value}
                  onChange={() => onChange('budgetLevel', b.value)} />
                <label htmlFor={b.id} style={{ flexDirection: 'row', gap: 12, padding: 12, justifyContent: 'flex-start' }}>
                  <span className="rc-icon" style={{ fontSize: 20, margin: 0 }}>{b.icon}</span>
                  <span className="rc-title">{b.title}</span>
                </label>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function Step6({ data, onChange }) {
  const zigzags = [
    {
      value: false, id: 'zz-linear', title: 'Лінійна',
      desc: 'Однакова калорійність щодня. Легше для звикання.',
      bars: [30, 30, 30, 30, 30, 30, 30],
    },
    {
      value: true, id: 'zz-zigzag', title: 'Zig-Zag',
      desc: 'Сходинки: чергування різних рівнів з чітмілами.',
      bars: [20, 38, 20, 38, 20, 38, 20],
    },
  ];

  const meals = [
    { value: 3, id: 'm-3', label: '3 прийоми' },
    { value: 4, id: 'm-4', label: '4 прийоми' },
    { value: 5, id: 'm-5', label: '5+ прийомів' },
  ];

  return (
    <div className="card">
      <h2 className="step-title">📈 Розклад калорійності</h2>
      <AiInsight icon="📈">
        <strong>Метаболічна адаптація (Zig-Zag):</strong> Чергування високих і низьких по калорійності днів запобігає звиканню організму до дієти.
      </AiInsight>

      <div className="section-lbl">Розподіл калорійності:</div>
      <div className="grid-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
        {zigzags.map(z => (
          <div key={z.id} className="radio-card">
            <input type="radio" name="zigzag" id={z.id} value={z.value}
              checked={data.zigzag === z.value}
              onChange={() => onChange('zigzag', z.value)} />
            <label htmlFor={z.id}>
              <div className="chart-box">
                {z.bars.map((h, i) => <div key={i} className="bar" style={{ height: h }} />)}
              </div>
              <span className="rc-title">{z.title}</span>
              <span className="rc-desc">{z.desc}</span>
            </label>
          </div>
        ))}
      </div>

      <div className="section-lbl" style={{ marginTop: 24 }}>Кількість прийомів їжі на день:</div>
      <div className="tag-cloud stretch">
        {meals.map(m => (
          <TagCheck key={m.value} type="radio" name="meals" id={m.id} value={m.value}
            checked={data.mealsPerDay === m.value} onChange={v => onChange('mealsPerDay', Number(v))} label={m.label} />
        ))}
      </div>
    </div>
  );
}



const PreferencesPage = () => {
  const [prefs, setPrefs] = useState({
    gender: 'MALE',
    age: 20,
    heightCm: 180,
    weightKg: 75,
    goal: 'WEIGHT_LOSS',
    activityLevel: 'MODERATE',
    goalIntensity: 'NORMAL',
    dietType: 'OMNIVORE',
    healthConditions: [],
    dislikedIngredients: [],
    mealsPerDay: 4,
    cookingComplexity: 'MEDIUM',
    budgetLevel: 'MEDIUM',
    cookingTimeMin: 30,
    zigzag: true
  });


  const [initialPrefs, setInitialPrefs] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);
  const [nextUrl, setNextUrl] = useState(null);
  const skipWarningRef = useRef(false);
  const [saveStatus, setSaveStatus] = useState('Зберегти зміни');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prefRes, profileRes] = await Promise.all([
          preferencesAPI.get().catch(() => ({ data: {} })),
          profileAPI.getProfile().catch(() => ({ data: {} }))
        ]);

        const prefData = prefRes.data || {};
        const profileData = profileRes.data || {};

        const mergedPrefs = {
          gender: profileData.gender || 'MALE',
          age: profileData.age || 20,
          heightCm: profileData.heightCm || 180,
          weightKg: profileData.currentWeightKg || profileData.weightKg || 75,
          goal: prefData.goal || 'WEIGHT_LOSS',
          activityLevel: prefData.activityLevel || 'MODERATE',
          goalIntensity: prefData.goalIntensity || 'NORMAL',
          dietType: prefData.dietType || 'OMNIVORE',
          healthConditions: prefData.healthConditions || [],
          dislikedIngredients: prefData.dislikedIngredients || [],
          mealsPerDay: prefData.mealsPerDay || 4,
          cookingComplexity: prefData.cookingComplexity || 'MEDIUM',
          budgetLevel: prefData.budgetLevel || 'MEDIUM',
          cookingTimeMin: prefData.cookingTimeMin || 30,
          zigzag: prefData.zigzag !== undefined ? prefData.zigzag : true
        };

        setPrefs(mergedPrefs);
        setInitialPrefs(mergedPrefs);
      } catch (error) {
        console.error('Error fetching user data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleChange = (name, value) => {
    setPrefs(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveStatus('🔄 Збереження...');
    try {
      await preferencesAPI.update(prefs);

      await profileAPI.updateProfile({
        gender: prefs.gender,
        age: prefs.age,
        heightCm: prefs.heightCm,
        currentWeightKg: prefs.weightKg,
      });

      setInitialPrefs(prefs);
      setSaveStatus('✓ Збережено');
      setTimeout(() => setSaveStatus('Зберегти зміни'), 2000);
    } catch (error) {
      console.error('Error saving preferences:', error);
      setSaveStatus('❌ Помилка');
      setTimeout(() => setSaveStatus('Зберегти зміни'), 2000);
    } finally {
      setSaving(false);
    }
  };


  const hasUnsavedChanges = JSON.stringify(prefs) !== JSON.stringify(initialPrefs);

  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges && !skipWarningRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    const handleLinkClick = (e) => {
      if (!hasUnsavedChanges) return;
      const link = e.target.closest('a');

      if (link && link.href && !link.target && !e.defaultPrevented) {
        if (link.origin === window.location.origin && link.pathname !== window.location.pathname) {
          e.preventDefault();
          e.stopPropagation();
          setNextUrl(link.href);
          setShowExitModal(true);
        }
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('click', handleLinkClick, { capture: true });

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('click', handleLinkClick, { capture: true });
    };
  }, [hasUnsavedChanges]);

  if (loading) {
    return <div className="page-loader"><div className="spinner"></div></div>;
  }

  return (
    <div className="preferences-page">
      <Navbar />
      <main className="container">
        <div className="profile-layout">
          <ProfileSidebar />

          <div className="content-area">
            {}
            <div className="page-header" style={{
              position: 'sticky',
              top: '79px',
              zIndex: 10,
              background: 'var(--neutral)',
              padding: '16px 0 10px 0',
              marginTop: '-16px',
              borderBottom: '1px solid var(--border)'
            }}>
              <h1 className="page-title">Налаштування раціону</h1>
              <button
                className="btn-save"
                onClick={handleSave}
                disabled={saving || !hasUnsavedChanges}
                style={{
                  background: saveStatus === '✓ Збережено' ? 'var(--success)' : '',
                  opacity: (!hasUnsavedChanges && saveStatus === 'Зберегти зміни') ? 0.6 : 1
                }}
              >
                {saveStatus}
              </button>
            </div>

            <div className="steps-container">
              <Step1 data={prefs} onChange={handleChange} />
              <Step2 data={prefs} onChange={handleChange} />
              <Step3 data={prefs} onChange={handleChange} />
              <Step4 data={prefs} onChange={handleChange} />
              <Step5 data={prefs} onChange={handleChange} />
              <Step6 data={prefs} onChange={handleChange} />
            </div>

          </div>
        </div>
      </main>
      {}
      {showExitModal && (
        <div className="modal-overlay" onClick={() => setShowExitModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '380px', textAlign: 'center', padding: '32px 24px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🏃‍♂️</div>
            <h3 style={{ margin: '0 0 12px 0', fontSize: '20px', color: 'var(--text)' }}>Незбережені зміни!</h3>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '24px', lineHeight: '1.5' }}>
              Ви змінили параметри раціону, але не зберегли їх. Якщо ви вийдете зараз, усі зміни буде втрачено.
            </p>
            <div style={{ display: 'flex', gap: '12px', width: '100%' }}>
              <button
                className="btn-cancel"
                style={{ flex: 1, padding: '12px', background: '#FEE2E2', color: '#991B1B', border: 'none', borderRadius: '12px', fontWeight: '600', cursor: 'pointer' }}
                onClick={() => {
                  skipWarningRef.current = true;
                  setShowExitModal(false);
                  if (nextUrl) window.location.href = nextUrl;
                }}
              >
                Вийти
              </button>
              <button
                className="btn-submit"
                style={{ flex: 1, padding: '12px', background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: '12px', fontWeight: '600', cursor: 'pointer' }}
                onClick={() => setShowExitModal(false)}
              >
                Залишитись
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PreferencesPage;