import React, { createContext, useContext, useState, useCallback } from 'react';
import './ConfirmModal.css';

const ConfirmContext = createContext();

export const useConfirm = () => useContext(ConfirmContext);

export const ConfirmProvider = ({ children }) => {
  const [modalState, setModalState] = useState({
    isOpen: false,
    message: '',
    onConfirm: null,
    onCancel: null,
  });

  const confirm = useCallback((message) => {
    return new Promise((resolve) => {
      setModalState({
        isOpen: true,
        message,
        onConfirm: () => {
          setModalState(prev => ({ ...prev, isOpen: false }));
          resolve(true);
        },
        onCancel: () => {
          setModalState(prev => ({ ...prev, isOpen: false }));
          resolve(false);
        }
      });
    });
  }, []);

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {modalState.isOpen && (
        <div className="confirm-modal-overlay">
          <div className="confirm-modal-content">
            <div className="confirm-icon">❓</div>
            <h3 className="confirm-title">Підтвердження</h3>
            <p className="confirm-message">{modalState.message}</p>
            <div className="confirm-actions">
              <button className="btn-cancel" onClick={modalState.onCancel}>Скасувати</button>
              <button className="btn-submit" onClick={modalState.onConfirm}>Підтвердити</button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
};
