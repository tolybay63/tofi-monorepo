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
          <a :href="site_url" class="q-pr-md text-white" style="font-size: 12px"
             target="_blank"> {{ $t("fish_model") }} </a>
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
import SetLocale from "../components/SetLocale.vue";

export default defineComponent({
  name: "MainLayout",
  components: { SetLocale },

  setup() {
    // Получаем переменную окружения
    const site_url = import.meta.env.QCLI_SITE_URL;

    // Для проверки в консоли
    //console.log("Все переменные окружения Vite:", import.meta.env);
    //console.log("Значение QCLI_SITE_URL:", site_url);

    // Возвращаем переменную в шаблон
    return {
      site_url
    };
  }
});
</script>
