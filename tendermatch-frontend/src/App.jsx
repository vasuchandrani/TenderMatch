import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Navbar } from './components/Navbar';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { NewTenderPage } from './pages/NewTenderPage';
import { TenderDetailPage } from './pages/TenderDetailPage';
import { ReviewPage } from './pages/ReviewPage';
import { ReviewSummaryPage } from './pages/ReviewSummaryPage';
import { ScoreDashboardPage } from './pages/ScoreDashboardPage';
import { VendorScoreDetailPage } from './pages/VendorScoreDetailPage';
import { ComparisonMatrixPage } from './pages/ComparisonMatrixPage';
import { WeightConfigPage } from './pages/WeightConfigPage';
import { AdminModelDashboard } from './pages/AdminModelDashboard';

function App() {
  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
          <div className="app-container">
            <Navbar />
            <Routes>
              {/* Public Routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Protected Application Routes */}
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/tenders/new" element={<NewTenderPage />} />
                <Route path="/tenders/:id" element={<TenderDetailPage />} />
                <Route path="/tenders/:id/review" element={<ReviewPage />} />
                <Route path="/tenders/:id/vendors/:vendorId/review" element={<ReviewPage />} />
                <Route path="/tenders/:id/vendors/:vendorId/review-summary" element={<ReviewSummaryPage />} />
                <Route path="/tenders/:id/scores" element={<ScoreDashboardPage />} />
                <Route path="/tenders/:id/scores/:vendorId" element={<VendorScoreDetailPage />} />
                <Route path="/tenders/:id/matrix" element={<ComparisonMatrixPage />} />
                <Route path="/tenders/:id/weights" element={<WeightConfigPage />} />
                <Route path="/admin/models" element={<AdminModelDashboard />} />
              </Route>

              {/* Default Catch-All Redirect */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </div>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
