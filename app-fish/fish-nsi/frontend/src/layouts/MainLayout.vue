<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>
        <!--Main App -->
        <q-btn class="q-mr-md" rounded color="primary" dense icon="grid_view" @click="mainApp">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('appName') }}
          </q-tooltip>
        </q-btn>

        <q-btn flat dense round icon="menu" @click="toggleLeftDrawer">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('menu') }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center">
          {{ $t('appNsiName') }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="router.push('/')"
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
            @click="loginOnOff"
            no-caps
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate" v-if="getUserName === ''">
              {{ $t('logIn') }}
            </q-tooltip>
            <q-tooltip v-else transition-show="rotate" transition-hide="rotate">
              {{ $t('logOut') }}
            </q-tooltip>

            {{ getUserName }}

            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon()" color="white" />
            </q-badge>
          </q-btn>
        </div>

        <!-- Текущий язык-->
        <SetLocale />
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
            <a :href="siteUrl" target="_blank" style="font-size: 12px" class="q-pr-md text-white">
              {{ $t('fish_model') }}
            </a>
          </span>
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer :width="230" v-model="leftDrawerOpen" show-if-above bordered elevated class="q-pa-sm">
      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth()">
        {{ $t('notLoginned') }}
      </h6>
      <h6 class="q-pa-md text-red text-bold" v-else-if="notAccess()">
        {{ $t('notAccess') }}
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
            <q-item-label>{{ $t(link.title) }}</q-item-label>
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

<script>
import { defineComponent, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import LoginUser from '../components/LoginUser.vue'
import SetLocale from '../components/SetLocale.vue'
import { api, authURL, urlMainApp } from '../boot/axios'
import { useUserStore } from '../stores/user-store'
import { hasTarget } from '../utils/jsutils.js'
import axios from "axios";

export default defineComponent({
  name: 'MainLayout',
  components: { SetLocale },

  setup() {
    console.info('Setup initialized!')
    const $q = useQuasar()
    const router = useRouter()
    const leftDrawerOpen = ref(true)

    const store = useUserStore()
    const { getUserName, isSysAdmin, getTarget, getUserId } = storeToRefs(store)
    const { setUserStore, clearUserStore } = store

    // Проверка авторизации при инициализации
    if (getUserId.value === 0) {
      clearUserStore()
      router.push('/')
    }

    const essentialLinks = [
      {
        title: 'kato',
        icon: 'home_work',
        link: '/kato',
        target: 'mon:kato',
      },
      {
        title: 'samplingStations',
        icon: 'houseboat',
        link: '/samplingstations',
        target: 'mon:st',
      },
      {
        title: 'FishGear',
        icon: 'phishing',
        link: '/fishGear',
        target: 'mon:fg',
      },
      {
        title: 'struct_enterprise',
        info: '',
        icon: 'apartment',
        link: '/struct_enterprise2',
        target: 'st:org',
      },
      {
        title: 'personnel',
        info: '',
        icon: 'group',
        link: '/personnel',
        target: 'st:per',
      },
    ]

    const mainApp = () => {
      open(urlMainApp, '_self')
    }

    const siteUrl = import.meta.env.QCLI_SITE_URL || 'https://tofishstocks-model.kz'

    const reqAuth = () => getUserName.value === ''

    const notAccess = () => !getTarget.value.includes('st') && !isSysAdmin.value

    const nameIcon = () => (getUserName.value === '' ? 'login' : 'logout')

    const loginOnOff = () => {
      if (getUserName.value === '') {
        leftDrawerOpen.value = true
        $q.dialog({
          component: LoginUser,
          componentProps: {},
        }).onOk((res) => {
          setUserStore(res)
          router.push('/')

          api.post('', {
            method: 'auth/checkTarget',
            params: ['nsi'],
          })
        })
      } else {
        axios
          .post(authURL + '/logout', new URLSearchParams())
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

    const toggleLeftDrawer = () => {
      leftDrawerOpen.value = !leftDrawerOpen.value
    }

    return {
      leftDrawerOpen,
      essentialLinks,
      getUserName,
      mainApp,
      siteUrl,
      hasTarget,
      reqAuth,
      notAccess,
      nameIcon,
      loginOnOff,
      toggleLeftDrawer,
      router,
    }
  },
})
</script>

<style></style>
