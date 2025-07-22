<template>
  <q-select
    v-model="locale"
    :model-value="locale"
    :options="localeOptions"
    emit-value
    dense
    borderless
    map-options
    options-dense
    @update:model-value="
      () => {
        setLang(locale);
      }
    "
    hide-dropdown-icon
  >
    <template v-slot:selected-item="scope">
      <q-chip color="primary" text-color="white" dense>
        <q-avatar>
          <q-btn class="q-pa-none" round color="primary" dense icon="language">
            <q-tooltip transition-show="rotate" transition-hide="rotate">{{
              $t("chooseLanguage")
            }}</q-tooltip>
          </q-btn>
        </q-avatar>

        {{ scope.opt.value }}
      </q-chip>
    </template>
  </q-select>
</template>

<script>
import languages from "quasar/lang/index.json";
import { Quasar } from "quasar";
import { useI18n } from "vue-i18n";
import { ref } from "vue";
import { baseURL } from "boot/axios.js";

let localeOptions;

export default {
  methods: {
    setLang(e) {
      import(`../../node_modules/quasar/lang/${e}.js`).then((l) => {
        this.$q.lang.set(l.default);
        let curLang = l.default.isoName;
        //
        localStorage.setItem("curLang", curLang);
      });
      //location.reload();
    },
  },
  created() {
    let langs = [];
    let appLanguages = [];
    let FD_Lang = [
      { id: 1, text: "Русский", code: "ru", sign: "ru" },
      { id: 2, text: "Қазақша", code: "kz", sign: "kk" },
      { id: 3, text: "English", code: "en", sign: "en-US" },
    ];

    /*
    this.$axios
      .post(baseURL, {
        method: "dict/load",
        params: [{ dict: "FD_Lang" }],
      })
      .then((response) => {
        langs = response.data.result.records.map((lang) => lang["sign"]);
        appLanguages = languages.filter((lang) => langs.includes(lang.isoName));
        //
        localeOptions = appLanguages.map((lang) => ({
          label: lang.nativeName,
          value: lang.isoName,
        }));
      });
*/
    langs = FD_Lang.map((lang) => lang["sign"]);
    appLanguages = languages.filter((lang) => langs.includes(lang.isoName));
    //
    localeOptions = appLanguages.map((lang) => ({
      label: lang.nativeName,
      value: lang.isoName,
    }));


  },

  setup() {
    const appLanguages = languages.filter((lang) =>
      ["kk", "ru", "en-US"].includes(lang.isoName)
    );
    localeOptions = appLanguages.map((lang) => ({
      label: lang.nativeName,
      value: lang.isoName,
    }));
    if (!localStorage.getItem("curLang")) localStorage.setItem("curLang", "ru");
    const curLang = localStorage.getItem("curLang");
    const lang = ref(curLang);
    import(`../../node_modules/quasar/lang/${curLang}.js`).then((lang) => {
      Quasar.lang.set(lang.default);
    });
    const { locale } = useI18n({ useScope: "global" });
    locale.value = curLang;

    return {
      locale,
      localeOptions,
    };
  },
};
</script>

<style scoped></style>
