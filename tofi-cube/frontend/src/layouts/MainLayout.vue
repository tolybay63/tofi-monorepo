<!-- <q-inner-loading :showing="loading" color="secondary"/> -->

<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar>
        <!-- Main App Button -->
        <q-btn
          class="q-mr-md"
          rounded
          color="primary"
          dense
          icon="grid_view"
          @click="mainApp"
          :aria-label="$t('appName')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>


        <!-- Menu Toggle -->
<!--        <q-btn
          flat
          dense
          round
          icon="menu"
          @click="toggleLeftDrawer"
          :aria-label="$t('menu')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("menu") }}
          </q-tooltip>
        </q-btn>-->

        <!-- App Title -->
        <q-toolbar-title class="text-center">
          {{ $t("appCubeName") }}
        </q-toolbar-title>

        <!-- Home Button -->
<!--
        <q-btn
          class="q-pa-md-sm"
          rounded
          color="primary"
          dense
          icon="home"
          @click="toHome"
          :aria-label="$t('mainPage')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>
-->

        <!-- User Login/Logout -->
        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm"
            rounded
            color="primary"
            dense
            icon="account_circle"
            @click="loginOnOff"
            no-caps
            :aria-label="getUserName ? $t('logOut') : $t('logIn')"
          >
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="!getUserName"
            >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              v-else
              transition-show="rotate"
              transition-hide="rotate"
            >{{ $t("logOut") }}
            </q-tooltip>

            {{ getUserName }}

            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon" color="white"/>
            </q-badge>
          </q-btn>
        </div>

        <!-- Language Selector -->
        <SetLocale/>


      </q-toolbar>
    </q-header>

    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="flex flex-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Logo"/>
          </q-icon>
          {{ $t("company") }}

        </q-toolbar-title>
      </q-toolbar>
    </q-footer>


<!--
    <q-drawer
      :width="400"
      v-model="leftDrawerOpen"
      show-if-above
      bordered
    >
      <div class="q-pa-sm">

        <h6 class="text-red text-bold" v-if="reqAuth">
          {{ $t("notLogined") }}
        </h6>
        <h6 class="text-red text-bold" v-else-if="notAccess">
          {{ $t("notAccess") }}
        </h6>
      </div>

    </q-drawer>
-->

    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script setup>

import {computed} from "vue";
import SetLocale from "components/SetLocale.vue";
import {useUserStore} from "stores/user-store.js";
import {storeToRefs} from "pinia";
import {api, authURL, baseURL, urlMainApp} from "boot/axios.js";
import {notifyError} from "src/utils/jsutils.js";
import LoginUser from "components/LoginUser.vue";
import {useQuasar} from "quasar";
import {useRouter} from "vue-router";

const store = useUserStore();
const {getUserName/*, isSysAdmin, getTarget*/} = storeToRefs(store);
const {setUserStore} = store;
const $q = useQuasar();
const router = useRouter();

const mainApp = () => {
  open(urlMainApp, "_self");
};

/*const toHome = function () {
  router.push("/")
}*/

//let reqAuth = computed(() => getUserName.value === "");
//let notAccess = computed(() => getTarget.value.length === 0 && !isSysAdmin.value);
let nameIcon = computed(() => getUserName.value === "" ? "login" : "logout");

const loginOnOff = async () => {
  if (getUserName.value === "") {
    const lang = localStorage.getItem("curLang");
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
            method: "cube/getCurUserInfo",
            params: [],
          })
          .then(
            (response) => {
              setUserStore(response.data.result);
            },
            (error) => {
              //console.log("error", error);
              setUserStore({});
              notifyError(error.message);
            }
          )
          .finally(() => {
            router.push("/cubes");
          });
      });
  } else {
    try {
      await api.post(authURL + "/logout", {params: {}});
      setUserStore({});
      await router.push("/");
      location.reload();
    } catch (error) {
      notifyError(error.message);
    }
  }
}


</script>
