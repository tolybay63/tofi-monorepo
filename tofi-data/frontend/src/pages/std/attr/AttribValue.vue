<template>
  <div class="q-pa-sm-sm">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      :separator="separator"
      :loading="loading"
      :dense="dense"
      :visible-columns="visibleColumns"
      :rows-per-page-options="[0]"
    >
      <template #header-cell="props">
        <q-th :props="props">
          {{ props.col.label }}
        </q-th>
      </template>

      <template #body-cell="props">
        <q-td :props="props">
          <q-bar class="bg-blue-grey-1" style="font-size: small; height: auto">
            <div
              v-if="props.col.field === 'provider'"
              style="font-size: small; height: auto"
            >
              {{ fnProvider(props.value) }}
            </div>

            <div
              v-else-if="props.col.field === 'status'"
              style="font-size: small; height: auto"
            >
              {{ fnStatus(props.value) }}
            </div>

            <div
              v-else-if="props.col.field === 'strVal'"
              style="font-size: small; height: auto"
            >
              {{ props.value }}
            </div>

            <div
              v-else-if="props.col.field === 'dateTimeVal'"
              style="font-size: small; height: auto"
            >
              {{ fnDate(props.value) }}
            </div>

            <div
              v-else-if="props.col.field === 'multiStrVal'"
              style="font-size: small; height: auto"
            >
              {{ props.value }}
            </div>

            <div
              v-else-if="props.col.field === 'propVal'"
              style="font-size: small; height: auto"
            >
              {{ fnPropVal(props) }}
            </div>

            <div
              v-else-if="props.col.field === 'cmd'"
              class="no-padding no-margin"
            >
              <q-btn
                color="primary" round size="sm" flat dense icon="more_vert"
              >
                <q-menu auto-close>
                  <q-btn
                    round size="sm" icon="edit" color="blue" flat dense
                    @click="fnEdit(props.row)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("update") }}
                    </q-tooltip>
                  </q-btn>

                  <q-btn
                    round size="sm" icon="delete" color="red" flat dense
                    @click="fnDelete(props)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("deletingRecord") }}
                    </q-tooltip>
                  </q-btn>

                  <q-btn
                    v-if="false"
                    :disable="props.row.id === undefined"
                    round size="sm" icon="visibility" flat dense color="blue"
                    @click="fnView(props)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("view") }}
                    </q-tooltip>
                  </q-btn>

                  <q-btn
                    v-if="requestParams.attribValType === 7"
                    :disable="props.row.id === undefined"
                    round size="sm" icon="file_download" flat dense
                    @click="fnDownload(props)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("download") }}
                    </q-tooltip>
                  </q-btn>
                </q-menu>
              </q-btn>
            </div>

            <div v-else style="font-size: small; height: auto">
              {{ props.value }}
            </div>
          </q-bar>
        </q-td>
      </template>
      <!-- href="http://localhost:8181/viewerjs/#../frontend/files/demodoc.pdf" -->
      <template v-slot:top>
        <div>
          <q-btn
            round
            size="medium"
            icon="post_add"
            style="color: #26a69a"
            flat
            dense
            @click="fnAdd()"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("addRecord") }}
            </q-tooltip>
          </q-btn>
        </div>
        <div>{{ $t("prop") }}:</div>
        <div class="text-blue q-ml-sm" style="font-size: medium">
          {{ propName }}
        </div>
        <q-space />

        <div style="font-size: 11px; margin-top: 2px">
          {{ $t("statusFactor") }}:
        </div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">
          {{ status ? $t("yes") : $t("no") }}
        </div>
        <div style="font-size: 11px; margin-top: 2px">
          {{ $t("providerTyp") }}:
        </div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">
          {{ provider ? $t("yes") : $t("no") }}
        </div>
        <div style="font-size: 11px; margin-top: 2px">
          {{ $t("attribValType") }}:
        </div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">{{ getValType() }}</div>
      </template>
    </q-table>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {date} from "quasar";
import {notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import allConsts from "pages/all-consts";
import UpdaterAttribValue from "pages/std/attr/UpdaterAttribValue.vue";
import {storeToRefs} from "pinia";
import {useParamsStore} from "stores/params-store";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  name: "AttribValue",
  props: ["status", "provider", "propName"],

  data: function () {
    return {
      cols: [],
      rows: [],
      rowsOrg: [],
      visibleColumns: ref([
        "provider",
        "status",
        "strVal",
        "multiStrVal",
        "dateTimeVal",
        "propVal",
        "fileVal",
      ]),
      loading: ref(false),
      separator: ref("cell"),
      dense: true,
      requestParams: {},

      Status: null,
      Provider: null,

      fltDt: null,
      fltPt: null,
      FD_AttribValType: null,
      PropVal: null,
      isMinio: undefined,
    };
  },

  methods: {
    fnProvider(val) {
      //console.info("fnProvider", val)
      if (this.Provider === null || this.Provider === undefined) return null;
      if (val === null || val === undefined || val === 0) return null;
      return this.Provider.get(val);
    },

    fnStatus(val) {
      //console.info("fnStatus", val)
      if (this.Status === null || this.Status === undefined) return null;
      if (val === null || val === undefined || val === 0) return null;
      return this.Status.get(val);
    },

    fnDate(val) {
      let fmt = this.requestParams.format;
      if (fmt === undefined) {
        if (this.requestParams.attribValType === 3) {
          fmt = "DD.MM.YYYY";
        } else if (this.requestParams.attribValType === 4) {
          fmt = "hh:mm";
        } else if (this.requestParams.attribValType === 5) {
          fmt = "DD.MM.YYYY hh:mm";
        }
      }
      if (val==="1800-01-01" || val==="3333-12-31")
        return "..."
      return date.formatDate(val, fmt);
    },

    fnPropVal(props) {
      //console.log("***requestParams***", this.requestParams)
      //console.log("***props***", props)

      let val = props.value
      if (this.PropVal === null || this.PropVal === undefined) return null;
      if (val === null || val === undefined || val === 0) return null;
      //if (this.PropVal.get("cod") === undefined || this.PropVal.get("name") === undefined) return null
      let r = this.PropVal[val];
      //console.log("***PropVal***", r)
      return r.cod + " - " + r.name;
    },

    getValType() {
      if (this.FD_AttribValType === null) return null;
      return this.FD_AttribValType.get(this.requestParams.attribValType);
    },

    fnDownload(props) {
      if (this.isMinio !== undefined && this.isMinio === true)
        this.fnDownloadMinio(props)
      else this.fnDownloadFs(props)
    },

    fnDownloadFs(props) {
      this.visible = ref(true);
      let rec = props.row;

      let formData = new FormData();
      formData.append("id", rec.fileVal);
      formData.append("filename", rec.fileName);
      formData.append("model", getModel.value);

      //console.info("formData", formData)

      api
        .post("/download",
          formData,
          {
          responseType: "arraybuffer",
          headers: {
            "Content-Type": "multipart/form-data",
          },
        })
        .then(response => {

            //console.log("response", response);
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const a = document.createElement("a");
            a.href = url;
            a.innerText = "Скачать";
            a.download = rec.fileName;
            a.click();
            window.URL.revokeObjectURL(url);
            //notifySuccess("Успешно сохранен в папке [Загрузки]!", 5000);
          },
          (error) => {
            this.visible = ref(false);
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnDownloadMinio(props) {
      this.visible = ref(true);
      let rec = props.row;
      //console.info("rec", rec)

      api
        .post(baseURL, {
          method: "dataStd/downloadMinio",
          params: [rec],
        })
        .then(
          (response) => {
            //console.info('response', response)
            notifySuccess("Успешно сохранен в папке [Загрузки]!", 5000);
          },
          (error) => {
            this.visible = ref(false);
            let msg = "networkError";
            if (error.response.data.error.message.includes("Failed to connect"))
              msg = "minioNotRun";
            else if (
              error.response.data.error.message.includes("already exists")
            )
              msg = "fileExists";
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(this.$t(msg));
          }
        )
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnView(props) {
      let rec = props.row;
      //openURL("/ViewerJS/#")
      notifyInfo("Not implemented :-(");

      /*      let err = false
            api
              .post(viewURL, {
                params: [this.rec]
              })
              .then(
                (response) => {
                  err = false
                  //console.info("response FlatTable Ins", response.data.result.records[0])
                  this.$emit("ok", {res: true});
                  notifySuccess(this.$t("success"))
                },
                (error) => {
                  err = true
                  console.log("error.response.data=>>>", error.response.data.error.message)
                  notifyError(error.response.data.error.message)
                })
              .finally(() => {
                this.visible = ref(false)
                if (!err) this.hide();
              });*/
    },

    fnDelete(props) {
      let rec = props.row
      let method = "data/deleteAttribValue"
      let params = { idDataProp: rec.dataProp, id: rec.id, model: getModel.value }
      let nm = rec.strVal
      if (this.requestParams.attribValType === allConsts.FD_AttribValType.file) {
        method = "data/deleteFileValue"
        nm = rec.fileName
        params = {
          idDataProp: rec.dataProp,
          id: rec.id,
          objorrelobj: this.requestParams.owner,
          fileId: rec.fileVal,
          model: getModel.value,
        }
      }
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord") +"</br>("+nm+")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post(baseURL, {
              method: method,
              params: [params],
            })
            .then(
              (response) => {
                this.loadData(this.requestParams)
              },
              (error) => {
                let msg = "";
                if (error.response) msg = error.response.data.error.message
                else msg = error.message
                notifyError(msg)
              }
            )
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
        })
    },

    hideProviderShowStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "strVal"];
    },

    hideProviderShowMultiStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "multiStrVal"];
    },

    hideProviderShowDateTimeVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "dateTimeVal"];
    },

    hideProviderShowEntity: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "propVal"];
    },

    hideProviderShowFile: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "fileVal"];
    },

    hideStatusShowStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "strVal"];
    },

    hideStatusShowMultiStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "multiStrVal"];
    },

    hideStatusShowDateTimeVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "dateTimeVal"];
    },

    hideStatusShowEntity: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "propVal"];
    },

    hideStatusShowFile: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "fileVal"];
    },

    ////////////////////
    hideProviderStatusShowStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["strVal"];
    },

    hideProviderStatusShowMultiStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["multiStrVal"];
    },

    hideProviderStatusShowDateTimeVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["dateTimeVal"];
    },

    hideProviderStatusShowEntity: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["propVal"];
    },

    hideProviderStatusShowFile: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["fileVal"];
    },

    /////////////////////

    showStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "strVal"];
    },

    showMultiStrVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "multiStrVal"];
    },

    showDateTimeVal: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "dateTimeVal"];
    },

    showEntity: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "propVal"];
    },

    showFile: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "fileVal"];
    },

    fnAdd() {
      //console.info("requestParams Add", this.requestParams);
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: UpdaterAttribValue,
          componentProps: {
            rec: {},
            requestParams: this.requestParams,
            lg: lg,
            dense: this.dense,
            mode: "ins",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        })
        .onCancel(() => {
          //console.log('Cancel!')
        });
    },

    fnEdit(rec) {
      //console.info("EDIT props rec", rec)
      this.requestParams.dataProp = rec.dataProp;
      this.requestParams.periodType = rec.periodType;
      this.requestParams.fltDt = this.fltDt;
      this.requestParams.fltPt = this.fltPt;

      //console.info("requestParams fnEdit", this.requestParams)

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterAttribValue,
          componentProps: {
            rec: rec,
            requestParams: this.requestParams,
            lg: lg,
            dense: this.dense,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        })
        .onCancel(() => {
          //console.log('Cancel!')
        });
    },

    filterDate(dte) {
      this.fltDt = dte;
      if (dte === null) {
        this.rows = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rows = this.rowsOrg.filter((r) => {
          return r.dbeg <= dte && dte <= r.dend;
        });
      }
    },

    filterPeriod(typPeriod) {
      this.fltPt = typPeriod;
      if (typPeriod === 0) {
        this.rows = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rows = this.rowsOrg.filter((r) => {
          return r.periodType === typPeriod;
        });
      }
    },

    getColumns() {
      return [
        {
          name: "provider",
          label: this.$t("providerTypShort"),
          field: "provider",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "status",
          label: this.$t("statusFactorShort"),
          field: "status",
          sortable: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "strVal",
          label: this.$t("val"),
          field: "strVal",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "multiStrVal",
          label: this.$t("val"),
          field: "multiStrVal",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "dateTimeVal",
          label: this.$t("val"),
          field: "dateTimeVal",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "propVal",
          label: this.$t("val"),
          field: "propVal",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "fileVal",
          label: this.$t("val"),
          field: "fileName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "period",
          required: true,
          label: this.$t("period"),
          field: "period",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 10%",
        },
        {
          name: "cmt",
          required: true,
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          sortable: false,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "cmd",
          required: true,
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 1%",
        },
      ];
    },

    loadData(params) {
      /////
      params.model = getModel.value
      params.metamodel = getMetaModel.value
      for (let key in params) {
        if (params.hasOwnProperty(key)) {
          this.requestParams[key] = params[key]
        }
      }
      this.requestParams.provider = this.provider
      this.requestParams.status = this.status

      //console.info("requestParams AttribValue", this.requestParams)
      /////

      if (!this.provider && this.status) {
        if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.str ||
          this.requestParams.attribValType ===
            allConsts.FD_AttribValType.mask ||
          this.requestParams.attribValType ===
            allConsts.FD_AttribValType.integ ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.num
        ) {
          this.hideProviderShowStrVal()
        } else if (
          this.requestParams.attribValType ===
          allConsts.FD_AttribValType.multistr
        ) {
          this.hideProviderShowMultiStrVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.dt ||
          this.requestParams.attribValType ===
            allConsts.FD_AttribValType.dttm ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.tm
        ) {
          this.hideProviderShowDateTimeVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.entity
        ) {
          this.hideProviderShowEntity()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.file
        ) {
          this.hideProviderShowFile()
        }
      } else if (this.provider && !this.status) {
        if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.str ||
          this.requestParams.attribValType ===
            allConsts.FD_AttribValType.mask
        ) {
          this.hideStatusShowStrVal()
        } else if (
          this.requestParams.attribValType ===
          allConsts.FD_AttribValType.multistr
        ) {
          this.hideStatusShowMultiStrVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.dt ||
          this.requestParams.attribValType ===
            allConsts.FD_AttribValType.dttm ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.tm
        ) {
          this.hideStatusShowDateTimeVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.entity
        ) {
          this.hideStatusShowEntity()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.file
        ) {
          this.hideStatusShowFile()
        }
      } else if (!this.provider && !this.status) {
        if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.str ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.mask
        ) {
          this.hideProviderStatusShowStrVal()
        } else if (
          this.requestParams.attribValType ===
          allConsts.FD_AttribValType.multistr
        ) {
          this.hideProviderStatusShowMultiStrVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.dt ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.dttm ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.tm
        ) {
          this.hideProviderStatusShowDateTimeVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.entity
        ) {
          this.hideProviderStatusShowEntity()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.file
        ) {
          this.hideProviderStatusShowFile()
        }
      } else {
        if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.str ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.mask
        ) {
          this.showStrVal()
        } else if (
          this.requestParams.attribValType ===
          allConsts.FD_AttribValType.multistr
        ) {
          this.showMultiStrVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.dt ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.dttm ||
          this.requestParams.attribValType === allConsts.FD_AttribValType.tm
        ) {
          this.showDateTimeVal()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.entity
        ) {
          this.showEntity()
        } else if (
          this.requestParams.attribValType === allConsts.FD_AttribValType.file
        ) {
          this.showFile()
        }
      }

      if (this.requestParams.providerTyp > 0) {
        this.loading = ref(true)
        api
          .post(baseURL, {
            method: "data/loadProvider",
            params: [this.requestParams.prop, getModel.value, getMetaModel.value],
          })
          .then((response) => {
            this.Provider = new Map()
            response.data.result.records.forEach((it) => {
              this.Provider.set(it.id, it.name)
            });
          })
          .finally(() => {
            this.loading = ref(false)
          });
      }

      if (this.requestParams.statusFactor > 0) {
        this.loading = ref(true)
        api
          .post(baseURL, {
            method: "data/loadStatus",
            params: [this.requestParams.prop]
          })
          .then((response) => {
            this.Status = new Map();
            response.data.result.records.forEach((it) => {
              this.Status.set(it.id, it.name)
            });
            //console.info("Status", this.Status)
          })
          .finally(() => {
            this.loading = ref(false)
          });
      }
      //
      this.loading = ref(true)
      api
        .post(baseURL, {
          method: "data/loadDict",
          params: ["FD_AttribValType"]
        })
        .then((response) => {
          this.FD_AttribValType = new Map()
          response.data.result.records.forEach((it) => {
            this.FD_AttribValType.set(it.id, it.text)
          });
        })
        .catch(error=> {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = ref(false)
        })

      //
      params.model = getModel.value
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadData",
          params: [params],
        })
        .then(
          (response) => {
            //setTimeout(()=> {
            //console.info("loadData", response.data.result)
            this.PropVal = response.data.result.mapPVent
            this.rows = response.data.result.store.records
            this.rowsOrg = response.data.result.store.records
            this.isMinio = response.data.result.isMinio

            //console.info("this.PropVal", this.PropVal)
            //console.info("this.rows", this.rows)
            //},500)
          })
        .catch(error => {
          let msg = error.message
          if (error.response) msg = error.response.data.error.message
          notifyError(msg)
        })
        .finally(() => {
          this.loading = ref(false)
        });
      //
    },
  },

  mounted() {
    //console.log("MOUNTED RefValue")
  },

  created() {
    this.lang = localStorage.getItem("curLang")
    this.lang = this.lang === "en-US" ? "en" : this.lang
    this.cols = this.getColumns()
  },

  setup() {
    return {}
  },
};
</script>

<style scoped></style>
