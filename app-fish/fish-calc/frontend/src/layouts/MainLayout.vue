<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>
        <!--Main App -->
        <q-btn class="q-mr-md" rounded color="primary" dense icon="grid_view" @click="mainApp()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('appName') }}
          </q-tooltip>
        </q-btn>

        <q-btn flat dense round icon="menu" @click="toggleLeftDrawer()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('menu') }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center">
          {{ $t('appCalcName') }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="this.$router['push']('/')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('mainPage') }}
          </q-tooltip>
        </q-btn>

        <!-- login user-->
        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm"
            rounded
            color="primary"
            dense
            icon="account_circle"
            @click="loginOnOff()"
            no-caps
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate" v-if="getUserName === ''"
              >{{ $t('logIn') }}
            </q-tooltip>
            <q-tooltip v-else transition-show="rotate" transition-hide="rotate"
              >{{ $t('logOut') }}
            </q-tooltip>

            {{ getUserName }}

            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon" color="white" />
            </q-badge>
          </q-btn>
        </div>

        <!-- Текущий язык-->
        <SetLocale></SetLocale>
      </q-toolbar>
    </q-header>

    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Logo" />
          </q-icon>
          {{ $t('company') }}

          <span class="absolute-right q-pt-sm">
            <a :href="site_url" target="_blank" style="font-size: 12px" class="q-pr-md text-white">
              {{ $t('fish_model') }}
            </a>
          </span>
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer :width="230" v-model="leftDrawerOpen" show-if-above bordered elevated class="q-pa-sm">
      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth">
        {{ $t('notLoginned') }}
      </h6>
      <h6 class="q-pa-md text-red text-bold" v-else-if="notAccess">
        {{ $t('notAccessService') }}
      </h6>

      <q-list v-for="link in essentialLinks" :key="link.title">
        <q-item
          class="q-table--bordered bg-blue-1"
          v-if="hasTarget(link.target)"
          clickable
          tag="a"
          :to="link.link"
          active-class="text-bold text-blue"
        >
          <q-item-section v-if="link.icon" avatar>
            <q-icon :name="link.icon" size="32px" />
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ link.title }}</q-item-label>
            <q-item-label caption>{{ link.info }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user-store'

import LoginUser from '@/components/LoginUser.vue'
import SetLocale from '@/components/SetLocale.vue'
import { api, authURL, urlMainApp } from '@/boot/axios'
import { useQuasar } from 'quasar'
import axios from 'axios'
import { useI18n } from 'vue-i18n'

// Composables
const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const store = useUserStore()
const { isSysAdmin, getUserName, getUserId, getTarget } = storeToRefs(store)
const { setUserStore, clearUserStore } = store
// Переменная окружения для ссылки в футере
const site_url = import.meta.env.QCLI_SITE_URL || 'https://tofishstocks-model.kz'

// Reactive state
const leftDrawerOpen = ref(true)
// Computed properties
const reqAuth = computed(() => getUserName.value === '')
const notAccess = computed(() => !getTarget.value.includes('adm') && !isSysAdmin.value)
const nameIcon = computed(() => (getUserName.value === '' ? 'login' : 'logout'))

// Essential links
const essentialLinks = computed(() => [
  {
    title: t('calc_A'),
    info: '',
    icon: 'apartment',
    link: '/calcA',
    target: '',
  },
  {
    title: t('calc_B'),
    info: '',
    icon: 'home_work',
    link: '/calcB',
    target: '',
  },
])

// Methods
const mainApp = () => {
  window.open(urlMainApp, '_self')
}

const toggleLeftDrawer = () => {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

const hasTarget = (tg) => {
  if (isSysAdmin.value) return true
  if (getTarget.value.length === 0) return false
  return getTarget.value.includes(tg)
}

const loginOnOff = () => {
  if (getUserName.value === '') {
    leftDrawerOpen.value = true
    $q.dialog({
      component: LoginUser,
      componentProps: {
        // ...
      },
    }).onOk((res) => {
      setUserStore(res)
      router.push('/')
/*      api.post('', {
        method: 'usr/checkTarget',
        params: ['adm'],
      })*/
    })
  } else {
    axios
      .post(authURL + '/logout', new URLSearchParams()) // <-- Склеиваем динамически со слэшем!
      .then(() => {
        clearUserStore()
      })
      .catch((err) => {
        console.error('Ошибка при logout на сервере:', err)
        clearUserStore()
      })
      .finally(() => {
        router.push('/')
      })
  }
}

// Lifecycle hooks
onMounted(() => {
  if (!getUserId.value > 0) {
    clearUserStore()
    router.push('/')
  }
})
</script>

<!--
<script>
import {defineComponent, ref} from 'vue'
import LoginUser from '../components/LoginUser.vue'
import SetLocale from '../components/SetLocale.vue'
import {api, authURL, urlMainApp} from '../boot/axios'

import {useUserStore} from '../stores/user-store'
import {storeToRefs} from 'pinia'
import {useRouter} from "vue-router";

export default defineComponent({
  name: 'MainLayout',
  components: { SetLocale },

  data() {
    return {}
  },

  methods: {
    mainApp() {
      open(urlMainApp, '_self')
    },

    site_url() {
      return process.env.SITE_URL
    },
  },

  created() {
    console.info('Created!')
    const store = useUserStore()
    const { clearUserStore } = store
    const { getUserId } = storeToRefs(store)
    const router = useRouter()

    if (getUserId.value === 0) {
      clearUserStore()
      router.push('/')
    }
  },

  setup() {
    console.info('Setup!')

    const leftDrawerOpen = ref(true)
    const store = useUserStore()
    const { isSysAdmin, getUserName, getTarget } = storeToRefs(store)
    const { setUserStore, clearUserStore } = store
    const router = useRouter()

    let getLinks = () => {
      return [
        {
          title: "Calculation A",
          info: "",
          icon: "apartment",
          link: "/calcA",
          target: "",
        },
        {
          title: "Calculation B",
          info: "",
          icon: "home_work",
          link: "/calcB",
          target: "",
        },
      ]
    }

    let essentialLinks = getLinks()

    return {
      getUserName,
      essentialLinks,

      reqAuth() {
        return getUserName.value === ''
      },

      notAccess() {
        return getTarget.value.length === 0 && !isSysAdmin.value
      },

      nameIcon() {
        if (getUserName.value === '') return 'login'
        else return 'logout'
      },

      hasTarget(tg) {
        if (isSysAdmin.value) return true
        if (getTarget.value.length === 0) return false
        return getTarget.value.includes(tg)
      },

      loginOnOff() {
        //console.info("OnOff")
        if (getUserName.value === '') {
          leftDrawerOpen.value = true
          this.$q
            .dialog({
              component: LoginUser,
              componentProps: {
                // ...
              },
            })
            .onOk((res) => {
              setUserStore(res)
              router.push('/')
            })
        } else {
          api
            .post(authURL + '/logout', {
              params: {},
            })
            .then(() => {
              clearUserStore()
            })
            .finally(() => {
              router.push('/')
            })
        }
      },

      leftDrawerOpen,
      toggleLeftDrawer() {
        leftDrawerOpen.value = !leftDrawerOpen.value
      },
    }
  },
})
</script>
-->

<style></style>
