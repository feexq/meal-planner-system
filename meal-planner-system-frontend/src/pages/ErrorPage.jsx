import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import './HomePage.css';

export default function ErrorPage({ code, message }) {
  const navigate = useNavigate();

  return (
    <>
      <Navbar />
      <main className="section bg-white" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="container" style={{ textAlign: 'center' }}>
          <h1 style={{ fontSize: '80px', color: 'var(--primary)', marginBottom: '20px' }}>{code}</h1>
          <h2 style={{ fontSize: '24px', color: 'var(--text)', marginBottom: '30px' }}>{message}</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '40px' }}>
            На жаль, такої сторінки не існує, або у вас немає до неї доступу.
          </p>
          <button 
            type="button" 
            className="btn-submit" 
            style={{ display: 'inline-block', width: 'auto', padding: '12px 32px' }}
            onClick={() => navigate('/')}
          >
            Повернутися на Головну
          </button>
        </div>
      </main>
    </>
  );
}
