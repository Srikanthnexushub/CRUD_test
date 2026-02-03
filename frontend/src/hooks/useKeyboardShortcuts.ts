import { useEffect } from 'react';

type KeyCombo = string; // e.g., 'ctrl+k', 'alt+s', 'escape'
type Handler = (event: KeyboardEvent) => void;

interface KeyboardShortcut {
  key: KeyCombo;
  handler: Handler;
  description?: string;
}

export const useKeyboardShortcuts = (shortcuts: KeyboardShortcut[]) => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      for (const shortcut of shortcuts) {
        const keys = shortcut.key.toLowerCase().split('+');
        const key = keys[keys.length - 1];
        const requiresCtrl = keys.includes('ctrl') || keys.includes('control');
        const requiresAlt = keys.includes('alt');
        const requiresShift = keys.includes('shift');
        const requiresMeta = keys.includes('meta') || keys.includes('cmd');

        const ctrlMatch = requiresCtrl ? event.ctrlKey || event.metaKey : !event.ctrlKey && !event.metaKey;
        const altMatch = requiresAlt ? event.altKey : !event.altKey;
        const shiftMatch = requiresShift ? event.shiftKey : !event.shiftKey;
        const metaMatch = requiresMeta ? event.metaKey : !event.metaKey;

        const keyMatch = event.key.toLowerCase() === key;

        if (keyMatch && ctrlMatch && altMatch && shiftMatch && metaMatch) {
          event.preventDefault();
          shortcut.handler(event);
          break;
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [shortcuts]);
};
