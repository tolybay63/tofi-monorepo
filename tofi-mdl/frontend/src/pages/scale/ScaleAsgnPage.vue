<template>
  <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      class="no-padding no-margin"
      horizontal
      style="height: calc(100vh - 250px); width: 100%"
      separator-class="bg-red"

  >
    <template v-slot:before>
      <q-table
        style="height: 100%; width: 100%"
        dense color="primary"
        card-class="bg-amber-1"
        row-key="id"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :loading="loading"
        :rows-per-page-options="[20, 25, 0]"
        selection="single"
        v-model:selected="selected"
        @update:selected="rowSelected"
        @request="requestData"
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
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("scaleSettings") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ins')"
            dense icon="post_add" color="secondary"
            :disable="loading" @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:upd')"
            dense icon="edit" color="secondary" class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:del')"
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

    <template v-slot:after>
      <q-table
          style="height: 100%; width: 100%"
          dense color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading2.value"
          :rows-per-page-options="[20, 25, 0]"
      >

        <template v-slot:body="props">
          <q-tr>
            <q-td key="val" :props="props">
              {{ props.row.val }}
            </q-td>

            <q-td key="name" :props="props">
              {{ props.row.name }}
            </q-td>

            <q-td key="fullName" :props="props">
              {{ props.row.fullName }}
            </q-td>

            <q-td key="scaleValColor" :props="props">
              <q-avatar size="28px" :style="fnStyle(props.row)" />
            </q-td>

          </q-tr>

        </template>



        <template v-slot:top>
          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("scaleValSettings") }}
          </div>

          <q-space/>
          <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:ins')"
              dense icon="edit_note" color="secondary"
              :disable="loading2.value || selected.length===0" @click="editRow2()"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("update") }}
            </q-tooltip>
          </q-btn>

        </template>

        <template #loading>
          <q-inner-loading :showing="loading2.value" color="secondary"></q-inner-loading>
        </template>
      </q-table>
    </template>

  </q-splitter>
</template>

<script>
import {ref} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {hasTarget, notifyError, notifySuccess} from "src/utils/jsutils";
import {date} from "quasar";
import UpdateScaleAsgn from "pages/scale/UpdateScaleAsgn.vue";
import UpdateScaleValAsgnUpd from "pages/scale/UpdateScaleValAsgnUpd.vue";


export default {
  name: 'ScaleAsgnPage',

  data: function () {
    return {
      cols: [],
      rows: [],
      FD_AccessLevel: null,
      loading: false,
      selected: [],
      splitterModel: 50,
      ///////////
      cols2: [],
      rows2: [],
      loading2: false,

      scale: 0,
    };
  },

  methods: {
    hasTarget,

    rowSelected(row) {
      if (row.length>0) {
        this.fetchData2(this.scale, row[0].id)
      } else {
        this.fetchData2(0, 0)
      }
    },

    fnStyle(row) {
      return "background:" + row["scaleValColor"]
    },

    requestData() {
      //console.info("reqParams", reqParams)
    },

    fetchData(scale) {
      this.loading = true;
      //
      api
          .post(baseURL, {
            method: "scale/loadScaleAsgn",
            params: [scale],
          })
          .then(
              (response) => {
                this.rows = response.data.result.records;
                this.selected = ref([]);
              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = false
          });
    },

    removeRow(rec) {
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
                  method: "scale/deleteScaleAsgn",
                  params: [ rec ],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response)
                        msg = error.response.data.error.message;

                      notifyError(msg)
                    }
                );
          });


    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        cod: "",
        name: "",
        fullName: "",
        accessLevel: 1,
        scale: this.scale,
        dbeg: null,
        dend: null,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          cod: rec.cod,
          name: rec.name,
          fullName: rec.fullName,
          accessLevel: rec.accessLevel,
          scale: rec.scale,
          dbeg: rec.dbeg==="1800-01-01" ? null : rec.dbeg,
          dend: rec.dend==="3333-12-31" ? null : rec.dend,
          cmt: rec.cmt,
        };
      }

      this.$q
          .dialog({
            component: UpdateScaleAsgn,
            componentProps: {
              data: data,
              mode: mode,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (mode === "ins") {
              this.rows.push(r);
              this.selected = [];
              this.selected.push(r);
              //this.updSelection(this.selected);
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          });


    },

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (val) =>
              this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
        },
        {
          name: "dbeg",
          label: this.$t("fldDbeg"),
          field: "dbeg",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
              val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "dend",
          label: this.$t("fldDend"),
          field: "dend",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
              val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
      ];
    },

    ////////////////////////////////////////////

    fetchData2(scale, scaleAsgn) {
      this.loading = true
      //
      api
          .post(baseURL, {
            method: "scale/loadScaleValAsgn",
            params: [scale, scaleAsgn],
          })
          .then(
              (response) => {
                this.rows2 = response.data.result.records;
              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = false
          });
    },

    editRow2() {
      this.$q
          .dialog({
            component: UpdateScaleValAsgnUpd,
            componentProps: {
              scale: this.scale,
              scaleAsgn: this.selected[0].id,
              nmAsgn: this.selected[0].name,
              // ...
            },
          })
          .onOk((r) => {
            if (r.res) {
              this.fetchData2(this.scale, this.selected[0].id, this.selected[0].name)
            }
          });
    },

    getColumns2() {
      return [
        {
          name: "val",
          label: this.$t("val"),
          field: "val",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
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
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 60%",
        },
        {
          name: "scaleValColor",
          label: this.$t("scaleColor"),
          field: "scaleValColor",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },
      ];
    },


  },

  created() {

    this.cols = this.getColumns()
    this.cols2 = this.getColumns2()

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.FD_AccessLevel = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AccessLevel.set(it["id"], it["text"]);
          });
        });

  },

  mounted() {
    this.scale = parseInt(this.$route["params"].scale, 10);
    this.fetchData(this.scale);
  },


  setup() {
  }



}
</script>
