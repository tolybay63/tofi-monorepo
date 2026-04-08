import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { LoadingBar, Notify } from 'quasar'
import { useUserStore } from "stores/user-store.js"

// 1. Константы и базовые настройки
let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP
//*******************************//
const SERVICE_NAME = 'monitoring';
//*******************************//
const url = 'http://127.0.0.1:8080'
let authURL = url + "/auth"
let baseURL = url + "/api"

// Настройка путей для PROD
if (import.meta.env.PROD) {
  const currentPath = window.location.pathname;
  if (currentPath.includes(`/fish/${SERVICE_NAME}/`)) {
    baseURL = `/fish/${SERVICE_NAME}/api/`;
  } else {
    baseURL = "/api";
  }
  authURL = "/auth";
}

// 2. Создание экземпляра API
const api = axios.create({
  baseURL: baseURL,
  headers: { 'Accept': 'application/json' }
})

// 3. Красивый оранжевый LoadingBar
LoadingBar.setDefaults({color: 'amber-14',size: '10px',position: 'top'})

// 4. Глобальный заголовок авторизации (ТЕПЕРЬ fish_token)
const token = localStorage.getItem('fish_token')
if (token && typeof token === 'string' && token !== 'null') {
  api.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

// 5. ИНТЕРЦЕПТОР ЗАПРОСА (Запуск полоски)
api.interceptors.request.use((config) => {
  LoadingBar.start()
  return config
})

export default defineBoot(({ app, router }) => {
  const userStore = useUserStore();

  // Инициализация данных пользователя из JWT при старте приложения
  if (token && token.length > 10) {
    userStore.initFromToken();
  }

  // 6. ИНТЕРЦЕПТОР ОТВЕТА (Остановка полоски и обработка ошибок)
  api.interceptors.response.use(
    (response) => {
      LoadingBar.stop()
      return response
    },
    async (error) => {
      LoadingBar.stop()

      const status = error.response?.status;
      const data = error.response?.data;
      let errorCode = error.message;

      if (errorCode.includes("Network Error")) errorCode = "networkError"

      // --- ПОИСК ТЕХНИЧЕСКОГО КЛЮЧА ОШИБКИ (для i18n) ---
      if (data) {
        let textContent = '';
        if (data instanceof ArrayBuffer) {
          textContent = new TextDecoder().decode(data);
        } else if (typeof data === 'string') {
          textContent = data;
        }
        errorCode = data.error?.message
        // Ищем в ответе (даже в HTML) ключ 'invalid_user_passwd'
        if (textContent && textContent.includes('invalid_user_passwd')) {
          errorCode = 'invalid_user_passwd';
        }
        if (textContent && textContent.includes('lifetime_expired')) {
          errorCode = 'lifetime_expired';
          userStore.clearUserStore();
          if (router) router.push("/");
        }
      }

      // Если сессия протухла
      if (status === 401 || errorCode === 'notLoginned') {
        userStore.clearUserStore();
        if (router) router.push("/");
        return Promise.reject(error);
      }

      // Вывод уведомления Notify
      Notify.create({
        type: 'negative',
        message: app.config.globalProperties.$t(errorCode) || errorCode,
        position: 'bottom-right',
        timeout: 5000,
        actions: [{ icon: 'close', color: 'white' }]
      });

      return Promise.reject(error);
    }
  );

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

// 7. Константы дат и экспорт
const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

export { api, authURL, urlMainApp, tofi_dbeg, tofi_dend };
