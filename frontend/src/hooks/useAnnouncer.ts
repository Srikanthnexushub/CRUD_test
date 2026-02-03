import { useEffect, useRef, useCallback } from 'react';

type AnnouncementPriority = 'polite' | 'assertive';

export const useAnnouncer = () => {
  const politeAnnouncerRef = useRef<HTMLDivElement | null>(null);
  const assertiveAnnouncerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // Create polite announcer
    if (!politeAnnouncerRef.current) {
      const politeDiv = document.createElement('div');
      politeDiv.setAttribute('role', 'status');
      politeDiv.setAttribute('aria-live', 'polite');
      politeDiv.setAttribute('aria-atomic', 'true');
      politeDiv.className = 'sr-only';
      document.body.appendChild(politeDiv);
      politeAnnouncerRef.current = politeDiv;
    }

    // Create assertive announcer
    if (!assertiveAnnouncerRef.current) {
      const assertiveDiv = document.createElement('div');
      assertiveDiv.setAttribute('role', 'alert');
      assertiveDiv.setAttribute('aria-live', 'assertive');
      assertiveDiv.setAttribute('aria-atomic', 'true');
      assertiveDiv.className = 'sr-only';
      document.body.appendChild(assertiveDiv);
      assertiveAnnouncerRef.current = assertiveDiv;
    }

    // Cleanup
    return () => {
      if (politeAnnouncerRef.current) {
        document.body.removeChild(politeAnnouncerRef.current);
        politeAnnouncerRef.current = null;
      }
      if (assertiveAnnouncerRef.current) {
        document.body.removeChild(assertiveAnnouncerRef.current);
        assertiveAnnouncerRef.current = null;
      }
    };
  }, []);

  const announce = useCallback((message: string, priority: AnnouncementPriority = 'polite') => {
    const announcer = priority === 'assertive'
      ? assertiveAnnouncerRef.current
      : politeAnnouncerRef.current;

    if (announcer) {
      // Clear previous message
      announcer.textContent = '';

      // Set new message after a brief delay to ensure screen reader picks it up
      setTimeout(() => {
        announcer.textContent = message;
      }, 100);
    }
  }, []);

  return { announce };
};
