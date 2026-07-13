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

        <q-toolbar-title class="text-center">
          {{ $t('appPersonnelName') }}
        </q-toolbar-title>

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

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent} from 'vue'
import LoginUser from '../components/LoginUser.vue'
import SetLocale from '../components/SetLocale.vue'
import {api, authURL, urlMainApp} from '../boot/axios'

import {useUserStore} from '../stores/user-store'
import {storeToRefs} from 'pinia'
import {useRouter} from "vue-router";
import {Notify} from "quasar";

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

  mounted() {
    const store = useUserStore()
    const { isSysAdmin, getUserName, getTarget } = storeToRefs(store)

    if (getUserName.value === '') {
      Notify.create({
        type: 'negative',
        message: this.$t("notLoginned"),
        position: 'top',
        timeout: 5000,
        actions: [{ icon: 'close', color: 'white' }]
      });
    } else if (getTarget.length === 0 && !isSysAdmin.value) {
      Notify.create({
        type: 'negative',
        message: this.$t("notAccess"),
        position: 'top',
        timeout: 5000,
        actions: [{ icon: 'close', color: 'white' }]
      });
    }
  },

  setup() {
    console.info('Setup!')
    const store = useUserStore()
    const { getUserName } = storeToRefs(store)
    const { setUserStore, clearUserStore } = store
    const router = useRouter()

    return {
      getUserName,
      nameIcon() {
        if (getUserName.value === '') return 'login'
        else return 'logout'
      },

      loginOnOff() {
        if (getUserName.value === '') {
          this.$q
            .dialog({
              component: LoginUser,
              componentProps: {
                // ...
              },
            })
            .onOk((res) => {
              setUserStore(res)
              router.push('/main')
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
              //
              this.$q
                .dialog({
                  component: LoginUser,
                  componentProps: {
                    // ...
                  },
                })
                .onOk((res) => {
                  setUserStore(res)
                  router.push('/main')
                })
            })
        }
      },
    }
  },
})
</script>

<style></style>
