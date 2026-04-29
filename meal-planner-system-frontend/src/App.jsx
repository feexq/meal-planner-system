import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthPage from './pages/AuthPage';
import HomePage from './pages/HomePage';
import AdminIngredientsPage from './pages/AdminIngredientsPage';
import AdminCategoriesPage from './pages/AdminCategoriesPage';
import ProductPage from './pages/ProductPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import SuccessPage from './pages/SuccessPage';
import RecipesPage from './pages/RecipesPage';
import CategoryRecipesPage from './pages/CategoryRecipesPage';
import RecipeDetailPage from './pages/RecipeDetailPage';
import ProductCatalogPage from './pages/ProductCatalogPage';
import UserSurveyPage from './pages/UserSurveyPage';
import PlanPreviewPage from './pages/PlanPreviewPage';
import TrackerPage from './pages/TrackerPage';
import ScrollToTop from './components/ScrollToTop';
import OAuth2RedirectHandler from './pages/OAuth2RedirectHandler';
import ErrorPage from './pages/ErrorPage';
import ProfilePage from './pages/ProfilePage';
import UserOrdersPage from './pages/UserOrdersPage';
import PreferencesPage from './pages/PreferencesPage';
import StatisticsPage from './pages/StatisticsPage';


function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="page-loader"><div className="spinner"></div></div>;
  if (!isAuthenticated) return <Navigate to="/auth" state={{ redirect: location.pathname + location.search }} replace />;
  return children;
}

function PublicOnlyRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="page-loader"><div className="spinner"></div></div>;
  if (isAuthenticated) {
    const redirect = location.state?.redirect || '/';
    return <Navigate to={redirect} replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <AuthProvider>
        <Routes>
          <Route path="/auth" element={<PublicOnlyRoute><AuthPage /></PublicOnlyRoute>} />
          <Route path="/" element={<HomePage />} />

          {}
          <Route path="/admin/ingredients" element={<ProtectedRoute><AdminIngredientsPage /></ProtectedRoute>} />
          <Route path="/admin/categories" element={<ProtectedRoute><AdminCategoriesPage /></ProtectedRoute>} />

          {}
          <Route path="/product/:slug" element={<ProductPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order-success" element={<SuccessPage />} />
          <Route path="/catalog" element={<ProductCatalogPage />} />
          <Route path="/catalog/category/:slug" element={<ProductCatalogPage />} />

          {}
          <Route path="/recipes" element={<RecipesPage />} />
          <Route path="/recipes/catalog" element={<CategoryRecipesPage />} />
          <Route path="/recipes/category/:slug" element={<CategoryRecipesPage />} />
          <Route path="/recipe/:slug" element={<RecipeDetailPage />} />

          {}
          <Route path="/survey" element={<ProtectedRoute><UserSurveyPage /></ProtectedRoute>} />
          <Route path="/plan-preview" element={<ProtectedRoute><PlanPreviewPage /></ProtectedRoute>} />
          <Route path="/tracker" element={<ProtectedRoute><TrackerPage /></ProtectedRoute>} />

          {}
          <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
          <Route path="/profile/orders" element={<ProtectedRoute><UserOrdersPage /></ProtectedRoute>} />
          <Route path="/profile/preferences" element={<ProtectedRoute><PreferencesPage /></ProtectedRoute>} />
          <Route path="/profile/statistics" element={<ProtectedRoute><StatisticsPage /></ProtectedRoute>} />

          <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />

          {}
          <Route path="/401" element={<ErrorPage code="401" message="В доступі відмовлено (Не авторизовано)" />} />
          <Route path="/403" element={<ErrorPage code="403" message="Заборонено (Немає права доступу)" />} />
          <Route path="*" element={<ErrorPage code="404" message="Сторінку не знайдено" />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
