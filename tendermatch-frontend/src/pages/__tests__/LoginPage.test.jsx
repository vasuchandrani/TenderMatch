import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { LoginPage } from '../LoginPage';
import * as AuthContextModule from '../../context/AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('LoginPage', () => {
  const mockLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      login: mockLogin,
      user: null,
      token: null,
      isAuthenticated: false,
    });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    );

  it('should render email and password inputs', () => {
    renderComponent();

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('should display brand title and link to registration', () => {
    renderComponent();

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/TenderMatch/i);
    expect(screen.getByRole('link', { name: /create an account/i })).toBeInTheDocument();
  });

  it('should call login API on valid submit and navigate to dashboard', async () => {
    mockLogin.mockResolvedValueOnce({ id: '1', email: 'buyer@example.com' });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'buyer@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'secret123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('buyer@example.com', 'secret123');
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('should show error message on invalid credentials', async () => {
    mockLogin.mockRejectedValueOnce(new Error('Invalid email or password'));
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'buyer@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'wrongPassword' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });
  });

  it('should show loading indicator while signing in', async () => {
    mockLogin.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 200)));
    renderComponent();

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'buyer@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'secret123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByText(/signing in\.\.\./i)).toBeInTheDocument();
  });
});
