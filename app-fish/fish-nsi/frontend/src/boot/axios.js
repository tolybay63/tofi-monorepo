import {defineBoot} from '#q-app/wrappers'
import axios from 'axios'
import { LoadingBar, Notify } from 'quasar'
import {useUserStore} from "stores/user-store.js";

let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP

//*******************************
const SERVICE_NAME = 'nsi';
//*******************************

const url = 'http://127.0.0.1:8080'
let authURL = url + "/auth"
let baseURL = url + "/api"

if (import.meta.env.PROD) {
  const currentPath = window.location.pathname;
  if (currentPath.includes(`/fish/${SERVICE_NAME}/`)) {
    baseURL = `/fish/${SERVICE_NAME}/api/`;
  } else {
    baseURL = "/api";
  }
  authURL = "/auth";
}

const api = axios.create({
  baseURL: baseURL,
  // Просим бэкенд отдавать JSON, но будем готовы к ArrayBuffer при ошибках 500
  headers: { 'Accept': 'application/json' }
})

// Восстанавливаем токен (строгая проверка типа)
const token = localStorage.getItem('fish_token')
if (token && typeof token === 'string' && token !== 'null' && token !== 'undefined') {
  api.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

LoadingBar.setDefaults({ color: 'amber-14', size: '10px', position: 'top' })

api.interceptors.request.use((config) => {
  LoadingBar.start()
  return config
})

export default defineBoot(({ app, router }) => {
  const userStore = useUserStore();

  // Исправление ошибки "object вместо строки" при старте:
  // Инициализируем только если токен — это не пустая строка
  if (token && typeof token === 'string' && token.length > 10) {
    userStore.initFromToken();
  }

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

      // --- ДЕКОДИРОВАНИЕ ArrayBuffer И ПОИСК ОШИБКИ ---
      if (data) {
        let textContent = '';

        if (data instanceof ArrayBuffer) {
          // Если пришел бинарный буфер (как на скриншоте), декодируем его в текст
          textContent = new TextDecoder().decode(data);
        } else if (typeof data === 'string') {
          textContent = data;
        } else if (typeof data === 'object') {
          errorCode = data.error?.message || data.message || data.error || error.message;
        }

        // Ищем ключевые фразы в тексте (HTML или декодированном буфере)
        if (textContent) {
          if (textContent.includes('invalid_user_passwd')) {
            errorCode = 'invalid_user_passwd';
          } else if (textContent.includes('notLoginned')) {
            errorCode = 'notLoginned';
          }
        }
      }

      // Защита сессии
      if (status === 401 || errorCode === 'notLoginned') {
        if (localStorage.getItem('fish_token')) return Promise.reject(error);
        userStore.clearUserStore();
        if (router) router.push("/");
        return Promise.reject(error);
      }

      // Теперь здесь будет 'invalid_user_passwd', и сработает ваш перевод
      Notify.create({
        type: 'negative',
        message: app.config.globalProperties.$t(errorCode) || errorCode,
        position: 'bottom-right',
        timeout: 5000
      });

      return Promise.reject(error);
    }
  );

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

export { api, authURL, urlMainApp, tofi_dbeg, tofi_dend };
