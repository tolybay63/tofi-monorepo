
<template>
  <div class="no-padding no-margin">
    <q-splitter
        v-model="splitterModel"
        :model-value="splitterModel"
        class="no-padding no-margin"
        horizontal
        style="height: calc(100vh - 300px); width: 100%"
        separator-class="bg-red"

    >

      <template v-slot:before>
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
            :rows-per-page-options="[10, 15, 20, 0]"
            @update:selected="updSelected"
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
              {{ $t("infoRow") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("relclss") }}</div>
            <q-space/>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
                dense
                icon="edit_note"
                color="secondary"
                :disable="loading"
                @click="createGroup()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("addGroupRecords") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
                dense
                class="q-ml-sm"
                icon="delete_sweep"
                color="secondary"
                :disable="loading"
                @click="deleteGroup()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deleteGroupRecords") }}
              </q-tooltip>
            </q-btn>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
                dense
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
                v-if="hasTarget('mdl:mn_ds:reltyp:memb:del')"
                dense
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

            <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel')"
                dense
                icon="pan_tool_alt"
                color="secondary"
                class="q-ml-lg"
                :disable="loading || selected.length === 0"
                @click="relClsSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>


          </template>

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <rel-cls-member :relcls="relclsId" ref="childMember" />
      </template>

    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import FD_Consts from "pages/all-consts";
import UpdateRelCls from "pages/reltyp/relcls/UpdateRelCls.vue";
import RelClsMember from "pages/reltyp/relcls/RelClsMember.vue";
import UpdateGroupRelCls from "pages/reltyp/relcls/UpdateGroupRelCls.vue";
import DeleteGroupRelCls from "pages/reltyp/relcls/DeleteGroupRelCls.vue";

export default {
  name: 'RelCls',
  components: {RelClsMember},

  data() {

    return {
      splitterModel: ref(70),
      cols: [],
      rows: [],
      loading: ref(false),
      //separator: ref("cell"),
      selected: ref([]),
      FD_AccessLevel: null,
      dataBase: null,
      reltypId: 0,
      relclsId: 0,
    }
  },

  methods: {
    hasTarget,

    updSelected(sel) {
      //console.log("cur***********", sel);
      this.$refs.childMember.selected = []

      if (sel.length > 0) {
        this.$refs.childMember.fetchData(sel[0].id)
      } else {
        this.$refs.childMember.fetchData(0)
      }
    },

    relClsSelect() {
      this.$router["push"]({
        name: "relclsSelected",
        params: {
          reltyp: this.reltypId,
          relcls: this.selected[0].id,
          tab: 'vers'
        },
      });
    },

    createGroup() {
      const lg = {name: this.lang}
      this.$q
          .dialog({
            component: UpdateGroupRelCls,
            componentProps: {
              data: {relTyp: this.reltypId},
              lg: lg,
              //dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (r.res) {
              //reload...
              this.fetchData(this.reltypId);
            }
          });
    },

    deleteGroup() {
      const lg = {name: this.lang}
      this.$q
          .dialog({
            component: DeleteGroupRelCls,
            componentProps: {
              relTyp: this.reltypId,
              lg: lg,
              //dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            this.fetchData(this.reltypId);
            this.selected = ref([]);
            this.updSelected(this.selected)

            if (r !== "") {
              notifyInfo(r)
            }
          });
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        cod: null,
        accessLevel: FD_Consts.FD_AccessLevel.common,
        name: "",
        fullName: "",
        relTyp: this.reltypId,
        isOpenness: true,
        dataBase: null,
        ord: null,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          cod: rec.cod,
          accessLevel: rec.accessLevel,
          name: rec.name,
          fullName: rec.fullName,
          relTyp: this.reltypId,
          isOpenness: rec.isOpenness,
          dataBase: rec.dataBase,
          ord: rec.ord,
          cmt: rec.cmt,
        };
      }

      //const lg = { name: this.lang };

      this.$q
          .dialog({
            component: UpdateRelCls,
            componentProps: {
              data: data,
              mode: mode,
              //lg: lg,
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
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          });
    },

    removeRow(rec) {
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.cod + "-" + rec.name +
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
                  method: "relcls/delete",
                  params: [rec],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      this.updSelected(this.selected)
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response) msg = error.response.data.error.message;

                      notifyError(msg);
                    }
                );
          })
    },

    fetchData(reltyp) {
      this.loading = ref(true);

      api
          .post(baseURL, {
            method: "relcls/load",
            params: [reltyp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            //
            this.selected = ref([]);

            if (this.relclsId > 0) {
              let index = this.rows.findIndex((row) => row.id === this.relclsId);
              this.selected.push(this.rows[index]);

              this.updSelected(this.selected)
            }
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

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
              this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
        },

        {
          name: "dataBase",
          label: this.$t("dbNameLabel"),
          field: "dataBase",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
              this.dataBase ? this.dataBase.get(val) : null,
        },

        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
      ];
    },


  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

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

    api
        .post(baseURL, {
          method: "database/loadDbForSelect",
          params: [],
        })
        .then((response) => {
          //console.info("db", response.data.result.records)
          this.dataBase = new Map();
          response.data.result.records.forEach((it) => {
            this.dataBase.set(it["id"], it["name"]);
          });
        });

    this.cols = this.getColumns();

  },

  mounted() {
    this.reltypId = parseInt(this.$route["params"].reltyp, 10);
    this.relclsId = parseInt(this.$route["params"].relcls, 10);
    this.fetchData(this.reltypId);
    //console.info("mounted RelCls", this.$route.params)
  },

  setup() {
    return {
      infoSelected(row) {
        return " " + row.cod + "-" + row.name;
      },
    }

  },

}
</script>
