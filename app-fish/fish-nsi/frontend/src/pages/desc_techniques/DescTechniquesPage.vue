<template>
  <div class="q-pa-md">

    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      class="no-padding no-margin"
      horizontal
      separator-class="bg-red"
    >

      <template v-slot:before>

        <q-table
          style="height: calc(100vh - 380px);"
          color="primary" dense
          card-class="bg-amber-1 text-brown"
          row-key="obj"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          selection="single"
          v-model:selected="selected"
          @update:selected="updateSelect"
          :rows-per-page-options="[0]"
        >

          <template #body-cell="props">
            <q-td :props="props">

              <div
                v-if="props.col.field === 'fvMethodStatus'"
              >
                      <span :class="fnFvClass(props.row.fvMethodStatus)">
                        {{ props.value }}
                      </span>
              </div>
              <div v-else>
                {{ props.value }}
              </div>

            </q-td>
          </template>

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
              <q-avatar color="black" text-color="white" icon="edit_document">
              </q-avatar>
              {{ $t("desc_techniques") }}
            </div>

            <q-space/>
            <q-btn v-if="hasTarget('nsi:om:ins')"
              icon="post_add" dense
              color="secondary"
              :disable="loading"
              @click="editObjProps(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn v-if="hasTarget('nsi:om:upd')"
              icon="edit" dense
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="editObjProps(selected[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn v-if="hasTarget('nsi:om:del')"
              icon="delete" dense
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="removeObjProps(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn v-if="hasTarget('nsi:om:fle')"
              class="q-ml-lg" dense
              icon="attach_file"
              color="secondary"
              :disable="loading || selected.length === 0"
              @click="attachFile(selected[0].obj)"
            >
              <q-tooltip
                transition-show="rotate"
                transition-hide="rotate"
              >
                {{ $t("attach_file") }}
              </q-tooltip>
            </q-btn>

            <q-space></q-space>
          </template>

        </q-table>

      </template>

      <template v-slot:after>

        <div v-if="selected.length > 0 && selected[0].hasFile>0">
          <q-bar class=" text-white bg-blue-grey-13" style="font-size: 1.2em; font-weight: bold;">
            {{ $t("attached_files") }}
          </q-bar>

          <q-table
            style="height: 200px; width: 100%"
            color="primary" dense
            card-class="bg-amber-1 text-brown"
            row-key="fileVal"
            :columns="cols2"
            :rows="rows2"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="horizontal"
            :hide-header="true"
            :loading="loading2"
            :rows-per-page-options="[0]"
          >

            <template #body-cell="props">
              <q-td :props="props">

                <div
                  v-if="props.col.field === 'fileName'"
                >
                      <span :class="fnNameFile()">
                        {{ props.value }}
                      </span>
                </div>

                <div v-else>
                  <q-btn v-if="hasTarget('nsi:om:fle:del')"
                    :disable="props.row['fileVal'] === undefined"
                    round size="sm" icon="delete" color="red" flat dense
                    @click="fnDeleteFile(props.row)"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("deletingFile") }}
                    </q-tooltip>
                  </q-btn>


                  <q-btn v-if="hasTarget('nsi:om:fle:cpy')"
                    :disable="props.row['fileVal'] === undefined"
                    round size="sm" icon="file_download" flat dense
                    @click="fnDownloadFs(props.row)"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("download_org") }}
                    </q-tooltip>
                  </q-btn>
                  <q-btn v-if="hasTarget('nsi:om:fle:viw')"
                    :disable="props.row['fileVal'] === undefined"
                    round size="sm" icon="visibility" flat dense color="blue"
                    @click="fnViewFile(props.row)"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("view") }}
                    </q-tooltip>
                  </q-btn>
                </div>

              </q-td>
            </template>

          </q-table>

        </div>

      </template>

    </q-splitter>


  </div>
</template>


<script>
import {defineComponent} from "vue";
import {api, tofi_dbeg, tofi_dend} from "boot/axios";
import {date} from "quasar";
import {hasTarget, notifyError, notifyInfo} from "src/utils/jsutils";
import UpdaterFile from "pages/regulatory_docs/UpdaterFile.vue";
import ViewPdf from "components/ViewPdf.vue";
import UpdaterMethodology from "pages/desc_techniques/UpdaterMethodology.vue";

export default defineComponent({
  name: "DescTechniquesPage",
  props: {},

  data() {
    return {
      splitterModel: 70,
      cols: [],
      rows: [],
      clsMethod: null,
      selected: [],
      mapFV: new Map,
      loading: false,
      cols2: [],
      rows2: [],
      loading2: false,
      mapFVcod: new Map
    };
  },

  methods: {
    hasTarget,

    fnFvClass(val) {
      if (val===this.mapFVcod.FV_Approved)
        return "bg-green text-white rounded-borders"
      else
        return "bg-blue text-white rounded-borders"
    },


    fnNameFile() {
      return "bg-purple text-white rounded-borders"
    },

    fnDeleteFile(row) {
      row.model = "nsidata"
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteFile") + "</br>(" + row.fileName + ")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post('', {
              method: "data/deleteFileValue",
              params: [row],
            })
            .then(
              () => {
                this.selected[0].hasFile = this.selected[0].hasFile > 0 ? this.selected[0].hasFile - 1 : 0
                this.loadAttachedFiles()
              },
              (error) => {
                let msg = error.message;
                if (error.response) msg = error.response.data.error.message
                notifyError(msg)
              }
            )
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
        })
    },

    fnViewFile(row) {
      this.$q
        .dialog({
          component: ViewPdf,
          componentProps: {
            id: parseInt(row['fileVal'], 10),
            fileName: row.fileName,
            // ...
          },
        })
        .onOk(() => {
        })
    },

    editObjProps(row, mode) {

      let data = {cls: this.clsMethod}
      if (mode === "upd") {
        //extend(true, data, row)
        Object.assign(data, row)
        if (data.MethodApprovalDate <= tofi_dbeg || data.MethodApprovalDate >= tofi_dend)
          data.MethodApprovalDate = null
      }


      this.$q
        .dialog({
          component: UpdaterMethodology,
          componentProps: {
            data: data,
            mode: mode,
            mapFV: this.mapFVcod,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === "ins") {
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

    removeObjProps(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord") + "</br>(" + rec.name + ")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post('', {
              method: "data/deleteOwnerWithProperties",
              params: [rec.obj, 1],
            })
            .then(
              () => {
                this.loadMethodology()
                this.selected = []
              },
              (error) => {
                let msg = error.message;
                if (error.response) msg = error.response.data.error.message
                notifyError(msg)
              }
            )
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
        })
    },

    updateSelect() {
      if (this.selected.length === 0) {
        this.rows2 = []
      } else {
        this.loadAttachedFiles()
      }
    },

    loadMethodology() {
      this.loading = true;
      api
        .post('', {
          method: "data/loadMethodology",
          params: ["Cls_Method", false, 0],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]
            //console.info("rows", this.rows)
          })
        .then(()=> {
          api
            .post('', {
              method: "data/mapFvNameFromId",
              params: [],
            })
            .then(
              (response) => {
                this.mapFV = response.data.result
              })
        })
        .then(()=> {
          api
            .post('', {
              method: "data/mapFvApproved",
              params: [],
            })
            .then(
              (response) => {
                this.mapFVcod = response.data.result
              })
        })
        .catch(error => {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = false
        })
    },

    loadAttachedFiles() {
      this.loading = true;
      api
        .post('', {
          method: "data/loadAttachedFiles",
          params: [this.selected[0].obj, "Prop_MethodDiscription"],
        })
        .then(
          (response) => {
            this.rows2 = response.data.result["records"]
            //console.info("rows2", this.rows2)
          })
        .catch(error => {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = false
        })

    },

    fnDownloadFs(rec) {
      this.loading = true;

      let formData = new FormData();
      formData.append("id", rec['fileVal']);
      formData.append("filename", rec.fileName);
      formData.append("model", "nsidata");

      //console.info("formData", formData)

      this.$axios
        .post("/fish_download",
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
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.loading = false;
        });
    },

    attachFile(obj) {
      this.$q
        .dialog({
          component: UpdaterFile,
          componentProps: {
            obj: obj,
            propCod: "Prop_MethodDiscription"
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          this.selected[0].hasFile = this.selected[0].hasFile + 1
          this.loadAttachedFiles()
        })
    },

    infoSelected(row) {
      return " " + row.name
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:70%",
        },
        {
          name: "fvMethodStatus",
          label: this.$t("status"),
          field: "fvMethodStatus",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
          format: (v) => this.mapFV ? this.mapFV[v] : null
        },
        {
          name: "MethodApprovalDate",
          label: this.$t("dateApproval"),
          field: "MethodApprovalDate",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: val =>
            val <= tofi_dbeg || val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },

      ]
    },

    getColumns2() {
      return [
        {
          name: "fileName",
          label: this.$t("fileName"),
          field: "fileName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:95%",
        },

        {
          name: "cmd",
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 5%",
        }

      ]
    },

  },

  created() {
    this.cols = this.getColumns()
    this.cols2 = this.getColumns2()

    this.loading = true
    api
      .post('', {
        method: "data/getClsIds",
        params: ["Cls_Method"],
      })
      .then(
        (response) => {
          this.clsMethod = response.data.result["Cls_Method"]
          //notifySuccess(this.$t("success"))
        })
      .catch(error => {
        //console.log(error.message)
        if (error.response.data.error.message.includes("@")) {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
          let msg = m1 + m2
          notifyError(msg)
        } else {
          notifyError(error.message)
        }
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loadMethodology()

  }

})

</script>


<style scoped>

</style>
