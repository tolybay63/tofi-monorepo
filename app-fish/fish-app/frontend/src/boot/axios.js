import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
//const api = axios.create({ baseURL: 'https://api.example.com' })

const appAdmURL = process.env.VITE_PRUDUCT_ADM_URL
const appModelURL = process.env.VITE_PRUDUCT_MODEL_URL
const appDataUserURL = process.env.VITE_PRUDUCT_DATAUSER_URL
const appDataURL = process.env.VITE_PRUDUCT_DATA_URL
const appNSIURL = process.env.VITE_PRUDUCT_NSI_URL
const appMonitoringURL = process.env.VITE_PRUDUCT_MONITORING_URL
const appCubeURL = process.env.VITE_PRUDUCT_CUBE_URL

/*
let url = 'http://localhost:8080'
if (import.meta.env.PROD) {
  url = process.env.VITE_PRODUCT_URL
}
*/

const baseURL = "/api"
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

export { api, baseURL, appAdmURL, appModelURL, appDataUserURL, appDataURL, appCubeURL, appNSIURL, appMonitoringURL }
