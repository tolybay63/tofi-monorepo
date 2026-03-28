import {defineBoot} from '#q-app/wrappers'
import axios from 'axios'
import {LoadingBar, Notify} from 'quasar'
import {useUserStore} from 'src/stores/user-store'

let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP

//**********************************
const SERVICE_NAME = 'admin';
//**********************************

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
const api = axios.create({ baseURL: baseURL })

// Восстанавливаем токен!
const token = localStorage.getItem('fish_token')
if (token) {
  api.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

LoadingBar.setDefaults({ color: 'amber-14', size: '10px', position: 'top' })

api.interceptors.request.use((config) => {
  LoadingBar.start()
  return config
})

export default defineBoot(({ app, router }) => {
  const userStore = useUserStore();

  // Инициализируем стор (чтобы имя пользователя появилось в шапке)
  userStore.initFromToken();

  api.interceptors.response.use(
    (response) => {
      LoadingBar.stop()
      return response
    },
    (error) => {
      LoadingBar.stop()
      const status = error.response?.status;
      const data = error.response?.data;
      const failingUrl = error.config?.url;

      let errorCode = (data && typeof data === 'object') ? (data?.error?.message || data?.message) : error.message;
      if (!errorCode) errorCode = error.message;

      // Защита сессии. Если токен в хранилище есть, значит мы залогинены.
      if (status === 401 || errorCode === 'notLoginned') {
        if (localStorage.getItem('fish_token')) {
          console.warn('[Session Control] JWT активен, игнорируем 401 для:', failingUrl);
          return Promise.reject(error);
        }
        userStore.clearUserStore();
        if (router) router.push("/");
        return Promise.reject(error);
      }

      Notify.create({
        type: 'negative',
        message: app.config.globalProperties.$t(errorCode) || errorCode,
        position: 'bottom-right'
      });
      return Promise.reject(error);
    }
  );

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

export { authURL, api, urlMainApp, tofi_dbeg, tofi_dend };
