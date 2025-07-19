<template>
  <q-page class="q-pa-md" style="height: 100px">

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
        :filter="filter"
        :loading="loading"
        :rows-per-page-options="[20, 25, 0]"
        :max="page"
        selection="single"
        v-model:selected="selected"
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
          <q-avatar color="black" text-color="white" icon="device_thermostat"></q-avatar>
          {{ $t("scales") }}
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
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:sel')"
            dense icon="pan_tool_alt" color="secondary" class="q-ml-lg"
            :disable="loading || selected.length === 0"
            @click="selectScale()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space/>
        <q-input
            dense debounce="300" color="primary" :model-value="filter.value" v-model="filter"
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


  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifySuccess} from "src/utils/jsutils";
import UpdateScale from "pages/scale/UpdateScale.vue";

const requestParams = {
  page: 1,
  limit: 20,
  total: 0
}


export default {
  name: 'ScalePage',

  data: function () {
    return {
      cols: [],
      rows: [],
      FD_AccessLevel: null,
      FD_ScaleType: null,
      filter: ref(""),
      loading: ref(false),
      page: 1,
      selected: ref([]),
      scale: 0,
    };
  },

  methods: {
    hasTarget,

    requestData() {
      //console.info("reqParams", reqParams)
    },

    fetchData(reqParams) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "scale/load",
            params: [reqParams],
          })
          .then(
              (response) => {
                //this.FD_AccessLevel = response.data.result.store["dictdata"]["FD_AccessLevel"]
                //console.log("FD_AL", this.FD_AccessLevel)
                //this.rows = response.data.result.store.records;
                this.rows = response.data.result.records;

/*
                const meta = response.data.result.meta;
                this.pagination.page = meta.page;
                this.pagination.rowsPerPage = meta.limit;
                this.pagination.rowsNumber = meta.total;
*/

                this.selected = ref([]);
                if (this.scale > 0) {
                  let index = this.rows.findIndex((row) => row.id === this.scale);
                  this.selected.push(this.rows[index]);
                }

              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            //setTimeout(() => {
            this.loading = ref(false);
            //}, 500)
          });
    },

    selectScale() {
      this.$router["push"]({
        name: "ScaleSelectedPage",
        params: {
          scale: this.selected[0].id,
          info: this.infoSelected(this.selected[0])
        },
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
                  method: "scale/delete",
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
        scaleType: 1,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          cod: rec.cod,
          name: rec.name,
          fullName: rec.fullName,
          accessLevel: rec.accessLevel,
          scaleType: rec.scaleType,
          cmt: rec.cmt,
        };
      }

      this.$q
          .dialog({
            component: UpdateScale,
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
          headerStyle: "font-size: 1.2em; width: 5%",
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
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "scaleType",
          label: this.$t("scaleType"),
          field: "scaleType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (val) =>
              this.FD_ScaleType ? this.FD_ScaleType.get(val) : null,
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
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
        },
      ];
    },

  },

  created() {

    this.cols = this.getColumns()

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
          method: "dict/load",
          params: [{dict: "FD_ScaleType"}],
        })
        .then((response) => {
          this.FD_ScaleType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_ScaleType.set(it["id"], it["text"]);
          });
        });

    this.fetchData(requestParams)
  },

  mounted() {
    this.scale = parseInt(this.$route["params"].scale, 10);
  },

  setup() {
  }



}
</script>
