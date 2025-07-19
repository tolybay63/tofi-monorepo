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
          @click="mainApp"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>


        <q-btn flat dense round icon="menu" @click="toggleLeftDrawer">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("menu") }}
          </q-tooltip>
        </q-btn>


        <q-toolbar-title  class="text-center">
          {{ $t("appModelName") }}
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
          <q-tooltip transition-show="rotate" transition-hide="rotate"
          >{{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>

        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            rounded
            color="primary"
            dense
            icon="account_circle"
            @click="loginOnOff"
            no-caps
          >
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="userName === ''"
            >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              transition-show="rotate"
              transition-hide="rotate"
              v-if="userName !== ''"
            >{{ $t("logOut") }}
            </q-tooltip>
              {{ userName }}
            <q-badge rounded color="primary" align="middle">
              <q-icon :name="nameIcon()" color="white"/>
            </q-badge>
          </q-btn>
        </div>
        <!-- Текущий язык-->
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

          <span class="absolute-right q-pt-sm" v-if="metamodel==='fish'">
          <a href="http://tofishstocks-model.kz" target="_blank" style="font-size: 12px" class="q-pr-md text-white"> {{$t("fish_model")}} </a>
          </span>

        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      class="bg-blue-1"
      elevated
      :width="270"
    >
      <div class="q-pa-md">
        <q-tree
          :nodes="getMenu()"
          node-key="id"
          label-key="label"
          selected-color="blue"
          :no-nodes-label="getMsg()"
          control-color="red"
          :color="getClr()"
          :style="getStyle()"
          v-model:selected="selected"
          :duration="500"
          @update:selected="updateSelected"
          no-selection-unset
          accordion
        />
      </div>
    </q-drawer>

    <q-page-container>
      <router-view/>
    </q-page-container>
  </q-layout>
</template>

<script>
import SetLocale from "components/SetLocale.vue";
import {computed, defineComponent, ref} from "vue";
import LoginUser from "components/LoginUser.vue";
import {api, authURL, baseURL, fnMeta, urlMainApp} from "boot/axios";
import {notifyError} from "src/utils/jsutils";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import {useRouter} from "vue-router";
import {extend} from "quasar";
import {useQuasar} from "quasar";

export default defineComponent({
  components: {
    SetLocale,
  },

  created() {
    console.info("Created!");
    const store = useUserStore();
    const { setUserStore } = store;
    const { getUserId } = storeToRefs(store);

    fnMeta()

    if (!getUserId.value > 0) {
      setUserStore({})
      this.$router["push"]("/")
    }

  },

  setup() {
    console.info("Setup")

    const selected = ref(null);
    const leftDrawerOpen = ref(false);

    const store = useUserStore();
    const {isSysAdmin, getUserName, getTarget, metamodel} =
      storeToRefs(store);
    const {setUserStore} = store;
    const router = useRouter();
    const $q = useQuasar();

    const userName = computed(() => getUserName.value);
    //console.info("target", getTarget.value)

    const setMenu = (parent, chlds) => {
      const res = {};
      if (
        isSysAdmin.value ||
        (getTarget.value.includes(parent.target) && chlds.length > 0)
      )
        extend(true, res, parent);
      const chs = [];
      chlds.forEach((ch) => {
        if (isSysAdmin.value || getTarget.value.includes(ch.target)) {
          chs.push(ch);
        }
      });
      extend(true, res, {children: chs});
      return res;
    };

    return {
      userName,
      metamodel,
      selected,
      mainApp() {
        open(urlMainApp, "_self");
      },

      nameIcon() {
        if (userName.value === "") return "login";
        else return "logout";
      },

      updateSelected() {
        router.push(selected.value);
      },

      loginOnOff() {
        //console.info("OnOff")
        if (userName.value === "") {
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
                  method: "auth/getCurUserInfo",
                  params: [],
                })
                .then(
                  (response) => {
                    setUserStore(response.data.result);
                  },
                  (error) => {
                    console.log("error", error);
                    setUserStore({});
                    notifyError(error.message);
                  }
                )
                .finally(() => {
                  router.push("/");
                  //location.reload()
                });
            });
        } else {
          api
            .post(authURL + "/logout", {
              params: {},
            })
            .then(() => {
              setUserStore({})
              router.push("/")
              location.reload()
            })
        }
      },

      getMenu() {
        let menu = [];

        //todo Добавлен 07.02.24, permis не настроен...
        const mn_as = [
          {
            id: "/database",
            label: this.$t("database"),
            icon: "storage",
            target: "mdl:mn_as:database",
          },
        ];

        const mn_ds = [
          {
            id: "/measure",
            label: this.$t("measures"),
            icon: "square_foot",
            target: "mdl:mn_ds:mea",
          },
          {
            id: "/attrib",
            label: this.$t("attributs"),
            icon: "format_shapes",
            target: "mdl:mn_ds:attr",
          },
          {
            id: "/factor/0",
            label: this.$t("factors"),
            icon: "account_tree",
            target: "mdl:mn_ds:fac",
          },
          {
            id: "/meter/0",
            label: this.$t("meters"),
            icon: "scale",
            target: "mdl:mn_ds:meter",
          },
          {
            id: "/role",
            label: this.$t("roles"),
            icon: "perm_contact_calendar",
            target: "mdl:mn_ds:role",
          },
          {
            id: "/typ/0",
            label: this.$t("typs"),
            icon: "view_quilt",
            target: "mdl:mn_ds:typ",
          },
          {
            id: "/reltyp/0",
            label: this.$t("reltyps"),
            icon: "view_column",
            target: "mdl:mn_ds:reltyp",
          },
          {
            id: "/props/0/0",
            label: this.$t("props"),
            icon: "app_registration",
            target: "mdl:mn_ds:prop",
          },
          {
            id: "/dimMultiProp/0/0",
            label: this.$t("dimMultiProp"),
            icon: "type_specimen",
            target: "mdl:mn_ds:pmdim",
          },
          {
            id: "/multiProp/0/0",
            label: this.$t("multiProp"),
            icon: "view_in_ar",
            target: "mdl:mn_ds:pm",
          },
          {
            id: "/flatTable",
            label: this.$t("flatTable"),
            icon: "table_rows",
            target: "mdl:mn_ds:ft",
          },
          {
            id: "/chargr/0/typchargr",
            label: this.$t("charprop"),
            icon: "table_chart",
            target: "mdl:mn_ds:cgp",
          },
        ];
        const mn_dp = [
          {
            id: "/scale/0",
            label: this.$t("scales"),
            icon: "device_thermostat",
            target: null,
          },

          {
            id: "/dimsperiod/0",
            label: this.$t("dimsPeriod"),
            icon: "pending_actions",
            target: "mdl:mn_dp:dmper",
          },
          {
            id: "/dimsprop/0/0",
            label: this.$t("dimsProp"),
            icon: "credit_score",
            target: "mdl:mn_dp:dmprop",
          },
          {
            id: "/dimsobj/0/0",
            label: this.$t("dimsObj"),
            icon: "pattern",
            target: "mdl:mn_dp:dmobj",
          },
          {
            id: "/cubes/0/0",
            label: this.$t("cubes"),
            icon: "view_in_ar",
            target: "mdl:mn_dp:cube",
          },
        ];
        const mn_tool = [
          /*          {
                      id: "/impObj",
                      label: "Импорт объектов с текстового файла",
                      icon: "cloud_download",
                      target: "mdl:mn_tool:impobj",
                    },
                    {
                      id: "/impFile",
                      label: "Скачать файл...",
                      icon: "cloud_download",
                      target: "mdl:mn_tool:impfile",
                    },*/
        ];


        if (userName.value === "" || (!isSysAdmin.value && !getTarget.value.includes("mdl")))
          return [];

        const el0 = setMenu(
          {
            id: "/dbSetting",
            label: this.$t("dbSetting"),
            icon: "settings",
            target: "mdl:mn_as",
          },
          mn_as
        );
        menu.push(el0);
        //

        const el1 = setMenu(
          {
            id: "/dataSetting",
            label: this.$t("dataSetting"),
            icon: "settings",
            target: "mdl:mn_ds",
          },
          mn_ds
        );
        menu.push(el1);
        //
        const el2 = setMenu(
          {
            id: "/dataProcessing",
            label: this.$t("dataProcessing"),
            icon: "settings",
            target: "mdl:mn_dp",
          },
          mn_dp
        );
        menu.push(el2);
        //
        const el4 = setMenu(
          {
            id: "/toolsAnalitic",
            label: this.$t("toolsAnalitic"),
            icon: "settings",
            target: "mdl:mn_tool",
          },
          mn_tool
        );
        menu.push(el4);
        //
        return menu;
      },

      getMsg() {
        if (userName.value !== "") return this.$t("notAccess");
        else return this.$t("notLogined");
      },

      getClr() {
        if (this.getMenu().length > 0) return "black";
        else return "red";
      },

      getStyle() {
        if (this.getMenu().length > 0) return "font-size: 16px";
        else return "font-size: 24px; text-align: center";
      },

      leftDrawerOpen,
      toggleLeftDrawer() {
        leftDrawerOpen.value = !leftDrawerOpen.value;
      },
    };
  },
});
</script>

<style>
</style>
