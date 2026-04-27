import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import './PlanPreviewPage.css';

// ─── Helpers ─────────────────────────────────────────────────

function getMealIcon(type) {
    const t = (type || '').toLowerCase();
    if (t.includes('breakfast') || t.includes('сніданок')) return '🍳';
    if (t.includes('lunch') || t.includes('обід')) return '🍲';
    if (t.includes('dinner') || t.includes('вечеря')) return '🐟';
    if (t.includes('snack') || t.includes('перекус')) return '🥜';
    return '🍽️';
}

function getMealLabel(type) {
    const t = (type || '').toLowerCase();
    if (t.includes('breakfast') || t.includes('сніданок')) return '🍳 Сніданок';
    if (t.includes('lunch') || t.includes('обід')) return '🍲 Обід';
    if (t.includes('dinner') || t.includes('вечеря')) return '🐟 Вечеря';
    if (t.includes('snack') || t.includes('перекус')) return '🥜 Перекус';
    return type || 'Прийом їжі';
}

// ─── Recipe Row ───────────────────────────────────────────────

function RecipeRow({ recipe, badge, badgeColor }) {
    if (!recipe) return null;
    return (
        <div className="recipe-row">
            <div className="recipe-img">{getMealIcon(recipe.name)}</div>
            <div className="recipe-info">
                <div className="recipe-badge" style={badgeColor ? { color: badgeColor } : {}}>
                    {badge}
                </div>
                <div className="recipe-name">{recipe.name}</div>
                {recipe.portionNote && <div className="recipe-portion">Порція: {recipe.portionNote}</div>}
            </div>
            <div className="recipe-stats">
                <div className="r-cal">{Math.round(recipe.estimatedCalories || 0)} ккал</div>
                <div className="r-mac">
                    Б: {Math.round(recipe.proteinG || 0)}г • Ж: {Math.round(recipe.fatG || 0)}г • В: {Math.round(recipe.carbsG || 0)}г
                </div>
            </div>
        </div>
    );
}

// ─── Meal Slot ────────────────────────────────────────────────

function MealSlot({ slot }) {
    // Бекед віддає об'єкти main та side. Робимо з них масив для рендеру
    const recipes = [];
    if (slot.main) recipes.push(slot.main);
    if (slot.side) recipes.push(slot.side);

    return (
        <div className="meal-slot">
            <div className="meal-header">
                <div className="meal-type">{getMealLabel(slot.mealType)}</div>
                <div className="meal-total">{Math.round(slot.slotTotalCalories || 0)} ккал</div>
            </div>
            {recipes.map((recipe, i) => (
                <div key={recipe.recipeId || i}>
                    {i > 0 && <div className="recipe-divider" />}
                    <RecipeRow
                        recipe={recipe}
                        badge={i === 0 ? 'Основна страва' : 'Додаток (Side)'}
                        badgeColor={i === 0 ? 'var(--primary)' : undefined}
                    />
                </div>
            ))}
        </div>
    );
}

// ─── Main ─────────────────────────────────────────────────────

export default function PlanPreviewPage() {
    const location = useLocation();
    const navigate = useNavigate();

    // Беремо дані, які передав SurveyPage
    const plan = location.state?.plan || {};

    const [activeDay, setActiveDay] = useState(0);
    const [approving, setApproving] = useState(false);
    const [approved, setApproved] = useState(false);

    // Адаптація під реальний JSON
    const finalizedPlan = plan.finalizedPlan || {};
    const days = finalizedPlan.days || plan.days || [];

    const currentDay = days[activeDay] || {};
    const slots = currentDay.slots || [];

    const handleApprove = async () => {
        setApproving(true);
        try {
            // Ви можете використати plan.planId для запиту активації
            // const res = await fetch(`/api/meal-plan/status`, { ... });

            // Поки що просто переходимо на трекер
            setApproved(true);
            setTimeout(() => navigate('/tracker'), 1000);
        } catch {
            setApproved(true);
            setTimeout(() => navigate('/tracker'), 1000);
        }
    };

    return (
        <>
            <Navbar />

            <main className="container preview-container">
                <div className="plan-hero">
                    <h1><span className="confetti">✨</span> Ваш персональний раціон готовий!</h1>
                    <p>ШІ-асистент проаналізував ваші дані та створив ідеальне меню. Перегляньте страви на тиждень перед тим, як почати трекінг.</p>
                </div>

                {/* Day Tabs */}
                <div className="days-nav">
                    {days.map((day, i) => (
                        <div
                            key={i}
                            className={`day-tab ${i === activeDay ? 'active' : ''}`}
                            onClick={() => setActiveDay(i)}
                        >
                            <div className="day-name">День {day.day || day.dayNumber || i + 1}</div>
                            <div className="day-cals">{Math.round(day.dayTotalCalories || day.dailyCalorieTarget || 0)}</div>
                        </div>
                    ))}
                </div>

                {/* Day Summary */}
                <div className="day-summary">
                    <div className="day-title">План на День {currentDay.day || activeDay + 1}</div>
                    <div className="day-macros">
                        <div className="macro-item">
                            <div className="macro-val">{Math.round(currentDay.dailyProteinG || 0)}<span>г</span></div>
                            <div className="macro-lbl">Білки</div>
                        </div>
                        <div className="macro-item">
                            <div className="macro-val">{Math.round(currentDay.dailyFatG || 0)}<span>г</span></div>
                            <div className="macro-lbl">Жири</div>
                        </div>
                        <div className="macro-item">
                            <div className="macro-val">{Math.round(currentDay.dailyCarbsG || 0)}<span>г</span></div>
                            <div className="macro-lbl">Вуглеводи</div>
                        </div>
                    </div>
                </div>

                {/* Meal Slots */}
                <div style={{ flexGrow: 1 }}>
                    {slots.length > 0 ? (
                        slots.map((slot, i) => <MealSlot key={i} slot={slot} />)
                    ) : (
                        <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--text-muted)' }}>
                            <div style={{ fontSize: 40, marginBottom: 12 }}>📋</div>
                            <p>Деталі раціону для цього дня ще генеруються...</p>
                        </div>
                    )}
                </div>
            </main>

            {/* Floating Action Bar */}
            <div className="action-bar">
                <div className="action-inner">
                    <div className="action-text">
                        <div className="action-title">Раціон успішно згенеровано</div>
                        <div className="action-sub">Усі інгредієнти вже готові до перегляду.</div>
                    </div>
                    <button className="btn-approve" onClick={handleApprove} disabled={approving || approved}>
                        {approved ? '✅ Активовано!' : approving ? '🔄 Зберігаємо...' : '🔥 Затвердити та Почати'}
                    </button>
                </div>
            </div>
        </>
    );
}