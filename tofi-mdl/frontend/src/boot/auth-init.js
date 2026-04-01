import { boot } from 'quasar/wrappers'
import { api } from 'boot/axios'
import { useUserStore } from 'stores/user-store.js'

export default boot(async ({ store }) => {
  const userStore = useUserStore(store)
  const token = localStorage.getItem('fish_token')

  if (token) {
    try {
      const response = await api.post('', { method: 'auth/getUserInfo', params: [] })
      if (response.data?.result) userStore.setUser(response.data.result)
    } catch (e) {
      console.error('Ошибка сессии:', e.response?.status)
    }
  }
})
