import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { cartAPI, deliveryAPI, ordersAPI, recipesAPI } from '../api/api';
import { useAuth } from '../context/AuthContext';
import './CartPage.css';

const MEAL_TYPE_LABELS = {
  BREAKFAST: 'Сніданок', LUNCH: 'Обід', DINNER: 'Вечеря',
  DESSERT: 'Десерт', DRINK: 'Напій', SNACK: 'Перекус',
  SAUCE_OR_CONDIMENT: 'Соус', UNCLASSIFIED: 'Інше',
};

const MEAL_TYPE_EMOJIS = {
  BREAKFAST: '🥞', LUNCH: '🥗', DINNER: '🐟',
  DESSERT: '🍰', DRINK: '🥤', SNACK: '🥑',
};

function RecommendedRecipeCard({ recipe, onAddIngredients, addingRecipeId }) {
  const [imgError, setImgError] = useState(false);
  const isAdding = addingRecipeId === recipe.recipeId;

  const missingCount = recipe.totalIngredients - recipe.matchedCount;
  const matchPct = recipe.matchPercent ? Math.round(recipe.matchPercent * 100) : 0;
  const mealKey = recipe.mealType?.toUpperCase();

  return (
    <div className="recipe-card">
      <div className="recipe-img">
        {(!recipe.imageUrl || imgError) ? (
          <div className="recipe-emoji-wrap">
            <span className="recipe-emoji">{MEAL_TYPE_EMOJIS[mealKey] || '🍽️'}</span>
            <span className="recipe-emoji-sub">{MEAL_TYPE_LABELS[mealKey] || 'Рецепт'}</span>
          </div>
        ) : (
          <img
            src={recipe.imageUrl}
            alt={recipe.recipeName}
            onError={() => setImgError(true)}
            loading="lazy"
          />
        )}
        <div className="match-badge">Збіг {matchPct}%</div>
      </div>

      <div className="recipe-info">
        <div className="recipe-title">{recipe.recipeName}</div>
        <div className="recipe-meta">
          <span>{recipe.matchedCount}/{recipe.totalIngredients} інгредієнтів</span>
        </div>
        <div className="recipe-actions">
          <Link to={`/recipe/${recipe.recipeId}`} className="btn-outline btn-view">
            Відкрити рецепт
          </Link>
          {missingCount > 0 && (
            <button
              className="btn-outline btn-add"
              onClick={() => onAddIngredients(recipe.recipeId)}
              disabled={isAdding}
            >
              {isAdding ? 'Додаємо...' : `Додати відсутнє (${missingCount})`}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export default function CartPage() {
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const { isAuthenticated } = useAuth();


  const [recommendedRecipes, setRecommendedRecipes] = useState([]);
  const [addingRecipeId, setAddingRecipeId] = useState(null);


  const [cityQuery, setCityQuery] = useState('');
  const [citySuggestions, setCitySuggestions] = useState([]);
  const [selectedCity, setSelectedCity] = useState(null);
  const [showCityDropdown, setShowCityDropdown] = useState(false);
  const [loadingCities, setLoadingCities] = useState(false);
  const cityRef = useRef(null);


  const [whQuery, setWhQuery] = useState('');
  const [whSuggestions, setWhSuggestions] = useState([]);
  const [selectedWarehouse, setSelectedWarehouse] = useState(null);
  const [showWhDropdown, setShowWhDropdown] = useState(false);
  const [loadingWarehouses, setLoadingWarehouses] = useState(false);
  const whRef = useRef(null);

  const debouncedCityQuery = useDebounce(cityQuery, 350);
  const debouncedWhQuery = useDebounce(whQuery, 350);
  const recipesScrollRef = useRef(null);


  const fetchCart = async () => {
    try {
      const { data } = await cartAPI.getCart();
      setCart(data);
      if (data && data.items && data.items.length > 0) {
        const ingredientIds = data.items.map(i => i.ingredientId);
        fetchRecommendedRecipes(ingredientIds);
      } else {
        setRecommendedRecipes([]);
      }
    } catch (err) {
      setError('Не вдалося завантажити кошик');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const scrollRecipes = (dir) => {
    if (recipesScrollRef.current) {

      const scrollAmount = recipesScrollRef.current.clientWidth;
      const amt = dir === 'left' ? -scrollAmount : scrollAmount;
      recipesScrollRef.current.scrollBy({ left: amt, behavior: 'smooth' });
    }
  };

  const fetchRecommendedRecipes = async (ingredientIds) => {
    try {
      const { data } = await recipesAPI.searchByIngredients(ingredientIds);
      setRecommendedRecipes(data || []);
    } catch (err) {
      console.error('Error fetching recommendations:', err);
    }
  };

  useEffect(() => { fetchCart(); }, []);


  useEffect(() => {
    if (!debouncedCityQuery || debouncedCityQuery.length < 2) {
      setCitySuggestions([]);
      return;
    }
    if (selectedCity && debouncedCityQuery === selectedCity.name) return;

    const search = async () => {
      setLoadingCities(true);
      try {
        const { data } = await deliveryAPI.searchCities(debouncedCityQuery);
        setCitySuggestions(data || []);
        setShowCityDropdown(true);
      } catch (err) {
        console.error('City search error:', err);
      } finally {
        setLoadingCities(false);
      }
    };
    search();
  }, [debouncedCityQuery]);


  useEffect(() => {
    if (!selectedCity) {
      setWhSuggestions([]);
      return;
    }
    if (!debouncedWhQuery || debouncedWhQuery.length < 1) {
      setWhSuggestions([]);
      return;
    }
    if (selectedWarehouse && debouncedWhQuery === selectedWarehouse.name) return;

    const search = async () => {
      setLoadingWarehouses(true);
      try {
        const { data } = await deliveryAPI.searchWarehouses(selectedCity.ref, debouncedWhQuery);
        setWhSuggestions(data || []);
        setShowWhDropdown(true);
      } catch (err) {
        console.error('Warehouse search error:', err);
      } finally {
        setLoadingWarehouses(false);
      }
    };
    search();
  }, [debouncedWhQuery, selectedCity]);


  useEffect(() => {
    const handler = (e) => {
      if (cityRef.current && !cityRef.current.contains(e.target)) setShowCityDropdown(false);
      if (whRef.current && !whRef.current.contains(e.target)) setShowWhDropdown(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSelectCity = (city) => {
    setSelectedCity(city);
    setCityQuery(city.name);
    setShowCityDropdown(false);
    setCitySuggestions([]);

    setSelectedWarehouse(null);
    setWhQuery('');
    setWhSuggestions([]);
  };

  const handleSelectWarehouse = (wh) => {
    setSelectedWarehouse(wh);
    setWhQuery(wh.name);
    setShowWhDropdown(false);
    setWhSuggestions([]);
  };

  const handleCityInputChange = (e) => {
    setCityQuery(e.target.value);
    if (selectedCity) {
      setSelectedCity(null);
      setSelectedWarehouse(null);
      setWhQuery('');
    }
  };

  const handleWhInputChange = (e) => {
    setWhQuery(e.target.value);
    if (selectedWarehouse) {
      setSelectedWarehouse(null);
    }
  };


  const handleUpdateQuantity = async (ingredientId, quantity) => {
    if (quantity < 1) return;
    setUpdatingId(ingredientId);
    try {
      await cartAPI.updateItem(ingredientId, quantity);
      await fetchCart();
    } catch (err) {
      console.error('Update quantity error:', err);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRemoveItem = async (ingredientId) => {
    setUpdatingId(ingredientId);
    try {
      await cartAPI.removeItem(ingredientId);
      await fetchCart();
    } catch (err) {
      console.error('Remove item error:', err);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleAddRecipeIngredients = async (recipeId) => {
    setAddingRecipeId(recipeId);
    try {
      await cartAPI.addRecipeIngredients(recipeId);
      await fetchCart();
    } catch (e) {
      console.error(e);
    } finally {
      setAddingRecipeId(null);
    }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Ви впевнені, що хочете очистити весь кошик?')) return;
    try {
      setLoading(true);
      await cartAPI.clearCart();
      await fetchCart();
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !cart) {
    return (
      <div className="cart-page">
        <Navbar cartCount={0} />
        <div className="cart-container" style={{ display: 'flex', justifyContent: 'center', padding: '100px 0' }}>
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <Navbar cartCount={cart?.totalItems || 0} />

      <main className="cart-container">

        <div className="page-header">
          <h1>Кошик</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>{cart?.totalItems || 0} товарів</span>
            <button className="btn-clear-all" onClick={handleClearCart}>
              Очистити все
            </button>
          </div>
        </div>

        {error ? (
          <div className="cart-empty">
            <h2>Сталася помилка</h2>
            <p>{error}</p>
            <button className="btn-checkout" style={{ width: 'auto', padding: '12px 24px' }} onClick={fetchCart}>Спробувати знову</button>
          </div>
        ) : !cart || !cart.items || cart.items.length === 0 ? (
          <div className="cart-empty">
            <h2>Ваш кошик порожній</h2>
            <p style={{ color: 'var(--text-muted)' }}>Час додати смачних інгредієнтів до замовлення!</p>
            <Link to="/" className="btn-checkout" style={{ display: 'inline-block', width: 'auto', textDecoration: 'none', padding: '12px 24px' }}>
              Перейти до магазину
            </Link>
          </div>
        ) : (
          <div className="cart-layout">

            {}
            <div className="cart-items-container">
              {cart.items.map((item) => (
                <div key={item.ingredientId} className={`cart-item ${updatingId === item.ingredientId ? 'updating' : ''}`}>
                  <Link to={`/product/${item.slug}`} className="item-img">
                    <img
                      src={item.imageUrl || '/image-placeholder.png'}
                      alt={item.normalizedName}
                      onError={(e) => { e.target.onerror = null; e.target.src = '/image-placeholder.png'; }}
                    />
                  </Link>

                  <div className="item-info">
                    <Link to={`/product/${item.slug}`} className="item-name">
                      {item.normalizedName}
                    </Link>
                    <span className="item-weight">
                      {item.unit.replace(/(\d+)([^\d]+)/, '$1 $2')}
                    </span>
                  </div>

                  <div className="quantity-selector">
                    <button
                      className="btn-qty"
                      onClick={() => handleUpdateQuantity(item.ingredientId, item.quantity - 1)}
                      disabled={item.quantity <= 1 || updatingId === item.ingredientId}
                    >-</button>
                    <input type="text" className="qty-input" value={item.quantity} readOnly />
                    <button
                      className="btn-qty"
                      onClick={() => handleUpdateQuantity(item.ingredientId, item.quantity + 1)}
                      disabled={updatingId === item.ingredientId}
                    >+</button>
                  </div>

                  <div className="item-price-block">
                    <div className="item-price">
                      {item.totalPrice != null ? `${item.totalPrice.toFixed(2)} ₴` : '—'}
                    </div>
                  </div>

                  <button
                    className="btn-remove"
                    title="Видалити"
                    onClick={() => handleRemoveItem(item.ingredientId)}
                    disabled={updatingId === item.ingredientId}
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                  </button>
                </div>
              ))}
            </div>

            {}
            <div className="right-sidebar">

              {}
              <div className="sidebar-card">
                <h2 className="sidebar-title">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="3" width="15" height="13"></rect><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon><circle cx="5.5" cy="18.5" r="2.5"></circle><circle cx="18.5" cy="18.5" r="2.5"></circle></svg>
                  Доставка
                </h2>

                {}
                <div className="form-group" ref={cityRef}>
                  <label>Місто</label>
                  <div className="autocomplete-wrapper">
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Почніть вводити назву міста..."
                      value={cityQuery}
                      onChange={handleCityInputChange}
                      onFocus={() => { if (citySuggestions.length > 0) setShowCityDropdown(true); }}
                    />
                    {loadingCities && <span className="autocomplete-spinner"></span>}
                    {selectedCity && <span className="autocomplete-check">✓</span>}
                    {showCityDropdown && citySuggestions.length > 0 && (
                      <ul className="autocomplete-dropdown">
                        {citySuggestions.map(city => (
                          <li key={city.ref} onClick={() => handleSelectCity(city)}>
                            {city.name}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>

                {}
                <div className="form-group" ref={whRef} style={{ marginBottom: 0 }}>
                  <label>Відділення Нової Пошти</label>
                  <div className="autocomplete-wrapper">
                    <input
                      type="text"
                      className="form-control"
                      placeholder={selectedCity ? 'Введіть номер або адресу відділення...' : 'Спочатку оберіть місто'}
                      value={whQuery}
                      onChange={handleWhInputChange}
                      onFocus={() => { if (whSuggestions.length > 0) setShowWhDropdown(true); }}
                      disabled={!selectedCity}
                    />
                    {loadingWarehouses && <span className="autocomplete-spinner"></span>}
                    {selectedWarehouse && <span className="autocomplete-check">✓</span>}
                    {showWhDropdown && whSuggestions.length > 0 && (
                      <ul className="autocomplete-dropdown">
                        {whSuggestions.map(wh => (
                          <li key={wh.ref} onClick={() => handleSelectWarehouse(wh)}>
                            {wh.name}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </div>

              {}
              <div className="sidebar-card">
                <h2 className="sidebar-title">Ваше замовлення</h2>

                <div className="summary-row">
                  <span>Товари ({cart.totalItems})</span>
                  <span>{cart.totalPrice ? `${cart.totalPrice.toFixed(2)} ₴` : '—'}</span>
                </div>
                <div className="summary-row">
                  <span>Вартість доставки</span>
                  <span>За тарифами перевізника</span>
                </div>

                <div className="summary-total">
                  <span>Разом</span>
                  <span>{cart.totalPrice ? `${cart.totalPrice.toFixed(2)} ₴` : '—'}</span>
                </div>
                <button
                  className="btn-checkout"
                  disabled={!selectedCity || !selectedWarehouse || checkoutLoading}
                  onClick={() => {
                    if (!isAuthenticated) {

                      sessionStorage.setItem('pendingDelivery', JSON.stringify({
                        city: selectedCity,
                        warehouse: selectedWarehouse
                      }));


                      navigate('/auth', { state: { redirect: '/checkout' } });
                      return;
                    }

                    navigate('/checkout', {
                      state: { city: selectedCity, warehouse: selectedWarehouse }
                    });
                  }}
                >
                  Оформити замовлення
                </button>
              </div>
            </div>

          </div>
        )}

        {}
        {cart?.items?.length > 0 && recommendedRecipes.length > 0 && (
          <section className="recommendations-section">

            {}
            <div className="recommendations-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '24px' }}>
              <div>
                <div className="section-title" style={{ marginBottom: '8px' }}>Що приготувати з цих продуктів?</div>
                <p className="section-subtitle" style={{ margin: 0 }}>Ми підібрали рецепти на основі продуктів, які вже є у вашому кошику.</p>
              </div>
              <div className="carousel-nav">
                <button onClick={() => scrollRecipes('left')} className="carousel-nav-btn">❮</button>
                <button onClick={() => scrollRecipes('right')} className="carousel-nav-btn">❯</button>
              </div>
            </div>

            <div className="recipes-grid" ref={recipesScrollRef}>
              {recommendedRecipes.slice(0, 10).map(recipe => (
                <RecommendedRecipeCard
                  key={recipe.recipeId}
                  recipe={recipe}
                  onAddIngredients={handleAddRecipeIngredients}
                  addingRecipeId={addingRecipeId}
                />
              ))}
            </div>
          </section>
        )}

      </main>
    </div>
  );
}
