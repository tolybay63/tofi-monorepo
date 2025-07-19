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
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        :separator="separator"
        :filter="filter"
        :loading="loading"
        :dense="dense"
        :rows-per-page-options="[0]"
        selection="single"
        v-model:selected="selected"
    >
      <template v-slot:body="props">
        <q-tr :props="props">
          <q-td style="width: 5px">
            <q-btn
                dense
                flat
                color="blue"
                :icon="
                selected.length === 1 && selected[0].id === props.row.id
                  ? 'check_box'
                  : 'check_box_outline_blank'
              "
                @click="selectedRow(props.row)"
            >
            </q-btn>
          </q-td>

          <q-td key="name" :props="props">
            <q-icon name="brightness_1" :color="getIcon(props.row)"></q-icon>
            {{ props.cols[0].value }}
          </q-td>
          <q-td key="fullName" :props="props">
            {{ props.cols[1].value }}
          </q-td>
          <q-td key="isReq" :props="props">
            {{ props.cols[2].value }}
          </q-td>
          <q-td key="isUniq" :props="props">
            {{ props.cols[3].value }}
          </q-td>
        </q-tr>
      </template>

      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0" class="text-bold">
          {{ this.infoSelected(selected[0]) }}
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoClusterFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clusterFactors") }}</div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:typ:sel:fac:ins')"
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
            v-if="hasTarget('mdl:mn_ds:typ:sel:fac:upd')"
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
            v-if="hasTarget('mdl:mn_ds:typ:sel:fac:del')"
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

        <q-toggle
            style="margin-left: 10px"
            :dense="dense"
            v-model="dense"
            :label="$t('isDense')"
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
import UpdateClusterFactor from "pages/typ/clusterfactor/UpdateClusterFactor.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({
  methods: {
    hasTarget,
    getIcon(row) {
      if (row.isOwn === 1) return "green";
      else return "gray";
    },

    selectedRow(row) {
      if (this.selected.length > 0) {
        if (this.selected[0].id !== row.id) {
          this.selected = [];
          this.selected.push(row);
        } else {
          this.selected = [];
        }
      } else this.selected.push(row);
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        name: "",
        fullName: "",
        typ: this.typId,
        factor: 0,
        isReq: true,
        isUniq: true,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          name: rec.name,
          fullName: rec.fullName,
          typ: rec.typ,
          factor: rec.factor,
          isReq: rec.isReq,
          isUniq: rec.isUniq,
        };
      }
      const lg = {name: this.lang};

      this.$q
          .dialog({
            component: UpdateClusterFactor,
            componentProps: {
              data: data,
              mode: mode,
              lg: lg,
              dense: this.dense,
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
          })
          .onCancel(() => {
            //console.log('Cancel!')
          });
    },

    fetchData(typ) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "typ/loadTypClusterFactor",
            params: [typ],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            this.selected = ref([]);
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
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "typ/deleteTypClusterFactor",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
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
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 30%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 40%",
        },
        {
          name: "isReq",
          label: this.$t("isReq"),
          field: "isReq",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) => (val ? this.$t("yes") : this.$t("no")),
        },
        {
          name: "isUniq",
          label: this.$t("isUniq"),
          field: "isUniq",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) => (val ? this.$t("yes") : this.$t("no")),
        },
      ];
    },

    infoSelected(row) {
      return this.$t("selectedRow") + ": " + row.name;
    },
  },

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: ref(""),
      loading: ref(false),
      separator: ref("cell"),
      selected: ref([]),
      typId: 0,
      dense: true,
    };
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  mounted() {
    //console.log("mounted")
    //console.log("params", this.$route.params.meterStruct)
    this.typId = parseInt(this.$route["params"].typ, 10);
    this.fetchData(this.typId);
  },

  setup() {
  },
});
</script>

<style></style>
