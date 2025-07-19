<template>

  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
    >
      <template v-slot:before>

        <q-table
          color="primary" dense
          card-class="bg-amber-1 text-brown"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          selection="single"
          v-model:selected="selected"
          :rows-per-page-options="[0]"
          @update:selected="updateSelect"
        >

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <q-avatar color="black" text-color="white" icon="menu_book">
              </q-avatar>
              {{ $t("regulatory_docs") }}
            </div>
          </template>

          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
              {{ $t("infoViewRow") }}
            </q-td>
          </template>


        </q-table>

      </template>

      <template v-slot:after>

        <q-splitter
          v-model="splitterModel2"
          :model-value="splitterModel2"
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
              :columns="cols2"
              :rows="rows2"
              :wrap-cells="true"
              :table-colspan="4"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading2"
              selection="single"
              v-model:selected="selected2"
              @update:selected="updateSelect2"
              :rows-per-page-options="[0]"
            >

              <template #bottom-row>
                <q-td colspan="100%" v-if="selected2.length > 0">
                  <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                  <span class="text-bold"> {{ this.infoSelected2(selected2[0]) }} </span>
                </q-td>
                <q-td colspan="100%" v-else-if="this.rows2.length > 0" class="text-bold">
                  {{ $t("infoRowExt") }}
                </q-td>
              </template>

              <template v-slot:top>

                <div style="font-size: 1.2em; font-weight: bold" v-if="selected.length > 0">
                  <q-avatar color="black" text-color="white"
                     :icon="selected[0].id===1020 ? 'description'
                          : selected[0].id===1021 ? 'article'
                          : selected[0].id===1022 ? 'newspaper'
                          : 'text_snippet'"/>
                  {{ selected[0].name }}
                </div>


                <q-space/>
                <q-btn v-if="hasTarget(this.tgIns)"
                  icon="post_add" dense
                  color="secondary"
                  :disable="loading || selected.length === 0"
                  @click="editObjProps(null, 'ins')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn v-if="hasTarget(this.tgUpd)"
                  icon="edit" dense
                  color="secondary"
                  class="q-ml-sm"
                  :disable="loading2 || selected2.length === 0"
                  @click="editObjProps(selected2[0], 'upd')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn v-if="hasTarget(this.tgDel)"
                  icon="delete" dense
                  color="secondary"
                  class="q-ml-sm"
                  :disable="loading2 || selected2.length === 0"
                  @click="removeObjProps(selected2[0])"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn v-if="hasTarget(this.tgFle)"
                  class="q-ml-lg" dense
                  icon="attach_file"
                  color="secondary"
                  :disable="loading2 || selected2.length === 0"
                  @click="attachFile(selected2[0].obj)"
                >
                  <q-tooltip
                    transition-show="rotate"
                    transition-hide="rotate"
                  >
                    {{ $t("attach_file") }}
                  </q-tooltip>
                </q-btn>

                <q-space/>
              </template>

            </q-table>

          </template>

          <template v-slot:after>

            <div v-if="selected2.length > 0 && selected2[0].hasFile>0">
              <q-bar class=" text-white bg-blue-grey-13" style="font-size: 1.2em; font-weight: bold;">
                {{ $t("attached_files") }}
              </q-bar>


              <q-table
                style="height: 200px; width: 100%"
                color="primary" dense
                card-class="bg-amber-1 text-brown"
                row-key="fileVal"
                :columns="cols3"
                :rows="rows3"
                :wrap-cells="true"
                :table-colspan="4"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="horizontal"
                :hide-header="true"
                :loading="loading3"
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
                      <q-btn v-if="hasTarget(this.tgFleDel)"
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


                      <q-btn v-if="hasTarget(this.tgFleCpy)"
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
                      <q-btn v-if="hasTarget(this.tgFleViw)"
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

      </template>

    </q-splitter>

  </div>


</template>


<script>
import {defineComponent} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {hasTarget, notifyError, notifyInfo} from "src/utils/jsutils";
import UpdaterFile from "pages/regulatory_docs/UpdaterFile.vue";
import ViewPdf from "components/ViewPdf.vue";
import UpdaterObjOfProperties from "pages/regulatory_docs/UpdaterObjOfProperties.vue";
import {date, extend} from "quasar";

export default defineComponent({
  name: "RegulatoryDocsPage",
  props: {},

  data() {
    return {
      splitterModel: 20,
      splitterModel2: 70,
      cols: [],
      rows: [],
      selected: [],
      loading: false,
      cols2: [],
      rows2: [],
      selected2: [],
      loading2: false,
      cols3: [],
      rows3: [],
      loading3: false,

      tgIns: "",
      tgUpd: "",
      tgDel: "",
      tgFle: "",
      tgFleDel: "",
      tgFleCpy: "",
      tgFleViw: "",

      isMinio: undefined,
    };
  },

  methods: {
    hasTarget,

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
          this.$axios
            .post(baseURL, {
              method: "data/deleteFileValue",
              params: [row],
            })
            .then(
              () => {
                this.selected2[0].hasFile = this.selected2[0].hasFile > 0 ? this.selected2[0].hasFile - 1 : 0
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

      let data = {cls: this.selected[0].id}
      if (mode === "upd") {
        extend(true, data, row)
        //Object.assign(data, row)
      }
      if (data.RegDocumentsDateApproval<=tofi_dbeg)
        data.RegDocumentsDateApproval = null
      if (data.RegDocumentsLifeDoc>=tofi_dend)
        data.RegDocumentsLifeDoc = null

      this.$q
        .dialog({
          component: UpdaterObjOfProperties,
          componentProps: {
            data: data,
            mode: mode
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === "ins") {
            this.rows2.push(r);
            this.selected2 = [];
            this.selected2.push(r);
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
          message: this.$t("deleteRecord") + "</br>(" + rec["RegDocumentsNameDoc"] + ")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.$axios
            .post(baseURL, {
              method: "data/deleteOwnerWithProperties",
              params: [rec.obj, 1],
            })
            .then(
              () => {
                this.loadRegulatoryDocs()
              },
              (error) => {
                let msg = error.message;
                if (error.response) msg = error.response.data.error.message
                notifyError(msg)
              }
            )
        })

    },

    updateSelect() {
      //console.info("selected", this.selected)
      this.rows2 = []
      this.selected2 = []

      if (this.selected.length > 0) {
        if (this.selected[0].id === 1020) {
          this.tgIns= "nsi:nd:opd:ins"
          this.tgUpd= "nsi:nd:opd:upd"
          this.tgDel= "nsi:nd:opd:del"
          this.tgFle= "nsi:nd:opd:fle"
          this.tgFleDel= "nsi:nd:opd:fle:del"
          this.tgFleCpy= "nsi:nd:opd:fle:cpy"
          this.tgFleViw= "nsi:nd:opd:fle:viw"
        }
        if (this.selected[0].id === 1021) {
          this.tgIns= "nsi:nd:nmd:ins"
          this.tgUpd= "nsi:nd:nmd:upd"
          this.tgDel= "nsi:nd:nmd:del"
          this.tgFle= "nsi:nd:nmd:fle"
          this.tgFleDel= "nsi:nd:nmd:fle:del"
          this.tgFleCpy= "nsi:nd:nmd:fle:cpy"
          this.tgFleViw= "nsi:nd:nmd:fle:viw"
        }
        if (this.selected[0].id === 1022) {
          this.tgIns= "nsi:nd:oc:ins"
          this.tgUpd= "nsi:nd:oc:upd"
          this.tgDel= "nsi:nd:oc:del"
          this.tgFle= "nsi:nd:oc:fle"
          this.tgFleDel= "nsi:nd:oc:fle:del"
          this.tgFleCpy= "nsi:nd:oc:fle:cpy"
          this.tgFleViw= "nsi:nd:oc:fle:viw"
        }
        if (this.selected[0].id === 1023) {
          this.tgIns= "nsi:nd:pa:ins"
          this.tgUpd= "nsi:nd:pa:upd"
          this.tgDel= "nsi:nd:pa:del"
          this.tgFle= "nsi:nd:pa:fle"
          this.tgFleDel= "nsi:pa:oc:fle:del"
          this.tgFleCpy= "nsi:pa:oc:fle:cpy"
          this.tgFleViw= "nsi:pa:oc:fle:viw"
        }

        this.loadRegulatoryDocs()
      }
    },

    updateSelect2() {
      if (this.selected2.length === 0) {
        this.rows3 = []
      } else {
        this.loadAttachedFiles()
      }
    },

    getColumns1() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:100%",
        },
      ]
    },

    getColumns2() {
      return [
        {
          name: "RegDocumentsNameDoc",
          label: this.$t("nameDoc"),
          field: "RegDocumentsNameDoc",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
        {
          name: "RegDocumentsNumberDoc",
          label: this.$t("numberDoc"),
          field: "RegDocumentsNumberDoc",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },
        {
          name: "RegDocumentsAuthorDoc",
          label: this.$t("authorDoc"),
          field: "RegDocumentsAuthorDoc",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "RegDocumentsNumberOrder",
          label: this.$t("numberOrder"),
          field: "RegDocumentsNumberOrder",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "RegDocumentsDateApproval",
          label: this.$t("dateApproval"),
          field: "RegDocumentsDateApproval",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: val =>
            val <= tofi_dbeg ? "" : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "RegDocumentsLifeDoc",
          label: this.$t("lifeDoc"),
          field: "RegDocumentsLifeDoc",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: val =>
            val >= tofi_dend ? "" : date.formatDate(val, "DD.MM.YYYY"),
        },

      ]
    },

    getColumns3() {
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

    loadRegulatoryDocs() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadRegDocuments",
          params: [this.selected[0].id, false],
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

    loadRegulatoryDocsCls() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadRegDocumentsCls",
          params: ["Typ_RegDocuments"],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]
          })
        .catch(error => {
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

    fnDownloadFs(rec) {
      this.loading = true;

      let formData = new FormData();
      formData.append("id", rec['fileVal']);
      formData.append("filename", rec.fileName);
      formData.append("model", "nsidata");

      //console.info("formData", formData)

      this.$axios
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
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.loading = false;
        });
    },

    loadAttachedFiles() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadAttachedFiles",
          params: [this.selected2[0].obj, "Prop_RegDocumentsFile"],
        })
        .then(
          (response) => {
            this.rows3 = response.data.result["records"]
            //console.info("rows3", this.rows3)
          })
        .catch(error => {
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = false
        })

    },

    attachFile(obj) {
      //console.info("cls", cls)

      this.$q
        .dialog({
          component: UpdaterFile,
          componentProps: {
            obj: obj,
            propCod: "Prop_RegDocumentsFile"
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          this.selected2[0].hasFile = this.selected2[0].hasFile + 1
          this.loadAttachedFiles()
        })
    },

    infoSelected(row) {
      return " " + row.name
    },

    infoSelected2(row) {
      return " " + row["RegDocumentsNameDoc"]
    },

  },

  created() {
    this.cols = this.getColumns1()
    this.cols2 = this.getColumns2()
    this.cols3 = this.getColumns3()
    this.loadRegulatoryDocsCls()

  }

})

</script>


<style scoped>

</style>

