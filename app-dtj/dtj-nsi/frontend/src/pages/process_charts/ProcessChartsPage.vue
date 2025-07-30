
<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 140px); width: 100%"
      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="obj"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      selection="single"
      v-model:selected="selected"
      :rows-per-page-options="[0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="assignment">
          </q-avatar>
          {{ $t("process_charts") }}
        </div>

        <q-space/>
        <q-btn v-if="hasTarget('nsi:ol:ins')"
               icon="post_add" dense
               color="secondary"
               :disable="loading"
               @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn v-if="hasTarget('nsi:ol:upd')"
               icon="edit" dense
               color="secondary"
               class="q-ml-sm"
               :disable="loading || selected.length === 0"
               @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn v-if="hasTarget('nsi:ol:del')"
               icon="delete" dense
               color="secondary"
               class="q-ml-lg"
               :disable="loading || selected.length === 0"
               @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space/>

        <q-input
          dense
          debounce="300"
          color="primary"
          :model-value="filter"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script>
import {extend} from "quasar";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo} from "src/utils/jsutils";
import UpdaterProcessCharts from "pages/process_charts/UpdaterProcessCharts.vue";

export default {
  name: "ProcessChartsPage",

  data: function () {
    return {
      loading: false,
      cols: [],
      rows: [],
      filter: "",
      selected: [],
      mapCls: null,
      mapFvSource: null,
      mapFvPeriodType: new Map(),

    }
  },

  methods: {
    hasTarget,

    loadData() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadProcessCharts",
          params: [0],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]
          })
        .catch(error=> {
          if (error.response.data.error.message.includes("@")) {
            let msgs = error.response.data.error.message.split("@")
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        })
        .finally(() => {
          this.loading = false
        })
    },

    editRow(row, mode) {
      let data = {
        accessLevel: 1,
      }

      if (mode === "upd") {
        extend(true, data, row)
      }

      this.$q
        .dialog({
          component: UpdaterProcessCharts,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Ok! updated", r);
          if (mode==="ins") {
            this.rows.push(r);
            this.selected = [];
            this.selected.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                row[key] = r[key];
              }
            }
          }
        })
    },

    removeRow(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + rec.TechCard + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          this.$axios
            .post(baseURL, {
              method: "data/deleteOwnerWithProperties",
              params: [ rec.obj, 1 ],
            })
            .then(
              () => {
                this.loadData()
                this.selected = []
              })
            .catch(error => {
              console.log(error.message)
              notifyInfo(error.message)
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        })
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },

        {
          name: "cls",
          label: this.$t("vidWork"),
          field: "cls",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (v) => this.mapCls ? this.mapCls.get(v): null
        },
        {
          name: "nameCollections",
          label: this.$t("source_collections"),
          field: "nameCollections",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:15%",
        },
        {
          name: "NumberSource",
          label: this.$t("Prop_NumberSource"),
          field: "NumberSource",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },
        {
          name: "fvPeriodType",
          label: this.$t("Prop_PeriodType"),
          field: "fvPeriodType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
          format: (v) => this.mapFvPeriodType ? this.mapFvPeriodType.get(v): null
        },
        {
          name: "Periodicity",
          label: this.$t("Prop_Periodicity"),
          field: "Periodicity",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },
      ]
    },

    infoSelected(row) {
      return " " + row.TechCard
    },

  },

  created() {
    api
      .post(baseURL, {
        method: "data/loadClsForSelect",
        params: ["Typ_Work"],
      })
      .then(
        (response) => {
          this.mapCls = new Map()
          response.data.result["records"].forEach(r => {
            this.mapCls.set(r.id, r.name);
          })
        })
      .then(()=> {
        api
          .post(baseURL, {
            method: "data/loadFvSource",
            params: ["Factor_Source"],
          })
          .then(
            (response) => {
              this.mapFvSource = new Map()
              response.data.result["records"].forEach(r => {
                this.mapFvSource.set(r.id, r.name);
              })
            })
      })
      .then(()=> {
        api
          .post(baseURL, {
            method: "data/loadFvPeriodType",
            params: ["Factor_PeriodType"],
          })
          .then(
            (response) => {
              response.data.result["records"].forEach(r => {
                this.mapFvPeriodType.set(r.id, r.name);
              })
            })
      })
      .then(()=> {
        this.cols = this.getColumns()
      })
      .then(()=> {
        this.loadData()
      })
  }

}
</script>

<style scoped>

</style>
