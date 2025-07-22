<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 280px); width: 100%"
      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
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
               class="q-mx-lg"
               :disable="loading || selected.length === 0"
               @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space/>

        <q-input
          dense class="q-mr-lg"
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
        <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script>
import {hasTarget, notifyError, notifyInfo} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import {extend} from "quasar";
import UpdaterParams from "pages/param_objects/UpdaterParams.vue";

export default {
  name: "ParamsPage",

  data: function () {
    return {
      loading: false,
      cols: [],
      rows: [],
      filter: "",
      selected: [],
      mapMeasure: new Map(),
      mapCollection: new Map(),
    }
  },

  methods: {
    hasTarget,

    loadData() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadParameters",
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
          component: UpdaterParams,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode==="ins") {
            this.rows.push(r);
            this.selected = [];
            this.selected.push(r);
          } else {
            for (let key in row) {
              if (key !== "id") {
                row[key] = null;
              }
            }
            for (let key in r) {
              //if (r.hasOwnProperty(key)) {
                row[key] = r[key];
              //}
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
            '<div style="color: plum">(' + rec.name + ")</div>",
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
              params: [ rec.id, 1 ],
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
          headerStyle: "font-size: 1.2em; width:30%",
        },
        {
          name: "meaParamsMeasure",
          label: this.$t("Prop_ParamsMeasure"),
          field: "meaParamsMeasure",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
          format: (v) => this.mapMeasure ? this.mapMeasure.get(v): null
        },
        {
          name: "objCollections",
          label: this.$t("Prop_Collections"),
          field: "objCollections",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
          format: (v) => this.mapCollection ? this.mapCollection.get(v): null
        },
        {
          name: "ParamsDescription",
          label: this.$t("description"),
          field: "ParamsDescription",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
      ]
    },

    infoSelected(row) {
      return " " + row.name
    },

  },

  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadMeasure',
        params: ['Prop_ParamsMeasure'],
      })
      .then(
        (response) => {
          response.data.result["records"].forEach(it => {
            this.mapMeasure.set(it.id, it.name);
          })
        })
      .then(()=> {
        api
          .post(baseURL, {
            method: 'data/loadCollections',
            params: ['Cls_Collections', 'Prop_Collections'],
          })
          .then(
            (response) => {
              response.data.result["records"].forEach(it => {
                this.mapCollection.set(it.id, it.name);
              })
            })
      })
      .catch((error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        })
      .finally(() => {
        this.loading = false
        this.cols = this.getColumns()
        this.loadData()
      })
  }

}
</script>

<style scoped>

</style>
