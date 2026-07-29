import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { LoadingBar, Notify } from 'quasar'
import { useUserStore } from "stores/user-store.js"

// =========================================================================
// 1. Настройка базовых URL (Одинаково для DEV и PROD)
// =========================================================================
const SERVICE_NAME = 'meta';
let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP;

let authURL = "/auth";
let baseURL = "/api";

// Если это ПРОД (сборка через Nginx или запуск без портов в браузере)
if (process.env.NODE_ENV === 'production' || (typeof window !== 'undefined' && !window.location.port)) {
  baseURL = `/fish/${SERVICE_NAME}/api/`;
  authURL = `/fish/${SERVICE_NAME}/auth`; // С закрывающим слэшем для идеальной работы proxy_cookie_path
}

// 2. Изолированный экземпляр для системных данных (всегда шлет JSON)
const api = axios.create({
  baseURL: baseURL,
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
})

// Автоматический перехватчик: вытаскивает токен из сессии НАПРЯМУЮ перед КАЖДЫМ запросом
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('fish_token');
    if (token && token !== 'null' && token !== 'undefined') {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 3. Изолированный экземпляр строго для авторизации и рефреша
const authApi = axios.create({
  withCredentials: true // Куки для работы с токенами привязываются только сюда
})

// Очередь для предотвращения многократного обновления токена
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Настройка LoadingBar
LoadingBar.setDefaults({ color: 'amber-14', size: '10px', position: 'top' })

// =========================================================================
// ИНТЕРЦЕПТОРЫ (ПЕРЕХВАТЧИКИ)
// =========================================================================

// ИНТЕРЦЕПТОР ЗАПРОСА
api.interceptors.request.use((config) => {
  LoadingBar.start()

  const token = sessionStorage.getItem('fish_token')
  if (token && typeof token === 'string' && token !== 'null') {
    config.headers['Authorization'] = `Bearer ${token}`
  }

  return config
}, (error) => {
  return Promise.reject(error)
})

export default defineBoot(({ app, router }) => {
  const userStore = useUserStore();

  // Автоматическая инициализация сессии при перезагрузке страницы
  const currentToken = sessionStorage.getItem('fish_token')
  if (currentToken && currentToken.length > 10 && currentToken !== 'null') {
    userStore.initFromToken();
  }

  // ИНТЕРЦЕПТОР ОТВЕТА
  api.interceptors.response.use(
    (response) => {
      LoadingBar.stop()
      return response
    },
    async (error) => {
      LoadingBar.stop()

      const status = error.response?.status;
      const data = error.response?.data;
      const originalRequest = error.config;

      let errorCode = 'unknownError';
      let table = "";

      let textContent = '';
      if (data) {
        if (typeof data === 'string') {
          textContent = data;
        } else if (data instanceof ArrayBuffer) {
          textContent = new TextDecoder().decode(data);
        } else if (typeof data === 'object') {
          textContent = JSON.stringify(data);
          errorCode = data.error?.message || data.message || errorCode;
        }
      }

      // Ваши системные парсеры ошибок Jandcode
      if (textContent.includes('invalid_user_passwd')) {
        errorCode = 'invalid_user_passwd';
      } else if (textContent.includes('login_temporarily_blocked')) {
        errorCode = 'login_temporarily_blocked';
      } else if (textContent.includes('lifetime_expired')) {
        errorCode = 'lifetime_expired';
      } else if (error.message && error.message.includes("Network Error")) {
        errorCode = "networkError";
      }

      // Работа с внешними ключами через сохраненную функцию findForeignKey
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

      if (errorCode.includes("@")) {
        table = ": [" + errorCode.split('@')[1] + "]";
        errorCode = errorCode.split("@")[0];
      }

      const isAuthError = status === 401 || errorCode === 'notLoginned' || errorCode === 'lifetime_expired';

      // Обработка протухания токена (Refresh Token)
      if (isAuthError && !originalRequest._retry) {

        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          })
            .then(token => {
              originalRequest.headers['Authorization'] = `Bearer ${token}`
              return api(originalRequest)
            })
            .catch(err => Promise.reject(err))
        }

        originalRequest._retry = true
        isRefreshing = true

        return new Promise((resolve, reject) => {
          // Вызываем рефреш через изолированный authApi, чтобы не сломать заголовки данных
          authApi.post(`${authURL}/refresh`, {})
            .then(({ data }) => {
              const newToken = data?.result?.token || data?.token;

              if (newToken) {
                userStore.updateTokenOnly(newToken);
                api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
                originalRequest.headers['Authorization'] = `Bearer ${newToken}`;

                processQueue(null, newToken);
                resolve(api(originalRequest));
              } else {
                throw new Error('No token in response');
              }
            })
            .catch((refreshError) => {
              processQueue(refreshError, null);
              userStore.clearUserStore();
              if (router) router.push("/");
              reject(refreshError);
            })
            .finally(() => {
              isRefreshing = false;
            });
        });
      }

      // Вывод всплывающих уведомлений для обычных ошибок данных
      if (!isAuthError) {
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
      }

      return Promise.reject(error);
    }
  );

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

// Восстановленная утилита поиска FK
function findForeignKey(str) {
  if (!str || typeof str !== 'string') return null;
  const match = str.match(/\bfk_\w+/);
  return match ? match[0] : null;
}

const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

// Полный набор всех старых и новых экспортов для совместимости со всем проектом
export { api, authApi, authURL, urlMainApp, tofi_dbeg, tofi_dend };
