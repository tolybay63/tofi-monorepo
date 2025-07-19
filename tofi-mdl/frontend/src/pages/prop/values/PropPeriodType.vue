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
      :rows-per-page-options="[20, 25, 0]"
  >
    <template v-slot:top>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:prop:val:period')"
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
import UpdaterPropPeriodType from "pages/prop/values/UpdaterPropPeriodType.vue";
import {hasTarget, notifyError} from "src/utils/jsutils";

export default {
  name: "PropPeriodType",

  data() {
    return {
      rows: [],
      cols: [],

      FD_PeriodType: null,
      loading: ref(false),
      propId: null,

      dense: true,
    };
  },

  methods: {
    hasTarget,
    editData() {
      this.$q
          .dialog({
            component: UpdaterPropPeriodType,
            componentProps: {
              prop: this.propId,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.fetchData(this.propId);
            }
          });
    },

    fetchData(prop) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "prop/loadPropPeriodType",
            params: [prop],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            console.info("loadPropPeriodType", this.rows)
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

    //console.log("mounted params", this.$route.params);
    this.propId = parseInt(this.$route["params"].prop, 10);

    this.fetchData(this.propId);
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
          console.info("this.FD_PeriodType", this.FD_PeriodType)
        });
  },

  setup() {
  }

};
</script>

<style scoped></style>
