import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import { Sparkles, AlertCircle, X, ArrowRight, Trophy } from 'lucide-react';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);
  const activeTrackedTendersRef = useRef(new Map()); // tenderId -> previous status
  const navigate = useNavigate();

  const removeNotification = useCallback((id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  }, []);

  const addNotification = useCallback(({ title, message, type = 'info', link, linkText, duration = 8000 }) => {
    const id = Date.now() + Math.random().toString(36).substr(2, 9);
    const notif = { id, title, message, type, link, linkText };
    setNotifications((prev) => [notif, ...prev.slice(0, 4)]);

    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id);
      }, duration);
    }
  }, [removeNotification]);

  // Track an in-flight tender explicitly
  const trackTender = useCallback((tenderId, initialStatus = 'PROCESSING') => {
    if (!tenderId) return;
    activeTrackedTendersRef.current.set(tenderId, initialStatus);
  }, []);

  // Background poller: Checks tenders list every 4 seconds for completion or failure
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const interval = setInterval(async () => {
      try {
        const res = await tendersApi.list(0, 10);
        if (res?.success && Array.isArray(res.data?.content)) {
          const tenders = res.data.content;

          tenders.forEach((tender) => {
            const prevStatus = activeTrackedTendersRef.current.get(tender.id);

            // If we are tracking this tender or it was previously in progress
            if (prevStatus && (prevStatus === 'PROCESSING' || prevStatus === 'MATCHING' || prevStatus === 'EXTRACTED')) {
              if (tender.status === 'COMPLETED' || tender.status === 'SCORED') {
                activeTrackedTendersRef.current.delete(tender.id);
                addNotification({
                  title: '🎉 Pipeline Matching Complete!',
                  message: `Tender "${tender.name}" has finished 100% AI document extraction & matching.`,
                  type: 'success',
                  link: `/tenders/${tender.id}/review`,
                  linkText: 'Open Review Workspace',
                  duration: 12000,
                });
              } else if (tender.status === 'FAILED') {
                activeTrackedTendersRef.current.delete(tender.id);
                addNotification({
                  title: '⚠️ Pipeline Processing Failed',
                  message: `Tender "${tender.name}" encountered an error during pipeline execution.`,
                  type: 'error',
                  link: `/tenders/${tender.id}`,
                  linkText: 'View Tender Workspace',
                  duration: 12000,
                });
              } else if (tender.status === 'PROCESSING' || tender.status === 'MATCHING' || tender.status === 'EXTRACTED') {
                // Keep tracking as in-flight
                activeTrackedTendersRef.current.set(tender.id, tender.status);
              }
            } else if (tender.status === 'PROCESSING' || tender.status === 'MATCHING' || tender.status === 'EXTRACTED') {
              // Auto-track any in-progress tenders found on the server
              activeTrackedTendersRef.current.set(tender.id, tender.status);
            }
          });
        }
      } catch (_) {
        // Silently catch polling issues if offline
      }
    }, 4000);

    return () => clearInterval(interval);
  }, [addNotification]);

  return (
    <NotificationContext.Provider value={{ notifications, addNotification, removeNotification, trackTender }}>
      {children}

      {/* Global In-App Notification Toast Overlay */}
      {notifications.length > 0 && (
        <div
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            zIndex: 99999,
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            maxWidth: '440px',
            width: 'calc(100vw - 48px)',
            pointerEvents: 'none',
          }}
        >
          {notifications.map((n) => {
            const isSuccess = n.type === 'success';
            const isError = n.type === 'error';

            return (
              <div
                key={n.id}
                style={{
                  pointerEvents: 'auto',
                  backgroundColor: '#ffffff',
                  borderRadius: '14px',
                  border: `1.5px solid ${isSuccess ? '#10b981' : isError ? '#ef4444' : 'var(--border-color)'}`,
                  boxShadow: '0 10px 30px rgba(0, 0, 0, 0.15)',
                  padding: '1rem 1.25rem',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '12px',
                  animation: 'slideInRight 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                }}
              >
                <div
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: '10px',
                    backgroundColor: isSuccess ? 'rgba(16, 185, 129, 0.12)' : isError ? 'rgba(239, 68, 68, 0.12)' : 'rgba(59, 130, 246, 0.12)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    color: isSuccess ? '#10b981' : isError ? '#ef4444' : '#2563eb',
                  }}
                >
                  {isSuccess ? <Sparkles size={18} /> : isError ? <AlertCircle size={18} /> : <Trophy size={18} />}
                </div>

                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 800, fontSize: '0.92rem', color: 'var(--text-primary)', marginBottom: '2px' }}>
                    {n.title}
                  </div>
                  <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.4, marginBottom: n.link ? '8px' : '0' }}>
                    {n.message}
                  </div>

                  {n.link && (
                    <button
                      type="button"
                      onClick={() => {
                        removeNotification(n.id);
                        navigate(n.link);
                      }}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        padding: '5px 12px',
                        borderRadius: '7px',
                        backgroundColor: isSuccess ? '#10b981' : 'var(--accent-primary)',
                        color: '#ffffff',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        border: 'none',
                        cursor: 'pointer',
                        transition: 'opacity 0.15s ease',
                      }}
                    >
                      <span>{n.linkText || 'View'}</span>
                      <ArrowRight size={13} />
                    </button>
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => removeNotification(n.id)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    padding: '2px',
                    display: 'flex',
                  }}
                  title="Dismiss"
                >
                  <X size={16} />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    return {
      notifications: [],
      addNotification: () => {},
      removeNotification: () => {},
      trackTender: () => {},
    };
  }
  return context;
};
