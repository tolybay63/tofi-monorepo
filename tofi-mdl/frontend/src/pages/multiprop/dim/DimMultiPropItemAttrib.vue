<template>

  <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      dense
      selection="single"
      v-model:selected="selected"
      :rows-per-page-options="[0]"
      @update:selected="selectedCheck"
  >
    <template #bottom-row>
      <q-td colspan="100%" v-if="selected.length > 0">
        <span class="text-blue"> {{ $t("selectedRow") }}: </span>
        <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
      </q-td>
      <q-td
          v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
        {{ $t("infoApp") }}
      </q-td>
    </template>

    <template v-slot:top>
      <div style="font-size: 1.2em; font-weight: bold;">
        {{ $t("multiPropItemAttr") }}
      </div>
      <q-space/>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
          dense
          icon="post_add"
          color="secondary"
          @click="editRow('ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("newRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow('upd')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>
    </template>

    <template #loading>
      <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
    </template>
  </q-table>


</template>

<script>
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError} from "src/utils/jsutils";
import {ref} from "vue";
import UpdateDimMultiPropItemAttrib from "pages/multiprop/dim/UpdateDimMultiPropItemAttrib.vue";

export default {
  name: 'DimMultiPropItemAttrib',
  props: ["dimMultiPropItem"],

  data() {
    return {
      cols: [],
      rows: [],
      FD_AttribValType: null,
      loading: false,
      selected: [],

    }
  },

  methods: {
    hasTarget,

    selectedCheck() {

    },

    infoSelected(row) {
      if (row)
        return " " + this.FD_AttribValType.get(row.attribValType)
      return null
    },

    loadDimMultiPropItemAttrib(dmpia) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "dimMultiProp/loadDimMultiPropItemAttrib",
            params: [dmpia],
          })
          .then((response) => {
            this.rows = response.data.result.records
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

    editRow(mode) {
      let data = {dimMultiPropItem: this.dimMultiPropItem};
      if (mode === "upd") {
        data = this.selected[0];
      }

      this.$q
          .dialog({
            component: UpdateDimMultiPropItemAttrib,
            componentProps: {
              data: data,
              mode: mode,
            },
          })
          .onOk((data) => {
            this.selected = []
            this.loadDimMultiPropItemAttrib(this.dimMultiPropItem);
            this.selected.push(data)
            //this.selectedCheck()
          });
    },

    removeRow() {
      let rec = this.selected[0];
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' + this.FD_AttribValType.get(rec.attribValType) +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            api
                .post(baseURL, {
                  method: "dimMultiProp/deleteDimMultiPropItemAttrib",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.loadDimMultiPropItemAttrib(this.dimMultiPropItem)
                      this.selected = []
                    },
                    (error) => {
                      notifyError(error.response.data.error.message)
                    }
                );
          });
    },

    getColumns() {
      return [
        {
          name: "attribValType",
          label: this.$t("attribValType"),
          field: "attribValType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 70%",
          format: (val) => (val) ? this.FD_AttribValType.get(val) : null
        },
        {
          name: "format",
          label: this.$t("formatValue"),
          field: "format",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },

      ]
    }


  },

  created() {
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AttribValType"}],
        })
        .then((response) => {
          this.FD_AttribValType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AttribValType.set(it["id"], it["text"]);
          })
        })


    this.cols = this.getColumns()
    this.loadDimMultiPropItemAttrib(this.dimMultiPropItem);
  },

  setup() {},


}
</script>
