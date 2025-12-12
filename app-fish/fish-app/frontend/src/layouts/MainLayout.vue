<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          {{ $t("appName") }}
        </q-toolbar-title>

        <SetLocale style="align-content: end"></SetLocale>
      </q-toolbar>
    </q-header>

    <q-footer reveal elevated>
      <q-toolbar>
        <q-toolbar-title class="text-center">
          <q-icon class="q-pa-sm">
            <img src="../assets/factor.png" alt="Logo" />
          </q-icon>
          {{ $t("company") }}

          <span class="absolute-right q-pt-sm">
          <a href="http://tofishstocks-model.kz" target="_blank" style="font-size: 12px" class="q-pr-md text-white"> {{$t("fish_model")}} </a>
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
import { defineComponent } from "vue";
import SetLocale from "components/SetLocale.vue";

export default defineComponent({
  name: "MainLayout",
  components: { SetLocale },

  setup() {
    return {};
  },
});
</script>
