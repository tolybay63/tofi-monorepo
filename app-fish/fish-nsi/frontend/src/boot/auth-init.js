import { boot } from 'quasar/wrappers'
import { api } from 'boot/axios'
import { useAuthStore } from 'src/stores/auth'

export default boot(async ({ store }) => {
  const authStore = useAuthStore(store)
  const token = localStorage.getItem('fish_token')

  if (token) {
    // 1. Принудительно ставим заголовок для всех будущих запросов
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`

    try {
      // 2. Делаем RPC-вызов. КРИТИЧНО: передаем method и params
      const response = await api.post('', {
        method: 'auth/getUserInfo',
        params: []
      }, {
        // Дублируем заголовок в самом запросе для надежности
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (response.data && response.data.result) {
        authStore.setUser(response.data.result)
        //console.log('✅ Мета: Сисадмин успешно опознан');
      }
    } catch (e) {
      console.error('❌ Ошибка вызова getUserInfo', e.response?.status)
    }
  }
})
