import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../AuthContext';
import { authApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    getMe: vi.fn(),
  },
}));

describe('useAuth & AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>;

  it('should initialize with unauthenticated state when localStorage is empty', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should authenticate user and store tokens on login', async () => {
    authApi.login.mockResolvedValueOnce({
      success: true,
      data: {
        accessToken: 'mock_access_token',
        refreshToken: 'mock_refresh_token',
        user: { id: 'u1', email: 'buyer@test.gov', name: 'Buyer One' },
      },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await result.current.login('buyer@test.gov', 'password123');
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.token).toBe('mock_access_token');
    expect(result.current.user?.email).toBe('buyer@test.gov');
    expect(localStorage.getItem('accessToken')).toBe('mock_access_token');
  });

  it('should clear user state and localStorage on logout', async () => {
    localStorage.setItem('accessToken', 'initial_token');
    localStorage.setItem('user', JSON.stringify({ email: 'user@test.com' }));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
  });
});
