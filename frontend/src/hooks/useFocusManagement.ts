import { useEffect, useRef } from 'react';

interface UseFocusManagementOptions {
  autoFocus?: boolean;
  restoreFocus?: boolean;
}

export const useFocusManagement = <T extends HTMLElement>(
  options: UseFocusManagementOptions = {}
) => {
  const { autoFocus = true, restoreFocus = true } = options;
  const elementRef = useRef<T>(null);
  const previouslyFocusedRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    // Store previously focused element
    if (restoreFocus) {
      previouslyFocusedRef.current = document.activeElement as HTMLElement;
    }

    // Auto-focus the element
    if (autoFocus && elementRef.current) {
      elementRef.current.focus();
    }

    // Restore focus on cleanup
    return () => {
      if (restoreFocus && previouslyFocusedRef.current) {
        previouslyFocusedRef.current.focus();
      }
    };
  }, [autoFocus, restoreFocus]);

  return elementRef;
};
