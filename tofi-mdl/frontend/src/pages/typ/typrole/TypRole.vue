<template>
  <div class="no-padding no-margin">
    <q-splitter v-model="splitterModel" :model-value="splitterModel" :limits="[60, 100]">
      <template v-slot:before>
        <q-table
            style="height: calc(100vh - 250px); width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading"
            :dense="dense"
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
            <q-td
                colspan="100%"
                v-else-if="this.rows.length > 0"
                class="text-bold"
            >
              {{ $t("infoRole") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("roles2") }}</div>

            <q-space/>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:ins')"
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
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:upd')"
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
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:del')"
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
            <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <q-table
            style="height: calc(100vh - 250px); width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            :columns="cols2"
            :rows="rows2"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2"
            :dense="dense"
            selection="single"
            v-model:selected="selected2"
            :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected2(selected2[0]) }} </span>
            </q-td>
            <q-td
                colspan="100%"
                v-else-if="this.rows.length > 0"
                class="text-bold"
            >
              {{ $t("infoLife") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("life") }}</div>
            <q-space/>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:ins')"
                :dense="dense"
                icon="post_add"
                color="secondary"
                :disable="loading && loading2"
                @click="editRow2(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:upd')"
                :dense="dense"
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                :disable="loading2 || selected2.length === 0"
                @click="editRow2(selected2[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:del')"
                :dense="dense"
                icon="delete"
                color="secondary"
                class="q-ml-sm"
                :disable="loading2 || selected2.length === 0"
                @click="removeRow2(selected2[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #loading>
            <q-inner-loading :showing="loading2" color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {date} from "quasar";
import UpdateTypRole from "pages/typ/typrole/UpdateTypRole.vue";
import UpdateTypRoleLifeInterval from "pages/typ/typrole/UpdateTypRoleLifeInterval.vue";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

export default {
  name: "TypRole",

  data() {
    //console.log("data")
    return {
      splitterModel: 100,
      cols: [],
      rows: [],
      loading: false,
      selected: [],
      dense: true,
      //
      cols2: [],
      rows2: [],
      loading2: false,
      selected2: [],

      typId: 0,
    };
  },

  methods: {
    hasTarget,
    updSelected(cur) {
      this.selected2 = [];
      if (cur.length > 0) {
        this.fetchData2(cur[0].id);
        this.splitterModel = ref(60);
      } else {
        this.splitterModel = ref(100);
      }
    },

    removeRow(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))

      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.name +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "typ/deleteTypRole",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      this.splitterModel = ref(100);
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

    editRow(rec, mode) {
      let data = {
        id: 0,
        typ: this.typId,
        role: 0,
        cmtVer: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          typ: rec.typ,
          role: rec.role,
          cmt: rec.cmt,
        };
      }
      //const upd = {isIns: ins};
      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateTypRole,
            componentProps: {
              data: data,
              typ: this.typId,
              mode: mode,
              lg: lg,
              dense: true,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (mode === "ins") {
              this.rows.push(r);
              this.selected = [];
              this.selected.push(r);
              this.splitterModel = ref(60);
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          })
          .onCancel(() => {
            //console.log('Cancel!')
          });
    },

    fetchData(typ) {
      this.loading = true;
      api
          .post(baseURL, {
            method: "typ/loadTypRole",
            params: [typ],
          })
          .then(
              (response) => {
                this.rows = response.data.result.records;
                //console.log("data TypRole", this.rows)
              },
              (error) => {

                this.$router["push"]("/");
                let msg
                if (error.response)
                  msg = this.$t(error.response.data.error.message);
                else msg = error.message;
                notifyError(msg);
              }
          )
          .finally(() => {
            //setTimeout(() => {
            this.loading = false;
            //}, 500)
          });
    },

    removeRow2(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))
      let d1 =
          rec.dbeg === "1800-01-01"
              ? "..."
              : date.formatDate(rec.dbeg, "DD.MM.YYYY");
      let d2 =
          rec.dend === "3333-12-31"
              ? "..."
              : date.formatDate(rec.dend, "DD.MM.YYYY");

      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                d1 +
                " - " +
                d2 +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
          })
          .onOk(() => {
            let index = this.rows2.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "typ/deleteTypRoleLife",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows2.splice(index, 1);
                      this.selected2 = ref([]);
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

    editRow2(rec, mode) {
      let data = {
        id: 0,
        typrole: this.selected[0].id,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          typrole: rec.typrole,
          dbeg: rec.dbeg > "1800-01-01" ? rec.dbeg : null,
          dend: rec.dend < "3333-12-31" ? rec.dend : null,
          cmt: rec.cmt,
        };
      }
      //const upd = {isIns: ins};
      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateTypRoleLifeInterval,
            componentProps: {
              data: data,
              mode: mode,
              lg: lg,
              dense: true,
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
                  rec[key] = r[key];
                }
              }
            }
          });
    },

    fetchData2(typrole) {
      this.loading2 = true;

      api
          .post(baseURL, {
            method: "typ/loadTypRoleLife",
            params: [typrole],
          })
          .then(
              (response) => {
                this.rows2 = response.data.result.records;
              },
              (error) => {

                this.$router["push"]("/");
                let msg
                if (error.response)
                  msg = this.$t(error.response.data.error.message);
                else msg = error.message;
                notifyError(msg);
              }
          )
          .finally(() => {
            //setTimeout(() => {
            this.loading2 = false;
            //}, 500)
          });
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 35%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 45%",
        },
      ];
    },

    getColumns2() {
      return [
        {
          name: "dbeg",
          label: this.$t("fldDbeg"),
          field: "dbeg",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 90px",
          format: (val) =>
              val <= "1800-01-01" ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "dend",
          label: this.$t("fldDend"),
          field: "dend",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em;",
          style: "width: 90px",
          format: (val) =>
              val >= "3333-12-01" ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 40%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.name;
    },

    infoSelected2(row) {
      let d1 =
          row.dbeg === "1800-01-01"
              ? "..."
              : date.formatDate(row.dbeg, "DD.MM.YYYY");
      let d2 =
          row.dend === "3333-12-31"
              ? "..."
              : date.formatDate(row.dend, "DD.MM.YYYY");
      return " " + d1 + " - " + d2;
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();
  },

  mounted() {
    //console.log("params", this.$route.params.meterStruct)
    this.typId = parseInt(this.$route["params"].typ, 10);
    this.fetchData(this.typId);
  },

  setup() {
  }

};
</script>

<style scoped></style>
