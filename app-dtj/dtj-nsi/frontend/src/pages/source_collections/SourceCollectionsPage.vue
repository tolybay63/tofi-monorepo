<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[70, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 140px); width: 100%"
    >

      <template v-slot:before>


        <q-table
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
          @update:selected="updateSelected"
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
              <q-avatar color="black" text-color="white" icon="home_work">
              </q-avatar>
              {{ $t("source_collections") }}
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

      </template>

      <template v-slot:after>

        <q-banner dense class="text-bold text-white bg-blue-grey-13" inline-actions>
          <div class="row">
            {{ $t('other_props') }}
          </div>
          <template v-slot:action>

            <q-btn
              icon="edit" dense no-caps
              :label="$t('editRecord')"
              color="secondary"
              class="q-mr-sm"
              @click="updated = !updated"
              :disable="updated"
            >
            </q-btn>

            <q-btn
              icon="save" dense no-caps
              :label="$t('save')"
              color="red"
              @click="saveOtherProps()"
              :disable="!updated"
              :loading="loading"
            >
              <template #loading>
                <q-spinner-hourglass color="white"/>
              </template>
            </q-btn>


          </template>

        </q-banner>

        <q-card class="bg-amber-1 text-brown">
          <q-card-section v-if="selected.length > 0">

            <!-- DocumentLinkToDepartment -->
            <q-item-label class="text-grey-7" style="font-size: 0.8em">{{ fnLabel('Prop_DocumentLinkToDepartment') }}
            </q-item-label>
            <treeselect
              :multiple="true"
              :flat="true"
              :options="optDepartment"
              :clear-on-select="true"
              v-model="refDepartment"
              :default-expand-level="2"
              :normalizer="normalizer"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
              class="q-mb-xl full-width"
              :disabled="!updated"
            />


            <div class="q-mt-xl full-width">
              <q-banner class=" text-white bg-blue-grey-13" style="font-size: 1.2em; font-weight: bold;" inline-actions>
                {{ $t("attached_files") }}

                <template v-slot:action>

                  <q-btn
                    icon="attach_file" dense no-caps
                    color="secondary"
                    class="q-mr-sm"
                    @click="attachFile(selected[0].id)"
                    :disable="loading || selected.length === 0"
                  >
                  </q-btn>
                </template>

              </q-banner>

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
                :loading="loading"
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


          </q-card-section>
        </q-card>

      </template>


    </q-splitter>
  </div>
</template>

<script>

import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {hasTarget, notifyError, notifyInfo, notifySuccess, pack} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import {extend} from "quasar";
import UpdaterSourceCollections from "pages/source_collections/UpdaterSourceCollections.vue";
import {ref} from "vue";
import ViewPdf from "components/ViewPdf.vue";
import UpdaterFile from "pages/source_collections/UpdaterFile.vue";

export default {
  name: "SourceCollectionsPage",
  components: {treeselect},


  data: function () {
    return {
      splitterModel: 100,
      loading: false,
      cols: [],
      rows: [],
      filter: "",
      selected: [],

      optDepartment: [],
      refDepartment: [],
      updated: false,
      own: 0,

      cols2: [],
      rows2: [],



    }
  },

  methods: {
    hasTarget,

    saveOtherProps() {
      console.info(this.refDepartment)
      this.loading = true
      let err = false
      api
        .post(baseURL, {
          method: "data/saveDepartment",
          params: [{
            isObj: 1, metamodel: "dtj", model: "nsidata",
            obj: this.own, ids: this.refDepartment
          }]
        })
        .then(
          () => {
          })
        .catch(error => {
          err = true
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          this.loading = false
          if (!err)
            this.updated = !this.updated
        })

    },

    updateSelected() {
      if (this.selected.length > 0) {
        this.loading = true
        this.own = this.selected[0].id
        let s = this.selected[0]
        this.selected = []
        setTimeout(() => {
          this.selected.push(s)
        }, 200)
        this.cols2 = this.getColumns2()
        this.updated = true
        let err = false
        api
          .post(baseURL, {
            method: "data/loadDepartmentsWithFile",
            params: [this.own],
          })
          .then(
            (response) => {
              this.refDepartment = []
              if (response.data.result["departments"] !== "") {
                response.data.result["departments"].split(",").forEach(item => {
                  this.refDepartment.push(parseInt(item, 10))
                })
              }
              //
              this.rows2 = response.data.result["files"]["records"]
              //console.info("this.refDepartment", this.refDepartment)

            })
          .catch(error => {
            notifyError(error.message)
          })
          .finally(() => {
            this.loading = false
            this.updated = false
            if (!err) {
              //setTimeout(()=> {
              this.splitterModel = 70
              console.info("this.refDepartment", this.refDepartment)
              //}, 10)
            }
          })
      } else {
        this.updated = false
        this.splitterModel = 100
      }
    },

    fnLabel(label) {
      return this.$t(label);
    },

    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
      };
    },

    loadData() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadSourceCollections",
          params: [0],
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

    editRow(row, mode) {
      let data = {
        accessLevel: 1,
      }

      if (mode === "upd") {
        extend(true, data, row)
      }

      this.$q
        .dialog({
          component: UpdaterSourceCollections,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Ok! updated", r);
          if (mode === "ins") {
            this.rows.push(r);
            this.selected = [];
            this.selected.push(r);
          } else {
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
              params: [rec.id, 1],
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
          name: "DocumentNumber",
          label: this.$t("Prop_DocumentNumber"),
          field: "DocumentNumber",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:15%",
        },
        {
          name: "DocumentApprovalDate",
          label: this.$t("Prop_DocumentApprovalDate"),
          field: "DocumentApprovalDate",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },
        {
          name: "DocumentAuthor",
          label: this.$t("Prop_DocumentAuthor"),
          field: "DocumentAuthor",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:25%",
        },
        {
          name: "DocumentStartDate",
          label: this.$t("Prop_DocumentStartDate"),
          field: "DocumentStartDate",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },
        {
          name: "DocumentEndDate",
          label: this.$t("Prop_DocumentEndDate"),
          field: "DocumentEndDate",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:10%",
        },



      ]
    },

    infoSelected(row) {
      return " " + row.name
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
          this.$axios
            .post(baseURL, {
              method: "data/deleteFileValue",
              params: [row],
            })
            .then(
              () => {
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

    attachFile(obj) {
      this.$q
        .dialog({
          component: UpdaterFile,
          componentProps: {
            obj: obj,
            propCod: "Prop_DocumentFiles"
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          this.loadAttachedFiles()
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
            notifyError(error.message);
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
          params: [this.own, "Prop_DocumentFiles"],
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

    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadDepartments',
        params: ['Typ_Location', 'Prop_LocationMulti'],
      })
      .then(
        (response) => {
          this.optDepartment = pack(response.data.result["records"], "id")
          console.log("optDepartment", this.optDepartment)
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loadData()
        this.loading = false
      })


  }


}
</script>

<style scoped>

</style>
