import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null, // Сюда упадут данные из au.getAttrs()
  }),
  actions: {
    // Метод для записи данных пользователя в память
    setUser(data) {
      this.user = data;
    },
    logout() {
      this.user = null;
      localStorage.removeItem('fish_token');
    }
  }
});
