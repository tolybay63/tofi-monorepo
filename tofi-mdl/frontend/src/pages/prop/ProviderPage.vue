<!--@update:selected="updSelection"-->
<template>
  <q-table
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      dense
      :rows-per-page-options="[25, 0]"
      selection="single"
      v-model:selected="selected"
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
      <q-btn
          v-if="hasTarget('mdl:mn_ds:attr:ins')"
          dense icon="post_add" color="secondary" :disable="loading"
          @click="editRow(null, 'ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("newRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:attr:upd')"
          dense icon="edit" color="secondary" class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:attr:del')"
          dense icon="delete" color="secondary" class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
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
import {ref} from "vue";
import UpdaterProvider from "pages/prop/UpdaterProvider.vue";
import {hasTarget, notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["act", "fk", "typ"],

  data() {
    return {
      cols: [],
      rows: [],
      selected: [],
      loading: ref(false),
    };
  },

  methods: {
    hasTarget,

    loadProvider() {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: this.act + "/loadProvider",
            params: [this.fk],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            //console.info("rows", this.rows)
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    removeRow(rec) {
      const inf = rec["nameCls"] + (rec["nameObj"] !== '') ? "-" + rec["nameObj"] : ""
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' + inf + ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: this.act + "/deleteProvider",
                  params: [rec.id],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                        let msg
                        if (error.response) msg = error.response.data.error.message;
                        else msg = error.message;
                        notifyError(msg)
                    }
                );
          });


    },

    editRow(row, mode) {

      let data = {
        id: 0,
        prop: this.fk,
        cls: null,
        obj: null,
        isDefault: false
      }
      if (this.act==="multiProp")
        data.multiProp = this.fk

      if (mode === 'upd') {
        data = {
          id: row.id,
          prop: row.prop,
          cls: row.cls,
          obj: row.obj,
          isDefault: row.isDefault
        }
        if (this.act==="multiProp")
          data.multiProp = row.prop
      }

      this.$q
          .dialog({
            component: UpdaterProvider,
            componentProps: {
              data: data,
              act: this.act,
              prop: this.fk,
              typ: this.typ,
              mode: mode
            },
          })
          .onOk((data) => {
            this.selected= []
            this.loadProvider();
            this.selected.push(data)
          });
    },

    getColumns() {
      return [
        {
          name: "nameCls",
          label: this.$t("cls"),
          field: "nameCls",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 45%",
        },
        {
          name: "nameObj",
          label: this.$t("obj"),
          field: "nameObj",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 50%",
        },
        {
          name: "isDefault",
          label: this.$t("isDefault"),
          field: "isDefault",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 5%",
          format: (val) =>
              val ? this.$t("yes") : this.$t("no")
        },
      ];
    },

    infoSelected(row) {
      const inf = row["nameCls"] + (row["nameObj"] !== '') ? "-" + row["nameObj"] : ""
      return " " + inf;
    },

  },
  created() {
    this.cols = this.getColumns();
    this.loadProvider();
  },

  setup() {
  }

};
</script>
