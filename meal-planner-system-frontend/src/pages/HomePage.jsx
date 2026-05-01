import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { categoriesAPI, productsAPI, cartAPI } from '../api/api';
import Navbar from '../components/Navbar';
import CategorySection from '../components/CategorySection';
import { Link } from 'react-router-dom';
import './HomePage.css';

const getCategoryIcon = (name) => {
  const lower = name.toLowerCase();
  if (lower.includes('овоч') || lower.includes('vegetable')) return '🥬';
  if (lower.includes('фрукт') || lower.includes('fruit')) return '🍎';
  if (lower.includes('м\'яс') || lower.includes('птиц') || lower.includes('meat') || lower.includes('poultry')) return '🥩';
  if (lower.includes('молоч') || lower.includes('сир') || lower.includes('dairy') || lower.includes('cheese')) return '🥛';
  if (lower.includes('випічка') || lower.includes('хліб') || lower.includes('bakery') || lower.includes('bread')) return '🍞';
  if (lower.includes('риб') || lower.includes('море') || lower.includes('fish') || lower.includes('seafood')) return '🐟';
  if (lower.includes('бакалія') || lower.includes('grocery')) return '🌾';
  if (lower.includes('напої') || lower.includes('beverage')) return '🥤';
  if (lower.includes('солод') || lower.includes('десерт') || lower.includes('dessert') || lower.includes('sweet')) return '🍫';
  return '🛒';
};

const HERO_SLIDES = [
  {
    title: "Свіжі продукти для ідеального раціону",
    subtitle: "Довірся алгоритму для створення меню на тиждень",
    btnText: "Спробувати",
    discountLabel: "Знижки до",
    discountVal: "-50%",
    bg: "linear-gradient(135deg, var(--primary-light) 0%, #E2D4FD 100%)",
    textColor: "var(--primary-dark)",
    link: "/survey"
  },
  {
    title: "Смачні рецепти на кожен день",
    subtitle: "Відкривай нові смаки та економ час на готуванні",
    btnText: "Переглянути рецепти",
    discountLabel: "Смачно",
    discountVal: "100%",
    bg: "linear-gradient(135deg, #FFF4ED 0%, #FEE2CE 100%)",
    textColor: "#D14343",
    link: "/recipes"
  },
  {
    title: "Контролюй своє харчування",
    subtitle: "Переглядай детальну статистику в особистому кабінеті",
    btnText: "Моя статистика",
    discountLabel: "Дуже",
    discountVal: "Зручно",
    bg: "linear-gradient(135deg, #E8F7EF 0%, #C4EDD8 100%)",
    textColor: "#1A9E5C",
    link: "/profile"
  }
];

export default function HomePage() {
  const [rootCategories, setRootCategories] = useState([]);
  const [categoryData, setCategoryData] = useState([]);
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [isFetching, setIsFetching] = useState(false);
  const [error, setError] = useState(null);
  const [cartCount, setCartCount] = useState(0);

  const [currentSlide, setCurrentSlide] = useState(0);
  const categoryScrollRef = useRef(null);
  const navigate = useNavigate();

  const fetchData = useCallback(async () => {
    setIsFetching(true);
    setError(null);
    try {
      const { data: categories } = await categoriesAPI.getRoots();
      const sorted = [...categories].sort((a, b) => (b.imageUrl ? 1 : 0) - (a.imageUrl ? 1 : 0));
      setRootCategories(sorted);

      const results = await Promise.all(
        categories.map(async (cat) => {
          try {
            const { data } = await productsAPI.getAll({
              categoryIds: cat.id,
              page: 0,
              size: 8,
            });
            return { category: cat, products: data.content || [] };
          } catch {
            return { category: cat, products: [] };
          }
        })
      );


      setCategoryData(results.filter((r) => r.products.length > 0));

      try {
        const { data: cart } = await cartAPI.getCart();
        setCartCount(cart.totalItems || 0);
      } catch {

      }
    } catch (err) {
      setError("Не вдалось завантажити дані. Перевірте з'єднання з сервером.");
      console.error(err);
    } finally {
      setIsFetching(false);
      setIsInitialLoad(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCartUpdate = (cart) => {
    setCartCount(cart.totalItems || 0);
  };

  const scrollCategories = (dir) => {
    if (categoryScrollRef.current) {
      const amt = dir === 'left' ? -200 : 200;
      categoryScrollRef.current.scrollBy({ left: amt, behavior: 'smooth' });
    }
  };

  const nextSlide = useCallback(() => setCurrentSlide(s => (s + 1) % HERO_SLIDES.length), []);
  const prevSlide = useCallback(() => setCurrentSlide(s => (s === 0 ? HERO_SLIDES.length - 1 : s - 1)), []);

  useEffect(() => {
    const timer = setInterval(() => {
      nextSlide();
    }, 5000);
    return () => clearInterval(timer);
  }, [nextSlide]);

  const slide = HERO_SLIDES[currentSlide];

  return (
    <>
      <Navbar cartCount={cartCount} />

      <main>
        { }
        <section className="section bg-white" style={{ paddingBottom: '32px' }}>
          <div className="container">
            <div className="hero-wrapper" style={{ marginBottom: '32px' }}>
              <div className="hero-banner" style={{ background: slide.bg }}>
                <h1 style={{ color: slide.textColor }}>{slide.title}</h1>
                <p>{slide.subtitle}</p>

                <button
                  className="hero-action-btn"
                  style={{ background: slide.textColor }}
                  onClick={() => navigate(slide.link || '/')}
                >
                  {slide.btnText}
                </button>

                <div className="hero-discount" style={{ background: slide.textColor }}>
                  <span>{slide.discountLabel}</span>
                  <span style={{ fontSize: slide.discountVal.length > 5 ? '20px' : '32px' }}>
                    {slide.discountVal}
                  </span>
                </div>

                <div className="hero-controls">
                  <button className="hero-nav" onClick={prevSlide}>❮</button>
                  <button className="hero-nav" onClick={nextSlide}>❯</button>
                </div>
              </div>

              <div className="hero-quick-links">
                <div className="quick-link-card" onClick={() => navigate('/catalog?filter=week')}>
                  <div className="quick-link-title">Товари тижня</div>
                  <div className="quick-link-icon" style={{ background: '#E8F7EF', color: '#1A9E5C' }}>🥬</div>
                </div>
                <div className="quick-link-card" onClick={() => navigate('/catalog?filter=promo')}>
                  <div className="quick-link-title">Всі акції</div>
                  <div className="quick-link-icon" style={{ background: '#FEF0E6', color: '#F2720C' }}>%</div>
                </div>
                <div className="quick-link-card" onClick={() => navigate('/catalog?filter=hot')}>
                  <div className="quick-link-title">Гарячі ціни</div>
                  <div className="quick-link-icon" style={{ background: '#FFF4ED', color: '#D14343' }}>🔥</div>
                </div>
                <div className="quick-link-card" onClick={() => navigate('/recipes')}>
                  <div className="quick-link-title">Рецепти та раціон</div>
                  <div className="quick-link-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}>🥗</div>
                </div>
              </div>
            </div>

            { }
            <div className="promo-algorithm-card">
              <div className="promo-algorithm-content">
                <div className="promo-icon-wrapper">
                  { }
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"></path>
                    <path d="M20 3v4"></path>
                    <path d="M22 5h-4"></path>
                    <path d="M4 17v2"></path>
                    <path d="M5 18H3"></path>
                  </svg>
                </div>
                <div className="promo-text-wrapper">
                  <h3>Створіть свій ідеальний раціон</h3>
                  <p>
                    Довіртесь нашому алгоритму! Наші працьовиті прибожки підберуть тижневе меню, яке ідеально підійде саме під ваші цілі, врахує всі побажання та згенерує кошик.
                  </p>
                </div>
              </div>
              <button
                className="promo-action-btn"
                onClick={() => navigate('/survey')}
              >
                Пройти опитування
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginLeft: '8px' }}>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                  <polyline points="12 5 19 12 12 19"></polyline>
                </svg>
              </button>
            </div>
          </div>
        </section>

        {isInitialLoad && (
          <section className="section bg-white" style={{ paddingTop: '32px' }}>
            <div className="container" style={{ textAlign: 'center', padding: '40px 0' }}>
              <div className="spinner" style={{ margin: '0 auto 16px' }}></div>
              <span style={{ color: 'var(--text-muted)' }}>Завантаження продуктів...</span>
            </div>
          </section>
        )}

        {error && (
          <section className="section bg-white" style={{ paddingTop: '32px' }}>
            <div className="container" style={{ textAlign: 'center', padding: '40px 0' }}>
              <p style={{ color: 'var(--danger)', marginBottom: '16px' }}>{error}</p>
              <button
                onClick={fetchData}
                style={{ background: 'var(--primary)', color: '#fff', padding: '10px 20px', borderRadius: '8px', border: 'none', cursor: 'pointer', fontWeight: 'bold' }}
              >
                Спробувати знову
              </button>
            </div>
          </section>
        )}

        {!isInitialLoad && !error && rootCategories.length > 0 && (
          <div className={`home-content-wrapper ${isFetching ? 'fetching-mask' : ''}`}>
            { }
            <section className="section section-alt" style={{ paddingTop: '32px' }}>
              <div className="container">
                <div className="section-title">
                  Категорії
                  <div className="category-section-actions">
                    <button onClick={() => scrollCategories('left')} className="carousel-nav-btn">❮</button>
                    <button onClick={() => scrollCategories('right')} className="carousel-nav-btn">❯</button>
                  </div>
                </div>
                <div className="carousel" ref={categoryScrollRef}>
                  {rootCategories.map((category) => (
                    <div className={`category-card ${!category.imageUrl ? 'no-image' : ''}`} key={`root-cat-${category.id}`} onClick={() => navigate(`/catalog/category/${category.slug}`)} title={category.name}>
                      {category.imageUrl ? (
                        <img src={category.imageUrl} alt={category.name} className="category-img" />
                      ) : (
                        <div className="category-content">
                          <div className="category-emoji">{getCategoryIcon(category.name)}</div>
                          <div className="category-name">{category.name}</div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </section>

            { }
            {categoryData.slice(0, 8).map(({ category, products }, index) => {
              const isAlt = index % 2 !== 0;
              return (
                <CategorySection
                  key={category.id}
                  category={category}
                  products={products}
                  onCartUpdate={handleCartUpdate}
                  isAlt={isAlt}
                />
              );
            })}
          </div>
        )}

        <section className={`section ${categoryData.slice(0, 8).length % 2 !== 0 ? 'bg-white' : 'section-alt'}`}>
          <div className="container">
            <div className="section-title">
              Рекомендовано для вас
              <Link to="/recipes" className="view-all">Всі рецепти →</Link>
            </div>
            <p style={{ color: 'var(--text-muted)' }}>Тут будуть рецепти, підібрані алгоритмом на основі вашого кошика...</p>
          </div>
        </section>

      </main>
    </>
  );
}
