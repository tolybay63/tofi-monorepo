import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
//const api = axios.create({ baseURL: 'https://api.example.com' })

let urlMainApp = process.env.VITE_PRODUCT_URL_MAIN_APP

let url = 'http://localhost:8080'
if (import.meta.env.PROD) {
  url = process.env.VITE_PRODUCT_URL
}

const authURL = url + "/auth"
const baseURL = url + "/api"
const api = axios.create({ baseURL: baseURL })




export default defineBoot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
})

export { authURL, api, baseURL, urlMainApp };
