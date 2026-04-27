import { useState, useCallback, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { mealPlanAPI, preferencesAPI } from '../api/api';
import Navbar from '../components/Navbar';
import './UserSurveyPage.css';

const TOTAL_STEPS = 6;

// --- Динамічний словник для зворотного перекладу (кеш) ---
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

// Компонент, який приймає англійську назву і динамічно малює українську
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
                .catch(() => { }); // Якщо помилка мережі, залишаємо англійську
        }
    }, [lowerName]);

    return <>{ukName}</>;
}

// ─── Sub-components ───────────────────────────────────────────

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

function RadioCard({ name, id, value, checked, onChange, icon, title, desc, children }) {
    return (
        <div className="radio-card">
            <input type="radio" name={name} id={id} value={value} checked={checked} onChange={() => onChange(value)} />
            <label htmlFor={id}>
                {children}
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

// ─── Step Definitions ─────────────────────────────────────────

function Step1({ data, onChange }) {
    return (
        <div>
            <h2 className="step-title">Ваші фізичні дані</h2>
            <p className="step-subtitle">
                Ці дані необхідні для точного розрахунку вашого базового метаболізму (BMR) та денної норми калорій.
            </p>

            <div className="section-lbl">Стать:</div>
            <div className="grid-cards" style={{ gridTemplateColumns: '1fr 1fr' }}>
                <RadioCard name="gender" id="g-male" value="MALE" checked={data.gender === 'MALE'} onChange={v => onChange('gender', v)} icon="👨" title="Чоловік" />
                <RadioCard name="gender" id="g-female" value="FEMALE" checked={data.gender === 'FEMALE'} onChange={v => onChange('gender', v)} icon="👩" title="Жінка" />
            </div>

            <div className="input-grid">
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
        { value: 'LIGHT', icon: '🚶‍♂️', title: 'Легка (Light)', desc: 'Прогулянки, 1-2 легкі тренування на тиждень.' },
        { value: 'MODERATE', icon: '🏃‍♀️', title: 'Помірна (Moderate)', desc: '3-4 інтенсивні тренування на тиждень.' },
        { value: 'ACTIVE', icon: '🏋️‍♂️', title: 'Активна (Active)', desc: 'Спорт майже щодня.' },
        { value: 'VERY_ACTIVE', icon: '🏗️', title: 'Дуже активна (Very Active)', desc: 'Фізична праця або 2 тренування на день.' },
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
        <div>
            <h2 className="step-title">Активність та Мета</h2>

            <div className="section-lbl">Рівень щоденної активності:</div>
            <div className="list-cards">
                {activities.map(a => (
                    <ListCard key={a.value} name="activity" id={`act-${a.value}`} value={a.value}
                        checked={data.activityLevel === a.value} onChange={v => onChange('activityLevel', v)}
                        icon={a.icon} title={a.title} desc={a.desc} />
                ))}
            </div>

            <div className="section-lbl">Ваша мета:</div>
            <div className="grid-cards">
                {goals.map(g => (
                    <RadioCard key={g.value} name="goal" id={`goal-${g.value}`} value={g.value}
                        checked={data.goal === g.value} onChange={v => onChange('goal', v)}
                        icon={g.icon} title={g.title} desc={g.desc} />
                ))}
            </div>

            {data.goal !== 'MAINTENANCE' && (
                <>
                    <div className="section-lbl">Інтенсивність:</div>
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
        { value: 'GERD', label: 'ГЕРХ (Рефлюкс)' },
        { value: 'IBS', label: 'IBS (СПК)' },
    ];

    const toggleMedical = (val) => {
        const arr = data.healthConditions || [];
        const next = arr.includes(val) ? arr.filter(x => x !== val) : [...arr, val];
        onChange('healthConditions', next);
    };

    return (
        <div>
            <h2 className="step-title">Медичний профіль та Дієти</h2>

            <AiInsight icon="🛡️">
                <strong>Медичні фільтри</strong><br />
                Алгоритм жорстко відсіює рецепти, якщо вони суперечать обраним медичним станам або дієтам.
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
        <div style={{ position: 'relative' }}>
            <h2 className="step-title">Виключення продуктів</h2>

            <AiInsight icon="🔍">
                <strong>Точний пошук</strong><br />
                Почніть вводити назву продукту українською (наприклад, "гриб"). Ми перекладемо і знайдемо його в базі.
            </AiInsight>

            <div className="section-lbl">Що слід повністю виключити з раціону?</div>

            <div className="exclusion-wrapper" style={{ position: 'relative' }}>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: inputVal ? 8 : 0 }}>
                    {(data.dislikedIngredients || []).map(tag => (
                        <div key={tag} className="excl-tag">
                            {/* Відображаємо українською за допомогою компонента */}
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
                            <span style={{ fontSize: 13, color: '#666' }}>Перекладаємо та шукаємо в базі...</span>
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
                                            {/* Відображаємо українською в підказках */}
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

            <div className="quick-adds">
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
        { value: 60, id: 'ct-60', icon: '⏳', title: 'До 60 хв' },
        { value: 240, id: 'ct-4h', icon: '🍲', title: 'До 4 годин' },
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
        <div>
            <h2 className="step-title">Параметри готування</h2>

            <AiInsight icon="😌">
                <strong>Не хвилюйтесь!</strong><br />
                Ці параметри є лише "м'якими" побажаннями. ШІ запропонує найкращі варіанти, навіть якщо вони трохи вийдуть за ці рамки.
            </AiInsight>

            <div className="section-lbl">Бажаний час приготування:</div>
            <div className="grid-cards" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(100px, 1fr))' }}>
                {ctimes.map(c => (
                    <RadioCard key={c.value} name="ctime" id={c.id} value={c.value}
                        checked={data.cookingTimeMin === c.value} onChange={v => onChange('cookingTimeMin', Number(v))}
                        icon={c.icon} title={c.title} />
                ))}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                <div>
                    <div className="section-lbl">Складність рецептів:</div>
                    <div className="grid-cards" style={{ gridTemplateColumns: '1fr', marginBottom: 0 }}>
                        {complexities.map(c => (
                            <div key={c.value} className="radio-card">
                                <input type="radio" name="ccomp" id={c.id} value={c.value}
                                    checked={data.cookingComplexity === c.value}
                                    onChange={() => onChange('cookingComplexity', c.value)} />
                                <label htmlFor={c.id} style={{ flexDirection: 'row', gap: 12, padding: 12 }}>
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
                                <label htmlFor={b.id} style={{ flexDirection: 'row', gap: 12, padding: 12 }}>
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
        <div>
            <h2 className="step-title">Розклад калорійності</h2>

            <AiInsight icon="📈">
                <strong>Метаболічна адаптація (Zig-Zag)</strong><br />
                Чергування високих і низьких по калорійності днів запобігає звиканню організму до дієти.
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

            <div className="section-lbl">Кількість прийомів їжі на день:</div>
            <div className="tag-cloud stretch">
                {meals.map(m => (
                    <TagCheck key={m.value} type="radio" name="meals" id={m.id} value={m.value}
                        checked={data.mealsPerDay === m.value} onChange={v => onChange('mealsPerDay', Number(v))} label={m.label} />
                ))}
            </div>
        </div>
    );
}

// ─── Main SurveyPage ─────────────────────────────────────────

const DEFAULT_FORM = {
    gender: 'MALE',
    age: 25,
    heightCm: 175,
    weightKg: 70,
    targetWeightKg: 70,
    activityLevel: 'SEDENTARY',
    goal: 'WEIGHT_LOSS',
    goalIntensity: 'SLOW',
    dietType: 'OMNIVORE',
    healthConditions: [],
    allergies: [],
    dislikedIngredients: [],
    mealsPerDay: 3,
    cookingComplexity: 'EASY',
    budgetLevel: 'LOW',
    cookingTimeMin: 30,
    zigzag: false,
};

export default function UserSurveyPage() {
    const [step, setStep] = useState(1);
    const [form, setForm] = useState(DEFAULT_FORM);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [showExistingPlanModal, setShowExistingPlanModal] = useState(false);
    const [hasExistingPlan, setHasExistingPlan] = useState(false);
    const navigate = useNavigate();

    const mapBackendToForm = (data) => {
        return {
            ...DEFAULT_FORM,
            gender: data.gender || DEFAULT_FORM.gender,
            age: data.age || DEFAULT_FORM.age,
            heightCm: data.heightCm || data.height || DEFAULT_FORM.heightCm,
            weightKg: data.weightKg || data.weight || DEFAULT_FORM.weightKg,
            targetWeightKg: data.targetWeightKg || data.weightKg || data.weight || DEFAULT_FORM.targetWeightKg,
            activityLevel: data.activityLevel || DEFAULT_FORM.activityLevel,
            goal: data.goal || DEFAULT_FORM.goal,
            goalIntensity: data.goalIntensity || DEFAULT_FORM.goalIntensity,
            dietType: data.dietType || DEFAULT_FORM.dietType,
            healthConditions: data.healthConditions || data.medicalConditions || DEFAULT_FORM.healthConditions,
            allergies: data.allergies || DEFAULT_FORM.allergies,
            dislikedIngredients: data.dislikedIngredients || data.excludedIngredients || DEFAULT_FORM.dislikedIngredients,
            mealsPerDay: data.mealsPerDay || DEFAULT_FORM.mealsPerDay,
            cookingComplexity: data.cookingComplexity || DEFAULT_FORM.cookingComplexity,
            budgetLevel: data.budgetLevel || data.budget || DEFAULT_FORM.budgetLevel,
            cookingTimeMin: data.cookingTimeMin || DEFAULT_FORM.cookingTimeMin,
            zigzag: data.zigzag !== undefined ? data.zigzag : DEFAULT_FORM.zigzag,
        };
    };

    useEffect(() => {
        const fetchPreferences = async () => {
            try {
                const response = await preferencesAPI.get();
                if (response.data && Object.keys(response.data).length > 0) {
                    setForm(mapBackendToForm(response.data));
                    setHasExistingPlan(true);
                    setShowExistingPlanModal(true);
                }
            } catch (err) {
                console.log('No existing preferences found');
            }
        };

        if (localStorage.getItem('accessToken')) {
            fetchPreferences();
        }
    }, []);

    const handleChange = useCallback((field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    }, []);

    const mapFormToBackend = (formData) => {
        return {
            gender: formData.gender,
            age: formData.age,
            heightCm: formData.heightCm || formData.height,
            weightKg: formData.weightKg || formData.weight,
            targetWeightKg: formData.targetWeightKg || formData.weightKg || formData.weight,
            activityLevel: formData.activityLevel,
            goal: formData.goal,
            goalIntensity: formData.goalIntensity,
            dietType: formData.dietType,
            healthConditions: formData.healthConditions || [],
            allergies: formData.allergies || [],
            dislikedIngredients: formData.dislikedIngredients || [],
            mealsPerDay: formData.mealsPerDay,
            cookingComplexity: formData.cookingComplexity,
            budgetLevel: formData.budgetLevel,
            cookingTimeMin: formData.cookingTimeMin,
            zigzag: formData.zigzag,
        };
    };

    const handleSubmit = async () => {
        setSubmitting(true);
        setError(null);

        try {
            const backendPayload = mapFormToBackend(form);

            if (!hasExistingPlan) {
                await preferencesAPI.save(backendPayload);
            }

            const response = await mealPlanAPI.generateFinal(backendPayload);
            const planData = response.data || response;

            navigate('/plan-preview', { state: { plan: planData } });

        } catch (err) {
            console.error("Помилка при генерації:", err);
            setError(err.response?.data?.message || err.message || 'Не вдалося створити раціон. Спробуйте ще раз.');
            setSubmitting(false);
        }
    };

    const goNext = () => {
        if (step < TOTAL_STEPS) setStep(s => s + 1);
        else handleSubmit();
    };
    const goPrev = () => setStep(s => s - 1);

    const STEPS = [Step1, Step2, Step3, Step4, Step5, Step6];
    const CurrentStep = STEPS[step - 1];

    return (
        <>
            <Navbar />

            <main className="survey-main">
                <div className="wizard-card">
                    <div className="progress-header">
                        <span className="step-indicator">Крок {step} з {TOTAL_STEPS}</span>
                        <div className="progress-segments">
                            {Array.from({ length: TOTAL_STEPS }, (_, i) => (
                                <div
                                    key={i}
                                    className={`segment ${i < step - 1 ? 'done' : i === step - 1 ? 'active' : ''}`}
                                />
                            ))}
                        </div>
                    </div>

                    <div className="wizard-body">
                        <div className="step active" key={step}>
                            <CurrentStep data={form} onChange={handleChange} />
                        </div>

                        {error && (
                            <div style={{ marginTop: 16, padding: '12px 16px', background: '#FEE2E2', borderRadius: 12, color: '#991B1B', fontSize: 14 }}>
                                {error}
                            </div>
                        )}

                        <div className="wizard-footer">
                            <button
                                type="button"
                                className={`btn btn-back ${step === 1 ? 'hidden' : ''}`}
                                onClick={goPrev}
                                disabled={submitting}
                            >
                                Назад
                            </button>
                            <button
                                type="button"
                                className="btn btn-next"
                                onClick={goNext}
                                disabled={submitting}
                            >
                                {submitting
                                    ? '⏳ Аналіз...'
                                    : step === TOTAL_STEPS
                                        ? '✨ Створити раціон'
                                        : 'Далі →'}
                            </button>
                        </div>
                    </div>
                </div>
            </main>

            {showExistingPlanModal && (
                <div className="modal-overlay" onClick={() => setShowExistingPlanModal(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div style={{ textAlign: 'center', marginBottom: 20 }}>
                            <span style={{ fontSize: 40 }}>🔄</span>
                            <h3>Анкета вже існує</h3>
                        </div>
                        <p style={{ color: '#4b5563', marginBottom: 20, textAlign: 'center', lineHeight: 1.5 }}>
                            Ми знайшли ваші попередні налаштування і заповнили їх для вас.
                            Ви можете одразу згенерувати новий раціон, або перевірити і змінити налаштування.
                        </p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <button
                                type="button"
                                className="btn btn-next"
                                style={{ width: '100%' }}
                                onClick={() => {
                                    setShowExistingPlanModal(false);
                                    handleSubmit();
                                }}
                                disabled={submitting}
                            >
                                {submitting ? '⏳ Генерація...' : 'Згенерувати відразу'}
                            </button>
                            <button
                                type="button"
                                className="btn btn-back"
                                style={{ width: '100%' }}
                                onClick={() => setShowExistingPlanModal(false)}
                                disabled={submitting}
                            >
                                Змінити налаштування
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}