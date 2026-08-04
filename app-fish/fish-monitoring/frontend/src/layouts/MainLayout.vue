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
          {{ $t('appMonitoringName') }}
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
              <q-icon :name="nameIcon()" color="white" />
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
          <a :href="site_url()" target="_blank" style="font-size: 12px" class="q-pr-md text-white"> {{$t("fish_model")}} </a>
          </span>

        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer :width="230" v-model="leftDrawerOpen" show-if-above bordered elevated class="q-pa-sm bg-blue-1">
      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth()">
        {{ $t('notLoginned') }}
      </h6>
      <h6 class="q-pa-md text-red text-bold" v-else-if="notAccess()">
        {{ $t('notAccess') }}
      </h6>

      <q-list v-else>
        <template v-for="item in essentialLinks" :key="item.label">
          <q-expansion-item v-if="item.children && hasTarget(item.target)"
                            :icon="item.icon" :label="$t(item.label)" class="q-table--bordered"
          >
            <q-item
              v-for="subItem in item.children"
              :key="$t(subItem.label)"
              v-ripple
              :active="isActive(subItem.to)"
              :to="subItem.to"
              active-class="text-bold text-blue bg-blue-2"
              class="q-pl-xl q-table--bordered"
              clickable
            >
              <q-item-section avatar>
                <q-icon :name="subItem.icon"/>
              </q-item-section>
              <q-item-section>{{ $t(subItem.label) }}</q-item-section>
            </q-item>
          </q-expansion-item>

          <q-item v-else v-if="hasTarget(item.target)"
                  v-ripple
                  :active="isActive(item.to)"
                  :to="item.to"
                  active-class="text-bold text-blue bg-blue-2"
                  clickable>
            <q-item-section avatar>
              <q-icon :name="item.icon"/>
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

<script>
import {defineComponent, ref} from 'vue'
import LoginUser from 'components/LoginUser.vue'
import SetLocale from 'components/SetLocale.vue'
import {api, authURL, urlMainApp} from 'boot/axios'

import {useUserStore} from 'stores/user-store'
import {storeToRefs} from 'pinia'
import {useRouter} from "vue-router";
import axios from "axios";
import {useQuasar} from "quasar";

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

    isActive(menuTo) {
      if (!menuTo || !this.$route.path) return false;
      const menuBase = menuTo.split('/')[1];
      const currentBase = this.$route.path.split('/')[1];
      return menuBase === currentBase;
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
    const $q = useQuasar();


    let getLinks = () => {
      return [
        {
          label: "kato",
          icon: "home_work",
          to: "/kato",
          target: "mon:kato",
        },
        {
          label: 'reservoirs',
          icon: 'sailing',
          to: '/reservoirs',
          target: 'mon:vod',
        },

        {
          label: 'samplingStations',
          icon: 'houseboat',
          to: '/samplingstations',
          target: 'mon:st',
        },
        {
          label: 'typesOfFish',
          icon: 'set_meal',
          to: '/typesfish',
          target: 'mon:tf',
        },

        {
          label: 'FishGear',
          icon: 'phishing',
          to: '/fishGear',
          target: 'mon:fg',
        },

        {
          label: 'piscesInReservoirs',
          icon: 'tsunami',
          to: '/piscesreservoirs',
          target: 'mon:rpv',
        },

        {
          label: 'fishing',
          icon: 'location_on',
          to: '/fishing',
          target: 'mon:fish',
        },
        {
          label: 'fill',
          icon: 'download',
          to: '/fill',
          target: 'mon:fill',
        },

/*        {
          label: 'charts',
          icon: 'folder_open',
          target: 'mon:charts',
          children: [
            {
              label: 'chart1',
              icon: 'legend_toggle',
              to: '/chart1',
              target: 'mon:chart1',
            },
            {
              label: 'chart7',
              icon: 'legend_toggle',
              to: '/chart7',
              target: 'mon:chart7',
            },



          ]
        },*/

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
        return !getTarget.value.includes("mon") && !isSysAdmin.value
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

              api
                .post("", {
                  method: "auth/checkTarget",
                  params: ["mon"],
                })
            })
        } else {
          axios
            .post(authURL + '/logout', new URLSearchParams()) // <-- Склеиваем динамически со слэшем!
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
      },

      leftDrawerOpen,
      toggleLeftDrawer() {
        leftDrawerOpen.value = !leftDrawerOpen.value
      },
    }
  },
})
</script>

<style></style>
