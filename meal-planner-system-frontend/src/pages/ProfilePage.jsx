import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ProfileSidebar from '../components/ProfileSidebar';
import { profileAPI, preferencesAPI, ordersAPI } from '../api/api';
import './ProfilePage.css';

const TIMEZONES = [
  'Europe/Kyiv',
  'Europe/Warsaw',
  'Europe/London',
  'Europe/Berlin',
  'America/New_York',
  'UTC'
];


const formatToDDMMYYYY = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const yyyy = d.getFullYear();
  return `${dd}.${mm}.${yyyy}`;
};

const parseFromDDMMYYYY = (str) => {
  if (!str || str.length !== 10) return null;
  const [dd, mm, yyyy] = str.split('.');
  return `${yyyy}-${mm}-${dd}`;
};

const handleMaskedDate = (e, setter) => {
  let val = e.target.value.replace(/\D/g, '');
  if (val.length > 8) val = val.substring(0, 8);

  let formatted = val;
  if (val.length >= 5) {
    formatted = `${val.substring(0, 2)}.${val.substring(2, 4)}.${val.substring(4)}`;
  } else if (val.length >= 3) {
    formatted = `${val.substring(0, 2)}.${val.substring(2)}`;
  }
  setter(formatted);
};

const ProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [hasPreferences, setHasPreferences] = useState(true);
  const [lastOrder, setLastOrder] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [loading, setLoading] = useState(true);
  const [weightHistory, setWeightHistory] = useState([]);


  const [isWeightModalOpen, setIsWeightModalOpen] = useState(false);
  const [weightInput, setWeightInput] = useState('');
  const [weightNote, setWeightNote] = useState('');
  const [weightDate, setWeightDate] = useState(() => formatToDDMMYYYY(new Date()));

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editForm, setEditForm] = useState({ bio: '', timezone: '', targetWeightKg: '', dateOfBirth: '' });

  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreview, setAvatarPreview] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [profileRes, prefExistsRes, ordersRes, weightHistoryRes] = await Promise.all([
        profileAPI.getProfile(),
        preferencesAPI.exists(),
        ordersAPI.getOrders(),
        profileAPI.getWeightHistory({ limit: 10 })
      ]);

      setProfile(profileRes.data);
      setWeightHistory(weightHistoryRes.data || []);
      setHasPreferences(prefExistsRes.data);

      if (ordersRes.data && ordersRes.data.length > 0) {
        const sortedOrders = [...ordersRes.data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        setLastOrder(sortedOrders[0]);
      }

      const todayDate = new Date();
      const to = todayDate.toISOString().split('T')[0];
      const startOfYear = new Date(todayDate.getFullYear(), 0, 1);
      const from = new Date(startOfYear.getTime() - (startOfYear.getTimezoneOffset() * 60000)).toISOString().split('T')[0];

      const heatmapRes = await profileAPI.getNutritionHeatmap(from, to);
      setHeatmap(heatmapRes.data || []);

    } catch (error) {
      console.error('Error fetching profile data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveWeight = async () => {
    if (weightDate.length !== 10) {
      alert('Будь ласка, введіть повну дату у форматі ДД.ММ.РРРР');
      return;
    }

    if (weightInput && !isNaN(parseFloat(weightInput))) {
      try {
        const payload = { weightKg: parseFloat(weightInput) };
        if (weightNote.trim()) {
          payload.note = weightNote.trim();
        }

        const apiDate = parseFromDDMMYYYY(weightDate);
        const todayApi = new Date().toISOString().split('T')[0];

        if (apiDate && apiDate !== todayApi) {
          await profileAPI.logWeightForDate(apiDate, payload);
        } else {
          await profileAPI.logWeight(payload);
        }

        setIsWeightModalOpen(false);
        setWeightInput('');
        setWeightNote('');
        setWeightDate(formatToDDMMYYYY(new Date()));
        fetchData();
      } catch (err) {
        alert('Помилка при збереженні ваги. Перевірте з\'єднання.');
      }
    }
  };

  const openEditModal = () => {
    setEditForm({
      bio: profile?.bio || '',
      timezone: profile?.timezone || 'Europe/Kyiv',
      targetWeightKg: profile?.targetWeightKg || '',
      dateOfBirth: profile?.dateOfBirth ? formatToDDMMYYYY(profile.dateOfBirth) : ''
    });
    setIsEditModalOpen(true);
  };

  const handleSaveProfile = async () => {
    if (editForm.dateOfBirth && editForm.dateOfBirth.length !== 10) {
      alert('Будь ласка, введіть повну дату народження у форматі ДД.ММ.РРРР');
      return;
    }

    try {
      await profileAPI.updateProfile({
        bio: editForm.bio,
        timezone: editForm.timezone,
        targetWeightKg: parseFloat(editForm.targetWeightKg) || null,
        dateOfBirth: editForm.dateOfBirth ? parseFromDDMMYYYY(editForm.dateOfBirth) : null
      });
      setIsEditModalOpen(false);
      fetchData();
    } catch (err) {
      alert('Помилка при оновленні профілю');
    }
  };

  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setAvatarFile(file);
      setAvatarPreview(URL.createObjectURL(file));
    }
  };

  const handleSaveAvatar = async () => {
    if (!avatarFile) return;
    try {
      const formData = new FormData();
      formData.append('file', avatarFile);
      await profileAPI.uploadAvatar(formData);
      setIsAvatarModalOpen(false);
      setAvatarFile(null);
      fetchData();
    } catch (err) {
      alert('Помилка при завантаженні фото');
    }
  };

  const currentYearGrid = useMemo(() => {
    const today = new Date();
    const startOfYear = new Date(today.getFullYear(), 0, 1);
    const grid = [];

    const diffTime = Math.abs(today - startOfYear);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    for (let i = 0; i <= diffDays; i++) {
      const d = new Date(startOfYear);
      d.setDate(startOfYear.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];

      const dayData = heatmap.find(h => h.date === dateStr);
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
  }, [heatmap]);

  const completedDays = heatmap.filter(d => d.completionRate > 0).length;

  const getMotivation = (days) => {
    if (days === 0) return "Почніть свою трансформацію вже сьогодні! Заповніть свій перший день. 💪";
    if (days < 10) return "Гарний старт! Ви на правильному шляху, так тримати! 🌱";
    if (days < 30) return "Чудова робота! Звичка формується, ви молодець! 🔥";
    return "Ви просто машина! Ваша дисципліна вражає! 🌋";
  };

  const getWeightMotivation = (percent) => {
    if (!percent || percent <= 0) return "Кожна велика подорож починається з першого кроку. Додайте ціль! 🎯";
    if (percent < 50) return "Початок покладено! Кожен грам робить вас ближче до мети. 🏃‍♂️";
    if (percent < 90) return "Більша частина шляху позаду! Не зупиняйтесь! 🚀";
    return "Ви досягли своєї мети або зовсім поруч! Час ставити нові рекорди! 🏆";
  };

  if (loading) {
    return <div className="page-loader"><div className="spinner"></div></div>;
  }

  const getUnifiedWeightMotivation = () => {
    if (!profile || (!profile.currentWeightKg && !profile.targetWeightKg)) return null;

    const last = weightHistory && weightHistory.length > 0 ? weightHistory[0] : null;
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);

    let lastEntryText = "";
    if (last) {
      const lastDate = new Date(last.recordedDate);
      let datePrefix = lastDate.toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' });
      if (lastDate.toDateString() === today.toDateString()) datePrefix = 'сьогодні';
      else if (lastDate.toDateString() === yesterday.toDateString()) datePrefix = 'вчора';
      lastEntryText = `Запис за ${datePrefix}. `;
    }

    let progressText = "";
    let isLosing = false;
    if (weightHistory && weightHistory.length > 1) {
      const weekAgoDate = new Date();
      weekAgoDate.setDate(today.getDate() - 7);
      const oldEntry = weightHistory.find(e => new Date(e.recordedDate) <= weekAgoDate) || weightHistory[weightHistory.length - 1];
      const diff = last.weightKg - oldEntry.weightKg;

      if (diff < 0) {
        progressText = `-${Math.abs(diff).toFixed(1)} кг за тиждень. `;
        isLosing = true;
      } else if (diff > 0) {
        progressText = `+${diff.toFixed(1)} кг за тиждень. `;
      }
    }

    let encouragement = isLosing ? "Так тримати! 🚀" : "Вперед до мети! 🎯";

    return (
      <div className="motivation-box" style={{
        marginTop: '8px',
        padding: '8px 12px',
        borderLeftWidth: '3px',
        borderLeftColor: isLosing ? '#10B981' : 'var(--primary)',
        background: isLosing ? 'linear-gradient(135deg, #ECFDF5 0%, #FFFFFF 100%)' : 'var(--neutral)',
        display: 'flex',
        alignItems: 'center',
        gap: '10px'
      }}>
        <span style={{ fontSize: '16px', lineHeight: 1 }}>{isLosing ? '📉' : '💡'}</span>
        <div style={{ fontSize: '11.5px', lineHeight: '1.3', color: 'var(--text-dark)', fontWeight: '500' }}>
          <span style={{ color: 'var(--text-muted)' }}>{lastEntryText}</span>
          <span style={{ fontWeight: '700' }}>{progressText}</span>
          <span style={{ fontStyle: 'italic', opacity: 0.8 }}>{encouragement}</span>
        </div>
      </div>
    );
  };

  return (
    <div className="profile-page">
      <Navbar />
      <main className="container">

        {!hasPreferences && (
          <div className="survey-banner">
            <div className="banner-text">
              <h3>⚠️ Ви ще не налаштували свій раціон</h3>
              <p>Пройдіть коротке опитування, щоб ШІ-асистент зміг підібрати ідеальне меню.</p>
            </div>
            <Link to="/survey" className="btn-banner">Пройти опитування</Link>
          </div>
        )}

        <div className="profile-layout">
          <ProfileSidebar />

          <div className="content-area">
            {profile && (
              <>
                <div className="profile-card">
                  <button className="btn-edit" onClick={openEditModal}>✏️ Редагувати</button>

                  <div className="avatar-large" onClick={() => setIsAvatarModalOpen(true)} style={{ cursor: 'pointer' }}>
                    {profile.avatarUrl ? (
                      <img src={profile.avatarUrl} alt="Avatar" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                    ) : (
                      '👨‍💻'
                    )}
                    <div className="avatar-edit" title="Змінити аватар">📷</div>
                  </div>

                  <div className="user-details">
                    <h2>{profile.firstName || 'Користувач'} {profile.lastName || ''}</h2>
                    <div className="user-meta">
                      <span>✉️ {profile.email}</span>
                      {profile.timezone && <span>🌍 {profile.timezone}</span>}
                      {profile.age && <span>🎂 {profile.age} років</span>}
                    </div>
                    <div className="user-bio">{profile.bio || 'Розкажіть трохи про себе...'}</div>
                  </div>
                </div>

                <div className="widgets-grid">
                  <div className="streak-card">
                    <div className="bg-fire">🔥</div>
                    <div className="streak-header">
                      <div className="streak-title">🔥 Активність</div>
                      <div className="streak-type">{profile.streakType === 'STRICT' ? 'Суворий режим' : 'Гнучкий режим'}</div>
                    </div>

                    <div className="streak-main">
                      <div className="streak-days">{profile.currentStreak ?? 0}</div>
                      <div className="streak-lbl">днів поспіль!</div>
                    </div>

                    {}
                    <div className="streak-footer" style={{ gap: '12px', paddingTop: '4px', marginBottom: '12px', display: 'flex' }}>
                      <div title="Всього активних днів" style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        background: '#FFFDF5',
                        border: '1px solid #FDE68A',
                        padding: '4px 12px',
                        borderRadius: '8px'
                      }}>
                        <span style={{ fontSize: '13px', fontWeight: '800', color: '#92400E' }}>{profile.totalActiveDays ?? 0}</span>
                        <span style={{ fontSize: '10px', fontWeight: '700', color: '#B45309', textTransform: 'uppercase' }}>днів всього</span>
                      </div>
                      <div title="Доступні заморозки" style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        background: '#FFFDF5',
                        border: '1px solid #FDE68A',
                        padding: '4px 12px',
                        borderRadius: '8px'
                      }}>
                        <span style={{ fontSize: '13px', fontWeight: '800', color: '#92400E' }}>❄️ {profile.freezesAvailable ?? 0}</span>
                        <span style={{ fontSize: '10px', fontWeight: '700', color: '#B45309', textTransform: 'uppercase' }}>заморозки</span>
                      </div>
                    </div>

                    <div className="streak-tiers" style={{ borderTop: '1px dashed rgba(217, 119, 6, 0.2)', paddingTop: '12px' }}>
                      <div className={`tier ${(profile.currentStreak ?? 0) >= 1 ? 'completed' : ''}`}>
                        <span className="t-icon">🌱</span><span className="t-days">1 дн</span>
                      </div>
                      <div className={`tier-line ${(profile.currentStreak ?? 0) >= 5 ? 'completed' : ''}`}></div>
                      <div className={`tier ${(profile.currentStreak ?? 0) >= 5 ? 'completed' : ''}`}>
                        <span className="t-icon">🔥</span><span className="t-days">5 дн</span>
                      </div>
                      <div className={`tier-line ${(profile.currentStreak ?? 0) >= 10 ? 'completed' : ''}`}></div>
                      <div className={`tier ${(profile.currentStreak ?? 0) >= 10 ? 'completed' : ''}`}>
                        <span className="t-icon">☄️</span><span className="t-days">10 дн</span>
                      </div>
                      <div className={`tier-line ${(profile.currentStreak ?? 0) >= 30 ? 'completed' : ''}`}></div>
                      <div className={`tier ${(profile.currentStreak ?? 0) >= 30 ? 'completed' : ''}`}>
                        <span className="t-icon">🌋</span><span className="t-days">30 дн</span>
                      </div>
                    </div>
                  </div>

                  <div className="weight-card">
                    <div className="w-header">
                      <div className="w-title">⚖️ Відстеження ваги</div>
                      <button className="w-btn" onClick={() => setIsWeightModalOpen(true)}>+ Записати</button>
                    </div>

                    {!profile.currentWeightKg && !profile.targetWeightKg ? (
                      <div className="empty-state">
                        <div className="empty-icon">⚖️</div>
                        <p>Ви ще не додали свою вагу.</p>
                        <button className="btn-primary-small" onClick={() => setIsWeightModalOpen(true)}>Додати зараз</button>
                      </div>
                    ) : (
                      <>
                        <div className="w-vals">
                          <div className="w-current">{profile.currentWeightKg || '--'} <span>кг зараз</span></div>
                          <div className="w-target">Мета: <span>{profile.targetWeightKg || '--'} кг</span></div>
                        </div>
                        <div className="progress-bar-wrap">
                          <div className="progress-bar" style={{ width: `${Math.min(100, profile.weightProgressPercent || 0)}%` }}></div>
                        </div>
                        <div className="w-percent">{Math.round(profile.weightProgressPercent || 0)}% пройдено</div>

                        {getUnifiedWeightMotivation()}
                      </>
                    )}
                  </div>
                </div>

                <div className="heatmap-card">
                  <div className="hm-header">
                    <div>
                      <div className="hm-title">🗓️ Історія харчування поточного року</div>
                      <div className="motivation-box mt-1 highlight-green">
                        <span className="mb-icon">🌿</span>
                        <span className="mb-text">{getMotivation(completedDays)}</span>
                      </div>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)', textAlign: 'right' }}>
                      <span style={{ color: 'var(--primary)', fontWeight: 'bold' }}>{completedDays}</span> днів дотримання
                    </div>
                  </div>

                  <div className="hm-scroll-container">
                    <div className="hm-grid" id="heatmapGrid">
                      {currentYearGrid.map((day, idx) => (
                        <div
                          key={idx}
                          className="hm-cell"
                          data-level={day.level}
                          title={`${day.date}: ${day.data ? `${day.data.completionRate}% (${Math.round(day.data.totalCalories)} ккал)` : 'Немає даних'}`}
                        ></div>
                      ))}
                    </div>
                  </div>

                  <div className="hm-footer">
                    <div>Відстежено: {currentYearGrid.length} днів у {new Date().getFullYear()} році</div>
                    <div className="hm-legend">
                      Менше
                      <div className="hm-legend-cells">
                        <div className="hm-cell" data-level="0"></div>
                        <div className="hm-cell" data-level="1"></div>
                        <div className="hm-cell" data-level="2"></div>
                        <div className="hm-cell" data-level="3"></div>
                        <div className="hm-cell" data-level="4"></div>
                      </div>
                      Більше
                    </div>
                  </div>
                </div>

                <div className="bottom-widgets">
                  <div className="stats-card">
                    <div className="st-header">
                      <div className="st-title-wrap"><span>📊</span> Тижнева статистика</div>
                      <Link to="/profile/statistics" className="st-link">Детальніше →</Link>
                    </div>

                    <div className="st-overview">
                      <div className="st-main-item">
                        <div className="st-main-val">{Math.round(profile.weeklyAverages?.avgCalories || 0)}</div>
                        <div className="st-main-lbl">Середні Ккал</div>
                      </div>
                      <div className="st-main-item">
                        <div className="st-main-val" style={{ color: 'var(--success)' }}>
                          {Math.round(profile.weeklyAverages?.avgCompletionRate || 0)}%
                        </div>
                        <div className="st-main-lbl">Виконання</div>
                      </div>
                      <div className="st-main-item">
                        <div className="st-main-val" style={{ color: 'var(--accent)' }}>
                          {profile.weeklyAverages?.cleanDays || 0}
                        </div>
                        <div className="st-main-lbl">Чистих днів</div>
                      </div>
                    </div>

                    <div className="st-subtitle">Середні показники макросів</div>

                    <div className="st-macros">
                      <div className="macro-col">
                        <div className="m-top"><span style={{ color: 'var(--text-muted)' }}>Білки</span> <span>{Math.round(profile.weeklyAverages?.avgProteinG || 0)} г</span></div>
                        <div className="m-bar-bg">
                          <div className="m-bar" style={{ width: `${Math.min(100, (profile.weeklyAverages?.avgProteinG || 0) / 1.5)}%`, background: '#3B82F6' }}></div>
                        </div>
                      </div>
                      <div className="macro-col">
                        <div className="m-top"><span style={{ color: 'var(--text-muted)' }}>Жири</span> <span>{Math.round(profile.weeklyAverages?.avgFatG || 0)} г</span></div>
                        <div className="m-bar-bg">
                          <div className="m-bar" style={{ width: `${Math.min(100, (profile.weeklyAverages?.avgFatG || 0) / 0.8)}%`, background: '#F59E0B' }}></div>
                        </div>
                      </div>
                      <div className="macro-col">
                        <div className="m-top"><span style={{ color: 'var(--text-muted)' }}>Вуглеводи</span> <span>{Math.round(profile.weeklyAverages?.avgCarbsG || 0)} г</span></div>
                        <div className="m-bar-bg">
                          <div className="m-bar" style={{ width: `${Math.min(100, (profile.weeklyAverages?.avgCarbsG || 0) / 2.5)}%`, background: '#10B981' }}></div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="order-card">
                    <div>
                      <div className="ord-header">
                        <div className="ord-title">🛒 Доставка</div>
                        <Link to="/profile/orders" className="ord-link">Всі →</Link>
                      </div>
                      {lastOrder ? (
                        <>
                          <div className="ord-status" style={{
                            background: lastOrder.status === 'FAILED' ? '#FEE2E2' : '#E8F5E9',
                            color: lastOrder.status === 'FAILED' ? '#991B1B' : '#047857',
                            border: `1px solid ${lastOrder.status === 'FAILED' ? '#FECACA' : '#A7F3D0'}`
                          }}>
                            {lastOrder.status === 'IN_TRANSIT' ? 'В дорозі' :
                              lastOrder.status === 'DELIVERED' ? 'Доставлено' :
                                lastOrder.status === 'PAID' ? 'Оплачено' :
                                  lastOrder.status === 'COURIER_ASSIGNED' ? 'Кур\'єр призначений' :
                                    lastOrder.status === 'FAILED' ? 'Помилка' : 'Обробляється'}
                          </div>
                          <div className="ord-details">Замовлення #{lastOrder.id}</div>
                          <div className="ord-date">
                            {new Date(lastOrder.createdAt).toLocaleDateString('uk-UA', { day: 'numeric', month: 'long' })}
                          </div>
                        </>
                      ) : (
                        <div className="ord-empty">Немає активних доставок</div>
                      )}
                    </div>
                    <div className="ord-box">📦</div>
                  </div>

                </div>
              </>
            )}
          </div>
        </div>
      </main>

      {}
      {isWeightModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsWeightModalOpen(false)}>
          <div className="custom-modal" onClick={e => e.stopPropagation()}>
            <h3>⚖️ Записати вагу</h3>
            <p className="modal-desc">Введіть вагу та оберіть дату.</p>

            {}
            <div className="form-group">
              <label>Дата зважування</label>
              <input
                type="text"
                className="modal-input"
                placeholder="ДД.ММ.РРРР"
                value={weightDate}
                onChange={(e) => handleMaskedDate(e, setWeightDate)}
              />
            </div>

            <div className="form-group">
              <label>Вага (кг)</label>
              <input
                type="number"
                className="modal-input"
                placeholder="Наприклад: 75.5"
                value={weightInput}
                onChange={(e) => setWeightInput(e.target.value)}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label>Нотатка (необов'язково)</label>
              <textarea
                className="modal-input"
                rows="2"
                placeholder="Як ви себе почуваєте?"
                value={weightNote}
                onChange={(e) => setWeightNote(e.target.value)}
              />
            </div>

            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setIsWeightModalOpen(false)}>Скасувати</button>
              <button className="btn-submit" onClick={handleSaveWeight} disabled={!weightInput}>Зберегти</button>
            </div>
          </div>
        </div>
      )}

      {isEditModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsEditModalOpen(false)}>
          <div className="custom-modal" onClick={e => e.stopPropagation()}>
            <h3>✏️ Редагувати профіль</h3>

            <div className="form-group">
              <label>Про себе (Біографія)</label>
              <textarea
                className="modal-input"
                rows="2"
                value={editForm.bio}
                onChange={(e) => setEditForm({ ...editForm, bio: e.target.value })}
                placeholder="Студент, люблю кодити..."
              />
            </div>

            <div className="form-group">
              <label>Цільова вага (кг)</label>
              <input
                type="number"
                className="modal-input"
                value={editForm.targetWeightKg}
                onChange={(e) => setEditForm({ ...editForm, targetWeightKg: e.target.value })}
              />
            </div>

            {}
            <div className="form-group">
              <label>Дата народження</label>
              <input
                type="text"
                className="modal-input"
                placeholder="ДД.ММ.РРРР"
                value={editForm.dateOfBirth}
                onChange={(e) => handleMaskedDate(e, (val) => setEditForm({ ...editForm, dateOfBirth: val }))}
              />
            </div>

            <div className="form-group">
              <label>Часовий пояс</label>
              <select
                className="modal-input"
                value={editForm.timezone}
                onChange={(e) => setEditForm({ ...editForm, timezone: e.target.value })}
              >
                {TIMEZONES.map(tz => (
                  <option key={tz} value={tz}>{tz}</option>
                ))}
              </select>
            </div>

            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setIsEditModalOpen(false)}>Скасувати</button>
              <button className="btn-submit" onClick={handleSaveProfile}>Зберегти зміни</button>
            </div>
          </div>
        </div>
      )}

      {isAvatarModalOpen && (
        <div className="modal-backdrop" onClick={() => { setIsAvatarModalOpen(false); setAvatarPreview(null); setAvatarFile(null); }}>
          <div className="custom-modal center-content" onClick={e => e.stopPropagation()}>
            <h3>📷 Змінити фото</h3>

            <div className="avatar-preview-wrap">
              {avatarPreview ? (
                <img src={avatarPreview} alt="Preview" className="avatar-preview-img" />
              ) : (
                <div className="avatar-placeholder">{(profile?.firstName?.[0] || 'U').toUpperCase()}</div>
              )}
            </div>

            <label className="btn-upload-label">
              Обрати файл
              <input type="file" accept="image/*" style={{ display: 'none' }} onChange={handleAvatarChange} />
            </label>

            <div className="modal-actions" style={{ width: '100%', marginTop: '20px' }}>
              <button className="btn-cancel" onClick={() => { setIsAvatarModalOpen(false); setAvatarPreview(null); setAvatarFile(null); }}>Скасувати</button>
              <button className="btn-submit" onClick={handleSaveAvatar} disabled={!avatarFile}>Завантажити</button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default ProfilePage;