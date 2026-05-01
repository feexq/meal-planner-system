import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './AdminPage.css';

// ─── API helpers ────────────────────────────────────────────────────────────
const BASE = '/api';
const token = () => localStorage.getItem('accessToken');
const headers = () => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${token()}` });

async function apiFetch(method, path, body, params = {}) {
    const url = new URL(BASE + path, window.location.origin);
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') url.searchParams.append(k, v);
    });
    const res = await fetch(url, {
        method,
        headers: headers(),
        body: body ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    if (res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
}

async function apiUpload(path, file) {
    const fd = new FormData();
    fd.append('file', file);
    const res = await fetch(BASE + path, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token()}` },
        body: fd
    });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    return res.json();
}

const api = {
    ingredients: {
        getAll: (p = {}) => apiFetch('GET', '/ingredients', null, p),
        create: (d) => apiFetch('POST', '/ingredients', d),
        update: (id, d) => apiFetch('PUT', `/ingredients/${id}`, d),
        remove: (id) => apiFetch('DELETE', `/ingredients/${id}`),
        updateDietary: (id, d) => apiFetch('PUT', `/ingredients/${id}/dietary-tags`, d),
        triggerDietary: () => apiFetch('POST', '/dietary-conditions/trigger-classification'),
    },
    categories: {
        getAll: () => apiFetch('GET', '/categories/all'),
        create: (d) => apiFetch('POST', '/categories', d),
        update: (id, d) => apiFetch('PUT', `/categories/${id}`, d),
        remove: (id) => apiFetch('DELETE', `/categories/${id}`),
        uploadImage: (id, file) => apiUpload(`/categories/${id}/image`, file),
    },
    products: {
        getAll: (p = {}) => apiFetch('GET', '/products', null, p),
        create: (d) => apiFetch('POST', '/products', d),
        update: (id, d) => apiFetch('PUT', `/products/${id}`, d),
        remove: (id) => apiFetch('DELETE', `/products/${id}`),
        uploadImage: (id, file) => apiUpload(`/products/${id}/image`, file),
    },
    recipes: {
        getAll: (p = {}) => apiFetch('GET', '/recipes', null, p),
    },
    tags: {
        getRecipes: () => apiFetch('GET', '/tags-recipes'),
        createRecipeTag: (d) => apiFetch('POST', '/tags-recipes', d),
        updateRecipeTag: (id, d) => apiFetch('PUT', `/tags-recipes/${id}`, d),
        getBase: () => apiFetch('GET', '/tags-base'),
        createBaseTag: (d) => apiFetch('POST', '/tags-base', d),
        updateBaseTag: (id, d) => apiFetch('PATCH', `/tags-base/${id}`, d),
        removeBaseTag: (id) => apiFetch('DELETE', `/tags-base/${id}`),
    },
    achievements: {
        getAll: () => apiFetch('GET', '/achievements'),
        create: (d) => apiFetch('POST', '/achievements', d),
        update: (id, d) => apiFetch('PUT', `/achievements/${id}`, d),
        remove: (id) => apiFetch('DELETE', `/achievements/${id}`),
    },
    mealPlanner: {
        generate: (email) => apiFetch('POST', `/meal-plan/generate/${email}`),
        generateFinal: (email) => apiFetch('POST', `/meal-plan/generate/final/${email}`),
    }
};

// ─── Icons ───────────────────────────────────────────────────────────────────
const Icon = ({ name, size = 18 }) => {
    const icons = {
        ingredient: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z" /><path d="M12 6v6l4 2" /></svg>,
        category: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /></svg>,
        product: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" /></svg>,
        recipe: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></svg>,
        tag: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>,
        edit: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>,
        trash: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" /><path d="M9 6V4h6v2" /></svg>,
        plus: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>,
        list: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" /><line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" /></svg>,
        grid: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /></svg>,
        cards: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>,
        search: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>,
        close: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>,
        check: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12" /></svg>,
        shield: <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>,
    };
    return icons[name] || null;
};

// ─── Toast ───────────────────────────────────────────────────────────────────
function useToast() {
    const [toasts, setToasts] = useState([]);
    const add = useCallback((msg, type = 'success') => {
        const id = Date.now();
        setToasts(t => [...t, { id, msg, type }]);
        setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3000);
    }, []);
    return { toasts, add };
}

// ─── Modal ───────────────────────────────────────────────────────────────────
function Modal({ title, onClose, children }) {
    useEffect(() => {
        const esc = (e) => e.key === 'Escape' && onClose();
        document.addEventListener('keydown', esc);
        return () => document.removeEventListener('keydown', esc);
    }, [onClose]);

    return (
        <div className="adm-modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
            <div className="adm-modal">
                <div className="adm-modal-header">
                    <h3>{title}</h3>
                    <button className="adm-modal-close" onClick={onClose}><Icon name="close" /></button>
                </div>
                <div className="adm-modal-body">{children}</div>
            </div>
        </div>
    );
}

// ─── Confirm Dialog ───────────────────────────────────────────────────────────
function Confirm({ msg, onYes, onNo }) {
    return (
        <div className="adm-modal-overlay">
            <div className="adm-modal adm-confirm">
                <p>{msg}</p>
                <div className="adm-confirm-btns">
                    <button className="adm-btn adm-btn-danger" onClick={onYes}>Видалити</button>
                    <button className="adm-btn adm-btn-ghost" onClick={onNo}>Скасувати</button>
                </div>
            </div>
        </div>
    );
}

// ─── Generic Form Field ───────────────────────────────────────────────────────
function Field({ label, name, value, onChange, type = 'text', required }) {
    return (
        <div className="adm-field">
            <label className="adm-label">{label}{required && <span className="adm-required">*</span>}</label>
            {type === 'textarea'
                ? <textarea className="adm-input" name={name} value={value || ''} onChange={onChange} rows={3} />
                : <input className="adm-input" type={type} name={name} value={value || ''} onChange={onChange} required={required} />
            }
        </div>
    );
}

// ─── View Toggle ─────────────────────────────────────────────────────────────
function ViewToggle({ view, onChange }) {
    return (
        <div className="adm-view-toggle">
            {['list', 'grid', 'cards'].map(v => (
                <button key={v} className={`adm-vt-btn ${view === v ? 'active' : ''}`} onClick={() => onChange(v)} title={v}>
                    <Icon name={v} size={16} />
                </button>
            ))}
        </div>
    );
}

// ─── Search ───────────────────────────────────────────────────────────────────
function SearchBar({ value, onChange, placeholder = 'Пошук...' }) {
    return (
        <div className="adm-search">
            <Icon name="search" size={16} />
            <input className="adm-search-input" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
            {value && <button className="adm-search-clear" onClick={() => onChange('')}><Icon name="close" size={14} /></button>}
        </div>
    );
}

// ─── Pagination ───────────────────────────────────────────────────────────────
function Pagination({ page, total, pageSize, onPage }) {
    const pages = Math.ceil(total / pageSize);
    if (pages <= 1) return null;
    return (
        <div className="adm-pagination">
            {Array.from({ length: Math.min(pages, 10) }, (_, i) => (
                <button key={i} className={`adm-page-btn ${page === i ? 'active' : ''}`} onClick={() => onPage(i)}>{i + 1}</button>
            ))}
        </div>
    );
}

// ─── Section: Ingredients ─────────────────────────────────────────────────────
function IngredientsSection({ toast }) {
    const [items, setItems] = useState([]);
    const [view, setView] = useState('list');
    const [q, setQ] = useState('');
    const [modal, setModal] = useState(null); // null | 'create' | item
    const [form, setForm] = useState({});
    const [confirm, setConfirm] = useState(null);
    const [loading, setLoading] = useState(false);

    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const size = 20;

    const load = useCallback(async () => {
        try {
            const res = await api.ingredients.getAll({ page, size, search: q, sort: 'normalizedName,asc' });
            setItems(res.content || []);
            setTotal(res.page?.totalElements || res.totalElements || 0);
        } catch { toast('Помилка завантаження', 'error'); }
    }, [page, q]);

    useEffect(() => { load(); }, [load]);

    const change = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    const openCreate = () => { setForm({}); setModal('create'); };
    const openEdit = (item) => { setForm({ ...item }); setModal(item); };

    const save = async () => {
        setLoading(true);
        try {
            if (modal === 'create') await api.ingredients.create(form);
            else await api.ingredients.update(modal.id, form);
            toast('Збережено'); setModal(null); load();
        } catch { toast('Помилка збереження', 'error'); }
        finally { setLoading(false); }
    };

    const remove = async (id) => {
        try { await api.ingredients.remove(id); toast('Видалено'); load(); }
        catch { toast('Помилка', 'error'); }
        setConfirm(null);
    };

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left">
                    <SearchBar value={q} onChange={setQ} placeholder="Пошук (server-side)..." />
                    <span className="adm-count">Показано {items.length} з {total}</span>
                </div>
                <div className="adm-section-right">
                    <ViewToggle view={view} onChange={setView} />
                    <button className="adm-btn adm-btn-primary" onClick={openCreate}><Icon name="plus" size={16} /> Додати</button>
                </div>
            </div>

            <DataView view={view} items={items} fields={['normalizedName', 'slug', 'unit']} onEdit={openEdit} onDelete={id => setConfirm(id)} />

            <div className="adm-section-footer">
                <Pagination page={page} total={total} pageSize={size} onPage={setPage} />
                <button className="adm-btn adm-btn-ghost" onClick={async () => {
                    try { await api.ingredients.triggerDietary(); toast('Класифікацію запущено'); }
                    catch { toast('Помилка', 'error'); }
                }}>
                    🔄 Оновити дієтичні теги
                </button>
            </div>

            {modal && (
                <Modal title={modal === 'create' ? 'Новий інгредієнт' : 'Редагування'} onClose={() => setModal(null)}>
                    <Field label="Нормалізована Назва" name="normalizedName" value={form.normalizedName} onChange={change} required />
                    <Field label="Фото (URL)" name="imageUrl" value={form.imageUrl} onChange={change} />
                    <Field label="Одиниця" name="unit" value={form.unit} onChange={change} />
                    <div className="adm-modal-actions">
                        <button className="adm-btn adm-btn-primary" onClick={save} disabled={loading}>{loading ? '...' : 'Зберегти'}</button>
                    </div>
                </Modal>
            )}
            {confirm && <Confirm msg="Видалити інгредієнт?" onYes={() => remove(confirm)} onNo={() => setConfirm(null)} />}
        </div>
    );
}

// ─── Section: Categories ──────────────────────────────────────────────────────
function CategoriesSection({ toast }) {
    const [items, setItems] = useState([]);
    const [view, setView] = useState('list');
    const [q, setQ] = useState('');
    const [modal, setModal] = useState(null);
    const [form, setForm] = useState({});
    const [confirm, setConfirm] = useState(null);

    const load = useCallback(async () => {
        try { setItems(await api.categories.getAll()); }
        catch { toast('Помилка завантаження', 'error'); }
    }, []);
    useEffect(() => { load(); }, [load]);

    const change = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

    const save = async () => {
        try {
            let res;
            if (modal === 'create') res = await api.categories.create(form);
            else res = await api.categories.update(modal.id, form);

            if (form.file) {
                await api.categories.uploadImage(res.id || modal.id, form.file);
            }

            toast('Збережено'); setModal(null); load();
        } catch { toast('Помилка', 'error'); }
    };

    const onFile = (e) => {
        if (e.target.files[0]) setForm({ ...form, file: e.target.files[0] });
    };

    const remove = async (id) => {
        try { await api.categories.remove(id); toast('Видалено'); load(); }
        catch { toast('Помилка видалення', 'error'); }
        setConfirm(null);
    };

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left"><span className="adm-count">{items.length} категорій</span></div>
                <div className="adm-section-right"><button className="adm-btn adm-btn-primary" onClick={() => { setForm({}); setModal('create'); }}><Icon name="plus" size={16} /> Додати</button></div>
            </div>
            <DataView view="list" items={items} fields={['name', 'slug', 'parentId']} onEdit={i => { setForm({ ...i }); setModal(i); }} onDelete={id => setConfirm(id)} />
            {modal && (
                <Modal title="Категорія" onClose={() => setModal(null)}>
                    <Field label="Назва" name="name" value={form.name} onChange={change} required />
                    <Field label="Slug" name="slug" value={form.slug} onChange={change} required />
                    <Field label="Parent ID" name="parentId" value={form.parentId} onChange={change} type="number" />
                    <div className="adm-field">
                        <label className="adm-label">Фото (Upload)</label>
                        <input type="file" onChange={onFile} className="adm-input" accept="image/*" />
                    </div>
                    <div className="adm-modal-actions">
                        <button className="adm-btn adm-btn-primary" onClick={save}>Зберегти</button>
                    </div>
                </Modal>
            )}
            {confirm && <Confirm msg="Видалити категорію?" onYes={() => remove(confirm)} onNo={() => setConfirm(null)} />}
        </div>
    );
}

// ─── Section: Products ────────────────────────────────────────────────────────
function ProductsSection({ toast }) {
    const [items, setItems] = useState([]);
    const [view, setView] = useState('cards');
    const [q, setQ] = useState('');
    const [modal, setModal] = useState(null);
    const [form, setForm] = useState({});
    const [confirm, setConfirm] = useState(null);
    const [loading, setLoading] = useState(false);

    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const size = 12;

    const load = useCallback(async () => {
        try {
            const res = await api.products.getAll({ page, size, search: q });
            setItems(res.content || []);
            setTotal(res.page?.totalElements || res.totalElements || 0);
        } catch { toast('Помилка завантаження', 'error'); }
    }, [page, q]);
    useEffect(() => { load(); }, [load]);

    const change = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

    const save = async () => {
        setLoading(true);
        try {
            let res;
            if (modal === 'create') res = await api.products.create(form);
            else res = await api.products.update(modal.id, form);

            if (form.file) {
                await api.products.uploadImage(res.id || modal.id, form.file);
            }

            toast('Продукт збережено'); setModal(null); load();
        } catch { toast('Помилка', 'error'); }
        finally { setLoading(false); }
    };

    const onFile = (e) => {
        if (e.target.files[0]) setForm({ ...form, file: e.target.files[0] });
    };

    const remove = async (id) => {
        try { await api.products.remove(id); toast('Видалено'); load(); }
        catch { toast('Помилка видалення', 'error'); }
        setConfirm(null);
    };

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left">
                    <SearchBar value={q} onChange={setQ} placeholder="Пошук продуктів..." />
                    <span className="adm-count">Показано {items.length} з {total}</span>
                </div>
                <div className="adm-section-right">
                    <ViewToggle view={view} onChange={setView} />
                    <button className="adm-btn adm-btn-primary" onClick={() => { setForm({}); setModal('create'); }}><Icon name="plus" size={16} /> Додати</button>
                </div>
            </div>

            <DataView view={view} items={items} fields={['nameUk', 'slug', 'price', 'unit']} imageField="imageUrl" onEdit={item => { setForm({ ...item }); setModal(item); }} onDelete={id => setConfirm(id)} />

            <Pagination page={page} total={total} pageSize={size} onPage={setPage} />

            {modal && (
                <Modal title={modal === 'create' ? 'Новий продукт' : 'Редагування продукту'} onClose={() => setModal(null)}>
                    <div className="adm-form-grid">
                        <Field label="Назва (Укр)" name="nameUk" value={form.nameUk} onChange={change} required />
                        <Field label="Ціна" name="price" value={form.price} onChange={change} type="number" />
                        <Field label="Одиниця" name="unit" value={form.unit} onChange={change} />
                        <div className="adm-field">
                            <label className="adm-label">Фото (Завантажити)</label>
                            <input type="file" onChange={onFile} className="adm-input" accept="image/*" />
                        </div>
                    </div>
                    <div className="adm-field">
                        <label className="adm-label">Теги продукту (IDs)</label>
                        <input className="adm-input" name="tagIds" value={Array.isArray(form.tagIds) ? form.tagIds.join(', ') : (form.tagIds || '')} onChange={e => setForm({ ...form, tagIds: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })} placeholder="1, 2" />
                    </div>
                    <Field label="Опис" name="description" value={form.description} onChange={change} type="textarea" />
                    <div className="adm-modal-actions">
                        <button className="adm-btn adm-btn-primary" onClick={save} disabled={loading}>{loading ? '...' : 'Зберегти'}</button>
                    </div>
                </Modal>
            )}
            {confirm && <Confirm msg="Видалити продукт?" onYes={() => remove(confirm)} onNo={() => setConfirm(null)} />}
        </div>
    );
}

// ─── Section: Recipes (read-only) ─────────────────────────────────────────────
function RecipesSection({ toast }) {
    const [items, setItems] = useState([]);
    const [view, setView] = useState('grid');
    const [q, setQ] = useState('');
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const size = 12;

    const load = useCallback(async () => {
        try {
            const res = await api.recipes.getAll({ page, size, search: q });
            setItems(res.content || []);
            setTotal(res.page?.totalElements || res.totalElements || 0);
        } catch { toast('Помилка завантаження', 'error'); }
    }, [page, q]);

    useEffect(() => { load(); }, [load]);

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left">
                    <SearchBar value={q} onChange={setQ} placeholder="Пошук рецептів..." />
                    <span className="adm-count">Показано {items.length} з {total}</span>
                </div>
                <div className="adm-section-right">
                    <ViewToggle view={view} onChange={setView} />
                </div>
            </div>
            <DataView view={view} items={items} fields={['name', 'slug', 'calories']} imageField="imageUrl" readOnly />
            <Pagination page={page} total={total} pageSize={size} onPage={setPage} />
        </div>
    );
}

// ─── Section: Tags ────────────────────────────────────────────────────────────
function TagsSection({ toast }) {
    const [recipeTags, setRecipeTags] = useState([]);
    const [baseTags, setBaseTags] = useState([]);
    const [type, setType] = useState('recipe'); // 'recipe' | 'base'
    const [q, setQ] = useState('');
    const [modal, setModal] = useState(null);
    const [form, setForm] = useState({});
    const [loading, setLoading] = useState(false);

    const load = useCallback(async () => {
        try {
            const [r, b] = await Promise.all([api.tags.getRecipes(), api.tags.getBase()]);
            setRecipeTags(r || []);
            setBaseTags(b || []);
        } catch { toast('Помилка завантаження', 'error'); }
    }, []);
    useEffect(() => { load(); }, [load]);

    const items = type === 'recipe' ? recipeTags : baseTags;
    const filtered = items.filter(x => x.name?.toLowerCase().includes(q.toLowerCase()));
    const change = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

    const save = async () => {
        setLoading(true);
        try {
            if (type === 'recipe') {
                if (!modal || modal === 'create') await api.tags.createRecipeTag(form);
                else await api.tags.updateRecipeTag(modal.id, form);
            } else {
                if (!modal || modal === 'create') await api.tags.createBaseTag(form);
                else await api.tags.updateBaseTag(modal.id, form);
            }
            toast('Збережено'); setModal(null); load();
        } catch { toast('Помилка', 'error'); }
        finally { setLoading(false); }
    };

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left">
                    <div className="adm-view-toggle">
                        <button className={`adm-vt-btn ${type === 'recipe' ? 'active' : ''}`} onClick={() => setType('recipe')}>Рецепти</button>
                        <button className={`adm-vt-btn ${type === 'base' ? 'active' : ''}`} onClick={() => setType('base')}>Продукти</button>
                    </div>
                    <SearchBar value={q} onChange={setQ} placeholder="Пошук..." />
                </div>
                <div className="adm-section-right">
                    <button className="adm-btn adm-btn-primary" onClick={() => { setForm({}); setModal('create'); }}><Icon name="plus" size={16} /> Додати</button>
                </div>
            </div>

            <div className="adm-tags-grid">
                {filtered.map(tag => (
                    <div key={tag.id} className="adm-tag-chip" style={{ '--tag-color': tag.color || '#6366f1' }}>
                        {tag.iconUrl && <img src={tag.iconUrl} className="adm-tag-icon" alt="" />}
                        {!tag.iconUrl && <span className="adm-tag-dot" />}
                        <span className="adm-tag-name">{tag.name}</span>
                        <button className="adm-tag-edit" onClick={() => { setForm({ ...tag }); setModal(tag); }}><Icon name="edit" size={13} /></button>
                    </div>
                ))}
            </div>

            {modal && (
                <Modal title={modal === 'create' ? 'Новий тег' : 'Редагування'} onClose={() => setModal(null)}>
                    <Field label="Назва" name="name" value={form.name} onChange={change} required />
                    {type === 'base' && (
                        <>
                            <Field label="Slug" name="slug" value={form.slug} onChange={change} />
                            <div className="adm-field">
                                <label className="adm-label">Колір</label>
                                <div className="adm-color-row">
                                    <input type="color" className="adm-color-input" name="color" value={form.color || '#6366f1'} onChange={change} />
                                    <input className="adm-input adm-color-text" name="color" value={form.color || ''} onChange={change} placeholder="#6366f1" />
                                </div>
                            </div>
                        </>
                    )}
                    <div className="adm-modal-actions">
                        <button className="adm-btn adm-btn-primary" onClick={save} disabled={loading}>{loading ? '...' : 'Зберегти'}</button>
                    </div>
                </Modal>
            )}
        </div>
    );
}

function AchievementsSection({ toast }) {
    const [items, setItems] = useState([]);
    const [modal, setModal] = useState(null);
    const [form, setForm] = useState({});
    const [confirm, setConfirm] = useState(null);

    const load = useCallback(async () => {
        try { setItems(await api.achievements.getAll()); }
        catch { toast('Помилка', 'error'); }
    }, []);
    useEffect(() => { load(); }, [load]);

    const save = async () => {
        try {
            if (modal === 'create') await api.achievements.create(form);
            else await api.achievements.update(modal.id, form);
            toast('Збережено'); setModal(null); load();
        } catch { toast('Помилка', 'error'); }
    };

    const remove = async (id) => {
        try { await api.achievements.remove(id); toast('Видалено'); load(); }
        catch { toast('Помилка', 'error'); }
        setConfirm(null);
    };

    return (
        <div className="adm-section">
            <div className="adm-section-header">
                <div className="adm-section-left"><span className="adm-count">{items.length} досягнень</span></div>
                <div className="adm-section-right">
                    <button className="adm-btn adm-btn-primary" onClick={() => { setForm({}); setModal('create'); }}><Icon name="plus" size={16} /> Додати</button>
                </div>
            </div>
            <DataView view="list" items={items} fields={['key', 'title', 'type', 'targetValue']} onEdit={i => { setForm({ ...i }); setModal(i); }} onDelete={id => setConfirm(id)} />
            {modal && (
                <Modal title="Досягнення" onClose={() => setModal(null)}>
                    <Field label="Ключ (унікальний)" name="key" value={form.key} onChange={e => setForm({ ...form, key: e.target.value })} required />
                    <Field label="Заголовок" name="title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} required />
                    <Field label="Опис" name="description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} type="textarea" />
                    <Field label="Цільове значення" name="targetValue" value={form.targetValue} onChange={e => setForm({ ...form, targetValue: e.target.value })} type="number" />
                    <div className="adm-field">
                        <label className="adm-label">Тип досягнення</label>
                        <select className="adm-input" name="type" value={form.type} onChange={e => setForm({ ...form, type: e.target.value })}>
                            <option value="">Оберіть тип</option>
                            <option value="STREAK">STREAK (Вогоник - дні підряд)</option>
                            <option value="RECIPES">RECIPES (Кількість рецептів)</option>
                        </select>
                        {form.type === 'STREAK' && <p className="adm-hint">Залежить від кількості днів вогника.</p>}
                    </div>
                    <Field label="Icon URL" name="iconUrl" value={form.iconUrl} onChange={e => setForm({ ...form, iconUrl: e.target.value })} />
                    <div className="adm-modal-actions">
                        <button className="adm-btn adm-btn-primary" onClick={save}>Зберегти</button>
                    </div>
                </Modal>
            )}
            {confirm && <Confirm msg="Видалити досягнення?" onYes={() => remove(confirm)} onNo={() => setConfirm(null)} />}
        </div>
    );
}

function MealPlannerSection({ toast }) {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);

    const run = async (mode) => {
        if (!email) return toast('Введіть email', 'error');
        setLoading(mode);
        try {
            if (mode === 'draft') await api.mealPlanner.generate(email);
            else await api.mealPlanner.generateFinal(email);
            toast('План згенеровано');
        } catch { toast('Помилка', 'error'); }
        finally { setLoading(false); }
    };

    return (
        <div className="adm-section">
            <div className="adm-card" style={{ maxWidth: 400, margin: '40px auto', padding: 20 }}>
                <h3 style={{ marginTop: 0 }}>Генератор раціону</h3>
                <p className="adm-card-desc" style={{ marginBottom: 20 }}>Згенерувати раціон для конкретного користувача вручну.</p>
                <Field label="Email користувача" value={email} onChange={e => setEmail(e.target.value)} placeholder="user@example.com" />
                <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                    <button className="adm-btn adm-btn-primary" onClick={() => run('draft')} disabled={!!loading}>
                        {loading === 'draft' ? '...' : 'Чернетка'}
                    </button>
                    <button className="adm-btn adm-btn-primary" style={{ background: '#059669' }} onClick={() => run('final')} disabled={!!loading}>
                        {loading === 'final' ? '...' : 'Фінальний'}
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─── DataView ─────────────────────────────────────────────────────────────────
function DataView({ view, items, fields, imageField, onEdit, onDelete, readOnly }) {
    if (!items.length) return <p className="adm-empty">Нічого не знайдено</p>;

    if (view === 'list') return (
        <div className="adm-table-wrap">
            <table className="adm-table">
                <thead>
                    <tr>
                        {fields.map(f => <th key={f}>{f}</th>)}
                        {!readOnly && <th className="adm-th-actions">Дії</th>}
                    </tr>
                </thead>
                <tbody>
                    {items.map(item => (
                        <tr key={item.id}>
                            {fields.map(f => <td key={f}>{item[f] ?? '—'}</td>)}
                            {!readOnly && (
                                <td className="adm-td-actions">
                                    <button className="adm-icon-btn adm-icon-edit" onClick={() => onEdit(item)}><Icon name="edit" size={15} /></button>
                                    <button className="adm-icon-btn adm-icon-del" onClick={() => onDelete(item.id)}><Icon name="trash" size={15} /></button>
                                </td>
                            )}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );

    if (view === 'grid') return (
        <div className="adm-grid">
            {items.map(item => (
                <div key={item.id} className="adm-grid-item">
                    {imageField && item[imageField]
                        ? <img src={item[imageField]} alt={item.name} className="adm-grid-img" />
                        : <div className="adm-grid-placeholder"><Icon name="product" size={28} /></div>
                    }
                    <div className="adm-grid-info">
                        <span className="adm-grid-name">{item.name || item[fields[0]]}</span>
                        {fields[1] && <span className="adm-grid-sub">{item[fields[1]]}</span>}
                    </div>
                    {!readOnly && (
                        <div className="adm-grid-actions">
                            <button className="adm-icon-btn adm-icon-edit" onClick={() => onEdit(item)}><Icon name="edit" size={14} /></button>
                            <button className="adm-icon-btn adm-icon-del" onClick={() => onDelete(item.id)}><Icon name="trash" size={14} /></button>
                        </div>
                    )}
                </div>
            ))}
        </div>
    );

    // cards
    return (
        <div className="adm-cards">
            {items.map(item => (
                <div key={item.id} className="adm-card">
                    {imageField && item[imageField]
                        ? <img src={item[imageField]} alt={item.name} className="adm-card-img" />
                        : <div className="adm-card-placeholder"><Icon name="product" size={36} /></div>
                    }
                    <div className="adm-card-body">
                        <h4 className="adm-card-title">{item.name || item[fields[0]]}</h4>
                        <div className="adm-card-meta">
                            {fields.slice(1).map(f => item[f] != null && (
                                <span key={f} className="adm-card-badge">{f}: {item[f]}</span>
                            ))}
                        </div>
                        {item.description && <p className="adm-card-desc">{item.description}</p>}
                    </div>
                    {!readOnly && (
                        <div className="adm-card-actions">
                            <button className="adm-btn adm-btn-sm adm-btn-ghost" onClick={() => onEdit(item)}><Icon name="edit" size={14} /> Редагувати</button>
                            <button className="adm-btn adm-btn-sm adm-btn-danger-ghost" onClick={() => onDelete(item.id)}><Icon name="trash" size={14} /></button>
                        </div>
                    )}
                </div>
            ))}
        </div>
    );
}

// ─── Sidebar ──────────────────────────────────────────────────────────────────
const SECTIONS = [
    { key: 'ingredients', label: 'Інгредієнти', icon: 'ingredient' },
    { key: 'categories', label: 'Категорії', icon: 'category' },
    { key: 'products', label: 'Продукти', icon: 'product' },
    { key: 'recipes', label: 'Рецепти', icon: 'recipe' },
    { key: 'tags', label: 'Теги', icon: 'tag' },
    { key: 'achievements', label: 'Досягнення', icon: 'shield' },
    { key: 'mealplanner', label: 'Генератор раціону', icon: 'list' },
];

function AdminSidebar({ active, onChange }) {
    return (
        <aside className="adm-sidebar">
            <div className="adm-sidebar-brand">
                <Icon name="shield" size={22} />
                <span>Адмін панель</span>
            </div>
            <nav className="adm-sidebar-nav">
                {SECTIONS.map(s => (
                    <button key={s.key} className={`adm-nav-item ${active === s.key ? 'active' : ''}`} onClick={() => onChange(s.key)}>
                        <Icon name={s.icon} size={18} />
                        <span>{s.label}</span>
                    </button>
                ))}
            </nav>
        </aside>
    );
}

// ─── Root ─────────────────────────────────────────────────────────────────────
export default function AdminPanel() {
    const [section, setSection] = useState('ingredients');
    const { toasts, add: toast } = useToast();
    const navigate = useNavigate();

    const sectionTitle = SECTIONS.find(s => s.key === section)?.label;

    return (
        <div className="adm-root">
            <AdminSidebar active={section} onChange={setSection} />
            <main className="adm-main">
                <div className="adm-topbar">
                    <h2 className="adm-page-title">{sectionTitle}</h2>
                    <button className="adm-btn adm-btn-ghost adm-back-btn" onClick={() => navigate('/')}>
                        <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="15 18 9 12 15 6" /></svg>
                        На сайт
                    </button>
                </div>
                <div className="adm-content">
                    {section === 'ingredients' && <IngredientsSection toast={toast} />}
                    {section === 'categories' && <CategoriesSection toast={toast} />}
                    {section === 'products' && <ProductsSection toast={toast} />}
                    {section === 'recipes' && <RecipesSection toast={toast} />}
                    {section === 'tags' && <TagsSection toast={toast} />}
                    {section === 'achievements' && <AchievementsSection toast={toast} />}
                    {section === 'mealplanner' && <MealPlannerSection toast={toast} />}
                </div>
            </main>

            {/* Toasts */}
            <div className="adm-toasts">
                {toasts.map(t => (
                    <div key={t.id} className={`adm-toast adm-toast-${t.type}`}>{t.msg}</div>
                ))}
            </div>
        </div>
    );
}