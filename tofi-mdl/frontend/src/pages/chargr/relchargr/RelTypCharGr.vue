<template>
  <div class="no-padding no-margin">
    <q-table
        style="height: calc(100vh - 220px); width: 100%"
        color="primary"
        card-class="bg-amber-1"
        table-class="text-grey-8"
        row-key="id"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        table-header-class="text-bold text-white bg-blue-grey-13"
        :dense="dense"
        separator="cell"
        :filter="filter"
        :loading="loading"
        :rows-per-page-options="[0]"
        selection="single"
        v-model:selected="selected"
        @update:selected="selectedRow"
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
          {{ $t("infoClusterFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("chargrrel") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:ins')"
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
            v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:upd')"
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
            v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:del')"
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

        <q-btn
            v-if="hasTarget('mdl:mn_ds:cgt:sel')"
            :dense="dense"
            icon="pan_tool_alt"
            color="secondary"
            class="q-ml-lg"
            :disable="loading || selected.length === 0"
            @click="charGrRelSel"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>


        <q-toggle
            style="margin-left: 10px"
            :dense="dense"
            v-model="dense"
            :model-value="dense"
            :label="$t('isDense')"
            @update:model-value="fnToggle"
        />

        <q-space/>
        <q-input
            :dense="dense"
            debounce="300"
            color="primary"
            v-model="filter"
            :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>


<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdaterRelTypCharGr from "pages/chargr/relchargr/UpdaterRelTypCharGr.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 15,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({

  emits: [
    "updateSelect"
  ],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: ref(""),
      loading: ref(false),
      separator: ref("cell"),
      selected: ref([]),
      dense: true,
      rcg: 0,

    };
  },

  methods: {
    hasTarget,

    charGrRelSel() {
      this.$router["push"]({
        name: "relTypCharGrProp",
        params: {
          relTypCharGr: this.selected[0].id,
        },
      });
    },

    fnToggle(val) {
      this.dense = val
    },

    selectedRow(row) {
      this.rcg = 0
      this.$emit("updateSelect", row);
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        accessLevel: 1,
        cod: "",
        name: "",
        fullName: "",
        relTyp: null,
        relCls: null,
        cmt: null
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          accessLevel: rec.accessLevel,
          cod: rec.cod,
          name: rec.name,
          fullName: rec.fullName,
          relTyp: rec.relTyp,
          relCls: rec.relCls,
          cmt: rec.cmt,
        };
      }
      const lg = {name: this.lang};

      this.$q
          .dialog({
            component: UpdaterRelTypCharGr,
            componentProps: {
              data: data,
              mode: mode,
              lg: lg,
              dense: this.dense,
              // ...
            },
          })
          .onOk(() => {
            this.fetchData()
          });
    },

    fetchData() {
      this.loading = ref(true);
      this.selected = ref([]);
      //
      api
          .post(baseURL, {
            method: "reltyp/loadRelTypCharGr",
            params: [],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .then(()=> {
            if (this.rcg > 0) {
              let index = this.rows.findIndex((row) => row.id === this.rcg)
              this.selected.push(this.rows[index])
            }
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
            //
          })
          .finally(() => {
            //setTimeout(() => {
            this.loading = ref(false);
            //}, 500)
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
                  method: "relTyp/deleteRelTypCharGr",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response) msg = error.response.data.error.message;

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
          headerStyle: "font-size: 1.2em; width: 10%",
        },

        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
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
          name: "relClsName",
          label: this.$t("relCls"),
          field: "relClsName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "modelName",
          label: this.$t("model"),
          field: "modelName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "dbNames",
          label: this.$t("database"),
          field: "dbNames",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.cod +" - " + row.name;
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  mounted() {
    //console.info("mounted RelTypCharGr", this.$route.params)
    let tab = this.$route["params"].tab
    this.rcg = 0
    if (tab==='relchargr')
      this.rcg = parseInt(this.$route["params"].chargr, 10);
    this.fetchData();
  },

  setup() {
  }

});
</script>

<style></style>
