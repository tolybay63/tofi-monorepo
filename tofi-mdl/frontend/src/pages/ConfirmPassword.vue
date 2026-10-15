<template>
  <div class="fixed-center text-center q-pa-md">
    <!-- Иконка и статус успешной активации -->
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
        @click="mainApp()"
      />
    </div>

    <!-- Ошибка (если зашли повторно или ссылка битая) -->
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
        @click="mainApp()"
      />
    </div>

    <!-- Спиннер во время запроса -->
    <div v-else>
      <q-spinner-cube color="primary" size="60px" />
      <div class="text-h6 text-grey-7 q-mt-md">Проверка ссылки подтверждения...</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import {api, urlMainApp} from 'boot/axios'


export default {
  name: 'ConfirmPassword',
  data () {
    return {
      success: false,
      error: false,
      errorMessage: ''
    }
  },

  methods: {

    mainApp() {
      window.open(urlMainApp, '_self')
    }

  },


  mounted () {
    const loginParam = this.$route.query.login

    if (!loginParam) {
      this.error = true
      this.errorMessage = 'Некорректная ссылка: отсутствует логин.'
      return
    }

    axios.post(api.defaults.baseURL, {
      method: 'psw/confirmPasswd',
      params: [{ login: loginParam }]
    })
      .then(() => {
        this.success = true
      })
      .catch((err) => {
        this.error = true
        this.errorMessage = err.response?.data?.error?.message || 'Запрос на сброс пароля не создавался или уже активирован.'
      })
  }
}
</script>
