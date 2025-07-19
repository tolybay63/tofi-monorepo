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

      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("chargrtyp") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:typ:sel:char:ins')"
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
            v-if="hasTarget('mdl:mn_ds:typ:sel:char:upd')"
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
            v-if="hasTarget('mdl:mn_ds:cgt:del')"
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
            @click="charGrTypSel"
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
            :model-value="filter.value"
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
  </div>

</template>


<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateTypCharGr from "pages/chargr/typchargr/UpdateTypCharGr.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({
  name: "TypCharGr",

  emits: [
    "updateSelect"
  ],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: ref(""),
      loading: ref(false),
      selected: ref([]),
      dense: true,
      tcg: 0
    };
  },

  methods: {
    hasTarget,
    charGrTypSel() {
      this.$router["push"]({
        name: "typCharGrProp",
        params: {
          typCharGr: this.selected[0].id,
        },
      });
    },

    fnToggle(val) {
      this.dense = val
    },

    selectedRow(val) {
      this.tcg = 0
      this.$emit("updateSelect", val);
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        accessLevel: 1,
        cod: "",
        name: "",
        fullName: "",
        typ: null,
        factorVal: null,
        cmt: null
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          accessLevel: rec.accessLevel,
          cod: rec.cod,
          name: rec.name,
          fullName: rec.fullName,
          typ: rec.typ,
          factorVal: rec.factorVal,
          cmt: rec.cmt,
        };
      }
      const lg = {name: this.lang};

      this.$q
          .dialog({
            component: UpdateTypCharGr,
            componentProps: {
              data: data,
              mode: mode,
              lg: lg,
              dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            if (mode === "ins") {
              this.rows.push(r);
              this.selected = [];
              this.selected.push(r);
              this.selectedRow(this.selected);
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          });
    },

    fetchData() {
      this.loading = ref(true);
      this.selected = ref([]);
      //
      api
          .post(baseURL, {
            method: "typ/loadTypCharGr",
            params: [],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .then(()=> {
            if (this.tcg > 0) {
              let index = this.rows.findIndex((row) => row.id === this.tcg)

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
                  method: "typ/deleteTypCharGr",
                  params: [rec],
                })
                .then(
                    () => {
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "typName",
          label: this.$t("typ"),
          field: "typName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "fvName",
          label: this.$t("clusterFV"),
          field: "fvName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
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
          headerStyle: "font-size: 1.2em; width: 15%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  mounted() {
    let tab = this.$route["params"].tab
    this.tcg = 0
    if (tab==='typchargr')
      this.tcg = parseInt(this.$route["params"].chargr, 10);
    this.fetchData();
  },

  setup() {
  }

});
</script>

<style></style>
