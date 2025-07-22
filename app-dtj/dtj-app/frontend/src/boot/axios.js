import { boot } from 'quasar/wrappers'
import axios from 'axios'

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
//const api = axios.create({ baseURL: 'https://api.example.com' })

const appAdmURL = "http://192.168.1.20:9172"
const appModelURL = "http://192.168.1.20:9173"
const appDataUserURL = "http://192.168.1.20:9174"
const appDataURL = "http://192.168.1.20:9175"
const appCubeURL = "http://192.168.1.20:9176"
const appNSIURL = "http://192.168.1.20:9177"


//const appAdmURL = "http://localhost:9172"
//const appModelURL = "http://localhost:9173"
//const appDataUserURL = "http://localhost:9174"
//const appDataURL = "http://localhost:9175"
//const appNSIURL = "http://localhost:9176"


let url = "http://localhost:8080"
if (process.env.NODE_ENV === 'production') {
  url = "http://192.168.1.20:9171"
//  url = "http://localhost:9171"
}

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

export { api, baseURL, appAdmURL, appModelURL, appDataUserURL, appDataURL, appNSIURL, appCubeURL }
