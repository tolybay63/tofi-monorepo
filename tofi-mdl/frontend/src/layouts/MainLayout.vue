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
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("mainPage") }}
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
            >
              {{ $t("logIn") }}
            </q-tooltip>
            <q-tooltip
              v-if="userName !== ''"
              transition-hide="rotate"
              transition-show="rotate"
            >
              {{ $t("logOut") }}
            </q-tooltip>
            {{ userName }}
            <q-badge align="middle" color="primary" rounded>
              <q-icon :name="nameIcon()" color="white" />
            </q-badge>
          </q-btn>
        </div>
        <!-- Текущий язык-->
        <SetLocale />
      </q-toolbar>
    </q-header>

    <q-footer elevated reveal>
      <q-toolbar>
        <q-toolbar-title class="flex flex-center">
          <q-icon class="q-pa-sm">
            <img alt="Logo" src="../assets/factor.png" />
          </q-icon>
          {{ $t("company") }}

          <span class="absolute-right q-pt-sm">
            <a
              :href="siteUrl"
              class="q-pr-md text-white"
              style="font-size: 12px"
              target="_blank"
            >
              {{ $t("fish_model") }}
            </a>
          </span>
        </q-toolbar-title>
      </q-toolbar>
    </q-footer>

    <q-drawer
      v-model="leftDrawerOpen"
      :width="270"
      elevated
      show-if-above
      bordered
      class="bg-grey-3"
    >
      <h6 v-if="notAccess" class="q-pa-md text-red text-bold">
        {{ $t("notAccessService") }}
      </h6>
      <h6 v-else-if="reqAuth" class="q-pa-md text-red text-bold">
        {{ $t("notLoginned") }}
      </h6>

      <q-list v-else class="q-list--bordered bg-blue-1">
        <template v-for="item in linksList()" :key="item.label">
          <q-expansion-item
            v-if="item.children && hasTarget(item.target)"
            :icon="item.icon"
            :label="item.label"
            class="q-list--bordered"
          >
            <q-item
              v-for="subItem in item.children"
              :key="subItem.label"
              v-ripple
              :active="isActive(subItem.to)"
              :to="subItem.to"
              active-class="text-bold text-blue bg-blue-2"
              class="q-pl-xl q-table--bordered bg-blue-1"
              clickable
            >
              <q-item-section avatar>
                <q-icon :name="subItem.icon" />
              </q-item-section>
              <q-item-section>{{ subItem.label }}</q-item-section>
            </q-item>
          </q-expansion-item>

          <q-item
            v-else-if="hasTarget(item.target)"
            v-ripple
            :active="isActive(item.to)"
            :to="item.to"
            active-class="text-bold text-blue bg-blue-2"
            class="q-table--bordered bg-blue-1"
            clickable
          >
            <q-item-section avatar>
              <q-icon :name="item.icon" />
            </q-item-section>
            <q-item-section>{{ item.label }}</q-item-section>
          </q-item>
        </template>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed, ref, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { storeToRefs } from "pinia";
import axios from "axios";

import SetLocale from "@/components/SetLocale.vue";
import LoginUser from "@/components/LoginUser.vue";
import { api, authURL, urlMainApp } from "@/boot/axios";
import { useUserStore } from "@/stores/user-store";
import { hasTarget } from "@/utils/jsutils.js";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const $q = useQuasar();

const store = useUserStore();
const { getUserName, getUserId, getTarget } = storeToRefs(store);
const { setUserStore, clearUserStore } = store;

const selected = ref(null);
const leftDrawerOpen = ref(false);

const userName = computed(() => getUserName.value);
const reqAuth = computed(() => getUserName.value === "");
const notAccess = computed(
  () => !getTarget.value.includes("mdl") && getUserId.value > 1
);

const toHome = () => {
  router.push("/");
};

const mainApp = () => {
  window.open(urlMainApp, "_self");
};

const isActive = (menuTo) => {
  if (!menuTo || !route.path) return false;
  const menuBase = menuTo.split("/")[1];
  const currentBase = route.path.split("/")[1];
  return menuBase === currentBase;
};

const siteUrl = import.meta.env.QCLI_SITE_URL || 'https://tofishstocks-model.kz'

const nameIcon = () => {
  if (userName.value === "") return "login";
  else return "logout";
};

const toggleLeftDrawer = () => {
  leftDrawerOpen.value = !leftDrawerOpen.value;
};

const loginOnOff = () => {
  if (getUserName.value === "") {
    leftDrawerOpen.value = true;
    $q.dialog({
      component: LoginUser,
      componentProps: {},
    }).onOk((res) => {
      setUserStore(res);
      router.push("/");

      api.post("", {
        method: "auth/checkTarget",
        params: ["mdl"],
      });
    });
  } else {
    axios
      .post(authURL + "/logout", new URLSearchParams())
      .then(() => {
        clearUserStore();
      })
      .catch((err) => {
        console.error("Ошибка при logout на сервере:", err);
        clearUserStore();
      })
      .finally(() => {
        router.push("/");
      });
  }
};

const linksList = () => {
  return [
    {
      to: "/database",
      label: proxy?.$t("database"),
      icon: "storage",
      target: "mdl:db",
    },
    {
      to: "/dataSetting",
      label: proxy?.$t("dataSetting"),
      icon: "folder_open",
      target: "mdl:mn_ds",
      children: [
        {
          to: "/measure",
          label: proxy?.$t("measures"),
          icon: "square_foot",
          target: "mdl:mn_ds:mea",
        },
        {
          to: "/attrib",
          label: proxy?.$t("attributs"),
          icon: "format_shapes",
          target: "mdl:mn_ds:attr",
        },
        {
          to: "/factor/0",
          label: proxy?.$t("factors"),
          icon: "account_tree",
          target: "mdl:mn_ds:fac",
        },
        {
          to: "/meter/0",
          label: proxy?.$t("meters"),
          icon: "scale",
          target: "mdl:mn_ds:meter",
        },
        {
          to: "/role",
          label: proxy?.$t("roles"),
          icon: "perm_contact_calendar",
          target: "mdl:mn_ds:role",
        },
        {
          to: "/typ/0",
          label: proxy?.$t("typs"),
          icon: "view_quilt",
          target: "mdl:mn_ds:typ",
        },
        {
          to: "/reltyp/0",
          label: proxy?.$t("reltyps"),
          icon: "view_column",
          target: "mdl:mn_ds:reltyp",
        },
        {
          to: "/props/0/0",
          label: proxy?.$t("props"),
          icon: "app_registration",
          target: "mdl:mn_ds:prop",
        },
        {
          to: "/dimMultiProp/0/0",
          label: proxy?.$t("dimMultiProp"),
          icon: "type_specimen",
          target: "mdl:mn_ds:pmdim",
        },
        {
          to: "/multiProp/0/0",
          label: proxy?.$t("multiProp"),
          icon: "view_in_ar",
          target: "mdl:mn_ds:pm",
        },
        {
          to: "/flatTable",
          label: proxy?.$t("flatTable"),
          icon: "table_rows",
          target: "mdl:mn_ds:ft",
        },
        {
          to: "/chargr/0/typchargr",
          label: proxy?.$t("charprop"),
          icon: "table_chart",
          target: "mdl:mn_ds:cgp",
        },
      ],
    },
    {
      to: "/dataProcessing",
      label: proxy?.$t("dataProcessing"),
      icon: "folder_open",
      target: "mdl:mn_dp",
      children: [
        {
          to: "/dimsperiod/0",
          label: proxy?.$t("dimsPeriod"),
          icon: "pending_actions",
          target: "mdl:mn_dp:dmper",
        },
        {
          to: "/dimsprop/0/0",
          label: proxy?.$t("dimsProp"),
          icon: "credit_score",
          target: "mdl:mn_dp:dmprop",
        },
        {
          to: "/dimsobj/0/0",
          label: proxy?.$t("dimsObj"),
          icon: "pattern",
          target: "mdl:mn_dp:dmobj",
        },
        {
          to: "/cubes/0/0",
          label: proxy?.$t("cubes"),
          icon: "view_in_ar",
          target: "mdl:mn_dp:cube",
        },
      ],
    },
    {
      to: "/dopEntities",
      label: proxy?.$t("dopEntities"),
      icon: "folder_open",
      target: "mdl:mn_dop_entity",
      children: [
        {
          to: "/stocks/0/0",
          label: proxy?.$t("stocks"),
          icon: "devices",
          target: "mdl:mn_dop:stocks",
        },
        {
          to: "/syscoding",
          label: proxy?.$t("syscoding"),
          icon: "pin",
          target: "mdl:mn_dop:syscoding",
        },
        {
          to: "/scale/0",
          label: proxy?.$t("scales"),
          icon: "device_thermostat",
          target: "mdl:mn_dop:scale",
        },
      ],
    },
    {
      to: "/toolsAnalitic",
      label: proxy?.$t("toolsAnalitic"),
      icon: "folder_open",
      target: "mdl:mn_tool",
      children: [],
    },
  ];
};

onMounted(() => {
  if (getUserId.value < 1) {
    clearUserStore();
    router.push("/");
  }
});
</script>

<style>
</style>
