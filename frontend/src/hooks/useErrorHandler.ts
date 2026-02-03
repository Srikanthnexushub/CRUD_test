import { useState, useCallback } from 'react';
import { ApiError } from '../types';

interface UseErrorHandlerReturn {
  error: ApiError | null;
  setError: (error: ApiError | Error | string) => void;
  clearError: () => void;
  handleError: (error: unknown) => void;
}

export const useErrorHandler = (): UseErrorHandlerReturn => {
  const [error, setErrorState] = useState<ApiError | null>(null);

  const setError = useCallback((error: ApiError | Error | string) => {
    if (typeof error === 'string') {
      setErrorState({ message: error });
    } else if (error instanceof Error) {
      setErrorState({ message: error.message });
    } else {
      setErrorState(error);
    }
  }, []);

  const clearError = useCallback(() => {
    setErrorState(null);
  }, []);

  const handleError = useCallback(
    (error: unknown) => {
      console.error('Error occurred:', error);

      if (error && typeof error === 'object' && 'message' in error) {
        setError(error as ApiError);
      } else if (typeof error === 'string') {
        setError(error);
      } else {
        setError('An unexpected error occurred');
      }
    },
    [setError]
  );

  return {
    error,
    setError,
    clearError,
    handleError,
  };
};
