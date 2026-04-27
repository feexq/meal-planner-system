import axios from 'axios';

// ─── Cart Session (guest UUID) ─────────────────

function getCartSessionId() {
  let id = localStorage.getItem('cartSessionId');
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem('cartSessionId', id);
  }
  return id;
}

function cartHeaders() {
  const token = localStorage.getItem('accessToken');
  if (token) return {};
  return { 'X-Cart-Session': getCartSessionId() };
}

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — auto-refresh on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) throw new Error('No refresh token');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = `/auth?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

// ─── Auth ──────────────────────────────────────

export const authAPI = {
  login: (credentials) => {
    const cartSessionId = localStorage.getItem('cartSessionId');
    return api.post('/auth/login', credentials, {
      headers: cartSessionId ? { 'X-Cart-Session': cartSessionId } : {}
    });
  },
  register: (data) => {
    const cartSessionId = localStorage.getItem('cartSessionId');
    return api.post('/auth/register', data, {
      headers: cartSessionId ? { 'X-Cart-Session': cartSessionId } : {}
    });
  },
  logout: () => {
    const token = localStorage.getItem('accessToken');
    return api.post('/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } });
  },
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  // me: () => api.get('/user/me'),
};

// ─── Profile ───────────────────────────────────

export const profileAPI = {
  getProfile: () => api.get('/profile/me'),
  updateProfile: (data) => api.put('/profile/me', data),
  getStreak: () => api.get('/profile/streak'),
  changeStreakType: (streakType) => api.put('/profile/streak/type', { streakType }),
  getWeightHistory: (params) => api.get('/profile/weight/history', { params }),
  logWeight: (data) => api.post('/profile/weight', data),
  logWeightForDate: (date, data) => api.post(`/profile/weight/${date}`, data),
  deleteWeight: (date) => api.delete(`/profile/weight/${date}`),
  uploadAvatar: (formData) => api.post('/profile/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteAvatar: () => api.delete('/profile/avatar'),
  getTopRecipes: (limit = 5) => api.get('/profile/statistics/top-recipes', { params: { limit } }),
  getNutritionHeatmap: (from, to) => api.get('/profile/statistics/nutrition-heatmap', { params: { from, to } }),
  getAchievements: () => api.get('/profile/achievements'),
};

// ─── User Preferences ──────────────────────────

export const preferencesAPI = {
  // GET existing preferences (to pre-fill survey)
  get: () => api.get('/user/preference'),
  // POST/PUT to save preferences
  save: (data) => api.post('/user/preference', data),
  update: (data) => api.put('/user/preference', data),
  exists: () => api.get('/user/preference/exists'),
};

// ─── Meal Plan ─────────────────────────────────

export const mealPlanAPI = {
  generateFinal: (preferencesData) => api.post('/meal-plan/generate/final', preferencesData),
  getStatus: () => api.get('/meal-plan/status'),
  activatePlan: (planId) => api.post('/meal-plan/status', { planId, action: 'ACTIVATE' }),
  markEaten: (slotId) => api.post(`/meal-plan/mark-eaten/${slotId}`),
  swapSlot: (slotId) => api.post(`/meal-plan/swap-slot/${slotId}`),
  logFood: (text) => api.post('/meal-plan/log-food', { text }),
};

// ─── Categories ────────────────────────────────

export const categoriesAPI = {
  getRoots: () => api.get('/categories'),
  getAll: () => api.get('/categories/all'),
  getById: (id) => api.get(`/categories/${id}`),
  getBySlug: (slug) => api.get(`/categories/slug/${slug}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  remove: (id) => api.delete(`/categories/${id}`),
};

// ─── Ingredients ───────────────────────────────

export const ingredientsAPI = {
  getAll: (params) => api.get('/ingredients', { params }),
  getById: (id) => api.get(`/ingredients/${id}`),
  getBySlug: (slug) => api.get(`/ingredients/slug/${slug}`),
  create: (data) => api.post('/ingredients', data),
  update: (id, data) => api.put(`/ingredients/${id}`, data),
  remove: (id) => api.delete(`/ingredients/${id}`),
  getDietaryTags: (id) => api.get(`/ingredients/${id}/dietary-tags`),
  updateDietaryTags: (id, tags) => api.put(`/ingredients/${id}/dietary-tags`, { tags }),
  getTags: (id) => api.get(`/ingredients/${id}/tags`),
};

// ─── Dietary ───────────────────────────────────

export const dietaryAPI = {
  getConditions: () => api.get('/dietary/conditions'),
  getByType: (type) => api.get(`/dietary/conditions/type/${type}`),
};

// ─── Products ───────────────────────────────────
export const productsAPI = {
  findAllByIngredients: (ids) =>
    api.get(`/products/by-ingredients?${ids.map(id => `ingredientIds=${id}`).join('&')}`)
      .then(res => res.data),
  getAll: (params) => api.get('/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  getBySlug: (slug) => api.get(`/products/slug/${slug}`),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  remove: (id) => api.delete(`/products/${id}`),
};

// ─── Recipes ───────────────────────────────────

export const recipesAPI = {
  getAll: (params) => api.get('/recipes', { params }),
  getAllWithFilters: (params) => api.get('/recipes/filters', { params }),
  getById: (id) => api.get(`/recipes/${id}`),
  getBySlug: (slug) => api.get(`/recipes/slug/${slug}`),
  getByIngredient: (ingredientId, params) => api.get(`/recipes/by-ingredient/${ingredientId}`, { params }),
  searchByIngredients: (ingredientIds) =>
    api.get('/recipes/search/by-ingredients', {
      params: { ingredientIds },
      paramsSerializer: { indexes: null },
    }),
};

// ─── Recipe Tags ───────────────────────────────

export const recipeTagsAPI = {
  getAll: () => api.get('/tags-recipes'),
  getById: (id) => api.get(`/tags-recipes/${id}`),
  create: (data) => api.post('/tags-recipes', data),
  update: (id, data) => api.put(`/tags-recipes/${id}`, data),
};

export const cartAPI = {
  getCart: () => api.get('/cart', { headers: cartHeaders() }),
  addItem: async (ingredientId, quantity) => {
    const res = await api.post('/cart/items', { ingredientId, quantity }, { headers: cartHeaders() });
    window.dispatchEvent(new Event('cartUpdated'));
    return res;
  },
  addRecipeIngredients: async (recipeId) => {
    const res = await api.post(`/cart/add-recipe/${recipeId}`, {}, { headers: cartHeaders() });
    window.dispatchEvent(new Event('cartUpdated'));
    return res;
  },
  updateItem: async (ingredientId, quantity) => {
    const res = await api.put(`/cart/items/${ingredientId}`, { quantity }, { headers: cartHeaders() });
    window.dispatchEvent(new Event('cartUpdated'));
    return res;
  },
  removeItem: async (ingredientId) => {
    const res = await api.delete(`/cart/items/${ingredientId}`, { headers: cartHeaders() });
    window.dispatchEvent(new Event('cartUpdated'));
    return res;
  },
  clearCart: async () => {
    const res = await api.delete('/cart', { headers: cartHeaders() });
    window.dispatchEvent(new Event('cartUpdated'));
    return res;
  },
  mergeCart: () => {
    const cartSessionId = localStorage.getItem('cartSessionId');
    if (!cartSessionId) return Promise.resolve();
    return api.post('/cart/merge', null, {
      headers: { 'X-Cart-Session': cartSessionId }
    });
  },
};

// ─── Delivery ──────────────────────────────────

export const deliveryAPI = {
  searchCities: (name) => api.get('/delivery/cities', { params: { name } }),
  searchWarehouses: (cityRef, search) =>
    api.get('/delivery/warehouses', { params: { cityRef, search } }),
};

// ─── Orders ────────────────────────────────────

export const ordersAPI = {
  checkout: (npCityRef, npWarehouseRef) =>
    api.post('/orders/checkout', { npCityRef, npWarehouseRef }),
  checkoutIntent: (npCityRef, npWarehouseRef) =>
    api.post('/orders/checkout-intent', { npCityRef, npWarehouseRef }),
  getOrders: () => api.get('/orders'),
  getOrderById: (id) => api.get(`/orders/${id}`),
};

export default api;