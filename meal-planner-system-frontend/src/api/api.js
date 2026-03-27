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
  if (token) return {};                       // auth user — header not needed
  return { 'X-Cart-Session': getCartSessionId() };
}

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
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
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);

        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/auth';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// ─── Auth ───────────────────────────────────────

export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  logout: () => {
    const token = localStorage.getItem('accessToken');
    return api.post('/auth/logout', null, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  me: () => api.get('/user/me'),
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
};

// ─── Dietary ───────────────────────────────────

export const dietaryAPI = {
  getConditions: () => api.get('/dietary/conditions'),
  getByType: (type) => api.get(`/dietary/conditions/type/${type}`),
};

// ─── Recipes ───────────────────────────────────

export const recipesAPI = {
  getByIngredient: (ingredientId, params) => api.get(`/recipes/by-ingredient/${ingredientId}`, { params }),
};

// ─── Cart ──────────────────────────────────────

export const cartAPI = {
  getCart: () =>
    api.get('/cart', { headers: cartHeaders() }),
  addItem: (ingredientId, quantity) =>
    api.post('/cart/items', { ingredientId, quantity }, { headers: cartHeaders() }),
  updateItem: (ingredientId, quantity) =>
    api.put(`/cart/items/${ingredientId}`, { quantity }, { headers: cartHeaders() }),
  removeItem: (ingredientId) =>
    api.delete(`/cart/items/${ingredientId}`, { headers: cartHeaders() }),
  clearCart: () =>
    api.delete('/cart', { headers: cartHeaders() }),
};

export default api;
