<template>
  <div class="fixed-center text-center q-pa-md">
    <div v-if="success">
      <q-icon name="check_circle" color="positive" size="80px" />
      <div class="text-h5 q-mt-md text-weight-bold text-grey-9">Пароль успешно активирован!</div>
      <div class="text-subtitle1 text-grey-7 q-mt-sm">
        Ваш разовый пароль из письма принят системой.
      </div>
      <q-btn
        color="primary"
        label="На главную страницу"
        class="q-mt-xl q-px-xl text-weight-bold"
        size="lg"
        @click="mainApp"
      />
    </div>

    <div v-else-if="error">
      <q-icon name="warning" color="warning" size="80px" />
      <div class="text-h5 q-mt-md text-weight-bold text-grey-9">Внимание</div>
      <div class="text-subtitle1 text-grey-7 q-mt-sm">
        {{ errorMessage || 'Запрос на сброс пароля не создавался или уже активирован.' }}
      </div>
      <q-btn
        color="grey-7"
        label="На главную страницу"
        class="q-mt-xl q-px-xl"
        @click="mainApp"
      />
    </div>

    <div v-else>
      <q-spinner-cube color="primary" size="60px" />
      <div class="text-h6 text-grey-7 q-mt-md">Проверка ссылки подтверждения...</div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {api, urlMainApp} from '@/boot/axios'

const route = useRoute()

const success = ref(false)
const error = ref(false)
const errorMessage = ref('')

const mainApp = () => {
  window.open(urlMainApp, '_self')
}

onMounted(() => {
  const loginParam = route.query.login

  if (!loginParam) {
    error.value = true
    errorMessage.value = 'Некорректная ссылка: отсутствует логин.'
    return
  }

  // Используем штатный экземпляр api (точно так же, как во всем проекте)
  api.post('', {
    method: 'psw/confirmPasswd',
    params: [{ login: loginParam }]
  })
    .then(() => {
      success.value = true
    })
    .catch((err) => {
      error.value = true
      errorMessage.value = err.response?.data?.error?.message || 'Запрос на сброс пароля не создавался или уже активирован.'
    })
})
</script>
