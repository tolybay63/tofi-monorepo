import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

const appFishURL = process.env.VITE_PRUDUCT_FISH_URL
const appDtjURL = process.env.VITE_PRUDUCT_DTJ_URL

const baseURL = "/api"
const api = axios.create({ baseURL: baseURL })

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

export { api, baseURL, appFishURL, appDtjURL}
