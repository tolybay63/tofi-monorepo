import {boot} from 'quasar/wrappers'
import axios from 'axios'

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation insid of the
// "export default () => {}" function below (which runs individually
// for each client)
//const api = axios.create({ baseURL: 'https://api.example.com' })

let appURL = "http://192.168.1.20:9171"
//let appURL = "http://localhost:9171"

let url = "http://localhost:8080"
if (process.env.NODE_ENV === 'production') {
  url = "http://192.168.1.20:9176"
//  url = "http://localhost:9176"
}

const authURL = url + "/auth"
const baseURL = url + "/api"
const api = axios.create({ baseURL: baseURL })

export default boot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
})

const tofi_dbeg = "1800-01-01";
const tofi_dend = "3333-12-31";

export { authURL, api, baseURL, appURL, tofi_dbeg, tofi_dend };
