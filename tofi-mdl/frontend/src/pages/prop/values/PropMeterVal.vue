<template>
  <q-bar class="bg-orange-1 q-mb-sm" style="height: 48px">
    <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:val:edit')"
        dense
        icon="edit_note"
        color="secondary"
        @click="editData()"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("update") }}
      </q-tooltip>
    </q-btn>
  </q-bar>

  <QTreeTable :cols="cols" :rows="rows" :icon_leaf="''" ref="childComp"/>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import QTreeTable from "components/QTreeTable.vue";
import {expandAll, hasTarget, notifyError, pack} from "src/utils/jsutils";
import UpdaterPropMeterVal from "pages/prop/values/UpdaterPropMeterVal.vue";

export default {
  props: ["propGr", "prop", "meter", "meterRate", "meterStruct", "dense"],
  components: {QTreeTable},

  data() {
    return {
      cols: [],
      rows: [],
      separator: ref("cell"),
      loading: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    hasTarget,
    loadPropMeter(prop) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "prop/loadPropMeter",
            params: [prop],
          })
          .then((response) => {
            this.rows = pack(response.data.result.records, "ord");
            expandAll(this.rows);
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    editData() {
      this.$q
          .dialog({
            component: UpdaterPropMeterVal,
            componentProps: {
              prop: this.prop,
              meter: this.meter,
              meterRate: this.meterRate,
              meterStruct: this.meterStruct,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.loadPropMeter(this.prop);
            }
          });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 35%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  mounted() {
    this.loadPropMeter(this.prop);
  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  setup() {
  }

};
</script>
