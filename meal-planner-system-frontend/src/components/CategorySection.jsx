import ProductCard from './ProductCard';
import './CategorySection.css';

export default function CategorySection({ category, products, onCartUpdate }) {
  return (
    <section className="category-section" id={`category-${category.id}`}>
      <div className="category-header">
        <h2 className="category-title">{category.name}</h2>
        <span className="category-count">
          {products.length} {products.length === 1 ? 'товар' : 'товарів'}
        </span>
      </div>

      <div className="category-products">
        {products.length > 0 ? (
          products.map((p) => (
            <ProductCard key={p.id} product={p} onCartUpdate={onCartUpdate} />
          ))
        ) : (
          <div className="category-empty">Поки що немає товарів у цій категорії</div>
        )}
      </div>
    </section>
  );
}
