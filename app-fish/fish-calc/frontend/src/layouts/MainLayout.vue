<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>
        <!--Main App -->
        <q-btn class="q-mr-md" rounded color="primary" dense icon="grid_view" @click="mainApp()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ tr('appName') }}
          </q-tooltip>
        </q-btn>

        <q-btn flat dense round icon="menu" @click="toggleLeftDrawer()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ tr('menu') }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center">
          {{ tr('appCalcName') }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="router['push']('/')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ tr('mainPage') }}
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
              >{{ tr('logIn') }}
            </q-tooltip>
            <q-tooltip v-else transition-show="rotate" transition-hide="rotate"
              >{{ tr('logOut') }}
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
          {{ tr('company') }}

          <span class="absolute-right q-pt-sm">
            <a :href="site_url" target="_blank" style="font-size: 12px" class="q-pr-md text-white">
              {{ tr('fish_model') }}
            </a>
          </span>
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer :width="260" v-model="leftDrawerOpen" show-if-above bordered elevated class="q-pa-sm bg-grey-3">

      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth">
        {{ tr('notLoginned') }}
      </h6>
      <h6 class="q-pa-md text-red text-bold" v-else-if="notAccess">
        {{ tr('notAccessService') }}
      </h6>

      <q-list class="q-list--bordered bg-blue-1">
        <template v-for="item in essentialLinks" :key="item.label">
          <q-expansion-item v-if="item.children && hasTarget(item.target)"
                            :icon="item.icon" :label="item.label"
                            class="q-list--bordered"
          >
            <q-item
              v-for="subItem in item.children"
              :key="subItem.label"
              v-ripple
              :active="isActive(subItem.to)"
              :to="subItem.to"
              active-class="text-bold text-blue bg-blue-2"
              class="q-pl-xl q-table--bordered bg-blue-1"
              clickable
            >
              <q-item-section avatar>
                <q-icon :name="subItem.icon"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ subItem.label }}</q-item-label>
                <q-item-label caption>{{ subItem.info }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-expansion-item>

          <q-item v-else v-if="hasTarget(item.target)"
                  v-ripple
                  :active="isActive(item.to)"
                  :to="item.to"
                  active-class="text-bold text-blue bg-blue-2"
                  class="q-table--bordered bg-blue-1"
                  clickable>
            <q-item-section avatar>
              <q-icon :name="item.icon"/>
            </q-item-section>
            <q-item-section>{{ item.label }}</q-item-section>
          </q-item>
        </template>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import {useUserStore} from '@/stores/user-store'

import LoginUser from '@/components/LoginUser.vue'
import SetLocale from '@/components/SetLocale.vue'
import {api, authURL, urlMainApp} from '@/boot/axios'
import {useQuasar} from 'quasar'
import axios from 'axios'
import {useI18n} from 'vue-i18n'

// Composables
const router = useRouter()
const route = useRoute()
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
const notAccess = computed(() => !getTarget.value.includes('calc') && !isSysAdmin.value)
const nameIcon = computed(() => (getUserName.value === '' ? 'login' : 'logout'))

// Essential links
const essentialLinks = computed(() => [
  {
    label: t('calcDeterm'),
    info: '',
    icon: 'calculate',
    to: '/calc_determ',
    target: 'calc',
  },

  {
    label: t('calcBayes'),
    info: '',
    icon: 'calculate',
    to: '/calc_bayes',
    target: 'calc',
  },


  {
    label: t('calcMPC'),
    info: '',
    icon: 'calculate',
    //to: '/calc_mpc',
    target: 'calc',
  },
  {
    label: 'Fast API EndPoints',
    info: '',
    icon: 'link',
    target: 'calc',
    children: [
      {
        label: "Test 1",
        info: 'Fast API from Meta',
        icon: 'link',
        to: '/calcA',
        target: 'calc',
      },
      {
        label: "Test 2",
        info: 'Fast API from NSI',
        icon: 'link',
        to: '/calcB',
        target: 'calc',
      }
    ]
  },

])

// Methods

const isActive = (menuTo) => {
  if (!menuTo || !route.path) return false
  const menuBase = menuTo.split('/')[1]
  const currentBase = route.path.split('/')[1]
  return menuBase === currentBase
}

const mainApp = () => {
  window.open(urlMainApp, '_self')
}

const toggleLeftDrawer = () => {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

const tr = (label) => {
  return t(label)
}
const hasTarget = (tg) => {
  if (isSysAdmin.value) return true
  if (getTarget.value.length === 0) return false
  return getTarget.value.includes(tg)
}

const loginOnOff = () => {
  if (getUserName.value === '') {
    leftDrawerOpen.value = true
    $q["dialog"]({
      component: LoginUser,
      componentProps: {
        // ...
      },
    }).onOk((res) => {
      setUserStore(res)
      router["push"]('/')
      api.post('', {
        method: 'auth/checkTarget',
        params: ['calc'],
      })
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
        router["push"]('/')
      })
  }
}

// Lifecycle hooks
onMounted(() => {
  if (!getUserId.value > 0) {
    clearUserStore()
    router["push"]('/')
  }
})
</script>

<style></style>
