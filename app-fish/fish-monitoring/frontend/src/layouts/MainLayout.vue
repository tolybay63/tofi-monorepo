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
          {{ $t('appMonitoringName') }}
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
              {{ $t("fish_model") }}
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

      <q-list v-else>
        <template v-for="item in essentialLinks" :key="item.label">
          <q-expansion-item
            v-if="item.children && hasTarget(item.target)"
            :icon="item.icon"
            :label="$t(item.label)"
            class="q-table--bordered"
          >
            <q-item
              v-for="subItem in item.children"
              :key="$t(subItem.label)"
              v-ripple
              :active="isActive(subItem.to)"
              :to="subItem.to"
              active-class="text-bold text-blue bg-blue-2"
              class="q-table--bordered bg-blue-1"
              clickable
            >
              <q-item-section avatar>
                <q-icon :name="subItem.icon" />
              </q-item-section>
              <q-item-section>{{ $t(subItem.label) }}</q-item-section>
            </q-item>
          </q-expansion-item>

          <q-item
            v-else-if="hasTarget(item.target)"
            v-ripple
            :active="isActive(item.to)"
            :to="item.to"
            class="q-table--bordered bg-blue-1"
            active-class="text-bold text-blue bg-blue-2"
            clickable
          >
            <q-item-section avatar>
              <q-icon :name="item.icon" />
            </q-item-section>
            <q-item-section>{{ $t(item.label) }}</q-item-section>
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
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import LoginUser from '@/components/LoginUser.vue'
import SetLocale from '@/components/SetLocale.vue'
import { api, urlMainApp } from '@/boot/axios'
import { useUserStore } from '@/stores/user-store'

const $q = useQuasar()
const router = useRouter()
const route = useRoute()

const leftDrawerOpen = ref(true)
const store = useUserStore()
const { isSysAdmin, getUserName, getTarget, getUserId } = storeToRefs(store)
const { setUserStore, clearUserStore } = store

// Проверка сессии при инициализации[cite: 15]
if (getUserId.value === 0) {
  clearUserStore()
  router.push('/')
}

const essentialLinks = [
  { label: 'reservoirs', icon: 'sailing', to: '/reservoirs', target: 'mon:vod' },
  { label: 'samplingStations', icon: 'houseboat', to: '/samplingstations', target: 'mon:st' },
  { label: 'typesOfFish', icon: 'set_meal', to: '/typesfish', target: 'mon:tf' },
  { label: 'piscesInReservoirs', icon: 'tsunami', to: '/piscesreservoirs', target: 'mon:rpv' },
  { label: 'fishing', icon: 'location_on', to: '/fishing', target: 'mon:fish' },
  { label: 'fill', icon: 'download', to: '/fill', target: 'mon:fill' },
]

const mainApp = () => {
  open(urlMainApp, '_self')
}

const siteUrl = import.meta.env.QCLI_SITE_URL || 'https://tofishstocks-model.kz'

const isActive = (menuTo) => {
  if (!menuTo || !route.path) return false
  const menuBase = menuTo.split('/')[1]
  const currentBase = route.path.split('/')[1]
  return menuBase === currentBase
}

const reqAuth = () => getUserName.value === ''

const notAccess = () => !getTarget.value.includes("mon") && !isSysAdmin.value

const nameIcon = () => (getUserName.value === '' ? 'login' : 'logout')

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
      componentProps: {},
    }).onOk((res) => {
      setUserStore(res)
      router.push('/')

      api.post('', {
        method: "auth/checkTarget",
        params: ["mon"],
      })
    })
  } else {
    // Исправленный вызов выхода через шлюз api.post вместо прямого axios 404
    api.post('', { method: 'auth/logout' })
      .then(() => {
        clearUserStore()
      })
      .catch((err) => {
        console.error("Ошибка при logout на сервере:", err)
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
</script>

<style></style>
