import { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate, useSearchParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { productsAPI, categoriesAPI } from '../api/api';
import ProductCard from '../components/ProductCard';
import './ProductCatalogPage.css';

export default function ProductCatalogPage() {
  const { slug } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [currentCategory, setCurrentCategory] = useState(null);

  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [isFetching, setIsFetching] = useState(false);
  const [isCategoriesLoading, setIsCategoriesLoading] = useState(true);

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(24);

  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [isCatExpanded, setIsCatExpanded] = useState(false);
  const [catSearchQuery, setCatSearchQuery] = useState('');


  useEffect(() => {
    const s = searchParams.get('search');
    if (s) {
      setSearchQuery(s);
      setDebouncedSearch(s);
    }
  }, [searchParams]);


  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchQuery), 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);


  useEffect(() => {
    setIsCategoriesLoading(true);
    categoriesAPI.getAll()
      .then(res => {
        const cats = res.data || [];
        setCategories(cats);
        if (slug) {
          const found = cats.find(c => c.slug?.toLowerCase() === slug.toLowerCase());
          setCurrentCategory(found ?? null);
        } else {
          setCurrentCategory(null);
        }
      })
      .catch(err => console.error("Failed to load categories", err))
      .finally(() => setIsCategoriesLoading(false));
  }, [slug]);


  useEffect(() => {
    setCurrentPage(0);
  }, [slug, debouncedSearch, pageSize]);


  const fetchProducts = useCallback(async () => {

    if (isCategoriesLoading) return;

    if (!isInitialLoad) setIsFetching(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize
      };

      if (currentCategory) {
        const ids = [currentCategory.id];
        const children = categories.filter(c => c.parentId === currentCategory.id);
        children.forEach(ch => ids.push(ch.id));
        params.categoryIds = ids;
      }
      if (debouncedSearch?.trim().length >= 2) params.search = debouncedSearch.trim();

      const { data } = await productsAPI.getAll(params);
      setProducts(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setTotalElements(data.page?.totalElements || 0);
    } catch (err) {
      console.error(err);
      setProducts([]);
    } finally {
      setIsFetching(false);
      setIsInitialLoad(false);
    }
  }, [currentCategory, debouncedSearch, currentPage, pageSize, isInitialLoad, categories, isCategoriesLoading]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const resetFilters = () => {
    setSearchQuery('');
    if (slug) navigate('/catalog');
  };

  const hasActiveFilters = searchQuery || slug;

  return (
    <div className="product-catalog-page">
      <Navbar />

      <main className="container">

        <div className="catalog-header">
          <div className="breadcrumbs">
            <Link to="/">Магазин</Link>
            {' / '}<span>{currentCategory?.name || 'Всі товари'}</span>
          </div>
          <h1>{currentCategory?.name || 'Каталог продуктів'}</h1>
          <p className="subtitle">
            {currentCategory?.description || 'Свіжі та якісні продукти для вашого столу, підібрані спеціально для вас.'}
          </p>

          {!isInitialLoad && !isCategoriesLoading && (
            <div className="header-stats">
              <div className="header-stat">
                <span className="header-stat-val">{totalElements}</span>
                <span className="header-stat-lbl">товарів</span>
              </div>
              {hasActiveFilters && (
                <div className="header-stat">
                  <span className="header-stat-val">Активні</span>
                  <span className="header-stat-lbl">фільтри</span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="main-layout">

          <aside className="sidebar">
            <div className="sidebar-header">
              <h2>Фільтри</h2>
              {hasActiveFilters && (
                <button className="btn-reset-text" onClick={resetFilters}>Скинути</button>
              )}
            </div>

            <div className="filter-group search-group">
              <div className="search-bar-local">
                <span className="search-icon">🔍</span>
                <input
                  type="text"
                  placeholder="Пошук продуктів..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            <div className="filter-group">
              <span className="filter-label">Категорії</span>
              <div className="tag-search-container">
                <input
                  type="text"
                  placeholder="Пошук категорій..."
                  value={catSearchQuery}
                  onChange={e => setCatSearchQuery(e.target.value)}
                />
              </div>

              <div className={`cat-list-container ${isCategoriesLoading ? 'fetching-mask' : ''}`}>
                <ul className="filter-list cat-list-scrollable">
                  <li className={`cat-item-link ${!slug ? 'active' : ''}`} onClick={() => navigate('/catalog')}>
                    Всі категорії
                  </li>
                  {categories
                    .filter(c => !c.parentId)
                    .filter(cat => cat.name?.toLowerCase().includes(catSearchQuery.toLowerCase()))
                    .map(root => (
                      <div key={root.id}>
                        <li
                          className={`cat-item-link root-cat ${slug === root.slug ? 'active' : ''}`}
                          onClick={() => navigate(`/catalog/category/${root.slug}`)}
                        >
                          {root.name}
                        </li>
                        {(slug === root.slug || categories.find(c => c.slug === slug)?.parentId === root.id || catSearchQuery) && (
                          <ul className="sub-cat-list">
                            {categories
                              .filter(c => c.parentId === root.id)
                              .map(child => (
                                <li
                                  key={child.id}
                                  className={`cat-item-link child-cat ${slug === child.slug ? 'active' : ''}`}
                                  onClick={() => navigate(`/catalog/category/${child.slug}`)}
                                >
                                  — {child.name}
                                </li>
                              ))}
                          </ul>
                        )}
                      </div>
                    ))}
                </ul>
              </div>

              {!catSearchQuery && categories.length > 12 && (
                <button className="btn-toggle-tags" onClick={() => setIsCatExpanded(!isCatExpanded)}>
                  {isCatExpanded ? 'Згорнути ↑' : `Показати всі (${categories.length}) ↓`}
                </button>
              )}
            </div>

            <div className="filter-group">
              <span className="filter-label">На сторінці</span>
              <select className="page-size-select" value={pageSize} onChange={e => setPageSize(Number(e.target.value))}>
                <option value={12}>12 товарів</option>
                <option value={24}>24 товари</option>
                <option value={48}>48 товарів</option>
              </select>
            </div>
          </aside>

          <section className="catalog-content">
            <div className="catalog-toolbar">
              <span className="results-count">
                Знайдено: <strong>{totalElements}</strong>
                {hasActiveFilters && (
                  <button className="btn-reset-inline" onClick={resetFilters}>Скинути фільтри</button>
                )}
              </span>
            </div>

            {isInitialLoad || isCategoriesLoading ? (
              <div className="grid-loading"><div className="spinner" /></div>
            ) : products.length === 0 && !isFetching ? (
              <div className="grid-empty">
                <h3>Нічого не знайдено 😕</h3>
                <p style={{ marginBottom: 16 }}>Спробуйте змінити запит або обрати іншу категорію.</p>
                <button className="btn-primary" onClick={resetFilters}>Скинути пошук</button>
              </div>
            ) : (
              <div className={`catalog-grid-wrapper ${isFetching ? 'fetching-mask' : ''}`}>
                <div className="product-grid">
                  {products.map(p => (
                    <ProductCard key={p.id} product={p} />
                  ))}
                </div>
              </div>
            )}

            {totalPages > 1 && (
              <div className="pagination">
                <button disabled={currentPage === 0} onClick={() => setCurrentPage(p => p - 1)} className="page-btn">← Назад</button>
                <div className="page-numbers">
                  {[...Array(totalPages)].map((_, i) => (
                    (i === 0 || i === totalPages - 1 || (i >= currentPage - 1 && i <= currentPage + 1)) ? (
                      <button key={i} onClick={() => setCurrentPage(i)} className={`page-num ${currentPage === i ? 'active' : ''}`}>{i + 1}</button>
                    ) : (
                      (i === 1 || i === totalPages - 2) ? <span key={i} className="pagination-dots">...</span> : null
                    )
                  ))}
                </div>
                <button disabled={currentPage >= totalPages - 1} onClick={() => setCurrentPage(p => p + 1)} className="page-btn">Далі →</button>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}