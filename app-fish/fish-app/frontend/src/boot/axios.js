import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

const appAdmURL = process.env.VITE_PRUDUCT_ADM_URL
const appModelURL = process.env.VITE_PRUDUCT_MODEL_URL
const appDataUserURL = process.env.VITE_PRUDUCT_DATAUSER_URL
const appDataURL = process.env.VITE_PRUDUCT_DATA_URL
const appCubeURL = process.env.VITE_PRUDUCT_CUBE_URL
const appMonitoringURL = process.env.VITE_PRUDUCT_MONITORING_URL
const appNSIURL = process.env.VITE_PRUDUCT_NSI_URL

const baseURL = "/api"
const api = axios.create({ baseURL: baseURL })

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

export { api, baseURL, appAdmURL, appModelURL, appDataUserURL, appDataURL, appCubeURL, appNSIURL, appMonitoringURL }
