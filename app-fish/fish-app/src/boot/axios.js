import {defineBoot} from '#q-app'
import axios from 'axios'

const appAdmURL = import.meta.env.QCLI_PRODUCT_ADM_URL
const appModelURL = import.meta.env.QCLI_PRODUCT_MODEL_URL
const appNsiURL = import.meta.env.QCLI_PRODUCT_NSI_URL
const appMonitoringURL = import.meta.env.QCLI_PRODUCT_MONITORING_URL
const appCalcURL = import.meta.env.QCLI_PRODUCT_CALC_URL

const url = 'http://127.0.0.1:8080'
let baseURL = url + "/api"

if (import.meta.env.PROD) {
    baseURL = "/api";
}

const api = axios.create({ baseURL: baseURL })

export default defineBoot(({ app }) => {

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

export { api, appAdmURL, appModelURL,
  appCalcURL, appNsiURL, appMonitoringURL}
