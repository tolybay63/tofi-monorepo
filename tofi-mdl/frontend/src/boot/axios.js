import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { LoadingBar, Notify } from 'quasar'
import { useUserStore } from "stores/user-store.js"

// 1. Константы и базовые настройки
let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP
const SERVICE_NAME = 'meta';
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

// 3. Настройка LoadingBar
LoadingBar.setDefaults({ color: 'amber-14', size: '10px', position: 'top' })

// 4. Глобальный заголовок авторизации
const token = localStorage.getItem('fish_token')
if (token && typeof token === 'string' && token !== 'null') {
  api.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

// 5. ИНТЕРЦЕПТОР ЗАПРОСА
api.interceptors.request.use((config) => {
  LoadingBar.start()
  return config
})

export default defineBoot(({ app, router }) => {
  const userStore = useUserStore();

  if (token && token.length > 10) {
    userStore.initFromToken();
  }

  // 6. ИНТЕРЦЕПТОР ОТВЕТА (Исправленный и усиленный)
  api.interceptors.response.use(
    (response) => {
      LoadingBar.stop()
      return response
    },
    async (error) => {
      LoadingBar.stop()

      const status = error.response?.status;
      const data = error.response?.data;

      // Дефолтное значение кода ошибки
      let errorCode = 'unknownError';
      let table = "";

      // Извлекаем текст ответа для поиска ключей XError
      let textContent = '';
      if (data) {
        if (typeof data === 'string') {
          textContent = data;
        } else if (data instanceof ArrayBuffer) {
          textContent = new TextDecoder().decode(data);
        } else if (typeof data === 'object') {
          textContent = JSON.stringify(data);
          // Если это чистый JSON с ошибкой от API
          errorCode = data.error?.message || data.message || errorCode;
        }
      }

      // --- ПОИСК ТЕХНИЧЕСКИХ КЛЮЧЕЙ (для Jandcode 2 XError) ---
      // Ищем вхождение ключей в любом формате (JSON или HTML стек)
      if (textContent.includes('invalid_user_passwd')) {
        errorCode = 'invalid_user_passwd';
      } else if (textContent.includes('login_temporarily_blocked')) {
        errorCode = 'login_temporarily_blocked';
      } else if (textContent.includes('lifetime_expired')) {
        errorCode = 'lifetime_expired';
      } else if (error.message && error.message.includes("Network Error")) {
        errorCode = "networkError";
      }

      // Обработка внешних ключей (FK) если errorCode еще не определен нашими ключами
      if (errorCode === 'unknownError' || errorCode.length > 50) {
        let fk = findForeignKey(textContent);
        if (fk) {
          if (fk.split('_')[2] === "parent") {
            errorCode = "hasChild";
          } else {
            errorCode = "refTable";
            table = ": [" + fk.split('_')[1] + "]";
          }
        }
      }

      // Отработка спец-символов в ключе
      if (errorCode.includes("@")) {
        table = ": [" + errorCode.split('@')[1] + "]";
        errorCode = errorCode.split("@")[0];
      }

      // --- ЛОГИКА АВТОРИЗАЦИИ ---
      if (status === 401 || errorCode === 'notLoginned' || errorCode === 'lifetime_expired') {
        userStore.clearUserStore();
        if (router) router.push("/");
        return Promise.reject(error);
      }

      // --- ВЫВОД УВЕДОМЛЕНИЯ NOTIFY ---
      // Пытаемся перевести errorCode через i18n
      const msg = app.config.globalProperties.$t(errorCode) || errorCode;
      const msg_tr = msg + table;

      Notify.create({
        type: 'negative',
        message: msg_tr,
        position: 'bottom-right',
        timeout: 5000,
        progress: true,
        actions: [{ icon: 'close', color: 'white' }]
      });

      return Promise.reject(error);
    }
  );

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

function findForeignKey(str) {
  if (!str || typeof str !== 'string') return null;
  const match = str.match(/\bfk_\w+/);
  return match ? match[0] : null;
}

const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

export { api, authURL, urlMainApp, tofi_dbeg, tofi_dend };
