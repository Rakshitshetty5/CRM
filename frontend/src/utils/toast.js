// Reusable Toast Notification Manager
class ToastManager {
  constructor() {
    this.listeners = new Set();
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  notify(message, type = 'info') {
    if (!message) return;
    const toastItem = {
      id: Date.now() + Math.random(),
      message,
      type,
    };
    this.listeners.forEach((listener) => listener(toastItem));
  }

  success(message) {
    this.notify(message, 'success');
  }

  error(message) {
    this.notify(message, 'error');
  }

  info(message) {
    this.notify(message, 'info');
  }
}

export const toast = new ToastManager();
