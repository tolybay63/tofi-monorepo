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
          @click="mainApp"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("appName") }}
          </q-tooltip>
        </q-btn>


        <q-btn dense flat icon="menu" round @click="toggleLeftDrawer">
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("menu") }}
          </q-tooltip>
        </q-btn>


        <q-toolbar-title class="text-center">
          {{ $t("appModelName") }}
        </q-toolbar-title>

        <!--Home -->
        <q-btn
          class="q-pa-md-sm"
          color="primary"
          dense
          icon="home"
          rounded
          @click="toHome"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate"
          >{{ $t("mainPage") }}
          </q-tooltip>
        </q-btn>

        <div class="q-pa-md q-gutter-sm">
          <q-btn
            class="q-pr-sm-sm"
            color="primary"
            dense
            icon="account_circle"
            no-caps
            rounded
            @click="loginOnOff"
          >
            <q-tooltip
              v-if="userName === ''"
              transition-hide="rotate"
              transition-show="rotate"
            >{{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              v-if="userName !== ''"
              transition-hide="rotate"
              transition-show="rotate"
            >{{ $t("logOut") }}
            </q-tooltip>
            {{ userName }}
            <q-badge align="middle" color="primary" rounded>
              <q-icon :name="nameIcon()" color="white"/>
            </q-badge>
          </q-btn>
        </div>
        <!-- Текущий язык-->
        <SetLocale/>
      </q-toolbar>
    </q-header>

    <q-footer elevated reveal>
      <q-toolbar>
        <q-toolbar-title class="flex flex-center">
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
      :width="270"
      class="bg-blue-1"
      elevated
      show-if-above
    >

      <h6 v-if="notAccess" class="q-pa-md text-red text-bold">
        {{ $t("notAccessService") }}
      </h6>
      <h6 v-else-if="reqAuth" class="q-pa-md text-red text-bold">
        {{ $t("notLoginned") }}
      </h6>

      <q-list v-else>
        <template v-for="item in linksList()" :key="item.label">
          <q-expansion-item v-if="item.children && hasTarget(item.target)"
                            :icon="item.icon" :label="item.label"
          >
            <q-item
              v-for="subItem in item.children"
              :key="subItem.label"
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
              <q-item-section>{{ subItem.label }}</q-item-section>
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
            <q-item-section>{{ item.label }}</q-item-section>
          </q-item>
        </template>
      </q-list>

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
import {api, authURL, urlMainApp} from "boot/axios";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import {useRouter} from "vue-router";
import {extend, useQuasar} from "quasar";
import {hasTarget} from "src/utils/jsutils.js";
import * as http from "node:http";

export default defineComponent({
  components: {
    SetLocale,
  },

  methods: {
  hasTarget
  },

  created() {
    const store = useUserStore();
    const {clearUserStore} = store;
    const {getUserId} = storeToRefs(store);

    if (getUserId.value < 1) {
      clearUserStore()
      this.$router["push"]("/")
    }

  },

  setup() {
    //console.info("Setup")

    const selected = ref(null);
    const leftDrawerOpen = ref(false);

    const store = useUserStore();
    const { getUserName, getUserId, getTarget } = storeToRefs(store);
    const {setUserStore, clearUserStore} = store;
    const router = useRouter();
    const $q = useQuasar();

    const userName = computed(() => getUserName.value);

    const reqAuth = computed(() => getUserName.value === '')
    const notAccess = computed(() => !getTarget.value["includes"]("meta") && getUserId.value > 1)


    return {
      userName,
      reqAuth,
      notAccess,
      selected,

      toHome() {
        router.push("/")
      },

      mainApp() {
        open(urlMainApp, "_self");
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

      nameIcon() {
        if (userName.value === "") return "login";
        else return "logout";
      },


      loginOnOff() {
        //console.info("OnOff")
        if (userName.value === "") {
          leftDrawerOpen.value = true;
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
                  params: ["meta"],
                })
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

      linksList() {
        return [
          {
            to: '/database',
            label: this.$t('database'),
            icon: 'storage',
            target: 'meta:db',
          },
          //Настройка данных
          {
            to: '/dataSetting',
            label: this.$t('dataSetting'),
            icon: 'folder_open',
            target: 'meta:mn_ds',
            children: [
              {
                to: '/measure',
                label: this.$t('measures'),
                icon: 'square_foot',
                target: 'meta:mn_ds:mea',
              },
              {
                to: '/attrib',
                label: this.$t('attributs'),
                icon: 'format_shapes',
                target: 'meta:mn_ds:attr',
              },
              {
                to: '/factor/0',
                label: this.$t('factors'),
                icon: 'account_tree',
                target: 'meta:mn_ds:fac',
              },
              {
                to: '/meter/0',
                label: this.$t('meters'),
                icon: 'scale',
                target: 'meta:mn_ds:meter',
              },
              {
                to: '/role',
                label: this.$t('roles'),
                icon: 'perm_contact_calendar',
                target: 'meta:mn_ds:role',
              },
              {
                to: '/typ/0',
                label: this.$t('typs'),
                icon: 'view_quilt',
                target: 'meta:mn_ds:typ',
              },
              {
                to: '/reltyp/0',
                label: this.$t('reltyps'),
                icon: 'view_column',
                target: 'meta:mn_ds:reltyp',
              },
              {
                to: '/props/0/0',
                label: this.$t('props'),
                icon: 'app_registration',
                target: 'meta:mn_ds:prop',
              },
              {
                to: '/dimMultiProp/0/0',
                label: this.$t('dimMultiProp'),
                icon: 'type_specimen',
                target: 'meta:mn_ds:pmdim',
              },
              {
                to: '/multiProp/0/0',
                label: this.$t('multiProp'),
                icon: 'view_in_ar',
                target: 'meta:mn_ds:pm',
              },
              {
                to: '/flatTable',
                label: this.$t('flatTable'),
                icon: 'table_rows',
                target: 'meta:mn_ds:ft',
              },
              {
                to: '/chargr/0/typchargr',
                label: this.$t('charprop'),
                icon: 'table_chart',
                target: 'meta:mn_ds:cgp',
              },
            ],
          },
          //Настройка обработки данных
          {
            to: '/dataProcessing',
            label: this.$t('dataProcessing'),
            icon: 'folder_open',
            target: 'meta:mn_dp',
            children: [
              {
                to: '/dimsperiod/0',
                label: this.$t('dimsPeriod'),
                icon: 'pending_actions',
                target: 'meta:mn_dp:dmper',
              },
              {
                to: '/dimsprop/0/0',
                label: this.$t('dimsProp'),
                icon: 'credit_score',
                target: 'meta:mn_dp:dmprop',
              },
              {
                to: '/dimsobj/0/0',
                label: this.$t('dimsObj'),
                icon: 'pattern',
                target: 'meta:mn_dp:dmobj',
              },
              {
                to: '/cubes/0/0',
                label: this.$t('cubes'),
                icon: 'view_in_ar',
                target: 'meta:mn_dp:cube',
              },
            ],
          },
          //Вспомогательные сущности
          {
            to: '/dopEntities',
            label: this.$t('dopEntities'),
            icon: 'folder_open',
            target: 'meta:mn_dop_entity',
            children: [
              {
                to: '/stocks/0/0',
                label: this.$t('stocks'),
                icon: 'devices',
                target: 'meta:mn_dop:stocks',
              },

              {
                to: '/syscoding',
                label: this.$t('syscoding'),
                icon: 'pin',
                target: 'meta:mn_dop:syscoding',
              },
              {
                to: '/scale/0',
                label: this.$t('scales'),
                icon: 'device_thermostat',
                target: 'meta:mn_dop:scale',
              },
            ],
          },
          //Инструменты аналитика
          {
            to: '/toolsAnalitic',
            label: this.$t('toolsAnalitic'),
            icon: 'folder_open',
            target: 'meta:mn_tool',
            children: [],
          },
        ]
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
