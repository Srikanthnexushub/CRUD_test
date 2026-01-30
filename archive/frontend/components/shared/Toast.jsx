import React from 'react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './Toast.css';

/**
 * Toast notification component wrapper with custom styling
 * Integrates with react-toastify
 *
 * @component
 * @returns {JSX.Element} ToastContainer component
 *
 * @example
 * // Import in App.jsx
 * import Toast from './components/shared/Toast';
 *
 * function App() {
 *   return (
 *     <>
 *       <Toast />
 *       {// rest of app}
 *     </>
 *   );
 * }
 *
 * // Use anywhere in your app
 * import { showToast } from './components/shared/Toast';
 *
 * showToast.success('Operation successful!');
 * showToast.error('Something went wrong');
 * showToast.warning('Please be careful');
 * showToast.info('Here is some information');
 */
const Toast = () => {
  return (
    <ToastContainer
      position="top-right"
      autoClose={4000}
      hideProgressBar={false}
      newestOnTop={true}
      closeOnClick
      rtl={false}
      pauseOnFocusLoss
      draggable
      pauseOnHover
      theme="light"
      className="custom-toast-container"
      toastClassName="custom-toast"
      bodyClassName="custom-toast-body"
      progressClassName="custom-toast-progress"
    />
  );
};

/**
 * Toast notification helper functions
 *
 * @typedef {Object} ToastOptions
 * @property {number} [autoClose=4000] - Auto close duration in ms
 * @property {boolean} [hideProgressBar=false] - Hide progress bar
 * @property {('top-left'|'top-right'|'top-center'|'bottom-left'|'bottom-right'|'bottom-center')} [position='top-right'] - Toast position
 * @property {boolean} [closeOnClick=true] - Close on click
 * @property {boolean} [pauseOnHover=true] - Pause auto close on hover
 * @property {boolean} [draggable=true] - Allow dragging to dismiss
 */

/**
 * Show success toast
 * @param {string} message - Toast message
 * @param {ToastOptions} [options] - Toast options
 */
const success = (message, options = {}) => {
  toast.success(message, {
    className: 'custom-toast custom-toast--success',
    ...options
  });
};

/**
 * Show error toast
 * @param {string} message - Toast message
 * @param {ToastOptions} [options] - Toast options
 */
const error = (message, options = {}) => {
  toast.error(message, {
    className: 'custom-toast custom-toast--error',
    ...options
  });
};

/**
 * Show warning toast
 * @param {string} message - Toast message
 * @param {ToastOptions} [options] - Toast options
 */
const warning = (message, options = {}) => {
  toast.warning(message, {
    className: 'custom-toast custom-toast--warning',
    ...options
  });
};

/**
 * Show info toast
 * @param {string} message - Toast message
 * @param {ToastOptions} [options] - Toast options
 */
const info = (message, options = {}) => {
  toast.info(message, {
    className: 'custom-toast custom-toast--info',
    ...options
  });
};

/**
 * Show loading toast
 * @param {string} message - Toast message
 * @returns {number|string} Toast ID for updating
 */
const loading = (message) => {
  return toast.loading(message, {
    className: 'custom-toast custom-toast--loading'
  });
};

/**
 * Update existing toast
 * @param {number|string} toastId - Toast ID to update
 * @param {Object} options - Update options
 * @param {string} options.render - New message
 * @param {('success'|'error'|'warning'|'info'|'default')} options.type - New type
 * @param {boolean} options.isLoading - Loading state
 */
const update = (toastId, options) => {
  toast.update(toastId, options);
};

/**
 * Dismiss toast
 * @param {number|string} [toastId] - Toast ID to dismiss (omit to dismiss all)
 */
const dismiss = (toastId) => {
  if (toastId) {
    toast.dismiss(toastId);
  } else {
    toast.dismiss();
  }
};

export const showToast = {
  success,
  error,
  warning,
  info,
  loading,
  update,
  dismiss
};

export default Toast;
