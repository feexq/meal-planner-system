import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  RadialLinearScale,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';
import { Line, Bar, Doughnut, Radar } from 'react-chartjs-2';
import Navbar from '../components/Navbar';
import ProfileSidebar from '../components/ProfileSidebar';
import { profileAPI, ordersAPI, preferencesAPI, mealPlanAPI, recipesAPI, productsAPI } from '../api/api';
import './StatisticsPage.css';

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement, BarElement,
  RadialLinearScale, ArcElement, Title, Tooltip, Legend, Filler
);

// --- Date helpers ---
const formatToDDMMYYYY = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()}`;
};
const parseFromDDMMYYYY = (str) => {
  if (!str || str.length !== 10) return null;
  const [dd, mm, yyyy] = str.split('.');
  return `${yyyy}-${mm}-${dd}`;
};
const handleMaskedDateInput = (e, setter) => {
  let val = e.target.value.replace(/\D/g, '');
  if (val.length > 8) val = val.substring(0, 8);
  let formatted = val;
  if (val.length >= 5) formatted = `${val.substring(0, 2)}.${val.substring(2, 4)}.${val.substring(4)}`;
  else if (val.length >= 3) formatted = `${val.substring(0, 2)}.${val.substring(2)}`;
  setter(formatted);
};

// --- Norms calculation ---
const calculateNorms = (prefs) => {
  if (!prefs) return { calories: 2000, protein: 120, fat: 65, carbs: 230 };
  const { gender, weightKg, heightCm, age, activityLevel, goal, dietType } = prefs;
  let bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
  bmr = gender === 'MALE' ? bmr + 5 : bmr - 161;
  const activityFactors = { SEDENTARY: 1.2, LIGHT: 1.375, MODERATE: 1.55, ACTIVE: 1.725, VERY_ACTIVE: 1.9 };
  let tdee = bmr * (activityFactors[activityLevel] || 1.2);
  if (goal === 'WEIGHT_LOSS') tdee -= 500;
  if (goal === 'WEIGHT_GAIN') tdee += 500;
  const targetCalories = Math.max(Math.round(tdee), 1200);
  let proteinMultiplier = 1.2;
  if (goal === 'WEIGHT_LOSS') proteinMultiplier = (activityLevel === 'ACTIVE' || activityLevel === 'VERY_ACTIVE') ? 2.0 : 1.5;
  else if (goal === 'WEIGHT_GAIN') proteinMultiplier = 2.0;
  else proteinMultiplier = activityLevel === 'SEDENTARY' ? 1.1 : 1.5;
  if (dietType === 'VEGETARIAN') proteinMultiplier *= 1.15;
  if (dietType === 'VEGAN' || dietType === 'PLANT_BASED') proteinMultiplier *= 1.20;
  const protein = Math.round(weightKg * proteinMultiplier);
  const fat = Math.round((targetCalories * 0.25) / 9);
  const carbs = Math.max(130, Math.round((targetCalories - (protein * 4) - (fat * 9)) / 4));
  return { calories: targetCalories, protein, fat, carbs };
};

// --- Contextual warning messages ---
const getWeightChartWarning = (history) => {
  if (!history || history.length === 0) return { type: 'info', msg: 'Додайте перший запис ваги — і ми відстежимо ваш прогрес 📊' };
  if (history.length < 3) return { type: 'info', msg: `Поки лише ${history.length} запис${history.length === 1 ? '' : 'и'} ваги. Чим більше — тим точніший графік! 📈` };
  return null;
};
const getCalorieChartWarning = (days) => {
  if (!days || days.length === 0) return { type: 'info', msg: 'Поки немає даних по калоріях. Почніть відмічати страви в плані харчування! 🍽️' };
  const activeDays = days.filter(d => d.consumedCalories > 0).length;
  if (activeDays < 3) return { type: 'warning', msg: `Лише ${activeDays} активних дн${activeDays === 1 ? 'ень' : 'і'} з 7. Все попереду — головне почати! 💪` };
  return null;
};
const getHeatmapWarning = (heatmap) => {
  const activeDays = heatmap?.filter(d => d.completionRate > 0).length || 0;
  if (activeDays === 0) return { type: 'info', msg: 'Тут з\'явиться ваш календар дисципліни. Зробіть перший крок! 🗓️' };
  if (activeDays < 7) return { type: 'info', msg: `Перші ${activeDays} дн${activeDays === 1 ? 'ень' : 'і'} вже є! Продовжуйте — звичка формується за 21 день 🌱` };
  return null;
};
const getStreakWarning = (profile) => {
  const streak = profile?.currentStreak || 0;
  if (streak === 0) return { type: 'info', msg: 'Розпочніть свій перший стрік вже сьогодні! Перший день — найважливіший 🔥' };
  if (streak < 3) return { type: 'info', msg: `${streak} день — відмінний старт! Ще ${3 - streak} дні до мінімального стріку 🎯` };
  if (streak >= 7) return { type: 'success', msg: `Тиждень поспіль — це вже характер! 🏆 Ваш рекорд: ${profile?.longestStreak || streak} днів` };
  return null;
};
const getAchievementWarning = (achievements) => {
  const earned = achievements?.filter(a => a.achieved).length || 0;
  const total = achievements?.length || 0;
  if (earned === 0) return { type: 'info', msg: 'Не переживайте — всі досягнення попереду! Кожна страва наближає вас до нагороди 🏅' };
  if (earned < total / 2) return { type: 'info', msg: `${earned} з ${total} досягнень отримано. Ви на вірному шляху! ✨` };
  return null;
};

const WarningBanner = ({ warning }) => {
  if (!warning) return null;
  const icons = { success: '🎉', warning: '⚠️', info: '💡' };
  return (
    <div className="chart-warning-banner" data-type={warning.type}>
      <span>{icons[warning.type]}</span>
      <span>{warning.msg}</span>
    </div>
  );
};

// --- Achievement Modal ---
const AchievementModal = ({ achievement, onClose }) => {
  if (!achievement) return null;
  const progress = achievement.targetValue > 0
    ? Math.min(100, Math.round((achievement.currentProgress / achievement.targetValue) * 100))
    : achievement.achieved ? 100 : 0;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="achievement-modal" onClick={e => e.stopPropagation()}>
        <button className="modal-close-btn" onClick={onClose}>✕</button>
        <div className={`modal-icon-wrap ${achievement.achieved ? 'earned' : ''}`}>
          {achievement.iconUrl && !achievement.iconUrl.includes('example.com')
            ? <img src={achievement.iconUrl} alt="" style={{ width: '60%', height: '60%', objectFit: 'contain' }} />
            : <span style={{ fontSize: '52px' }}>🏆</span>
          }
        </div>
        <h3 className="modal-title">{achievement.title}</h3>
        <p className="modal-desc">{achievement.description}</p>
        {achievement.targetValue > 0 && (
          <div className="modal-progress-section">
            <div className="modal-progress-labels">
              <span>Прогрес</span>
              <span>{achievement.currentProgress} / {achievement.targetValue}</span>
            </div>
            <div className="modal-progress-bg">
              <div className="modal-progress-fill" style={{ width: `${progress}%`, background: achievement.achieved ? 'var(--success)' : 'var(--primary)' }} />
            </div>
          </div>
        )}
        {achievement.achieved && achievement.achievedAt && (
          <div className="modal-earned-date">
            🗓 Отримано: {new Date(achievement.achievedAt).toLocaleDateString('uk-UA', { day: 'numeric', month: 'long', year: 'numeric' })}
          </div>
        )}
        <div className={`modal-status ${achievement.achieved ? 'success' : 'pending'}`}>
          {achievement.achieved ? '✓ Досягнення отримано' : `В процесі · ${progress}%`}
        </div>
      </div>
    </div>
  );
};

// --- Ingredient word cloud (uses ingredientId as key for deduplication) ---
const INGREDIENT_BLACKLIST = [
  'сіль', 'вода', 'цукор', 'перець', 'олія', 'соль', 'сахар', 'масло',
  'оливкова олія', 'соняшникова олія', 'чорний перець', 'часник', 'лук',
  'цибуля', 'сода', 'борошно', 'крохмаль', 'оцет', 'лавровий лист', 'кмин',
  'паприка', 'куркума', 'зелень', 'лимонний сік', 'сіль і перець'
];

const buildIngredientCloud = (topRecipeDetails) => {
  // Use ingredientId-based deduplication — same ingredient across recipes counted once per recipe
  const ingredientMap = {}; // ingredientId -> { name, count }
  topRecipeDetails?.forEach(recipe => {
    const seenInThisRecipe = new Set();
    recipe?.ingredients?.forEach(ing => {
      const id = ing.ingredientId;
      const rawName = (ing.rawName || '').toLowerCase().trim();
      const cleanName = rawName.replace(/\d+(\.\d+)?\s*(г|кг|мл|л|шт|ст\.л|ч\.л|склянки?|пучок|зубчик[ів]*)\.?\s*/gi, '').trim();

      if (!cleanName || cleanName.length < 3) return;
      if (INGREDIENT_BLACKLIST.some(b => cleanName.includes(b))) return;
      if (seenInThisRecipe.has(id || cleanName)) return;

      seenInThisRecipe.add(id || cleanName);
      const key = id || cleanName;
      if (!ingredientMap[key]) {
        const capitalized = cleanName.charAt(0).toUpperCase() + cleanName.slice(1);
        ingredientMap[key] = { name: capitalized, count: 0 };
      }
      ingredientMap[key].count++;
    });
  });

  return Object.values(ingredientMap)
    .sort((a, b) => b.count - a.count)
    .slice(0, 16);
};

const StatisticsPage = () => {
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState('7');
  const [macroTab, setMacroTab] = useState('protein');
  const [selectedAchievement, setSelectedAchievement] = useState(null);
  const [sliderIdx, setSliderIdx] = useState(0);
  const [data, setData] = useState({
    profile: null, preferences: null, planStatus: null,
    weightHistory: [], heatmap: [], achievements: [],
    topRecipes: [], topRecipeDetails: [], orders: [],
    topProducts: []
  });

  const [isWeightModalOpen, setIsWeightModalOpen] = useState(false);
  const [weightInput, setWeightInput] = useState('');
  const [weightNote, setWeightNote] = useState('');
  const [weightDate, setWeightDate] = useState('');

  // Period-aware date range for weight/heatmap
  const getDateRange = useCallback((p) => {
    const today = new Date();
    const from = new Date();
    if (p === '7') from.setDate(today.getDate() - 7);
    else if (p === '30') from.setDate(today.getDate() - 30);
    else if (p === '90') from.setDate(today.getDate() - 90);
    else from.setMonth(today.getMonth() - 6);
    return {
      from: from.toISOString().split('T')[0],
      to: today.toISOString().split('T')[0]
    };
  }, []);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const { from: heatFrom, to: heatTo } = getDateRange('180'); // heatmap always 6mo
      const { from: weightFrom } = getDateRange(period);

      const [profileRes, prefRes, weightRes, heatmapRes, achRes, topRes, ordRes, statusRes] = await Promise.all([
        profileAPI.getProfile(),
        preferencesAPI.get(),
        profileAPI.getWeightHistory({ from: weightFrom, limit: period === '7' ? 14 : period === '30' ? 35 : 100 }),
        profileAPI.getNutritionHeatmap(heatFrom, heatTo),
        profileAPI.getAchievements(),
        profileAPI.getTopRecipes(),
        ordersAPI.getOrders(),
        mealPlanAPI.getStatus()
      ]);

      const topRecipes = topRes.data || [];
      let topDetails = [];
      if (topRecipes.length > 0) {
        const detailResponses = await Promise.all(topRecipes.map(r => recipesAPI.getById(r.recipeId)));
        topDetails = detailResponses.map(res => res.data);
      }

      // Sort achievements: earned first
      const sortedAchievements = (achRes.data || []).sort((a, b) => {
        if (a.achieved === b.achieved) return 0;
        return a.achieved ? -1 : 1;
      });

      // Extract ingredientIds for the word cloud products
      const allIngredientIds = Array.from(new Set(
        topDetails.flatMap(r => r.ingredients?.map(ing => ing.ingredientId) || [])
          .filter(id => !!id)
      ));

      let products = [];
      if (allIngredientIds.length > 0) {
        try {
          products = await productsAPI.findAllByIngredients(allIngredientIds);
        } catch (err) {
          console.warn("Failed to fetch products for cloud:", err);
        }
      }

      setData({
        profile: profileRes.data,
        preferences: prefRes.data,
        planStatus: statusRes.data,
        weightHistory: weightRes.data || [],
        heatmap: heatmapRes.data || [],
        achievements: sortedAchievements,
        topRecipes: topRecipes,
        topRecipeDetails: topDetails,
        orders: ordRes.data || [],
        topProducts: products
      });
    } catch (error) {
      console.error('Error fetching statistics:', error);
    } finally {
      setLoading(false);
    }
  }, [period, getDateRange]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const sixMonthsGrid = useMemo(() => {
    const today = new Date();
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setMonth(today.getMonth() - 6);
    const grid = [];
    const diffDays = Math.ceil(Math.abs(today - sixMonthsAgo) / (1000 * 60 * 60 * 24));
    for (let i = 0; i <= diffDays; i++) {
      const d = new Date(sixMonthsAgo);
      d.setDate(sixMonthsAgo.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];
      const dayData = data.heatmap.find(h => h.date === dateStr);
      let level = 0;
      if (dayData) {
        if (dayData.completionRate >= 100) level = 4;
        else if (dayData.completionRate >= 80) level = 3;
        else if (dayData.completionRate >= 50) level = 2;
        else if (dayData.completionRate > 0) level = 1;
      }
      grid.push({ date: dateStr, level, data: dayData });
    }
    return grid;
  }, [data.heatmap]);

  // Dynamic averages based on selected period (calculated from heatmap data)
  const periodAverages = useMemo(() => {
    if (!data.heatmap.length) return null;
    const { from } = getDateRange(period);

    // Filter heatmap data for the selected period using string comparison
    const filtered = data.heatmap.filter(d => d.date >= from);
    if (!filtered.length) return null;

    const daysWithData = filtered.filter(d => d.totalCalories > 0 || d.completionRate > 0);
    const activeDaysCount = daysWithData.length || 1;

    return {
      avgCalories: filtered.reduce((acc, d) => acc + d.totalCalories, 0) / activeDaysCount,
      avgCompletion: filtered.reduce((acc, d) => acc + d.completionRate, 0) / activeDaysCount,
      cleanDays: filtered.filter(d => d.completionRate >= 90).length
    };
  }, [data.heatmap, period, getDateRange]);

  const openWeightModal = () => {
    setWeightDate(formatToDDMMYYYY(new Date()));
    setWeightInput(''); setWeightNote('');
    setIsWeightModalOpen(true);
  };

  const handleSaveWeight = async () => {
    if (weightDate.length !== 10) { alert('Будь ласка, введіть повну дату у форматі ДД.ММ.РРРР'); return; }
    if (weightInput && !isNaN(parseFloat(weightInput))) {
      try {
        const payload = { weightKg: parseFloat(weightInput) };
        if (weightNote.trim()) payload.note = weightNote.trim();
        const apiDate = parseFromDDMMYYYY(weightDate);
        const todayApi = new Date().toISOString().split('T')[0];
        if (apiDate && apiDate !== todayApi) await profileAPI.logWeightForDate(apiDate, payload);
        else await profileAPI.logWeight(payload);
        setIsWeightModalOpen(false);
        fetchData();
      } catch { alert('Помилка при збереженні ваги.'); }
    }
  };


  const norms = calculateNorms(data.preferences);

  // Weight chart
  const weightLabels = data.weightHistory.length > 0
    ? [...data.weightHistory].reverse().map(d => new Date(d.recordedDate).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' }))
    : ['Немає даних'];
  const weightValues = data.weightHistory.length > 0
    ? [...data.weightHistory].reverse().map(d => d.weightKg) : [0];

  const currentWeight = data.profile?.currentWeightKg || 0;
  const targetWeight = data.profile?.targetWeightKg || 0;
  const startWeight = data.weightHistory.length > 0 ? data.weightHistory[data.weightHistory.length - 1]?.weightKg : currentWeight;

  const weightChartData = {
    labels: weightLabels,
    datasets: [
      {
        label: 'Вага (кг)', data: weightValues, borderColor: '#6C3FC5',
        backgroundColor: (context) => {
          const ctx = context.chart.ctx;
          const gradient = ctx.createLinearGradient(0, 0, 0, 250);
          gradient.addColorStop(0, 'rgba(108, 63, 197, 0.15)');
          gradient.addColorStop(1, 'rgba(108, 63, 197, 0)');
          return gradient;
        },
        borderWidth: 3, tension: 0.4, fill: true,
        pointBackgroundColor: '#fff', pointBorderColor: '#6C3FC5', pointBorderWidth: 2, pointRadius: 4, pointHoverRadius: 6
      },
      {
        label: 'Ціль',
        data: Array(weightLabels.length).fill(data.profile?.targetWeightKg || 0),
        borderColor: '#10B981', borderWidth: 2, borderDash: [4, 4], pointRadius: 0, fill: false
      }
    ]
  };
  const weightOptions = {
    responsive: true, maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
    plugins: { legend: { display: false } },
    scales: {
      y: {
        min: Math.min(...weightValues, targetWeight) - 2,
        max: Math.max(...weightValues, targetWeight) + 2,
        grid: { color: '#F7F8FA' }
      },
      x: {
        grid: { display: false },
        ticks: {
          maxTicksLimit: period === '7' ? 7 : period === '30' ? 10 : 15,
          maxRotation: 0,
          font: { size: 10 }
        }
      }
    }
  };

  // Goal donut
  const weightProgress = data.profile?.weightProgressPercent || 0;
  const goalChartData = {
    labels: ['Пройдено', 'Залишилось'],
    datasets: [{ data: [weightProgress, 100 - weightProgress], backgroundColor: ['#6C3FC5', '#F0EAFE'], borderWidth: 0, cutout: '80%', borderRadius: 10 }]
  };


  // Calories plan vs fact
  const last7Days = data.planStatus?.days || [];
  const zigzagLabels = last7Days.map(d => `День ${d.dayNumber}`);
  const zigzagData = {
    labels: zigzagLabels.length > 0 ? zigzagLabels : ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд'],
    datasets: [
      {
        label: 'Факт',
        data: last7Days.map(d => d.consumedCalories),
        backgroundColor: (ctx) => {
          const val = ctx.raw;
          const target = last7Days[ctx.dataIndex]?.targetCalories || norms.calories;
          if (val === 0) return 'rgba(0,0,0,0.05)';
          return val > target * 1.1 ? '#EF4444' : '#6C3FC5';
        },
        borderRadius: 6
      },
      {
        label: 'Норма', data: last7Days.map(d => d.targetCalories),
        type: 'line', borderColor: '#10B981', borderWidth: 2, borderDash: [4, 4], pointRadius: 0, fill: false
      }
    ]
  };

  // Aggregates for radar
  const aggregates = useMemo(() => {
    const days = data.planStatus?.days || [];
    const divisor = Math.max(days.length, 1);
    const stats = { target: { cal: 0, p: 0, f: 0, c: 0 }, actual: { cal: 0, p: 0, f: 0, c: 0 } };
    days.forEach(day => {
      day.slots?.forEach(slot => {
        stats.target.cal += slot.targetCalories || 0;
        stats.target.p += slot.proteinG || 0;
        stats.target.f += slot.fatG || 0;
        stats.target.c += slot.carbsG || 0;
        if (slot.status?.toUpperCase() === 'EATEN') {
          stats.actual.cal += slot.actualCalories || slot.targetCalories || 0;
          stats.actual.p += slot.proteinG || 0;
          stats.actual.f += slot.fatG || 0;
          stats.actual.c += slot.carbsG || 0;
        }
      });
      day.extraFood?.forEach(food => {
        stats.actual.cal += food.totalCalories || 0;
        stats.actual.p += food.proteinG || 0;
        stats.actual.f += food.fatG || 0;
        stats.actual.c += food.carbsG || 0;
      });
    });
    return {
      target: { cal: stats.target.cal / divisor, p: stats.target.p / divisor, f: stats.target.f / divisor, c: stats.target.c / divisor },
      actual: { cal: stats.actual.cal / divisor, p: stats.actual.p / divisor, f: stats.actual.f / divisor, c: stats.actual.c / divisor }
    };
  }, [data.planStatus]);

  const radarData = {
    labels: ['Калорії', 'Білки', 'Жири', 'Вуглеводи'],
    datasets: [
      {
        label: 'Факт (%)',
        data: [
          (aggregates.actual.cal / (aggregates.target.cal || 1)) * 100,
          (aggregates.actual.p / (aggregates.target.p || 1)) * 100,
          (aggregates.actual.f / (aggregates.target.f || 1)) * 100,
          (aggregates.actual.c / (aggregates.target.c || 1)) * 100,
        ],
        borderColor: 'rgba(108, 63, 197, 0.8)', backgroundColor: 'rgba(108, 63, 197, 0.1)',
        borderWidth: 2, pointBackgroundColor: '#6C3FC5',
      },
      {
        label: 'План (100%)', data: [100, 100, 100, 100],
        borderColor: 'rgba(16, 185, 129, 0.6)', backgroundColor: 'rgba(16, 185, 129, 0.05)',
        borderWidth: 2, borderDash: [4, 4], pointRadius: 3, pointBackgroundColor: '#10B981',
      }
    ]
  };

  const macroBarData = {
    labels: zigzagLabels.length > 0 ? zigzagLabels : ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд'],
    datasets: [
      {
        label: 'Факт',
        data: last7Days.map(d => {
          if (macroTab === 'protein') return d.slots?.reduce((acc, s) => acc + (s.proteinG || 0), 0) || 0;
          if (macroTab === 'fat') return d.slots?.reduce((acc, s) => acc + (s.fatG || 0), 0) || 0;
          return d.slots?.reduce((acc, s) => acc + (s.carbsG || 0), 0) || 0;
        }),
        backgroundColor: macroTab === 'protein' ? '#3B82F6' : macroTab === 'fat' ? '#F59E0B' : '#10B981',
        borderRadius: 4
      },
      {
        label: 'Норма',
        data: Array(zigzagLabels.length || 7).fill(macroTab === 'protein' ? norms.protein : macroTab === 'fat' ? norms.fat : norms.carbs),
        type: 'line',
        borderColor: macroTab === 'protein' ? '#3B82F6' : macroTab === 'fat' ? '#F59E0B' : '#10B981',
        borderWidth: 2, borderDash: [4, 4], pointRadius: 0, fill: false
      }
    ]
  };

  // Product cloud logic
  const sortedProducts = useMemo(() => {
    const counts = {}; // slug -> { name, count, slug }
    const blacklist = [
      'сіль', 'вода', 'цукор', 'перець', 'олія', 'соль', 'сахар', 'масло',
      'оливкова олія', 'соняшникова олія', 'чорний перець', 'часник', 'лук',
      'цибуля', 'сода', 'борошно', 'крохмаль', 'оцет', 'лавровий лист',
      'кмин', 'паприка', 'куркума', 'зелень', 'лимонний сік', 'сіль і перець'
    ];

    // Quick lookup for products by their ID
    const productById = {};
    data.topProducts?.forEach(p => {
      if (p.id) productById[p.id] = p;
    });

    data.topRecipeDetails?.forEach(recipe => {
      recipe?.ingredients?.forEach(ing => {
        // Link recipe ingredient to product via productId
        const prod = productById[ing.productId];
        if (!prod) return;

        const displayName = prod.nameUk || prod.name || 'Продукт';
        const cleanName = displayName.toLowerCase().trim();
        if (cleanName.length < 3 || blacklist.some(b => cleanName.includes(b))) return;

        if (!counts[prod.slug]) {
          counts[prod.slug] = { name: displayName, count: 0, slug: prod.slug };
        }
        counts[prod.slug].count++;
      });
    });

    return Object.values(counts)
      .sort((a, b) => b.count - a.count)
      .slice(0, 16);
  }, [data.topProducts, data.topRecipeDetails]);

  // Completion by meal type — include SIDE slots as snacks
  const calculateCompletion = (mealType) => {
    if (!data.planStatus) return { percent: 0, count: 0, eaten: 0 };
    const allDays = data.planStatus.days || [];
    let count = 0, eaten = 0;
    allDays.forEach(day => {
      let slots = [];
      if (mealType === 'SNACK') {
        // Include both SNACK and SIDE slot roles
        slots = day.slots?.filter(s =>
          s.mealType?.toUpperCase() === 'SNACK' ||
          s.slotRole?.toUpperCase() === 'SIDE'
        ) || [];
      } else {
        slots = day.slots?.filter(s =>
          s.mealType?.toUpperCase() === mealType.toUpperCase() &&
          s.slotRole?.toUpperCase() !== 'SIDE'
        ) || [];
      }
      slots.forEach(slot => {
        count++;
        if (slot.status?.toUpperCase() === 'EATEN') eaten++;
      });
    });
    if (count === 0) return null; // null = no slots of this type at all
    return { percent: Math.round((eaten / count) * 100), count, eaten };
  };

  const radarOptions = {
    responsive: true, maintainAspectRatio: false,
    scales: {
      r: { beginAtZero: true, max: 150, ticks: { display: false }, grid: { color: '#F0F2F5' }, pointLabels: { font: { size: 11, weight: '600' }, color: '#64748B' } }
    },
    plugins: { legend: { display: false } }
  };

  // Achievements slider
  const achievementsPerPage = 3;
  const achPages = Math.ceil((data.achievements?.length || 0) / achievementsPerPage);
  const visibleAchievements = data.achievements.slice(sliderIdx * achievementsPerPage, (sliderIdx + 1) * achievementsPerPage);

  const mealTypes = [
    { label: '🌅 Сніданок', type: 'BREAKFAST' },
    { label: '☀️ Обід', type: 'LUNCH' },
    { label: '🌙 Вечеря', type: 'DINNER' },
    { label: '🥜 Перекус + Гарніри', type: 'SNACK' }
  ];

  if (loading) return <div className="page-loader"><div className="spinner"></div></div>;

  return (
    <div className="statistics-page">
      <Navbar />
      <main className="container">
        <div className="profile-layout">
          <ProfileSidebar />

          <div className="content-area">
            <div>
              <div className="page-header">
                <h1 className="page-title">Аналітика та Прогрес</h1>
                <div className="period-tabs">
                  {[{ v: '7', l: '7 днів' }, { v: '30', l: '30 днів' }, { v: '90', l: '3 місяці' }].map(tab => (
                    <button key={tab.v} className={`period-tab ${period === tab.v ? 'active' : ''}`} onClick={() => setPeriod(tab.v)}>{tab.l}</button>
                  ))}
                </div>
              </div>
              <p className="page-subtitle">Оновлено щойно • На основі ваших даних з Трекеру Раціону</p>
            </div>

            <div className="kpi-row">
              <div className="kpi-card">
                <div className="kpi-label">Середні Ккал/день</div>
                <div className="kpi-value" style={{ color: 'var(--primary)' }}>
                  {Math.round(periodAverages?.avgCalories || data.profile?.weeklyAverages?.avgCalories || 0)}
                </div>
                <div className="kpi-sub">
                  {period === '7' ? 'за останній тиждень' : period === '30' ? 'за останні 30 днів' : 'за останні 3 місяці'}
                </div>
              </div>
              <div className="kpi-card">
                <div className="kpi-label">Виконання плану</div>
                <div className="kpi-value" style={{ color: 'var(--success)' }}>
                  {Math.round(periodAverages?.avgCompletion || data.profile?.weeklyAverages?.avgCompletionRate || 0)}%
                </div>
                <div className="kpi-sub">середній показник</div>
              </div>
              <div className="kpi-card">
                <div className="kpi-label">Чистих днів</div>
                <div className="kpi-value" style={{ color: 'var(--accent)' }}>
                  {periodAverages?.cleanDays ?? data.profile?.weeklyAverages?.cleanDays ?? 0}
                </div>
                <div className="kpi-sub">
                  {period === '7' ? 'за тиждень' : period === '30' ? 'за 30 днів' : 'за 3 місяці'}
                </div>
              </div>
              <div className="kpi-card">
                <div className="kpi-label">Поточний стрік</div>
                <div className="kpi-value" style={{ color: '#D97706' }}>🔥 {data.profile?.currentStreak || 0}</div>
                <div className="kpi-sub">Рекорд: <strong>{data.profile?.longestStreak || 0}</strong> днів</div>
              </div>
            </div>

            <div className="row-goals">
              <div className="card">
                <div className="card-header">
                  <div className="card-title">📉 Динаміка ваги</div>
                  <button className="btn-add-weight" onClick={openWeightModal}>+ Додати вагу</button>
                </div>
                <WarningBanner warning={getWeightChartWarning(data.weightHistory)} />
                <div className="chart-container">
                  {data.weightHistory.length > 0 ? (
                    <Line data={weightChartData} options={weightOptions} />
                  ) : (
                    <div className="empty-chart-state">Додайте перший запис ваги, щоб побачити графік</div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '16px', marginTop: '12px', fontSize: '12px', fontWeight: 600 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><span style={{ display: 'inline-block', width: '12px', height: '3px', background: 'var(--primary)', borderRadius: '2px' }}></span> Фактична вага</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><span style={{ display: 'inline-block', width: '12px', height: '3px', borderTop: '2px dashed var(--success)' }}></span> Ціль</span>
                </div>
              </div>

              <div className="card goal-widget">
                <div className="card-header" style={{ width: '100%' }}><div className="card-title">🎯 Прогрес до цілі</div></div>
                <div className="goal-donut">
                  <Doughnut data={goalChartData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { enabled: false } } }} />
                  <div className="goal-text-inner">
                    <strong>{Math.round(weightProgress)}%</strong>
                    <span>Пройдено</span>
                  </div>
                </div>
                <div className="goal-motivation">✨ Залишилось {Math.max(0, currentWeight - targetWeight).toFixed(1)} кг!</div>
                <div className="goal-stats-row">
                  <div className="goal-stat"><div className="goal-stat-val">{startWeight} кг</div><div className="goal-stat-lbl">Початок</div></div>
                  <div className="goal-stat"><div className="goal-stat-val" style={{ color: 'var(--primary)' }}>{currentWeight} кг</div><div className="goal-stat-lbl">Зараз</div></div>
                  <div className="goal-stat"><div className="goal-stat-val" style={{ color: 'var(--success)' }}>{targetWeight} кг</div><div className="goal-stat-lbl">Ціль</div></div>
                </div>
              </div>
            </div>

            <div className="row-ai">
              <div className="card">
                <div className="card-header">
                  <div className="card-title">📈 Калорії: План vs Факт</div>
                  <span className="badge-tag">DailyNutrition</span>
                </div>
                <WarningBanner warning={getCalorieChartWarning(last7Days)} />
                <div className="chart-container">
                  <Bar data={zigzagData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }} />
                </div>
                <div style={{ display: 'flex', gap: '12px', marginTop: '12px', fontSize: '12px', fontWeight: 600 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ display: 'inline-block', width: '10px', height: '10px', background: 'var(--primary)', borderRadius: '2px' }}></span> Факт</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ display: 'inline-block', width: '10px', height: '10px', background: '#EF4444', borderRadius: '2px' }}></span> Перевищення</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ display: 'inline-block', width: '12px', height: '2px', borderTop: '2px dashed var(--success)' }}></span> Норма</span>
                </div>
              </div>

              <div className="card">
                <div className="card-header">
                  <div style={{ flex: 1 }}>
                    <div className="card-title">🕸️ Баланс БЖВ (Радар)</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>Середній факт за 7 днів відносно норми</div>
                  </div>
                  <div className="radar-legend">
                    <span><span className="dot" style={{ background: 'rgba(108,63,197,0.6)' }}></span> Факт</span>
                    <span><span className="dot" style={{ background: 'rgba(16,185,129,0.4)' }}></span> План</span>
                  </div>
                </div>
                <div className="chart-container"><Radar data={radarData} options={radarOptions} /></div>
                <div className="radar-values-grid">
                  <div className="r-val-item"><span className="r-val-lbl">Білки</span><span className="r-val-num">{Math.round(aggregates.actual.p)}г <small>/ {Math.round(aggregates.target.p)}г</small></span></div>
                  <div className="r-val-item"><span className="r-val-lbl">Жири</span><span className="r-val-num">{Math.round(aggregates.actual.f)}г <small>/ {Math.round(aggregates.target.f)}г</small></span></div>
                  <div className="r-val-item"><span className="r-val-lbl">Вуглев.</span><span className="r-val-num">{Math.round(aggregates.actual.c)}г <small>/ {Math.round(aggregates.target.c)}г</small></span></div>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <div className="card-title">⚖️ Тижневий звіт макронутрієнтів</div>
                <div className="macro-tabs">
                  <span className={`macro-tab protein ${macroTab === 'protein' ? 'active' : ''}`} onClick={() => setMacroTab('protein')}>Білки 🥩</span>
                  <span className={`macro-tab fat ${macroTab === 'fat' ? 'active' : ''}`} onClick={() => setMacroTab('fat')}>Жири 🧈</span>
                  <span className={`macro-tab carbs ${macroTab === 'carbs' ? 'active' : ''}`} onClick={() => setMacroTab('carbs')}>Вуглеводи 🌾</span>
                </div>
              </div>
              <div className="chart-container" style={{ height: '220px' }}>
                <Bar data={macroBarData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }} />
              </div>
              <div style={{ display: 'flex', gap: '16px', marginTop: '10px', fontSize: '12px', fontWeight: 600 }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ display: 'inline-block', width: '10px', height: '10px', background: macroTab === 'protein' ? '#3B82F6' : macroTab === 'fat' ? '#F59E0B' : '#10B981', borderRadius: '2px' }}></span> Факт</span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ display: 'inline-block', width: '10px', height: '10px', border: `2px dashed ${macroTab === 'protein' ? '#3B82F6' : macroTab === 'fat' ? '#F59E0B' : '#10B981'}`, borderRadius: '2px' }}></span> Норма</span>
              </div>
            </div>

            {/* Heatmap */}
            <div className="card">
              <div className="card-header" style={{ marginBottom: '12px' }}>
                <div className="card-title">🗓️ Календар дисципліни (Останні 6 місяців)</div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{data.heatmap.filter(d => d.completionRate > 0).length} днів виконання</div>
              </div>
              <WarningBanner warning={getHeatmapWarning(data.heatmap)} />
              <div className="hm-scroll-container">
                <div className="hm-grid">
                  {sixMonthsGrid.map((day, idx) => {
                    const isOutsidePeriod = new Date(day.date) < new Date(getDateRange(period).from);
                    return (
                      <div key={idx}
                        className={`hm-cell ${isOutsidePeriod ? 'hm-dim' : ''}`}
                        data-level={day.level}
                        title={`${day.date}: ${day.data ? `${day.data.completionRate}%` : 'Немає даних'}`}
                      ></div>
                    );
                  })}
                </div>
              </div>
              <div className="hm-footer">
                <div className="hm-legend">
                  Менше
                  <div className="hm-legend-cells">
                    {[0, 1, 2, 3, 4].map(l => <div key={l} className="hm-cell" data-level={l}></div>)}
                  </div>
                  Більше
                </div>
              </div>
            </div>

            {/* Combined Streak + Achievements card */}
            <div className="card combined-gamification-card">
              <div className="streak-section">
                <WarningBanner warning={getStreakWarning(data.profile)} />
                <div className="bg-fire">🔥</div>
                <div className="streak-title-row">
                  <div className="streak-title">🔥 Поточний стрік</div>
                  <div className="streak-type-badge">{data.profile?.streakType || 'CASUAL'}</div>
                </div>
                <div className="streak-main-num">{data.profile?.currentStreak || 0}</div>
                <div className="streak-main-lbl">днів поспіль</div>
                <div className="streak-stats">
                  <div className="s-stat"><div className="s-stat-val">{data.profile?.longestStreak || 0}</div><div className="s-stat-lbl">Рекорд</div></div>
                  <div className="s-stat"><div className="s-stat-val">{data.profile?.totalActiveDays || 0}</div><div className="s-stat-lbl">Всього днів</div></div>
                  <div className="s-stat"><div className="s-stat-val">{data.profile?.freezesAvailable || 0} ❄️</div><div className="s-stat-lbl">Заморозки</div></div>
                </div>
              </div>

              <div className="divider-v"></div>

              <div className="achievements-section">
                <div className="ach-header">
                  <div className="card-title">🏅 Досягнення</div>
                  <div className="ach-header-right">
                    <span className="ach-count">
                      {data.achievements.filter(a => a.achieved).length}/{data.achievements.length}
                    </span>
                    {achPages > 1 && (
                      <div className="slider-controls">
                        <button onClick={() => setSliderIdx(Math.max(0, sliderIdx - 1))} disabled={sliderIdx === 0}>‹</button>
                        <button onClick={() => setSliderIdx(Math.min(achPages - 1, sliderIdx + 1))} disabled={sliderIdx === achPages - 1}>›</button>
                      </div>
                    )}
                  </div>
                </div>

                <WarningBanner warning={getAchievementWarning(data.achievements)} />

                <div className="achievements-slider-container">
                  <div className="achievements-grid">
                    {visibleAchievements.map(ach => (
                      <div key={ach.id} className={`badge ${ach.achieved ? 'earned' : 'locked'}`}
                        title={ach.description} onClick={() => setSelectedAchievement(ach)}>
                        <div className="badge-icon">
                          {ach.iconUrl && !ach.iconUrl.includes('example.com')
                            ? <img src={ach.iconUrl} alt="" onError={e => e.target.style.display = 'none'} />
                            : null}
                          <span>{ach.iconUrl?.includes('example.com') || !ach.iconUrl ? '🏆' : ''}</span>
                        </div>
                        <div className="badge-name">{ach.title}</div>
                        <div className="badge-progress" style={{ color: ach.achieved ? 'var(--success)' : 'inherit' }}>
                          {ach.achieved ? '✓ Отримано' : `${ach.targetValue > 0 ? `${ach.currentProgress}/${ach.targetValue}` : 'В процесі'}`}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {achPages > 1 && (
                  <div className="slider-dots">
                    {Array.from({ length: achPages }).map((_, i) => (
                      <div key={i} className={`slider-dot ${i === sliderIdx ? 'active' : ''}`} onClick={() => setSliderIdx(i)} />
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Top recipes + Ingredient cloud */}
            <div className="row-food">
              <div className="card">
                <div className="card-header"><div className="card-title">🏆 Топ-5 улюблених страв</div></div>
                <div className="top-list">
                  {data.topRecipes.length > 0 ? data.topRecipes.map((recipe, idx) => (
                    <div key={recipe.recipeId} className="top-item">
                      <div className="top-item-info">
                        <div className={`top-rank rank-${idx + 1}`}>{idx + 1}</div>
                        <div style={{ flex: 1 }}>
                          <div className="top-name">{recipe.recipeName}</div>
                          <div className="top-bar-wrap">
                            <div className="top-bar-fill" style={{ width: `${data.topRecipes[0]?.count ? (recipe.count / data.topRecipes[0].count) * 100 : 0}%` }}></div>
                          </div>
                        </div>
                      </div>
                      <div className="top-count">{recipe.count} разів</div>
                    </div>
                  )) : <div className="empty-state">Ви ще не з'їли жодної страви</div>}
                </div>
              </div>

              <div className="card">
                <div className="card-header"><div className="card-title">☁️ Продукти, які їси найчастіше</div></div>
                <div className="word-cloud">
                  {sortedProducts.length > 0 ? sortedProducts.map((prod, idx) => {
                    const sizeClass = idx === 0 ? 'w-huge' : idx < 3 ? 'w-large' : idx < 7 ? 'w-medium' : 'w-small';
                    return (
                      <Link key={prod.slug} to={`/product/${prod.slug}`} className={`word ${sizeClass}`} title={`${prod.count} рецептів`}>
                        {prod.name}
                      </Link>
                    );
                  }) : <div className="empty-state">Поки немає статистики по продуктах</div>}
                </div>
                <div style={{ textAlign: 'center', fontSize: '12px', color: 'var(--text-muted)', marginTop: 'auto', paddingTop: '8px' }}>
                  На основі продуктів ваших улюблених страв
                </div>
              </div>
            </div>

            {/* Meal completion + Spending */}
            <div className="row-extra">
              <div className="card">
                <div className="card-header"><div className="card-title">🍽️ Виконання по прийомах їжі</div></div>
                <div className="completion-bar-list">
                  {mealTypes.map(meal => {
                    const result = calculateCompletion(meal.type);
                    if (result === null) return null; // skip if no slots at all
                    const { percent } = result;
                    return (
                      <div key={meal.type} className="comp-item">
                        <div className="comp-top">
                          <div className="comp-label">{meal.label}</div>
                          <div className="comp-val" style={{ color: percent > 80 ? 'var(--success)' : percent > 50 ? 'var(--warning)' : 'var(--danger)' }}>{percent}%</div>
                        </div>
                        <div className="comp-bar-bg">
                          <div className="comp-bar" style={{ width: `${percent}%`, background: percent > 80 ? 'var(--success)' : percent > 50 ? 'var(--warning)' : 'var(--danger)' }}></div>
                        </div>
                      </div>
                    );
                  })}
                  {mealTypes.every(m => calculateCompletion(m.type) === null) && (
                    <div className="empty-state" style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-muted)', fontSize: '13px' }}>
                      💡 Поки немає даних по прийомах їжі. Дотримуйтесь плану харчування!
                    </div>
                  )}
                </div>
              </div>

              <div className="card">
                <div className="card-header"><div className="card-title">🛒 Витрати на продукти</div></div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '16px' }}>
                  <span style={{ fontSize: '30px', fontWeight: 800, color: 'var(--primary)' }}>
                    ₴ {data.orders.filter(o => {
                      const d = new Date(o.createdAt);
                      return d.getMonth() === new Date().getMonth();
                    }).reduce((acc, o) => acc + (o.totalAmount || 0), 0)}
                  </span>
                  <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>цього місяця</span>
                </div>
                <div className="budget-list">
                  {data.orders.slice(0, 4).map(order => (
                    <div key={order.id} className="budget-row">
                      <div className="budget-icon">{order.status === 'DELIVERED' ? '✅' : '📦'}</div>
                      <div className="budget-info">
                        <div className="budget-name">Замовлення #{order.id}</div>
                        <div className="budget-date">{new Date(order.createdAt).toLocaleDateString('uk-UA')} · {order.status}</div>
                      </div>
                      <div className="budget-amount">₴ {order.totalAmount}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

          </div>
        </div>
      </main>

      {/* Weight modal */}
      {isWeightModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsWeightModalOpen(false)}>
          <div className="custom-modal" onClick={e => e.stopPropagation()}>
            <h3>⚖️ Записати вагу</h3>
            <p className="modal-desc">Введіть вагу та оберіть дату.</p>
            <div className="form-group">
              <label>Дата зважування</label>
              <input type="text" className="modal-input" placeholder="ДД.ММ.РРРР" value={weightDate} onChange={e => handleMaskedDateInput(e, setWeightDate)} />
            </div>
            <div className="form-group">
              <label>Вага (кг)</label>
              <input type="number" className="modal-input" placeholder="Наприклад: 75.5" value={weightInput} onChange={e => setWeightInput(e.target.value)} autoFocus />
            </div>
            <div className="form-group">
              <label>Нотатка (необов'язково)</label>
              <textarea className="modal-input" rows="2" placeholder="Як ви себе почуваєте?" value={weightNote} onChange={e => setWeightNote(e.target.value)} />
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setIsWeightModalOpen(false)}>Скасувати</button>
              <button className="btn-submit" onClick={handleSaveWeight} disabled={!weightInput}>Зберегти</button>
            </div>
          </div>
        </div>
      )}

      {/* Achievement modal */}
      {selectedAchievement && (
        <AchievementModal achievement={selectedAchievement} onClose={() => setSelectedAchievement(null)} />
      )}
    </div>
  );
};

export default StatisticsPage;