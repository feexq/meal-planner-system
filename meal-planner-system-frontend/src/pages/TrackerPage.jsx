import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import './TrackerPage.css';

const DAY_NAMES = ['Нд', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
const MEAL_LABELS = { BREAKFAST: '🍳 Сніданок', LUNCH: '🍲 Обід', DINNER: '🐟 Вечеря', SNACK: '🥜 Перекус' };
const MEAL_ORDER = { 'breakfast': 1, 'lunch': 2, 'snack': 3, 'dinner': 4 };

function getMealIcon(type) {
    const t = (type || '').toUpperCase();
    if (t.includes('BREAKFAST') || t.includes('СНІДАНОК')) return '🍳';
    if (t.includes('LUNCH') || t.includes('ОБІД')) return '🍲';
    if (t.includes('DINNER') || t.includes('ВЕЧЕРЯ')) return '🐟';
    if (t.includes('SNACK') || t.includes('ПЕРЕКУС')) return '🥜';
    return '🍽️';
}

function getMealLabel(type) {
    return MEAL_LABELS[(type || '').toUpperCase()] || type || 'Прийом їжі';
}

function ProgressRing({ pct, danger }) {
    const clamped = Math.min(100, Math.max(0, pct));
    const color = danger ? 'var(--danger, #EF4444)' : 'var(--primary)';
    return (
        <div className="progress-ring-wrap">
            <div className="progress-ring" style={{ background: `conic-gradient(${color} ${clamped}%, var(--neutral) ${clamped}%)` }}>
                <div className="progress-inner">
                    <span className="progress-val" style={{ color: danger ? 'var(--danger, #EF4444)' : 'var(--text)' }}>
                        {Math.round(clamped)}%
                    </span>
                    <span className="progress-lbl">Тиждень</span>
                </div>
            </div>
        </div>
    );
}

function MealCard({ slot, adaptation, onMarkEaten, onSwap, isToday = false }) {
    const navigate = useNavigate();
    const [swapping, setSwapping] = useState(false);
    const [isConfirming, setIsConfirming] = useState(false);
    const [ateAll, setAteAll] = useState(true);
    const [customCals, setCustomCals] = useState(0);

    const isEaten = slot.eaten;
    const action = isEaten ? 'NONE' : (adaptation?.action || 'NONE');

    // Визначаємо роль: Side чи Main
    const isSide = slot.slotRole === 'side';
    const isMain = slot.slotRole === 'main' || !isSide;
    const ratio = adaptation?.ratio || 1;

    // Складна логіка адаптації в залежності від ролі
    const isDropTarget = (action === 'DROP_SIDES' && isSide) || (action === 'DROP_SIDES_AND_SCALE' && isSide);
    const isScaleTarget = (action === 'SCALE_PORTIONS' && isMain) || (action === 'DROP_SIDES_AND_SCALE' && isMain);
    const isSwapTarget = action === 'REQUIRE_SWAP' && isMain;

    const displayRatio = isScaleTarget ? ratio : 1;
    const adjustedCalories = isDropTarget ? 0 : slot.calories * displayRatio;

    // Масштабуємо макроси, якщо порція зменшена
    const p = Math.round((slot.proteinG || 0) * displayRatio);
    const f = Math.round((slot.fatG || 0) * displayRatio);
    const c = Math.round((slot.carbsG || 0) * displayRatio);

    useEffect(() => {
        if (isConfirming) setCustomCals(Math.round(adjustedCalories));
    }, [isConfirming, adjustedCalories]);

    const handleSwap = async (e) => {
        e.stopPropagation();
        setSwapping(true);
        try { await onSwap(slot.id); } finally { setSwapping(false); }
    };

    const handleConfirmEaten = (e) => {
        e.stopPropagation();
        onMarkEaten(slot.id, ateAll ? null : Number(customCals));
        setIsConfirming(false);
    };

    return (
        <div className={`meal-card ${isEaten ? 'eaten' : ''} ${isSwapTarget ? 'swap-suggested' : ''}`}
            style={{ opacity: isDropTarget ? 0.6 : 1 }}>
            <div className="meal-card-main">
                <div className="meal-info clickable"
                    onClick={() => slot.recipeSlug
                        ? navigate(`/recipe/${slot.recipeSlug}`)
                        : navigate(`/recipe/id/${slot.recipeId}`)
                    }>
                    <div className="meal-icon">{getMealIcon(slot.mealType || slot.type)}</div>
                    <div className="meal-details">
                        <span className="m-type">
                            {getMealLabel(slot.mealType || slot.type)}
                            {slot.aiAdapted && <span className="ai-badge">Адаптовано</span>}
                        </span>

                        <span className="m-name" style={{ textDecoration: isDropTarget ? 'line-through' : 'none' }}>
                            {slot.recipeName || slot.name}
                        </span>

                        <div className="m-cals-wrap">
                            {(isDropTarget || isScaleTarget) && !isEaten ? (
                                <>
                                    <span className="cals-old">{Math.round(slot.calories)}</span>
                                    <span className="cals-new">{Math.round(adjustedCalories)} ккал</span>
                                </>
                            ) : (
                                <span className="m-cals">{Math.round(slot.calories)} ккал</span>
                            )}
                        </div>

                        {/* НОВИЙ БЛОК: Мета-дані (Роль + Макроси) */}
                        <div className="m-meta-row">
                            <span className={`role-badge ${isSide ? 'side' : 'main'}`}>
                                {isSide ? 'Додаток' : 'Основна'}
                            </span>
                            <div className="m-macros">
                                <span className="macro p" title="Білки">Б: {p}г</span>
                                <span className="macro f" title="Жири">Ж: {f}г</span>
                                <span className="macro c" title="Вуглеводи">В: {c}г</span>
                            </div>
                        </div>

                        {/* Підказки ШІ */}
                        {isDropTarget && !isEaten && <span style={{ fontSize: 11, color: '#F59E0B', fontWeight: 800, marginTop: 4 }}>⚠️ Рекомендуємо пропустити</span>}
                        {isScaleTarget && !isEaten && <span style={{ fontSize: 11, color: '#F59E0B', fontWeight: 800, marginTop: 4 }}>⚖️ Зменште порцію на {Math.round((1 - ratio) * 100)}%</span>}
                        {isSwapTarget && !isEaten && <span style={{ fontSize: 11, color: '#EF4444', fontWeight: 800, marginTop: 4 }}>🔄 Натисніть кнопку заміни ➡️</span>}
                    </div>
                </div>

                <div className="meal-actions">
                    <button className="swap-btn-clean" onClick={handleSwap} disabled={swapping || isEaten} title="Замінити страву">
                        {swapping ? '⏳' : (
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M21 2v6h-6"></path><path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path><path d="M3 22v-6h6"></path><path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path>
                            </svg>
                        )}
                    </button>
                    {!isEaten && !isConfirming && (
                        <button className={`status-btn ${!isToday ? 'disabled' : ''}`} onClick={isToday ? (e) => { e.stopPropagation(); setIsConfirming(true); } : undefined} disabled={!isToday}>✓</button>
                    )}
                    {isEaten && <button className="status-btn checked" disabled>✓</button>}
                </div>
            </div>

            {isConfirming && (
                <div className="meal-confirm-panel">
                    {isDropTarget && (
                        <div style={{ background: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', padding: '14px', borderRadius: '12px', marginBottom: '16px', fontSize: '13.5px' }}>
                            ⚠️ <strong>ШІ рекомендував пропустити цю страву</strong><br />
                            Якщо ви її з'їсте, на наступні дні будуть застосовані жорсткіші фільтри.
                        </div>
                    )}
                    <div className="confirm-row">
                        <label className="toggle-label">
                            <input type="checkbox" checked={ateAll} onChange={e => setAteAll(e.target.checked)} />
                            З'їв(ла) <strong>цільову порцію</strong> ({Math.round(adjustedCalories)} ккал)
                        </label>
                    </div>
                    {!ateAll && (
                        <div className="custom-cal-input">
                            <span>Фактичні калорії:</span>
                            <input type="number" value={customCals} onChange={e => setCustomCals(e.target.value)} min="0" />
                            <span className="unit">ккал</span>
                        </div>
                    )}
                    <div className="confirm-actions">
                        <button className="btn-cancel" onClick={() => setIsConfirming(false)}>Скасувати</button>
                        <button className="btn-save" onClick={handleConfirmEaten}>Підтвердити</button>
                    </div>
                </div>
            )}
        </div>
    );
}

function ExtraFoodItem({ item }) {
    const timeStr = item.time || (item.loggedAt ? new Date(item.loggedAt).toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' }) : 'Щойно');
    const hasMacros = item.proteinG > 0 || item.fatG > 0 || item.carbsG > 0;

    return (
        <div className="extra-item">
            <div style={{ flexGrow: 1 }}>
                <div className="ei-name">
                    {item.name} {item.count > 1 && <span className="ei-badge-count">x{item.count}</span>}
                </div>
                <div className="ei-meta">{timeStr}</div>

                {/* Компактний вивід макросів для незапланованої їжі */}
                {hasMacros && (
                    <div className="m-macros" style={{ marginTop: '8px' }}>
                        <span className="macro p">Б: {Math.round(item.proteinG)}г</span>
                        <span className="macro f">Ж: {Math.round(item.fatG)}г</span>
                        <span className="macro c">В: {Math.round(item.carbsG)}г</span>
                    </div>
                )}
            </div>
            <div className="ei-cals" style={{ fontWeight: '800' }}>+{Math.round(item.totalCalories)} ккал</div>
        </div>
    );
}

// ─── Main Component ─────────────────────────────────────────

export default function TrackerPage() {
    const navigate = useNavigate();
    const today = new Date();

    const [planData, setPlanData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeDayIndex, setActiveDayIndex] = useState(0);
    const [groceryModalOpen, setGroceryModalOpen] = useState(false);
    const [groceryList, setGroceryList] = useState([]);
    const [groceryLoading, setGroceryLoading] = useState(false);
    const [cartLoading, setCartLoading] = useState(false);

    const [extraInput, setExtraInput] = useState('');
    const [logLoading, setLogLoading] = useState(false);

    const fetchStatus = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetch('/api/meal-plan/status', {
                headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
            });
            if (res.status === 404) { navigate('/survey'); return; }
            if (!res.ok) throw new Error('Не вдалось завантажити план');
            setPlanData(await res.json());
        } catch (err) {
            setError("Не вдалось завантажити план");
        } finally {
            setLoading(false);
        }
    }, [navigate]);

    useEffect(() => { fetchStatus(); }, [fetchStatus]);

    useEffect(() => {
        if (planData && planData.weekStartDate) {
            const [y, m, d] = planData.weekStartDate.split('-');
            const planStart = new Date(y, m - 1, d);
            planStart.setHours(0, 0, 0, 0);
            const now = new Date();
            now.setHours(0, 0, 0, 0);
            const diffDays = Math.floor((now - planStart) / (1000 * 60 * 60 * 24));
            setActiveDayIndex(Math.max(0, Math.min((planData.days?.length || 1) - 1, diffDays)));
        }
    }, [planData]);

    const handleOpenGrocery = async () => {
        setGroceryModalOpen(true);
        setGroceryLoading(true);

        try {
            const currentRecipeIds = [...new Set(sortedSlots
                .map(slot => slot.recipeId)
                .filter(id => id != null)
            )];

            if (currentRecipeIds.length === 0) {
                setGroceryList([]);
                return;
            }

            const recipesPromises = currentRecipeIds.map(async id => {
                try {
                    const res = await fetch(`/api/recipes/${id}`, {
                        headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
                    });
                    if (!res.ok) {
                        console.warn(`Рецепт з ID ${id} не знайдено або помилка сервера (Status: ${res.status})`);
                        return null;
                    }
                    return await res.json();
                } catch (e) {
                    console.error(`Помилка завантаження рецепта ${id}:`, e);
                    return null;
                }
            });
            const recipesDataRaw = await Promise.all(recipesPromises);
            const recipesData = recipesDataRaw.filter(r => r !== null);

            if (recipesData.length === 0) {
                console.warn("Жоден рецепт не був завантажений.");
                setGroceryList([]);
                return;
            }

            // 1. Збираємо всі унікальні ID інгредієнтів
            const allIngredientIds = new Set();
            recipesData.forEach(recipe => {
                if (recipe.ingredients) {
                    recipe.ingredients.forEach(ing => {
                        if (ing.ingredientId) allIngredientIds.add(ing.ingredientId);
                    });
                }
            });

            let productsList = [];
            if (allIngredientIds.size > 0) {
                const idsArray = Array.from(allIngredientIds);
                try {
                    const productsRes = await fetch(`/api/products/by-ingredients?ingredientIds=${idsArray.join(',')}`, {
                        headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
                    });
                    if (productsRes.ok) {
                        const contentType = productsRes.headers.get("content-type");
                        if (contentType && contentType.includes("application/json")) {
                            productsList = await productsRes.json();
                        } else {
                            const text = await productsRes.text();
                            console.error("Очікувався JSON, але отримано:", text.substring(0, 100));
                        }
                    } else {
                        console.error("Failed to fetch products. Status:", productsRes.status);
                    }
                } catch (e) {
                    console.error("Помилка при запиті продуктів:", e);
                }
            }

            // 3. Допоміжні функції для роботи з одиницями
            // 3. Допоміжні функції для роботи з одиницями
            // 3. Допоміжні функції для роботи з одиницями
            const convertToCanonical = (amount, unit, fallbackUnit) => {
                let u = (unit || '').trim().toLowerCase();

                // Якщо в рецепті одиниця не вказана, беремо базову від продукту (з БД)
                if (!u && fallbackUnit) {
                    u = fallbackUnit.trim().toLowerCase();
                }

                let val = amount;

                // НОВИЙ БЛОК: Розділяємо склеєні числа та одиниці (наприклад, "100g" -> 100 та "g")
                const match = u.match(/^([\d.,]+)\s*([a-zа-яєіїґ]+)$/i);
                if (match) {
                    const multiplier = parseFloat(match[1].replace(',', '.'));
                    val = val * multiplier;
                    u = match[2].toLowerCase();
                }

                // Вага
                if (['kg', 'кг', 'кілограм'].includes(u)) return { val: val * 1000, type: 'г' };
                if (['ounce', 'ounces', 'oz', 'унція'].includes(u)) return { val: val * 28.35, type: 'г' };
                if (['gram', 'grams', 'g', 'г', 'грам'].includes(u)) return { val: val, type: 'г' };

                // Об'єм
                if (['l', 'л', 'літр'].includes(u)) return { val: val * 1000, type: 'мл' };
                if (['cup', 'cups', 'склянка', 'склянки'].includes(u)) return { val: val * 250, type: 'мл' };
                if (['tablespoon', 'tbsp', 'ст.л.', 'ст. л.', 'столова ложка'].includes(u)) return { val: val * 15, type: 'мл' };
                if (['teaspoon', 'tsp', 'ч.л.', 'ч. л.', 'чайна ложка'].includes(u)) return { val: val * 5, type: 'мл' };
                if (['ml', 'мл', 'мілілітр'].includes(u)) return { val: val, type: 'мл' };

                // Якщо одиниця невідома (напр. "щіпка", "упаковка"), залишаємо її, інакше дефолт 'шт'
                return { val: val, type: u || 'шт' };
            };

            const convertToMatchShopUnit = (amount, fromUnit, toUnit) => {
                if (!fromUnit || !toUnit) return amount;

                fromUnit = fromUnit.trim().toLowerCase();
                toUnit = toUnit.trim().toLowerCase();

                if (fromUnit === toUnit) return amount;

                const toIsKg = toUnit === "кг";
                const toIsG = toUnit === "г";
                const toIsL = toUnit === "л";
                const toIsMl = toUnit === "мл" || toUnit === "ml";

                if (fromUnit === "ст.л." || fromUnit === "ст. л.") {
                    if (toIsG || toIsMl) return amount * 15.0;
                    if (toIsKg || toIsL) return (amount * 15.0) / 1000.0;
                }
                if (fromUnit === "ч.л." || fromUnit === "ч. л.") {
                    if (toIsG || toIsMl) return amount * 5.0;
                    if (toIsKg || toIsL) return (amount * 5.0) / 1000.0;
                }

                if (fromUnit === "склянка" || fromUnit === "склянки") {
                    if (toIsG || toIsMl) return amount * 250.0;
                    if (toIsKg || toIsL) return (amount * 250.0) / 1000.0;
                }

                if (fromUnit === "кг" && toIsG) return amount * 1000.0;
                if (fromUnit === "г" && toIsKg) return amount / 1000.0;

                if (fromUnit === "л" && toIsMl) return amount * 1000.0;
                if ((fromUnit === "мл" || fromUnit === "ml") && toIsL) return amount / 1000.0;

                if ((fromUnit === "мл" || fromUnit === "ml") && toIsG) return amount;
                if ((fromUnit === "мл" || fromUnit === "ml") && toIsKg) return amount / 1000.0;
                if (fromUnit === "л" && toIsG) return amount * 1000.0;
                if (fromUnit === "л" && toIsKg) return amount;
                if (fromUnit === "г" && toIsMl) return amount;
                if (fromUnit === "г" && toIsL) return amount / 1000.0;
                if (fromUnit === "кг" && toIsMl) return amount * 1000.0;
                if (fromUnit === "кг" && toIsL) return amount;

                if (fromUnit === "шт" || fromUnit === "штук") {
                    if (toIsG) return amount * 150.0;
                    if (toIsKg) return (amount * 150.0) / 1000.0;
                }

                if ((fromUnit === "г" || fromUnit === "г" || fromUnit === "мл" || fromUnit === "ml") && (toUnit === "шт" || toUnit === "штук")) {
                    return amount / 150.0; // Приблизно 150г/мл на одну шт, якщо інше не вказано
                }

                if (fromUnit.includes("смак") || fromUnit.includes("дрібк")) {
                    return 0.01;
                }

                return amount;
            };

            const formatCanonical = (val, type) => {
                if (type === 'g' || type === 'г') {
                    if (val >= 1000) return `${(val / 1000).toFixed(2).replace(/\.?0+$/, '')} кг`;
                    return `${Math.round(val)} г`;
                }
                if (type === 'ml' || type === 'мл' || type === 'л') {
                    if (val >= 1000) return `${(val / 1000).toFixed(2).replace(/\.?0+$/, '')} л`;
                    return `${Math.round(val)} мл`;
                }

                // Якщо це щось на кшталт "за смаком" або "щіпка"
                if (type.includes('смак') || type.includes('дрібк') || type.includes('щіпк')) {
                    return type; // Виводимо просто текст без числа
                }

                // Відображаємо цілі числа або акуратно дробові (напр. 1.5 упаковки)
                const displayVal = val % 1 === 0 ? val : Number(val.toFixed(1));
                return `${displayVal} ${type}`;
            };

            // 4. Агрегуємо продукти
            const groceryMap = {};
            recipesData.forEach(recipe => {
                // Парсимо JSON з розширеною інформацією про інгредієнти (як на бекенді)
                let parsedDetails = [];
                try {
                    if (recipe.ingredientsRawStr) {
                        parsedDetails = JSON.parse(recipe.ingredientsRawStr);
                    }
                } catch (e) {
                    console.error("Error parsing ingredientsRawStr", e);
                }

                if (recipe.ingredients) {
                    recipe.ingredients.forEach(ing => {
                        const product = productsList.find(p => p.id === ing.productId) ||
                            productsList.find(p => p.id === ing.product_id) ||
                            productsList.find(p => p.ingredientId === ing.ingredientId) || null;

                        if (!product) return;

                        // ШУКАЄМО ДЕТАЛІ В JSON (Аналог бекенд-фільтрації по імені)
                        const productNameUk = (product.nameUk || product.name_uk || product.name || "").toLowerCase();
                        const detail = (Array.isArray(parsedDetails) ? parsedDetails : []).find(d => {
                            const jsonName = (d.name_uk || d.nameUk || d.name || "").toLowerCase();
                            if (!jsonName || !productNameUk) return false;
                            return productNameUk.includes(jsonName) || jsonName.includes(productNameUk);
                        });

                        let recipeAmount = 0;
                        let recipeUnit = "";

                        if (detail && detail.amount != null && detail.unit != null) {
                            recipeAmount = parseFloat(detail.amount);
                            recipeUnit = detail.unit.toLowerCase();
                        } else {
                            // Фолбек до полів об'єкта ing, якщо в JSON не знайшли
                            recipeAmount = parseFloat(ing.amount || ing.rawAmount || ing.quantity || ing.raw_amount || ing.total_amount) || 0;
                            recipeUnit = (ing.unit || '').toLowerCase();
                        }

                        // Якщо все ще 0, пробуємо розпарсити з тексту або ставимо 1
                        // Якщо все ще 0, пробуємо розпарсити з тексту або ставимо 1
                        if (recipeAmount <= 0) {
                            const rawName = (ing.rawName || '').toLowerCase();
                            const numMatch = rawName.match(/^([\d\s\.,\/\-]+)\s+(.*)/);
                            if (numMatch) {
                                let amountPart = numMatch[1].trim();
                                recipeAmount = amountPart.includes('/') ? 0.5 : parseFloat(amountPart.replace(',', '.')) || 1;
                                recipeUnit = numMatch[2].trim().split(' ')[0];
                            } else {
                                recipeAmount = 1;
                                recipeUnit = ''; // Змінено: залишаємо порожнім, щоб спрацював фолбек на базу продукту
                            }
                        }

                        // Логіка розрахунку упаковок (аналог бекенда)
                        const packageUnit = (product.packageUnit || product.unit || recipeUnit).toLowerCase();
                        const convertedAmount = convertToMatchShopUnit(recipeAmount, recipeUnit, packageUnit);

                        let qtyToAdd = 1; // За замовчуванням додаємо 1 упаковку/товар

                        if (product.packageAmount && product.packageAmount > 0) {
                            // Якщо бекенд передав об'єм упаковки (напр. 500мл), рахуємо скільки упаковок треба
                            qtyToAdd = Math.ceil(convertedAmount / product.packageAmount);
                        } else if (convertedAmount > 0 && (packageUnit === 'шт' || packageUnit === 'штук')) {
                            // Якщо товар поштучний (яблука, яйця), беремо стільки штук, скільки в рецепті
                            qtyToAdd = Math.ceil(convertedAmount);
                        }

                        // Запобіжник від нулів та NaN
                        if (isNaN(qtyToAdd) || qtyToAdd < 1) {
                            qtyToAdd = 1;
                        }

                        // Канонічна кількість для відображення
                        const canonical = convertToCanonical(recipeAmount, recipeUnit, product.unit || product.packageUnit);

                        const productId = product.id;
                        const getLocalizedName = () => {
                            const possibleFields = [product.nameUk, product.name_uk, product.name];
                            for (let n of possibleFields) {
                                if (n && typeof n === 'string' && n.trim().length > 0) {
                                    if (n.includes('|||')) return n.split('|||')[1].trim();
                                    if (/[а-яА-ЯёЁіІєЄїЇґҐ]/.test(n)) return n;
                                }
                            }
                            return product.nameUk || product.name_uk || product.name;
                        };

                        const displayName = getLocalizedName() || ing.name || 'Продукт';
                        const key = `${productId}`;

                        if (groceryMap[key]) {
                            groceryMap[key].numPacks += qtyToAdd;
                            groceryMap[key].canonicalAmount += canonical.val;
                        } else {
                            groceryMap[key] = {
                                id: productId,
                                name: displayName,
                                numPacks: qtyToAdd,
                                canonicalAmount: canonical.val,
                                canonicalType: canonical.type,
                                available: product.available,
                                basePrice: product.price,
                                imageUrl: product.imageUrl,
                                slug: product.slug
                            };
                        }
                    });
                }
            });

            const aggregatedList = Object.values(groceryMap).map(item => {
                return {
                    ...item,
                    amount: formatCanonical(item.canonicalAmount, item.canonicalType),
                    price: item.basePrice * item.numPacks
                };
            });

            aggregatedList.sort((a, b) => (a.available === b.available) ? 0 : a.available ? -1 : 1);
            setGroceryList(aggregatedList);
        } catch (err) {
            console.error("Помилка завантаження інгредієнтів:", err);
            alert("Не вдалося завантажити список продуктів. Перевірте з'єднання.");
        } finally {
            setGroceryLoading(false);
        }
    };

    const handleAddToCart = async () => {
        setCartLoading(true);
        try {
            // Отримуємо всі ID рецептів для поточного дня
            const currentRecipeIds = [...new Set(sortedSlots
                .map(slot => slot.recipeId)
                .filter(id => id != null)
            )];

            if (currentRecipeIds.length === 0) return;

            // Відправляємо паралельні запити на додавання кожного рецепта в кошик
            const promises = currentRecipeIds.map(id =>
                fetch(`/api/cart/add-recipe/${id}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${localStorage.getItem('accessToken')}`
                    }
                })
            );

            const responses = await Promise.all(promises);
            const failed = responses.some(res => !res.ok);

            if (failed) {
                alert('Частину продуктів додано, але виникли помилки з деякими рецептами.');
                window.dispatchEvent(new Event('cartUpdated')); // <--- ДОДАЄМО ТУТ
            } else {
                alert('✅ Всі доступні продукти успішно додано до вашого кошика!');
                setGroceryModalOpen(false);
                window.dispatchEvent(new Event('cartUpdated')); // <--- ДОДАЄМО ТУТ
            }
        } catch (err) {
            console.error("Помилка кошика:", err);
            alert('Помилка при додаванні продуктів у кошик.');
        } finally {
            setCartLoading(false);
        }
    };

    const handleMarkEaten = async (slotId, actualCalories) => {
        try {
            await fetch(`/api/meal-plan/mark-eaten`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${localStorage.getItem('accessToken')}`
                },
                body: JSON.stringify({ slotId, actualCalories })
            });
            await fetchStatus(); // Перезавантажуємо, щоб бекенд перерахував тижневий баланс
        } catch (err) {
            console.error(err);
        }
    };

    const handleSwap = async (slotId) => {
        try {
            const res = await fetch(`/api/meal-plan/swap-slot/${slotId}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
            });
            if (res.ok) await fetchStatus();
        } catch {
            alert('Не вдалось замінити страву. Спробуйте ще раз.');
        }
    };

    const handleLogFood = async () => {
        const val = extraInput.trim();
        if (!val) return;
        setLogLoading(true);
        const currentDayNumber = planData.days[activeDayIndex]?.dayNumber || 1;
        try {
            const res = await fetch('/api/meal-plan/log-food', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
                body: JSON.stringify({ foodText: val, dayNumber: currentDayNumber }),
            });
            if (res.ok) {
                await fetchStatus();
                setExtraInput('');
            }
        } finally {
            setLogLoading(false);
        }
    };

    if (loading) return <div style={{ textAlign: 'center', paddingTop: 80 }}><div className="spinner" style={{ margin: '0 auto 16px' }} /></div>;
    if (error) return <div style={{ textAlign: 'center', paddingTop: 80 }}>{error}</div>;
    if (!planData || !planData.days) return null;

    let startDow = 1;
    if (planData.weekStartDate) {
        const [y, m, d] = planData.weekStartDate.split('-');
        startDow = new Date(y, m - 1, d).getDay();
    }

    const tabs = Array.from({ length: planData.days.length }).map((_, i) => ({ index: i, name: DAY_NAMES[(startDow + i) % 7] }));
    const currentDay = planData.days[activeDayIndex] || planData.days[0];

    const sortedSlots = [...(currentDay.slots || [])].sort((a, b) => (MEAL_ORDER[(a.mealType || '').toLowerCase()] || 99) - (MEAL_ORDER[(b.mealType || '').toLowerCase()] || 99));

    // Групуємо додаткову їжу (Яблуко x2)
    const extraFoodRaw = currentDay.extraFood || [];
    const groupedExtraObj = extraFoodRaw.reduce((acc, item) => {
        const name = item.rawInput || item.name || item.recognizedName || 'Невідома їжа';
        if (!acc[name]) {
            acc[name] = {
                ...item,
                name,
                count: 1,
                totalCalories: item.totalCalories || item.calories || 0,
                proteinG: item.proteinG || 0,
                fatG: item.fatG || 0,
                carbsG: item.carbsG || 0
            };
        } else {
            acc[name].count += 1;
            acc[name].totalCalories += (item.totalCalories || item.calories || 0);
            acc[name].proteinG += (item.proteinG || 0);
            acc[name].fatG += (item.fatG || 0);
            acc[name].carbsG += (item.carbsG || 0);
        }
        return acc;
    }, {});
    const groupedExtra = Object.values(groupedExtraObj);
    const extraTotal = currentDay.extraCalories || groupedExtra.reduce((s, i) => s + i.totalCalories, 0);

    const balance = planData.weeklyBalance || {};
    const weeklyGoal = balance.weeklyCalorieTarget || 0;
    const weeklyRemaining = balance.remainingBudget || 0;
    const weeklyPct = weeklyGoal > 0 ? Math.round(((weeklyGoal - weeklyRemaining) / weeklyGoal) * 100) : 0;

    // Отримуємо цілі для поточного вибраного дня (з об'єкта updatedTargets)
    const dayTargetInfo = planData.updatedTargets?.find(t => t.dayNumber === currentDay.dayNumber);
    const targetDelta = dayTargetInfo?.delta || 0;
    const suggestedAction = dayTargetInfo?.suggestedAction || 'NONE'; // NONE, DROP_SIDES, SCALE_PORTIONS, REQUIRE_SWAP
    const adjustedTarget = dayTargetInfo?.adjustedTarget || currentDay.plannedCalories;

    let mainCals = 0;
    currentDay.slots?.forEach(s => { if (s.slotRole !== 'side') mainCals += s.targetCalories; });

    let adaptationRatio = 1;
    if (suggestedAction === 'SCALE_PORTIONS' && currentDay.plannedCalories > 0) {
        adaptationRatio = adjustedTarget / currentDay.plannedCalories;
    } else if (suggestedAction === 'DROP_SIDES_AND_SCALE' && mainCals > 0) {
        // Оскільки сайди обнуляються, нова ціль (adjustedTarget) лягає тільки на основні страви
        adaptationRatio = adjustedTarget / mainCals;
    }

    const adaptation = { action: suggestedAction, ratio: Math.min(1, Math.max(0.5, adaptationRatio)) }; // Обмеження ratio (від 50% до 100%)

    const displayDate = new Date();
    if (planData.weekStartDate) {
        const [y, m, d] = planData.weekStartDate.split('-');
        displayDate.setFullYear(y, m - 1, d);
        displayDate.setDate(displayDate.getDate() + activeDayIndex);
    }
    const todayZero = new Date(); todayZero.setHours(0, 0, 0, 0);
    displayDate.setHours(0, 0, 0, 0);
    const isToday = displayDate.getTime() === todayZero.getTime();
    const displayDateStr = displayDate.toLocaleDateString('uk-UA', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

    // Якщо список вже завантажено, показуємо точну цифру. Інакше - не виводимо число взагалі.
    const exactIngredientsCount = groceryList.length > 0 ? groceryList.length : null;

    // 1. Розрахунок макросів зі слотів (з урахуванням адаптації ШІ)
    // 1. Розрахунок макросів зі слотів (з урахуванням адаптації ШІ)
    const dailyTotals = sortedSlots.reduce((acc, slot) => {
        const isSide = slot.slotRole === 'side';
        const isDropped = (suggestedAction === 'DROP_SIDES' || suggestedAction === 'DROP_SIDES_AND_SCALE') && isSide;

        if (!isDropped) {
            const isScaled = (suggestedAction === 'SCALE_PORTIONS') || (suggestedAction === 'DROP_SIDES_AND_SCALE' && !isSide);
            const ratio = isScaled ? adaptationRatio : 1;

            acc.p += (slot.proteinG || 0) * ratio;
            acc.f += (slot.fatG || 0) * ratio;
            acc.c += (slot.carbsG || 0) * ratio;
        }
        return acc;
    }, { p: 0, f: 0, c: 0 });

    // 2. ДОДАЄМО макроси від незапланованої їжі (Яблуко, перекуси тощо)
    extraFoodRaw.forEach(extra => {
        dailyTotals.p += (extra.proteinG || 0);
        dailyTotals.f += (extra.fatG || 0);
        dailyTotals.c += (extra.carbsG || 0);
    });

    return (
        <>
            <Navbar />
            <main className="container tracker-main">
                <div className="dash-header">
                    <div className="dash-title">
                        <h1>{isToday ? 'Сьогодні' : `План на День ${currentDay.dayNumber}`}</h1>
                        <p>{displayDateStr} • День {currentDay.dayNumber} з {planData.days.length}</p>
                    </div>
                    <div className="day-selector">
                        {tabs.map((tab) => (
                            <button
                                key={tab.index}
                                className={`day-btn ${tab.index === activeDayIndex ? 'active' : ''}`}
                                onClick={() => setActiveDayIndex(tab.index)}
                            >
                                {tab.name}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="weekly-card">
                    <ProgressRing pct={weeklyPct} danger={weeklyPct > 100} />
                    <div className="weekly-stats">
                        <div className="w-stat">
                            <span className="w-lbl">Мета на тиждень</span>
                            <span className="w-val">{Math.round(weeklyGoal).toLocaleString()} <span>ккал</span></span>
                        </div>
                        <div className="w-stat">
                            <span className="w-lbl">Залишок бюджету</span>
                            <span className="w-val">{Math.round(weeklyRemaining).toLocaleString()} <span>ккал</span></span>
                        </div>
                        <div className="w-stat">
                            <span className="w-lbl">{isToday ? 'Сьогоднішня норма' : 'Норма на цей день'}</span>
                            <span className="w-val">
                                {Math.round(currentDay.plannedCalories || 0)}

                                {/* Використовуємо надійний інлайн-стиль */}
                                {extraTotal > 0 && (
                                    <span style={{ color: '#EF4444', marginLeft: '4px', fontSize: '14px', fontWeight: '800', letterSpacing: '-0.5px' }}>
                                        +{Math.round(extraTotal)}
                                    </span>
                                )}

                                {/* Якщо ШІ змінив норму (Мінус або Плюс з бекенду) */}
                                {targetDelta !== 0 && (
                                    <span style={{ color: targetDelta < 0 ? '#F59E0B' : '#10B981', marginLeft: 4 }}>
                                        {targetDelta > 0 ? '+' : ''}{Math.round(targetDelta)}
                                    </span>
                                )}
                                <span> ккал</span>
                            </span>
                            <div className="w-macros-summary">
                                <span className="w-macro-tag p" title="Білки">Б: {Math.round(dailyTotals.p)}г</span>
                                <span className="w-macro-tag f" title="Жири">Ж: {Math.round(dailyTotals.f)}г</span>
                                <span className="w-macro-tag c" title="Вуглеводи">В: {Math.round(dailyTotals.c)}г</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="main-grid">
                    <div className="meal-list-wrap">
                        {suggestedAction !== 'NONE' && (
                            <div className="ai-adjustment">
                                <div className="ai-adj-icon">🤖</div>
                                <div className="ai-adj-text">
                                    <strong>План адаптовано ШІ!</strong> Ваша ціль на цей день змінена на <strong><span style={{ color: '#EF4444' }}>{Math.round(targetDelta)} ккал</span></strong>.
                                    <br />
                                    {suggestedAction === 'DROP_SIDES' && 'Ми перекреслили другорядні страви. Рекомендуємо їх пропустити.'}
                                    {suggestedAction === 'DROP_1_SIDE' && 'Для балансу рекомендуємо пропустити одну з другорядних страв (гарнір або снек).'}
                                    {suggestedAction === 'SCALE_PORTIONS' && `Будь ласка, зменште порції на ~${Math.round((1 - adaptationRatio) * 100)}%.`}
                                    {suggestedAction === 'DROP_SIDES_AND_SCALE' && `Для компенсації переїдання ми радимо пропустити другорядні страви, а порції основних зменшити на ~${Math.round((1 - adaptationRatio) * 100)}%.`}
                                    {suggestedAction === 'REQUIRE_SWAP' && 'Занадто великий дефіцит! Будь ласка, натисніть кнопку 🔄 біля калорійних страв, щоб замінити їх на легші.'}
                                </div>
                            </div>
                        )}
                        {/* Банер покупок */}
                        <div className="day-grocery-banner">
                            <div className="grocery-info">
                                <h3>🛒 Купити продукти на день</h3>
                                <p>Сформуйте кошик продуктів для {sortedSlots.length} страв на сьогодні</p>
                            </div>
                            <div className="grocery-actions">
                                <button className="btn-grocery-view" onClick={handleOpenGrocery}>Переглянути</button>
                                <button className="btn-grocery-cart" onClick={handleAddToCart}>В кошик</button>
                            </div>
                        </div>

                        <div className="meal-list">
                            {sortedSlots.length > 0 ? sortedSlots.map(slot => (
                                <MealCard
                                    key={slot.slotId}
                                    slot={{
                                        id: slot.slotId,
                                        recipeId: slot.recipeId,
                                        mealType: slot.mealType,
                                        recipeName: slot.recipeName,
                                        recipeSlug: slot.recipeSlug,
                                        calories: slot.targetCalories,
                                        slotRole: slot.slotRole, // Передаємо роль
                                        proteinG: slot.proteinG, // Передаємо макроси
                                        fatG: slot.fatG,
                                        carbsG: slot.carbsG,
                                        eaten: slot.status === 'EATEN' || slot.actualCalories !== null,
                                    }}
                                    adaptation={adaptation}
                                    onMarkEaten={handleMarkEaten}
                                    onSwap={handleSwap}
                                    isToday={isToday}
                                />
                            )) : (
                                <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>Немає запланованих прийомів їжі.</div>
                            )}
                        </div>
                    </div>

                    <aside>
                        <div className="extra-food-panel">
                            <div className="extra-header">
                                Незапланована їжа
                                {extraTotal > 0 && <span className="extra-badge">+{Math.round(extraTotal)} ккал</span>}
                            </div>

                            {!isToday ? (
                                <div className="extra-disabled-msg">
                                    ⚠️ Додавати або змінювати незаплановану їжу можна лише для поточного дня (Сьогодні).
                                </div>
                            ) : (
                                <>
                                    <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16 }}>
                                        З'їли щось поза планом? Опишіть це звичайними словами.
                                    </p>
                                    <div className="extra-input-wrap">
                                        <input
                                            type="text"
                                            value={extraInput}
                                            onChange={e => setExtraInput(e.target.value)}
                                            onKeyDown={e => e.key === 'Enter' && handleLogFood()}
                                            placeholder="Напр: великий Снікерс..."
                                            disabled={logLoading}
                                        />
                                        <button className="btn-add-extra" onClick={handleLogFood} disabled={logLoading}>
                                            {logLoading ? '⏳' : 'Додати'}
                                        </button>
                                    </div>
                                </>
                            )}

                            <div className="extra-list">
                                {groupedExtra.length > 0 ? (
                                    groupedExtra.map((item, i) => <ExtraFoodItem key={i} item={item} />)
                                ) : (
                                    <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-muted)', fontSize: 13 }}>
                                        Поки нічого не додано 🎉
                                    </div>
                                )}
                            </div>
                        </div>
                    </aside>
                </div>
            </main>
            {/* Модальне вікно інгредієнтів */}
            {groceryModalOpen && (
                <div className="grocery-modal-overlay" onClick={() => setGroceryModalOpen(false)}>
                    <div className="grocery-modal-content" onClick={e => e.stopPropagation()}>
                        <div className="g-modal-header">
                            <h3>🛒 Продукти ({groceryList.length} шт.)</h3>
                            <button className="g-close-btn" onClick={() => setGroceryModalOpen(false)}>&times;</button>
                        </div>

                        <div className="g-modal-body">
                            {groceryLoading ? (
                                <div style={{ textAlign: 'center', padding: '40px 0' }}>⏳ Завантажуємо список...</div>
                            ) : groceryList.length > 0 ? (
                                <ul className="groc-list">
                                    {groceryList.map((item, idx) => (
                                        <li
                                            key={`${item.id}-${idx}`}
                                            className={`groc-item ${item.slug ? 'clickable' : ''}`}
                                            style={{ opacity: item.available ? 1 : 0.6 }}
                                            onClick={() => item.slug && navigate(`/product/${item.slug}`)}
                                        >
                                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                                                <span className="groc-name">{item.name}</span>
                                                {!item.available ? (
                                                    <span style={{ fontSize: 11, color: '#EF4444', fontWeight: 600, marginTop: 2 }}>
                                                        Немає в наявності
                                                    </span>
                                                ) : item.price > 0 ? (
                                                    <span style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                                                        {item.numPacks > 1 && (
                                                            <span style={{ fontWeight: 600 }}>{item.numPacks} уп. x {item.basePrice.toFixed(2)} ₴ = </span>
                                                        )}
                                                        Орієнтовно: {item.price.toFixed(2)} ₴
                                                    </span>
                                                ) : null}
                                            </div>
                                            <span className="groc-amount">
                                                {item.amount || '—'}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div style={{ textAlign: 'center', color: '#666', padding: '20px 0' }}>
                                    Список інгредієнтів порожній або рецепти не знайдені.
                                </div>
                            )}
                        </div>

                        <div className="g-modal-footer">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                                <span style={{ fontWeight: 600, color: 'var(--text)', fontSize: 15 }}>Доступно до замовлення:</span>
                                <span style={{ fontWeight: 800, fontSize: 20, color: 'var(--primary)' }}>
                                    {groceryList
                                        .filter(item => item.available)
                                        .reduce((sum, item) => sum + item.price, 0)
                                        .toFixed(2)} ₴
                                </span>
                            </div>
                            <button className="btn-save" onClick={handleAddToCart} disabled={cartLoading} style={{ width: '100%' }}>
                                {cartLoading ? 'Додаємо в кошик...' : 'Додати доступні продукти в кошик'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}