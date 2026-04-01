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
          {{ $t("appNSIName") }}
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
            class="q-pr-sm"
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
              v-else
              transition-hide="rotate"
              transition-show="rotate"
            >{{ $t("logOut") }}
            </q-tooltip>

            {{ getUserName }}

            <q-badge align="middle" color="primary" rounded>
              <q-icon :name="nameIcon()" color="white"/>
            </q-badge>
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

          <span class="absolute-right q-pt-sm">
          <a :href="site_url()" class="q-pr-md text-white" style="font-size: 12px"
             target="_blank"> {{ $t("fish_model") }} </a>
          </span>

        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer
      v-model="leftDrawerOpen"
      :width="230"
      bordered
      class="q-pa-sm"
      elevated
      show-if-above
    >
      <h6 v-if="reqAuth()" class="q-pa-md text-red text-bold">
        {{ $t("notLoginned") }}
      </h6>
      <h6 v-else-if="notAccess()" class="q-pa-md text-red text-bold">
        {{ $t("notAccess") }}
      </h6>

      <q-list v-for="link in essentialLinks" :key="link.title">
        <q-item
          v-if="hasTarget(link.target)"
          :to="link.link"
          active-class="text-bold text-blue"
          class="q-table--bordered bg-blue-1"
          clickable
          tag="a"
        >
          <q-item-section v-if="link.icon" avatar>
            <q-icon :name="link.icon" size="32px"/>
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ $t(link.title) }}</q-item-label>
            <q-item-label caption>{{ link.info }}</q-item-label>

          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script>
import {defineComponent, ref} from "vue";
import LoginUser from "components/LoginUser.vue";
import SetLocale from "components/SetLocale.vue";
import {api, authURL, urlMainApp} from 'boot/axios'
import {hasTarget} from "src/utils/jsutils";

import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import {useRouter} from "vue-router";
import {Notify} from "quasar";

export default defineComponent({
  name: "MainLayout",
  components: {SetLocale},

  data() {
    return {
      essentialLinks: ref([{}]),
    };
  },

  methods: {
    hasTarget,

    site_url() {
      return process.env.SITE_URL
    },

    mainApp() {
      //console.info("mainApp", urlMainApp)
      open(urlMainApp, "_self");
    },

    //sailing
    getLinks() {
      return [
        {
          title: "fishing_tools",
          info: "",
          icon: "phishing",
          link: "/fishing_tools",
          target: "nsi:ol",
        },
        {
          title: "desc_techniques",
          info: "",
          icon: "edit_document",
          link: "/desc_techniques",
          target: "nsi:om",
        },
        {
          title: "regulatory_docs",
          info: "",
          icon: "menu_book",
          link: "/regulatory_docs",
          target: "nsi:nd",
        },
        {
          title: "kato",
          info: "",
          icon: "home_work",
          link: "/kato",
          target: "nsi:kato",
        },
        {
          title: "struct_enterprise",
          info: "",
          icon: "apartment",
          link: "/struct_enterprise",
          target: "nsi:ose",
        },
      ];
    },
  },

  created() {
    console.info("Created!");
    const store = useUserStore();
    const {setUserStore} = store;
    const router = useRouter();
    const {getUserId} = storeToRefs(store);

    this.essentialLinks = this.getLinks();

    if (!getUserId.value > 0) {
      setUserStore({})
      router.push("/")
    }

  },

  setup() {
    console.info("Setup!");

    const leftDrawerOpen = ref(true);
    const store = useUserStore();
    const {isSysAdmin, getUserName, getTarget} = storeToRefs(store);
    const {setUserStore, clearUserStore} = store;
    const router = useRouter();

    return {
      getUserName,

      reqAuth() {
        return getUserName.value === "";
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
          const lang = localStorage.getItem("curLang");
          leftDrawerOpen.value = true;
          this.$q
            .dialog({
              component: LoginUser,
              componentProps: {
                lg: lang,
                // ...
              },
            })
            .onOk((res) => {
              setUserStore(res)
              router.push('/')
            });
        } else {
          api
            .post(authURL + "/logout", {
              params: [],
            })
            .then(() => {
              clearUserStore()
            })
            .finally(()=> {
              router.push('/')
            })
        }
      },

      leftDrawerOpen,
      toggleLeftDrawer() {
        leftDrawerOpen.value = !leftDrawerOpen.value;
      },
    };
  },
});
</script>

<style></style>
