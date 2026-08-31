import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle 401 token expiration and refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');

      if (refreshToken) {
        try {
          const res = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
            refreshToken,
          });

          if (res.data?.success && res.data?.data?.accessToken) {
            const { accessToken, refreshToken: newRefreshToken } = res.data.data;
            localStorage.setItem('accessToken', accessToken);
            if (newRefreshToken) {
              localStorage.setItem('refreshToken', newRefreshToken);
            }
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          }
        } catch (refreshErr) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
        }
      } else {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    return res.data;
  },
  register: async (name, email, password) => {
    const res = await api.post('/auth/register', { name, email, password });
    return res.data;
  },
  getMe: async () => {
    const res = await api.get('/auth/me');
    return res.data;
  },
};

export const tendersApi = {
  list: async (page = 0, size = 20) => {
    const res = await api.get(`/tenders?page=${page}&size=${size}`);
    return res.data;
  },
  getById: async (id) => {
    const res = await api.get(`/tenders/${id}`);
    return res.data;
  },
  create: async (name, description) => {
    const res = await api.post('/tenders', { name, description });
    return res.data;
  },
  uploadRequirement: async (id, file, onUploadProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/tenders/${id}/requirement`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    });
    return res.data;
  },
  uploadVendors: async (id, files, onUploadProgress) => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });
    const res = await api.post(`/tenders/${id}/vendors`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    });
    return res.data;
  },
  triggerMatch: async (id) => {
    const res = await api.post(`/tenders/${id}/match`);
    return res.data;
  },
  triggerProcess: async (id) => {
    const res = await api.post(`/tenders/${id}/process`);
    return res.data;
  },
  getProcessingStatus: async (id) => {
    const res = await api.get(`/tenders/${id}/processing-status`);
    return res.data;
  },
  getProducts: async (id) => {
    const res = await api.get(`/tenders/${id}/products`);
    return res.data;
  },
  getVendorProducts: async (id) => {
    const res = await api.get(`/tenders/${id}/vendor-products`);
    return res.data;
  },
  getVendorSubmissionProducts: async (tenderId, vendorId) => {
    const res = await api.get(`/tenders/${tenderId}/vendors/${vendorId}/products`);
    return res.data;
  },
  getProductSpecs: async (tenderId, productId) => {
    const res = await api.get(`/tenders/${tenderId}/products/${productId}/specs`);
    return res.data;
  },
  getMatchStatus: async (id) => {
    const res = await api.get(`/tenders/${id}/match-status`);
    return res.data;
  },
  getMatchResults: async (id) => {
    const res = await api.get(`/tenders/${id}/results`);
    return res.data;
  },
  getVendorResults: async (tenderId, vendorId) => {
    const res = await api.get(`/tenders/${tenderId}/vendors/${vendorId}/results`);
    return res.data;
  },
  getMatchSummary: async (id) => {
    const res = await api.get(`/tenders/${id}/results/summary`);
    return res.data;
  },
  getTenderReview: async (id) => {
    const res = await api.get(`/tenders/${id}/review`);
    return res.data;
  },
  getVendorReview: async (tenderId, vendorId) => {
    const res = await api.get(`/tenders/${tenderId}/vendors/${vendorId}/review`);
    return res.data;
  },
  getVendorReviewProgress: async (tenderId, vendorId) => {
    const res = await api.get(`/tenders/${tenderId}/vendors/${vendorId}/review/progress`);
    return res.data;
  },
  submitVerdict: async (tenderId, resultId, humanVerdict, comment) => {
    const res = await api.put(`/tenders/${tenderId}/results/${resultId}/verdict`, {
      humanVerdict,
      comment,
    });
    return res.data;
  },
  bulkConfirmReview: async (tenderId, vendorId) => {
    const res = await api.post(`/tenders/${tenderId}/vendors/${vendorId}/bulk-confirm`);
    return res.data;
  },
  getReviewSummaryReport: async (tenderId, vendorId) => {
    const res = await api.get(`/tenders/${tenderId}/vendors/${vendorId}/review-summary`);
    return res.data;
  },
  finalizeReview: async (tenderId, vendorId) => {
    const res = await api.post(`/tenders/${tenderId}/vendors/${vendorId}/finalize`);
    return res.data;
  },
  reverifyReflection: async (tenderId, vendorId) => {
    const res = await api.post(`/tenders/${tenderId}/vendors/${vendorId}/reverify-reflection`);
    return res.data;
  },
  reverifyAllReflection: async (tenderId) => {
    const res = await api.post(`/tenders/${tenderId}/reverify-all-reflection`);
    return res.data;
  },
  calculateScores: async (id) => {

    const res = await api.post(`/tenders/${id}/calculate-scores`);
    return res.data;
  },
  getScores: async (id) => {
    const res = await api.get(`/tenders/${id}/scores`);
    return res.data;
  },
  getVendorScore: async (id, vendorId) => {
    const res = await api.get(`/tenders/${id}/scores/${vendorId}`);
    return res.data;
  },
  getComparisonMatrix: async (id) => {
    const res = await api.get(`/tenders/${id}/comparison-matrix`);
    return res.data;
  },
  getWeights: async (id) => {
    const res = await api.get(`/tenders/${id}/weights`);
    return res.data;
  },
  updateWeights: async (id, weights) => {
    const res = await api.put(`/tenders/${id}/weights`, { weights });
    return res.data;
  },
  saveWeightTemplate: async (id, templateName, weights) => {
    const res = await api.post(`/tenders/${id}/weights/template`, { templateName, weights });
    return res.data;
  },
  getWeightTemplates: async () => {
    const res = await api.get('/weights/templates');
    return res.data;
  },
  applyWeightTemplate: async (id, templateName) => {
    const res = await api.post(`/tenders/${id}/weights/from-template/${templateName}`);
    return res.data;
  },
  exportCsv: async (id) => {
    const res = await api.get(`/tenders/${id}/export/csv`, { responseType: 'blob' });
    return res.data;
  },
  exportPdf: async (id) => {
    const res = await api.get(`/tenders/${id}/export/pdf`, { responseType: 'blob' });
    return res.data;
  },
  recordOutcome: async (id, winningVendorId, outcomeNotes) => {
    const res = await api.post(`/tenders/${id}/outcome`, { winningVendorId, outcomeNotes });
    return res.data;
  },
  getOutcome: async (id) => {
    const res = await api.get(`/tenders/${id}/outcome`);
    return res.data;
  },
  delete: async (id) => {
    const res = await api.delete(`/tenders/${id}`);
    return res.data;
  },
};

export const adminApi = {
  getModels: async () => {
    const res = await api.get('/admin/models');
    return res.data;
  },
  getModelVersions: async (modelName) => {
    const res = await api.get(`/admin/models/${modelName}/versions`);
    return res.data;
  },
  getTrainingDataStats: async () => {
    const res = await api.get('/admin/training-data-stats');
    return res.data;
  },
  promoteModel: async (modelName, version) => {
    const res = await api.post(`/admin/models/${modelName}/${version}/promote`);
    return res.data;
  },
  rollbackModel: async (modelName, version) => {
    const res = await api.post(`/admin/models/${modelName}/${version}/rollback`);
    return res.data;
  },
  getShadowStats: async (modelName) => {
    const res = await api.get(`/admin/models/${modelName}/shadow-stats`);
    return res.data;
  },
  checkRetrain: async () => {
    const res = await api.get('/admin/retrain-check');
    return res.data;
  },
  triggerRetrain: async (modelName) => {
    const res = await api.post(`/admin/retrain/${modelName}`);
    return res.data;
  },
};

export default api;
