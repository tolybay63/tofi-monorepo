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
        <q-btn
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
        </q-btn>

        <!-- App Title -->
        <q-toolbar-title class="text-center">
          {{ $t("appNSIName") }}
        </q-toolbar-title>

        <!-- Home Button -->
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
        <SetLocale />
      </q-toolbar>
    </q-header>

    <!-- Footer -->
    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Company Logo"/>
          </q-icon>
          {{ $t("company") }}
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <!-- Navigation Drawer -->
    <q-drawer
      :width="250"
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      elevated
    >
      <div class="q-pa-sm">
        <h6 class="text-red text-bold" v-if="reqAuth">
          {{ $t("notLogined") }}
        </h6>
        <h6 class="text-red text-bold" v-else-if="notAccess">
          {{ $t("notAccess") }}
        </h6>
        <q-list v-else>
          <q-item

            v-for="link in essentialLinks"
            v-show="hasTarget(link['target'])"
            :key="link['title']"
            class="q-table--bordered bg-blue-1"
            clickable
            tag="a"
            :to="link['link']"
            active-class="text-bold text-blue"
          >
            <q-item-section v-if="link['icon']" avatar>
              <q-icon :name="link.icon" size="32px"/>
            </q-item-section>

            <q-item-section>
              <q-item-label>{{ $t(link.title) }}</q-item-label>
              <q-item-label caption>{{ link.info }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </q-drawer>

    <!-- Main Content -->
    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent, ref, computed} from "vue";
import LoginUser from "components/LoginUser.vue";
import SetLocale from "components/SetLocale.vue";
import {api, authURL, baseURL, urlMainApp} from "boot/axios";
import {hasTarget, notifyError} from "src/utils/jsutils";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import {useQuasar} from "quasar";
import { useRouter } from 'vue-router';

const NAVIGATION_LINKS = [
  {
    title: "process_charts",
    info: "",
    icon: "assignment",
    link: "/process_charts",
    target: "nsi:ol",
  },
  {
    title: "source_collections",
    info: "",
    icon: "schedule",
    link: "/source_collections",
    target: "nsi:om",
  },
  {
    title: "types_objects_comps",
    info: "",
    icon: "dashboard",
    link: "/types_objects",
    target: "nsi:kato",
  },
  {
    title: "param_objects",
    info: "",
    icon: "pin",
    link: "/param_objects",
    target: "nsi:kato",
  },
  {
    title: "defect_objects",
    info: "",
    icon: "report_off",
    link: "/defect_objects",
    target: "nsi:kato",
  },
  {
    title: "charts_objects",
    info: "",
    icon: "assignment_turned_in",
    link: "/charts_objects",
    target: "nsi:ose",
  },
  {
    title: "separation_point",
    info: "",
    icon: "train",
    link: "/separation_point",
    target: "nsi:ose",
  },
  {
    title: "hauls",
    info: "",
    icon: "multiple_stop",
    link: "/hauls",
    target: "nsi:ose",
  },
];

export default defineComponent({
  name: "MainLayout",
  components: {SetLocale},

  setup() {
    const $q = useQuasar();
    const router = useRouter();
    const leftDrawerOpen = ref(true);
    const store = useUserStore();
    const {isSysAdmin, getUserName, getTarget} = storeToRefs(store);
    const {setUserStore} = store;

    const essentialLinks = ref(NAVIGATION_LINKS);

    const reqAuth = computed(() => getUserName.value === "");
    const notAccess = computed(() => getTarget.value.length === 0 && !isSysAdmin.value);
    const nameIcon = computed(() => getUserName.value === "" ? "login" : "logout");

    const mainApp = () => {
      open(urlMainApp, "_self");
    };

    const toggleLeftDrawer = () => {
      leftDrawerOpen.value = !leftDrawerOpen.value;
    };

    const toHome = function() {
      router.push("/")
    }

    const loginOnOff = async () => {
      if (getUserName.value === "") {
        const lang = localStorage.getItem("curLang");
        leftDrawerOpen.value = true;
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
                method: "data/getCurUserInfo",
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
                router.push("/");
              });
          });
      } else {
        try {
          await api.post(authURL + "/logout", { params: {} });
          setUserStore({});
          await router.push("/");
          location.reload();
        } catch (error) {
          notifyError(error.message);
        }
      }
    };

    return {
      toHome,
      leftDrawerOpen,
      essentialLinks,
      getUserName,
      reqAuth,
      notAccess,
      nameIcon,
      mainApp,
      toggleLeftDrawer,
      loginOnOff,
      hasTarget,
    };
  },

  created() {
    const store = useUserStore();
    const {getUserId} = storeToRefs(store);
    const router = useRouter();

    if (!getUserId.value > 0) {
      store.setUserStore({});
      router.push("/");
    }
  },
});
</script>

<style></style>
