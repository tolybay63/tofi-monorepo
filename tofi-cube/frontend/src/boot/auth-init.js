import { boot } from 'quasar/wrappers'
import { useUserStore } from 'stores/user-store.js'
import { api } from 'boot/axios'

export default boot(async ({ store }) => {
  const userStore = useUserStore(store)
  const token = sessionStorage.getItem('fish_token')

  if (token && token !== 'null') {
    try {
      const response = await api.post('', { method: 'auth/getUserInfo', params: [] })
      if (response.data?.result) userStore.setUser(response.data.result)
    } catch (e) {
      // Ошибка 401 перехватится в axios.js, поэтому тут просто пишем предупреждение в варнинг
      console.warn('Токен устарел при инициализации. Будет выполнена попытка автоматического продления.');
    }
  }
})
