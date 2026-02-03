import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { useAuthStore } from './stores';
import ErrorBoundary from './components/ErrorBoundary';
import SkipLink from './components/SkipLink';
import RegistrationForm from './components/RegistrationForm';
import LoginForm from './components/LoginForm';
import UserDashboard from './components/UserDashboard';
import ProtectedRoute from './components/ProtectedRoute';
import './App.css';

const App: React.FC = () => {
  const initializeAuth = useAuthStore((state) => state.initializeAuth);

  useEffect(() => {
    // Initialize auth on app mount
    initializeAuth();
  }, [initializeAuth]);

  return (
    <ErrorBoundary>
      <BrowserRouter>
        <SkipLink href="#main-content">Skip to main content</SkipLink>
        <ErrorBoundary
          fallback={
            <div style={{ padding: '2rem', textAlign: 'center' }} role="alert">
              <h2>Navigation Error</h2>
              <p>There was an error loading the page.</p>
              <button onClick={() => window.location.reload()}>Reload</button>
            </div>
          }
        >
          <Routes>
            <Route path="/login" element={<LoginForm />} />
            <Route path="/register" element={<RegistrationForm />} />
            <Route
              path="/dashboard"
              element={
                <ErrorBoundary>
                  <ProtectedRoute>
                    <UserDashboard />
                  </ProtectedRoute>
                </ErrorBoundary>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </ErrorBoundary>
        <ToastContainer
          position="top-right"
          autoClose={5000}
          hideProgressBar={false}
          newestOnTop
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="light"
          role="region"
          aria-label="Notifications"
        />
      </BrowserRouter>
    </ErrorBoundary>
  );
};

export default App;
