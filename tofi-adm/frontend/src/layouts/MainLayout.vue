<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>

        <!--Main App -->
        <q-btn
          class="q-mr-md"
          color="primary"
          dense
          icon="grid_view"
          rounded
          @click="mainApp()"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>

        <q-btn dense flat icon="menu" round @click="toggleLeftDrawer()">
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("menu") }}
          </q-tooltip>
        </q-btn>

        <q-toolbar-title class="text-center">
          {{ $t("appAdmName") }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          color="primary"
          dense
          icon="home"
          rounded
          @click="this.$router.push('/')"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>

        <!-- login user-->
        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            color="primary"
            dense
            icon="account_circle"
            no-caps
            rounded
            @click="loginOnOff()"
          >
            <q-tooltip
              v-if="getUserName === ''"
              transition-hide="rotate"
              transition-show="rotate"
            >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              v-if="getUserName !== ''"
              transition-hide="rotate"
              transition-show="rotate"
            >{{ $t("logOut") }}
            </q-tooltip>

            <div
              v-if="getUserName !== ''"
              itemid="USERID"
              style="margin-left: 5px"
              @click.prevent="profileUser()"
            >
              {{ getUserName }}
              <q-tooltip v-if="getUserName !== ''"
              >{{ $t("myProfile") }}
              </q-tooltip>
            </div>
            <q-badge align="middle" color="primary" rounded>
              <q-icon :name="nameIcon" color="white"/>
            </q-badge>
          </q-btn>
        </div>
        <!-- reg user-->
        <div>
          <q-btn
            class="q-pr-sm-sm"
            color="primary"
            dense
            icon="person_add"
            rounded
            style="margin-right: 10px"
            @click="regUser()"
          >
            <q-tooltip transition-hide="rotate" transition-show="rotate"
            >{{ $t("registration") }}
            </q-tooltip>
          </q-btn>
        </div>

        <!-- Текущий язык-->
        <SetLocale></SetLocale>
      </q-toolbar>
    </q-header>

    <q-footer elevated reveal>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          <q-icon class="q-pa-sm">
            <img alt="Logo" src="../assets/factor.png"/>
          </q-icon>
          {{ $t("company") }}
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer
      v-model="leftDrawerOpen"
      :width="250"
      bordered
      class="q-pa-sm bg-blue-1"
      elevated
      show-if-above
      style="font-size: 16px"
    >
      <h6 v-if="reqAuth" class="q-pa-md text-red text-bold">
        {{ $t("notLoginned") }}
      </h6>
      <h6 v-else-if="notAccess" class="q-pa-md text-red text-bold">
        {{ $t("notAccessService") }}
      </h6>
      <q-list v-for="item in essentialLinks" :key="item.title" v-else>
        <q-item
          v-if="hasTarget(item.target)"
          :active="isActive(item.to)"
          :to="item.to"
          active-class="text-bold text-blue bg-blue-2"
          class="q-pl-xl q-table--bordered"
          clickable
        >
          <q-item-section v-if="item.icon" avatar>
            <q-icon :name="item.icon"/>
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ item.title }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>

    </q-drawer>

    <q-page-container>
      <router-view/>
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
import {api, authURL, urlMainApp} from 'boot/axios'
import {notifyError, notifySuccess} from 'src/utils/jsutils'
import {useQuasar} from 'quasar'

// Composables
const router = useRouter()
const $q = useQuasar()
const {t} = useI18n()
const store = useUserStore()
const {isSysAdmin, getUserName, getUserId, getTarget} = storeToRefs(store)
const {setUserStore, clearUserStore} = store

// Reactive state
const leftDrawerOpen = ref(true)
// Computed properties
const reqAuth = computed(() => getUserName.value === '')
const notAccess = computed(() => !getTarget.value.includes("adm") && !isSysAdmin.value)
const nameIcon = computed(() => getUserName.value === '' ? 'login' : 'logout')

// Essential links
const essentialLinks = computed(() => [
  {
    title: t('roles2'),
    icon: 'manage_accounts',
    to: '/roles/0',
    target: 'adm:role'
  },
  {
    title: t('users'),
    icon: 'supervisor_account',
    to: '/users/0/0',
    target: 'adm:usr'
  },
  {
    title: t('tml_permis'),
    icon: 'code',
    to: '/permis',
    target: 'adm:tml'
  }
])

// Methods
const mainApp = () => {
  window.open(urlMainApp, '_self')
}

const isActive = (menuTo) => {
  if (!menuTo || !router.currentRoute.value.path) {
    return false;
  }
  const menuBase = menuTo.split('/')[1];
  const currentBase = router.currentRoute.value.path.split('/')[1];
  return menuBase === currentBase;
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
    $q
      .dialog({
        component: LoginUser,
        componentProps: {
          // ...
        },
      })
      .onOk((res) => {
        setUserStore(res)
        router.push('/')
        api
          .post("", {
            method: "auth/checkTarget",
            params: ["adm"],
          })
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
}

const regUser = async () => {
  try {
    $q.dialog({
      component: RegUser,
    })
      .onOk((response) => {
        if (response.res) {
          notifySuccess(t('success'))
        }
      })
  } catch (error) {
    notifyError(error.message)
  }
}

const profileUser = () => {
  if (getUserName.value && getUserId.value !== 1) {
    $q.dialog({
      component: ProfileUser,
      componentProps: {
        userId: getUserId.value
      }
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

<style></style>
