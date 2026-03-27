import { useState, useEffect, useCallback } from 'react';
import { categoriesAPI, ingredientsAPI, cartAPI } from '../api/api';
import Navbar from '../components/Navbar';
import CategorySection from '../components/CategorySection';
import './HomePage.css';

export default function HomePage() {
  const [categoryData, setCategoryData] = useState([]); // { category, products }[]
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cartCount, setCartCount] = useState(0);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch root categories
      const { data: categories } = await categoriesAPI.getRoots();

      // For each category, fetch first 4 ingredients
      const results = await Promise.all(
        categories.map(async (cat) => {
          try {
            const { data } = await ingredientsAPI.getAll({
              categoryId: cat.id,
              page: 0,
              size: 4,
            });
            return { category: cat, products: data.content || [] };
          } catch {
            return { category: cat, products: [] };
          }
        })
      );

      setCategoryData(results.filter((r) => r.products.length > 0));

      // Fetch cart count
      try {
        const { data: cart } = await cartAPI.getCart();
        setCartCount(cart.totalItems || 0);
      } catch {
        // cart might not be available without auth, ignore
      }
    } catch (err) {
      setError("Не вдалось завантажити дані. Перевірте з'єднання з сервером.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCartUpdate = (cart) => {
    setCartCount(cart.totalItems || 0);
  };

  return (
    <div className="home-page">
      <Navbar cartCount={cartCount} />

      <div className="hero">
        <h1 className="hero-title">
          <span className="gradient-text">Магазин продуктів</span>
        </h1>
        <p className="hero-subtitle">
          Свіжі та якісні інгредієнти для ваших улюблених рецептів
        </p>
      </div>

      <main className="home-content">
        {loading ? (
          <div className="home-loading">
            <div className="spinner"></div>
            <span>Завантаження продуктів...</span>
          </div>
        ) : error ? (
          <div className="home-error">
            <p>{error}</p>
            <button className="home-error-btn" onClick={fetchData}>
              Спробувати знову
            </button>
          </div>
        ) : categoryData.length === 0 ? (
          <div className="home-loading">
            <span>Немає доступних категорій</span>
          </div>
        ) : (
          categoryData.map(({ category, products }) => (
            <CategorySection
              key={category.id}
              category={category}
              products={products}
              onCartUpdate={handleCartUpdate}
            />
          ))
        )}
      </main>
    </div>
  );
}
