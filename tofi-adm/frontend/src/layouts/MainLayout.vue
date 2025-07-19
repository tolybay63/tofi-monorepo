<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>

        <!--Main App -->
        <q-btn
          class="q-mr-md"
          rounded
          color="primary"
          dense
          icon="grid_view"
          @click="mainApp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>

        <q-btn flat dense round icon="menu" @click="toggleLeftDrawer()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("menu") }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center">
          {{ $t("appAdmName") }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="this.$router.push('/')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>

        <!-- login user-->
        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            rounded
            color="primary"
            dense
            icon="account_circle"
            @click="loginOnOff()"
            no-caps
          >
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="getUserName === ''"
              >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="getUserName !== ''"
              >{{ $t("logOut") }}
            </q-tooltip>

            <div
              itemid="USERID"
              v-if="getUserName !== ''"
              @click.prevent="profileUser()"
              style="margin-left: 5px"
            >
              {{ getUserName }}
              <q-tooltip v-if="getUserName !== ''"
                >{{ $t("myProfile") }}
              </q-tooltip>
            </div>
            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon" color="white" />
            </q-badge>
          </q-btn>
        </div>
        <!-- reg user-->
        <div>
          <q-btn
            class="q-pr-sm-sm"
            rounded
            color="primary"
            dense
            icon="person_add"
            @click="regUser()"
            style="margin-right: 10px"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate"
              >{{ $t("registration") }}
            </q-tooltip>
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
          {{ $t("company") }}
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer
      :width="230"
      class="q-pa-sm"
      style="font-size: 16px"
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      elevated
    >
      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth">
        {{ $t("notLogined") }}
      </h6>
      <h6 class="q-pa-md text-red text-bold" v-else-if="notAccess">
        {{ $t("notAccess") }}
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
            <q-icon :name="link.icon" />
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ link.title }}</q-item-label>
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
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import {useUserStore} from 'stores/user-store'
import {useI18n} from 'vue-i18n'
import RegUser from 'components/RegUser.vue'
import ProfileUser from 'components/ProfileUser.vue'
import LoginUser from 'components/LoginUser.vue'
import SetLocale from 'components/SetLocale.vue'
import {api, authURL, baseURL, urlMainApp} from 'boot/axios'
import {notifyError, notifySuccess} from 'src/utils/jsutils'
import {useQuasar} from 'quasar'

// Composables
const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const store = useUserStore()
const { isSysAdmin, getUserName, getUserId, getTarget } = storeToRefs(store)
const { setUserStore } = store

// Reactive state
const leftDrawerOpen = ref(true)

// Computed properties
const reqAuth = computed(() => getUserName.value === '')
const notAccess = computed(() => getTarget.value.length === 0 && !isSysAdmin.value)
const nameIcon = computed(() => getUserName.value === '' ? 'login' : 'logout')

// Essential links
const essentialLinks = computed(() => [
  {
    title: t('roles2'),
    icon: 'manage_accounts',
    link: '/roles/0',
    target: 'adm:role'
  },
  {
    title: t('users'),
    icon: 'supervisor_account',
    link: '/users/0/0',
    target: 'adm:usr'
  },
  {
    title: t('tml_permis'),
    icon: 'code',
    link: '/permis',
    target: 'adm:tml'
  }
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
    const lang = localStorage.getItem('curLang')
    leftDrawerOpen.value = true
    $q
      .dialog({
        component: LoginUser,
        componentProps: {
          lg: lang,
          // ...
        },
      })
      .onOk(() => {
        api
          .post(baseURL, {
            method: 'auth/getCurUserInfo',
            params: [],
          })
          .then(
            (response) => {
              setUserStore(response.data.result)
            },
            (error) => {
              //console.log("error", error);
              setUserStore({})
              notifyError(error.message)
            }
          )
          .finally(() => {
            router.push('/')
            //location.reload()
          })
      })
  } else {
    api
      .post(authURL + '/logout', {
        params: {},
      })
      .then(() => {
        setUserStore({})
      })
      .finally(() => {
        router.push('/')
        location.reload()
      })
  }
}

const regUser = async () => {
  const lang = localStorage.getItem('curLang')
  try {
    const result = await $q.dialog({
      component: RegUser,
      componentProps: { lg: lang }
    })

    if (result?.res) {
      notifySuccess(t('success'))
    }
  } catch (error) {
    notifyError(error.message)
  }
}

const profileUser = () => {
  if (getUserName.value) {
    const lang = localStorage.getItem('curLang')
    $q.dialog({
      component: ProfileUser,
      componentProps: {
        lg: lang,
        userId: getUserId.value
      }
    })
  }
}

// Lifecycle hooks
onMounted(() => {
  if (!getUserId.value > 0) {
    setUserStore({})
    router.push('/')
  }
})
</script>

<style></style>
