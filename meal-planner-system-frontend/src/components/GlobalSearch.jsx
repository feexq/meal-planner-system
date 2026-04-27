import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { productsAPI, recipesAPI } from '../api/api';
import './GlobalSearch.css';

function useDebounce(value, delay) {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const handler = setTimeout(() => setDebouncedValue(value), delay);
        return () => clearTimeout(handler);
    }, [value, delay]);
    return debouncedValue;
}

export default function GlobalSearch() {
    const [query, setQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [results, setResults] = useState({ products: [], recipes: [] });

    const debouncedQuery = useDebounce(query, 400);
    const navigate = useNavigate();
    const searchRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (searchRef.current && !searchRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    useEffect(() => {
        if (!debouncedQuery.trim() || debouncedQuery.length < 2) {
            setResults({ products: [], recipes: [] });
            setIsOpen(false);
            return;
        }

        const fetchResults = async () => {
            setLoading(true);
            setIsOpen(true);
            try {
                const [prodRes, recRes] = await Promise.all([
                    productsAPI.getAll({ search: debouncedQuery, size: 4 }),
                    recipesAPI.getAll({ search: debouncedQuery, size: 2 })
                ]);

                setResults({
                    products: prodRes.data.content || [],
                    recipes: recRes.data.content || []
                });
            } catch (error) {
                console.error("Search error:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchResults();
    }, [debouncedQuery]);

    const handleNavigate = (path) => {
        setIsOpen(false);
        setQuery('');
        navigate(path);
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && query.trim()) {
            handleNavigate(`/catalog?search=${encodeURIComponent(query.trim())}`);
        }
    };

    return (
        <div className="global-search-container" ref={searchRef}>
            <div className="search-input-wrapper">
                <span className="search-icon">🔍</span>
                <input
                    type="text"
                    placeholder="Пошук продуктів, рецептів чи категорій..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onFocus={() => { if (query.length >= 2) setIsOpen(true); }}
                />
                {loading && <div className="search-spinner"></div>}
            </div>

            {isOpen && (
                <div className="search-dropdown">
                    {results.products.length === 0 && results.recipes.length === 0 && !loading ? (
                        <div className="search-empty">Нічого не знайдено за запитом "{query}"</div>
                    ) : (
                        <>
                            {results.products.length > 0 && (
                                <div className="search-group">
                                    <div className="search-group-title">Продукти</div>
                                    {results.products.map(p => (
                                        <div key={p.id} className="search-item" onClick={() => handleNavigate(`/product/${p.slug}`)}>
                                            {p.imageUrl ? <img src={p.imageUrl} alt={p.nameUk} /> : <div className="search-item-placeholder">🛒</div>}
                                            <div className="search-item-info">
                                                <span className="search-item-name">{p.nameUk}</span>
                                                <span className="search-item-price">{p.price} ₴ / {p.unit}</span>
                                            </div>
                                        </div>
                                    ))}
                                    <div className="search-view-all" onClick={() => handleNavigate(`/catalog?search=${encodeURIComponent(query)}`)}>
                                        Усі продукти «{query}» →
                                    </div>
                                </div>
                            )}

                            {results.recipes.length > 0 && (
                                <div className="search-group">
                                    <div className="search-group-title">Рецепти</div>
                                    {results.recipes.map(r => (
                                        <div key={r.id} className="search-item recipe-item" onClick={() => handleNavigate(`/recipe/${r.slug}`)}>
                                            {r.imageUrl ? <img src={r.imageUrl} alt={r.name} /> : <div className="search-item-placeholder">🥗</div>}
                                            <div className="search-item-info">
                                                <span className="search-item-name">{r.name}</span>
                                            </div>
                                        </div>
                                    ))}
                                    <div className="search-view-all" onClick={() => handleNavigate(`/recipes?search=${encodeURIComponent(query)}`)}>
                                        Усі рецепти «{query}» →
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}
        </div>
    );
}