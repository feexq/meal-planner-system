import { useRef } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from './ProductCard';
import './CategorySection.css';

export default function CategorySection({ category, products, onCartUpdate, isAlt }) {
  const scrollRef = useRef(null);

  const scroll = (direction) => {
    if (scrollRef.current) {
      const scrollAmount = direction === 'left' ? -260 : 260;
      scrollRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  };

  return (
    <section className={`section ${isAlt ? 'section-alt' : 'bg-white'}`} id={`category-${category.id}`}>
      <div className="container">
        <div className="section-title">
          {category.name}
          
          <div className="category-section-actions">
            <button onClick={() => scroll('left')} className="carousel-nav-btn">❮</button>
            <button onClick={() => scroll('right')} className="carousel-nav-btn">❯</button>
            <Link to={`/catalog/category/${category.slug}`} className="view-all" style={{ marginLeft: '12px' }}>
              Дивитись всі {category.name.toLowerCase()} →
            </Link>
          </div>
        </div>

        <div className="products-carousel" ref={scrollRef}>
          {products.length > 0 ? (
            products.map((p) => (
              <ProductCard key={p.id} product={p} onCartUpdate={onCartUpdate} />
            ))
          ) : (
            <div className="category-empty">Поки що немає товарів у цій категорії</div>
          )}
        </div>
      </div>
    </section>
  );
}
