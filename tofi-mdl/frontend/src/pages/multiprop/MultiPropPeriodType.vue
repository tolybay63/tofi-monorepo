<template>
  <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="cod"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      table-header-style="size: 3em"
      separator="cell"
      :loading="loading"
      :dense="dense"
  >
    <template v-slot:top>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:per:edit')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="editData()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-table>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import UpdaterMultiPropPeriodType from "pages/multiprop/UpdaterMultiPropPeriodType.vue";
import {hasTarget, notifyError} from "src/utils/jsutils";

export default {
  name: "MultiPropPeriodType",

  data() {
    return {
      rows: [],
      cols: [],

      FD_PeriodType: null,
      loading: ref(false),
      separator: ref("cell"),
      multiPropId: null,

      dense: true,
    };
  },

  methods: {
    hasTarget,
    editData() {
      this.$q
          .dialog({
            component: UpdaterMultiPropPeriodType,
            componentProps: {
              multiProp: this.multiPropId,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.fetchData(this.multiPropId);
            }
          });
    },

    fetchData(multiProp) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadMultiPropPeriodType",
            params: [multiProp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .catch((error) => {

            this.$router["push"]("/");
            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    getColumns() {
      return [
        {
          name: "periodType",
          label: this.$t("periodType"),
          field: "periodType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 100%",
          format: (val) =>
              this.FD_PeriodType ? this.FD_PeriodType.get(val) : null,
        },
      ];
    },
  },

  mounted() {

    //console.log("mounted params", this.$route.params)
    this.multiPropId = parseInt(this.$route["params"].mp, 10);
    this.fetchData(this.multiPropId);
  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_PeriodType"}],
        })
        .then((response) => {
          this.FD_PeriodType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_PeriodType.set(it["id"], it["text"]);
          });
        });
  },

  setup() {
  }

};
</script>

<style scoped></style>
