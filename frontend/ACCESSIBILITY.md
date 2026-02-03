# Accessibility Features

This application implements **WCAG 2.1 Level AA** compliance standards to ensure accessibility for all users.

## Table of Contents

- [Overview](#overview)
- [Keyboard Navigation](#keyboard-navigation)
- [Screen Reader Support](#screen-reader-support)
- [Focus Management](#focus-management)
- [Color and Contrast](#color-and-contrast)
- [Testing](#testing)
- [Components](#components)

## Overview

The application follows Web Content Accessibility Guidelines (WCAG) 2.1 Level AA to provide:

- **Perceivable**: Content is available to all senses
- **Operable**: Interface works with keyboard, mouse, and assistive technologies
- **Understandable**: Content and operation are clear and predictable
- **Robust**: Works across different browsers and assistive technologies

## Keyboard Navigation

### Global Shortcuts

- **Escape**: Close modals and dialogs
- **Tab**: Navigate forward through interactive elements
- **Shift+Tab**: Navigate backward through interactive elements

### Skip Links

A "Skip to main content" link appears at the top of each page when focused, allowing keyboard users to bypass navigation and jump directly to the main content.

```tsx
<SkipLink href="#main-content">Skip to main content</SkipLink>
```

All main content areas have `id="main-content"` for skip link targets.

### Focus Traps

Modals and dialogs implement focus trapping to ensure keyboard navigation stays within the modal until it's closed:

- Focus automatically moves to the modal when opened
- Tab cycles through modal elements only
- Escape key closes the modal
- Focus returns to the trigger element when closed

## Screen Reader Support

### ARIA Labels and Roles

All interactive elements have appropriate ARIA attributes:

```tsx
// Buttons
<button aria-label="Edit John Doe">Edit</button>

// Forms
<input
  aria-required="true"
  aria-invalid={hasError}
  aria-describedby="input-hint input-error"
/>

// Status messages
<span role="status">Loading...</span>
<div role="alert">Error occurred</div>
```

### Live Regions

Dynamic content updates are announced via ARIA live regions:

- **Polite**: Non-urgent updates (success messages, data loaded)
- **Assertive**: Urgent updates (errors, warnings)

```tsx
const { announce } = useAnnouncer();

// Polite announcement
announce('User updated successfully', 'polite');

// Urgent announcement
announce('Failed to save changes', 'assertive');
```

### Semantic HTML

The application uses semantic HTML5 elements:

- `<header role="banner">`: Page headers
- `<main role="main">`: Main content
- `<footer role="contentinfo">`: Page footers
- `<nav role="navigation">`: Navigation sections
- `<article>`: Self-contained content (user cards)

## Focus Management

### Auto-Focus

Forms and modals automatically focus the first interactive element using `useFocusManagement`:

```tsx
const inputRef = useFocusManagement<HTMLInputElement>({ autoFocus: true });
```

### Focus Restoration

When modals close, focus returns to the element that opened them:

```tsx
const elementRef = useFocusManagement({
  autoFocus: true,
  restoreFocus: true
});
```

### Visible Focus Indicators

All interactive elements have clear focus indicators:

- **3px solid outline** with contrasting color
- **2px offset** for visibility
- **Box shadow** for additional emphasis
- Enhanced in high contrast mode

CSS:

```css
*:focus-visible {
  outline: 3px solid #667eea;
  outline-offset: 2px;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}
```

## Color and Contrast

### Contrast Ratios

All text meets WCAG AA contrast requirements:

- **Normal text**: Minimum 4.5:1 contrast ratio
- **Large text** (18pt+): Minimum 3:1 contrast ratio
- **Interactive elements**: Minimum 3:1 contrast ratio

### Color Independence

Information is never conveyed by color alone:

- Error states use red color + icon + text
- Success states use green color + icon + text
- Required fields use asterisk + aria-required attribute
- Role badges use color + text label

### High Contrast Mode

The application respects system high contrast preferences:

```css
@media (prefers-contrast: high) {
  * {
    border-width: 2px;
  }
  *:focus-visible {
    outline-width: 4px;
  }
}
```

## Reduced Motion

Respects user preference for reduced motion:

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

## Touch Targets

All interactive elements meet minimum touch target size (WCAG 2.5.5):

- **Minimum size**: 44x44 pixels
- Applied automatically on touch devices

```css
@media (pointer: coarse) {
  button, a, input[type="button"] {
    min-height: 44px;
    min-width: 44px;
  }
}
```

## Components

### Accessibility Components

#### SkipLink

Allows keyboard users to skip navigation:

```tsx
<SkipLink href="#main-content">Skip to main content</SkipLink>
```

#### FocusTrap

Traps focus within modals:

```tsx
<FocusTrap onEscape={closeModal}>
  <div className="modal-content">
    {/* Modal content */}
  </div>
</FocusTrap>
```

### Accessibility Hooks

#### useFocusManagement

Manages focus for interactive elements:

```tsx
const elementRef = useFocusManagement<HTMLInputElement>({
  autoFocus: true,
  restoreFocus: true
});
```

#### useAnnouncer

Creates screen reader announcements:

```tsx
const { announce } = useAnnouncer();
announce('Operation completed', 'polite');
```

#### useKeyboardShortcuts

Adds keyboard shortcuts:

```tsx
useKeyboardShortcuts([
  {
    key: 'escape',
    handler: () => closeModal(),
    description: 'Close modal'
  }
]);
```

## Testing

### Manual Testing

1. **Keyboard Navigation**
   - Navigate entire app using only keyboard
   - Verify all interactive elements are reachable
   - Check focus indicators are visible
   - Test skip links

2. **Screen Reader Testing**
   - macOS: VoiceOver (Cmd+F5)
   - Windows: NVDA or JAWS
   - Test all pages and interactive flows
   - Verify announcements are clear

3. **Browser Zoom**
   - Test at 200% zoom level
   - Verify content remains readable
   - Check layout doesn't break

4. **High Contrast Mode**
   - Enable system high contrast
   - Verify all content is visible
   - Check focus indicators stand out

### Automated Testing

Use accessibility testing tools:

- **axe DevTools**: Browser extension for automated checks
- **WAVE**: Web accessibility evaluation tool
- **Lighthouse**: Chrome DevTools accessibility audit

```bash
# Install axe-core for automated testing
npm install --save-dev @axe-core/react

# Run Lighthouse
npx lighthouse http://localhost:5173 --view
```

### Testing Checklist

- [ ] All images have alt text
- [ ] Forms have proper labels
- [ ] Focus is visible on all interactive elements
- [ ] Keyboard navigation works throughout
- [ ] Screen reader announces dynamic content
- [ ] Color contrast meets WCAG AA
- [ ] No keyboard traps (except intentional focus traps)
- [ ] Skip links function correctly
- [ ] Error messages are associated with form fields
- [ ] Modal focus is trapped and restored
- [ ] All functionality available via keyboard

## Implementation Details

### Form Accessibility

```tsx
<form aria-busy={loading}>
  <label htmlFor="username">
    Username <span className="required" aria-label="required">*</span>
  </label>
  <input
    id="username"
    type="text"
    required
    aria-required="true"
    aria-invalid={!!errors.username}
    aria-describedby="username-hint username-error"
  />
  <span id="username-hint" className="hint">
    3-50 characters, letters, numbers, and underscores only
  </span>
  {errors.username && (
    <span id="username-error" className="error-message" role="alert">
      {errors.username}
    </span>
  )}
</form>
```

### Modal Accessibility

```tsx
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="modal-title"
  aria-describedby="modal-description"
>
  <FocusTrap onEscape={closeModal}>
    <h2 id="modal-title">Edit User</h2>
    <p id="modal-description">Update user information below</p>
    {/* Modal content */}
  </FocusTrap>
</div>
```

### Loading States

```tsx
<div aria-busy={loading}>
  {loading ? (
    <SkeletonLoader type="card" count={6} />
  ) : (
    <div role="list" aria-label="User list">
      {users.map(user => (
        <article key={user.id} role="listitem">
          {/* User content */}
        </article>
      ))}
    </div>
  )}
</div>
```

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [ARIA Authoring Practices](https://www.w3.org/WAI/ARIA/apg/)
- [WebAIM Resources](https://webaim.org/resources/)
- [MDN Accessibility](https://developer.mozilla.org/en-US/docs/Web/Accessibility)

## Continuous Improvement

Accessibility is an ongoing effort. Regular testing and user feedback help identify areas for improvement. Consider:

- User testing with people who use assistive technologies
- Regular automated accessibility audits
- Staying updated with WCAG guidelines
- Monitoring browser and assistive technology updates
