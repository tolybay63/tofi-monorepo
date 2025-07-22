<template xmlns="http://www.w3.org/1999/html">
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

        <q-toolbar-title class="text-center">
          {{ $t("appUserDataName") }}
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

        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            color="primary" dense
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
            >
              {{ getUserName }}
            </div>

            <q-badge align="middle" color="primary" rounded>
              <q-icon :name="nameIcon()" color="white"/>
            </q-badge>
          </q-btn>
        </div>


        <div class="q-gutter-md q-pr-md">
          <q-btn icon="notifications" round>
            <q-badge color="red" floating label="3" rounded/>
          </q-btn>
        </div>

        <!-- Текущий язык-->
        <SetLocale></SetLocale>

      </q-toolbar>

      <q-tabs align="left" v-if="getUserName !== ''">
        <q-route-tab to="/myprofile" :label="$t('myProfile')" icon="manage_accounts" />
        <q-route-tab to="/groupuser" :label="$t('groupUsers')" icon="groups" />

      </q-tabs>

    </q-header>

    <q-footer elevated reveal>
      <q-toolbar>

        <q-toolbar-title class="flex flex-center">
          <q-icon class="q-pa-sm">
            <img alt="Logo" src="../assets/factor.png"/>
          </q-icon>
          {{ $t("company") }}
        </q-toolbar-title>

      </q-toolbar>
    </q-footer>

<!--
    <q-drawer
      v-model="leftDrawerOpen"
      :width="230"
      bordered
      elevated
      show-if-above

    >

    </q-drawer>
-->

    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent, ref} from "vue";
import SetLocale from "components/SetLocale.vue";
import {api, authURL, baseURL, urlMainApp} from "boot/axios";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import {QSpinnerFacebook, useQuasar} from "quasar";
import {notifyError} from "src/utils/jsutils.js";
const store = useUserStore();

export default defineComponent({
  name: "MainLayout",
  components: {SetLocale},

  created() {
    const $q = useQuasar()
    $q.loading.show({
      boxClass: 'bg-grey-2 text-grey-9',
      spinner: QSpinnerFacebook,
      spinnerColor: 'yellow',
      spinnerSize: 140,
      backgroundColor: 'purple',
      message: 'Обработка плоских таблиц...',
      messageColor: 'red'
    })

    api
      .post(baseURL, {
        method: "data/analizeFlatTable",
        params: ["DB_UserData"],
      })
      .then(
        () => {

        },
        (error) => {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length>1) ? " ["+msgs[1]+"]" : ""
          let msg = m1 + m2
          notifyError(msg);
        }
      )
      .finally(() => {
        $q.loading.hide()
        this.$router["push"]("/auth")
      })
  },

  setup() {

    const store = useUserStore();
    const {isSysAdmin, getUserName, getTarget} =
      storeToRefs(store);
    const {setUserStore, setUserName } = store;

    return {
      getUserName,

      mainApp() {
        open(urlMainApp, "_self");
      },

      notAccess() {
        return getTarget.value.length === 0 && !isSysAdmin.value;
      },

      nameIcon() {
        if (getUserName.value === "") return "login";
        else return "logout";
      },

      loginOnOff() {
        //console.info("OnOff")
        if (getUserName.value === "") {
          this.$router.push("/")
          setTimeout(()=>{
            this.$router.push("/auth");
          }, 200)

        } else {
          api
            .post(authURL + "/logout", {
              params: {},
            })
            .then(() => {
              setUserStore({})
              setUserName("")
            })
            .finally(() => {
              this.$router.push("/auth");
              //this.$router.push("/");
              //location.reload()
            });
        }
      },
    };
  },
});
</script>

<style></style>
