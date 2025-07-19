<template>
  <q-table
      color="primary"
      card-class="bg-amber-1 text-brown"
      table-class="text-grey-8"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :dense="dense"
      :rows-per-page-options="[0]"
      selection="single"
      v-model:selected="selected"
  >
    <template v-slot:top>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:cond:ins')"
          :dense="dense"
          icon="post_add"
          color="secondary"
          :disable="loading"
          @click="editRow(null, 'ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("newRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:cond:upd')"
          :dense="dense"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:cond:del')"
          :dense="dense"
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>
    </template>

    <template #loading>
      <q-inner-loading showing color="secondary"></q-inner-loading>
    </template>
  </q-table>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdaterMultiPropCond from "pages/multiprop/UpdaterMultiPropCond.vue";

export default {
  props: ["lg", "dense"],

  data() {
    return {
      lang: this.lg,
      cols: [],
      rows: [],
      loading: ref(false),
      multiPropId: null,
      selected: ref([]),
      maxLen: 0,
    };
  },

  methods: {
    hasTarget,
    selectedRow(item) {
      if (this.selected.length > 0 && item.id === this.selected[0].id) {
        this.selected = [];
      } else {
        this.selected = [];
        this.selected.push(item);
        //this.updSelected(this.selected)
      }
    },

    load(multiProp) {

      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadMultiPropCond",
            params: [multiProp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    editRow(rec, mode) {
      let data = {
        id: null,
        multiProp: this.multiProp,
        factor: null,
        typ: null,
        relTyp: null,
        isReq: false,
      };
      if (mode === "upd") {
        for (let key in rec) {
          if (rec.hasOwnProperty(key)) {
            data[key] = rec[key];
          }
        }
      }
      this.$q
          .dialog({
            component: UpdaterMultiPropCond,
            componentProps: {
              data: data,
              mode: mode,
              multiProp: this.multiPropId,
              dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (r.res) {
              this.load(this.multiPropId);
            }
          });
    },

    removeRow(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.cod +
                ": " +
                rec.name +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "multiProp/deleteMultiPropCond",
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
                      notifyError(msg);
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
          style: "width: 25%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "flag",
          label: this.$t("entityType"),
          field: "flag",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
          format: (val) => this.$t(val),
        },
        {
          name: "isReq",
          label: this.$t("isReq"),
          field: "isReq",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) => (val ? this.$t("yes") : this.$t("no")),
        },
      ];
    },
  },

  mounted() {

    //console.log("mounted params", this.$route.params);
    this.multiPropId = parseInt(this.$route["params"].mp, 10);

    this.load(this.multiPropId);
  },

  created() {
    this.cols = this.getColumns();
  },

  setup() {}

};
</script>
