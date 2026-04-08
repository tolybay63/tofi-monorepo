import { defineStore } from "pinia";
import { api } from 'boot/axios'

function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) { return null; }
}

export const useUserStore = defineStore("user", {
  state: () => ({
    user: { id: 0, name: "", target: "", metamodel: "" },
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
    setUser(data) { this.user = data; },

    initFromToken() {
      const token = localStorage.getItem('fish_token');
      if (token) {
        const decoded = parseJwt(token);
        if (decoded && decoded.attrs) {
          const a = decoded.attrs;
          this.user = {
            id: a.id || 0,
            name: a.name || "",
            target: a.target || "",
            metamodel: a.metamodel || ""
          };
          api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }
      }
      this.initialized = true;
    },

    setUserStore(token) {
      if (token && typeof token === 'string') {
        localStorage.setItem('fish_token', token);
        this.initFromToken();
      }
    },

    clearUserStore() {
      localStorage.removeItem('fish_token');
      delete api.defaults.headers.common['Authorization'];
      this.user = { id: 0, name: "", target: "", metamodel: "" };
    }
  }
});
