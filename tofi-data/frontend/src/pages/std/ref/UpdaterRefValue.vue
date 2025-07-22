<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-width
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-card-section>
        <q-table
          color="primary"
          card-class="bg-amber-1"
          table-class="text-grey-8"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          table-header-style="size: 3em"
          separator="cell" dense
          :rows-per-page-options="[0]"
          :visible-columns="visibleColumns"
        >
          <template v-slot:body="props">
            <q-tr :props="props">
              <q-td key="checked" :props="props">
                <q-btn
                  dense flat
                  color="blue"
                  :icon="
                    props.row.checked == 1
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="selectedRow(props.row)"
                >
                </q-btn>
              </q-td>

              <q-td
                key="provider"
                :props="props"
                style="font-size: small; height: auto"
              >
                {{ fnProvider(props.row.provider) }}
              </q-td>

              <q-td
                key="status"
                :props="props"
                style="font-size: small; height: auto"
              >
                {{ fnStatus(props.row.status) }}
              </q-td>

              <q-td key="cod" :props="props">
                {{ props.row.cod }}
              </q-td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>

              <q-td key="dbeg" :props="props">
                {{ fnDbeg(props.row.dbeg) }}
              </q-td>

              <q-td key="dend" :props="props">
                {{ fnDend(props.row.dend) }}
              </q-td>

              <q-td key="cmt" :props="props">
                {{ props.row.cmt }}
              </q-td>

              <q-td key="cmd" :props="props">
                <q-btn
                  round
                  size="sm"
                  icon="edit"
                  color="blue"
                  flat
                  dense
                  @click="fnEdit(props.row)"
                  :disable="!props.row.checked"
                />
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          dense color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>

        <q-btn
          dense color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";
import {date, extend} from "quasar";
import UpdaterRefValueRowIns from "pages/std/ref/UpdaterRefValueRow.vue";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);


export default {
  props: ["rec", "requestParams", "lg", "mode"],

  data() {
    console.info("UpdateRefValue: rec", this.rec);
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      reqParams: this.requestParams,
      visibleColumns: ref(["provider", "status"]),
      Provider: null,
      Status: null,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    hideProvider: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status"];
    },

    hideStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider"];
    },

    hideProviderStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = [];
    },

    showAllCols() {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status"];
    },

    fnProvider(val) {
      if (val === undefined) return null;
      return this.Provider.get(val);
    },

    fnStatus(val) {
      if (val === undefined) return null;
      return this.Status.get(val);
    },

    fnEdit(row) {
      this.fnIns(row);

      /*
      if (this.mode==="upd") {
        this.fnUpd(row)
      } else {
        this.fnIns(row)
      }
*/
    },

    fnIns(row) {
      const lg = { name: this.lang };
      row.dbeg = row.dbeg === "1800-01-01" ? null : row.dbeg;
      row.dend = row.dend === "3333-12-31" ? null : row.dend;

      let rec = {};

      extend(true, rec, row);
      console.info("rec to 1", rec);
      console.info("rec to 2", this.reqParams);
      this.$q
        .dialog({
          component: UpdaterRefValueRowIns,
          componentProps: {
            rec: rec,
            requestParams: this.reqParams,
            lg: lg,
            valueName: row.cod + " - " + row.name,
            // ...
          },
        })
        .onOk((d) => {
          console.info("onOk", d.rec);
          let r = d.rec;
          //let add = d.add;
          if (d.add) {
            this.rows.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                row[key] = r[key];
              }
            }
          }
          //console.info("upd Ok", r)
          //console.info("upd Ok reqParams", this.reqParams)
        })
        .onCancel(() => {
          //console.log('Cancel!')
        });
    },

    selectedRow(row) {
      row.checked = !row.checked;
      if (row.checked) {
        this.fnEdit(row);
      }
    },

    validSave() {
      return false;
    },

    fnDbeg(val) {
      return val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY");
    },

    fnDend(val) {
      return val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY");
    },

    getColumns() {
      return [
        {
          name: "checked",
          field: "checked",
          required: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 3%",
        },
        {
          name: "provider",
          label: this.$t("providerTypShort"),
          field: "provider",
          align: "left",
          sortable: false,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 15%",
        },
        {
          name: "status",
          label: this.$t("statusFactorShort"),
          field: "status",
          sortable: false,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 15%",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          required: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          required: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
        {
          name: "dbeg",
          label: this.$t("fldDbegShort"),
          field: "dbeg",
          required: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 7%",
          //format: val => val <= tofi_dbeg ? '...' : date.formatDate(val, 'DD.MM.YYYY')
        },
        {
          name: "dend",
          label: this.$t("fldDendShort"),
          field: "dend",
          required: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 7%",
          //format: val => val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY')
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          required: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
        {
          name: "cmd",
          field: "cmd",
          required: true,
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 3%",
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {
      this.loading = ref(true);
      let dta = [];
      this.rows.forEach((r) => {
        if (r.checked) {
          dta.push(r);
        }
      });
      this.reqParams.mode = this.mode;
      this.reqParams.model = getModel.value;
      this.reqParams.metamodel = getMetaModel.value;
      //console.info(this.rec.dataProp, dta)

      let err = false;
      api
        .post(baseURL, {
          method: "data/saveRefValue",
          params: [{ reqParams: this.reqParams, st: dta }],
        })
        .then(
          (response) => {
            err = false;
            this.$emit("ok", { res: true });
          },
          (error) => {
            err = true;
            let msg = error.message;
            if (error.response.data.error.message)
              msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
          if (!err) this.hide();
        });
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

  created() {
    this.cols = this.getColumns();
    //
    if (!this.reqParams.provider && this.reqParams.status) this.hideProvider();
    else if (this.reqParams.provider && !this.reqParams.status)
      this.hideStatus();
    else if (!this.reqParams.provider && !this.reqParams.status)
      this.hideProviderStatus();
    else this.showAllCols();

    if (this.reqParams.providerTyp > 0) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadProvider",
          params: [this.reqParams.prop, getModel.value, getMetaModel.value],
        })
        .then((response) => {
          this.Provider = new Map();
          response.data.result.records.forEach((it) => {
            this.Provider.set(it.id, it.name);
          });
        });
    }

    if (this.reqParams.statusFactor > 0) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadStatus",
          params: [this.reqParams.prop],
        })
        .then((response) => {
          this.Status = new Map();
          response.data.result.records.forEach((it) => {
            this.Status.set(it.id, it.name);
          });
        });
    }
    //
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "data/loadRefValueForUpd",
        params: [{ rec: this.rec, reqParams: this.requestParams }],
      })
      .then(
        (response) => {
          console.info("info", response.data.result);
          this.rows = response.data.result.store.records;
        })
      .catch(error => {
        let msg = "";
        if (error.response) msg = error.response.data.error.message;
        else msg = error.message;
        notifyError(msg);
      })
      .finally(() => {
        this.loading = ref(false);
      });

    return {};
  },
};
</script>
