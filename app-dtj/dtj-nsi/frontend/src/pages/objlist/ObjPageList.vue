
<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 140px); width: 100%"
      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
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
      @update:selected="updSelected"
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
          <q-avatar color="black" text-color="white" icon="phishing">
          </q-avatar>
          {{ title }}
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

import {hasTarget, notifyError, notifyInfo,} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import UpdaterObj from "pages/objlist/UpdaterObj.vue";
import {extend} from "quasar";


export default {
  name: "ObjPageList",
  props: ["typCod", "title"],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: "",
      selected: [],
      loading: false,
    };
  },

  methods: {
    hasTarget,

    updSelected(sel) {
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
          component: UpdaterObj,
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
            '<div style="color: plum">(' + rec.name + ' - ' + rec.nameCls + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          this.$axios
            .post(baseURL, {
              method: "data/deleteOwner",
              params: [ rec.id, 1 ],
            })
            .then(
              () => {
                this.loadFishingTools()
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

    loadFishingTools() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadObjWithCls",
          params: ["Typ_FishGear"],
        })
        .then(
          (response) => {
            this.rows = response.data.result.records
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

    getColumnsFishingTools() {
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
          name: "nameCls",
          label: this.$t("nameCls"),
          field: "nameCls",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 70%",
        },
      ]
    },

    infoSelected(row) {
      return " " + row.name + " (" + row.nameCls + ")"
    },

  },

  created() {
    this.lang = localStorage.getItem("curLang")
    this.lang = this.lang === "en-US" ? "en" : this.lang

    // load Water Bodies
    this.cols = this.getColumnsFishingTools()
    this.loadFishingTools()
  },

  setup() {
    return {}
  },
};
</script>

<style scoped>
</style>
