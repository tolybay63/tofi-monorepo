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
          <a href="http://tofishstocks-model.kz" target="_blank" style="font-size: 12px" class="q-pr-md text-white"> {{$t("fish_model")}} </a>
          </span>

        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer :width="230" v-model="leftDrawerOpen" show-if-above bordered elevated class="q-pa-sm">
      <h6 class="q-pa-md text-red text-bold" v-if="reqAuth()">
        {{ $t('notLogined') }}
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
import {defineComponent, ref} from 'vue'
import LoginUser from 'components/LoginUser.vue'
import SetLocale from 'components/SetLocale.vue'
import {api, authURL, urlMainApp} from 'boot/axios'
import {notifyError} from 'src/utils/jsutils'

import {useUserStore} from 'stores/user-store'
import {storeToRefs} from 'pinia'

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
  },

  created() {
    console.info('Created!')
    const store = useUserStore()
    const { setUserStore } = store
    const { getUserId } = storeToRefs(store)

    if (!getUserId.value > 0) {
      setUserStore({})
      this.$router.push('/')
    }
  },

  setup() {
    console.info('Setup!')

    const leftDrawerOpen = ref(true)
    const store = useUserStore()
    const { isSysAdmin, getUserName, getTarget } = storeToRefs(store)
    const { setUserStore } = store

    let getLinks = () => {
      return [
        {
          title: 'typesOfFish',
          info: '',
          icon: 'set_meal',
          link: '/typesfish',
          target: 'mon:vr',
        },
        {
          title: 'reservoirs',
          info: '',
          icon: 'sailing',
          link: '/reservoirs',
          target: 'mon:vod',
        },
        {
          title: 'samplingStations',
          info: '',
          icon: 'houseboat',
          link: '/samplingstations',
          target: 'mon:st',
        },
        {
          title: 'piscesInReservoirs',
          info: '',
          icon: 'tsunami',
          link: '/piscesreservoirs',
          target: 'mon:rpv',
        },
        {
          title: 'sampling',
          info: '',
          icon: 'filter_alt',
          link: '/sampling',
          target: 'mon:zp',
        },
/*
        {
          title: 'samplingResults',
          info: '',
          icon: 'free_cancellation',
          link: '/samplingresults',
          target: 'mon:rzp',
        },
*/
        {
          title: 'test',
          info: '',
          icon: 'download',
          link: '/test',
          target: 'mon:fill',
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
          const lang = localStorage.getItem('curLang')
          leftDrawerOpen.value = true
          this.$q
            .dialog({
              component: LoginUser,
              componentProps: {
                lg: lang,
                // ...
              },
            })
            .onOk(() => {
              api
                .post('', {
                  method: 'data/getCurUserInfo',
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
                  this.$router.push('/')
                  //location.reload()
                })
            })
        } else {
          this.$axios
            .post(authURL + '/logout', {
              params: {},
            })
            .then(() => {
              setUserStore({})
            })
            .finally(() => {
              this.$router.push('/')
              location.reload()
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
