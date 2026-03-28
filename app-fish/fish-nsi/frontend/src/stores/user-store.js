import { defineStore } from "pinia";

// Вспомогательная функция для декодирования JWT (без сторонних библиотек)
function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

export const useUserStore = defineStore("user", {
  state: () => ({
    // Инициализируем состояние сразу из токена в localStorage
    user: {
      id: 0,
      name: "",
      target: "",
      metamodel: "",
    },

    initialized: false
  }),

  getters: {
    getUserId: (state) => state.user.id,
    isSysAdmin: (state) => parseInt(state.user.id, 10) === 1,
    getUserName: (state) => state.user.name,
    getTarget: (state) => state.user.target ? state.user.target.split(",") : [],
    getMetaModel: (state) => state.user.metamodel,
    isAuthenticated: (state) => !!state.user.id && state.user.id !== 0,
  },

  actions: {
    // Этот метод мы вызываем при загрузке
    setUser(data) {
      this.user = data // записываем объект в state.user
    },

    // Вызывается при старте приложения (например, в boot-файле)
    initFromToken() {
      const token = localStorage.getItem('fish_token');
      if (token) {
        const decoded = parseJwt(token);
        if (import.meta.env.DEV)
          console.log('Декодированный токен:', decoded);
        if (decoded) {
          this.user.id = decoded.attrs.id || 0;
          this.user.name = decoded.attrs.fullname || decoded.attrs.login || "";
          this.user.target = decoded.attrs.target || "";
          this.user.metamodel = decoded.attrs.metamodel || "";
        }
      }
      // ВАЖНО: Ставим true в самом конце,
      // даже если токена нет, чтобы приложение знало: "проверка завершена"
      this.initialized = true;
    },

    // Вызывается только ОДИН раз при успешном логине
    setUserStore(token) {
      if (import.meta.env.DEV) {
        console.log('Токен:', token); // <--- ВАЖНО: посмотрите, это строка или объект?
      }

      if (token && typeof token === 'string') {
        localStorage.setItem('fish_token', token);
        this.initFromToken();
        if (import.meta.env.DEV)
          console.log('Данные извлечены:', this.user);
      } else {
        if (import.meta.env.DEV)
          console.error('ОШИБКА: Сюда должен приходить только ТОКЕН (строка), а пришло:', typeof token);
      }
    },

    // Глобальная очистка (для logout и ошибок 401)
    clearUserStore() {
      localStorage.removeItem('fish_token');
      this.user = { id: 0, name: "", target: "", metamodel: "" };
    },
  },
});
