<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
        style="height: 100%; width: 100%"
        color="primary"
        card-class="bg-amber-1"
        row-key="cod"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :filter="filter"
        :loading="loading"
        dense
        selection="single"
        v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoApp") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="storage"></q-avatar>
          {{ $t("database") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ins')"
            icon="post_add"
            color="secondary"
            dense
            :disable="loading"
            @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:upd')"
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
            v-if="hasTarget('mdl:mn_ds:fac:del')"
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

        <q-space/>
        <q-input
            dense
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
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {useUserStore} from "stores/user-store";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateDataBase from "pages/database/UpdateDataBase.vue";

export default defineComponent({
  data: function () {
    return {
      cols: [],
      rows: [],
      filter: ref(""),
      loading: ref(false),
      selected: ref([]),
      FD_DataBaseType: null,
    };
  },

  methods: {
    hasTarget,
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
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; bold",
          style: "width: 5%",
        },
        {
          name: "modelName",
          label: this.$t("modelNameLabel"),
          field: "modelName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
        },
        {
          name: "dataBaseType",
          label: this.$t("dataBaseType"),
          field: "dataBaseType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) => this.FD_DataBaseType ? this.FD_DataBaseType.get(val) : null,
        },
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
          style: "width: 30%",
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

    load() {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "database/load",
            params: [],
          })
          .then(
              (response) => {
                this.rows = response.data.result.records;
              },
              (error) => {
                const store = useUserStore();
                let {setUserName} = store;
                setUserName("");
                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = ref(false);
          });
    },

    editRow(rec, mode) {
      let data = {};
      if (mode === "ins") {
        this.loading = ref(true);
        api
            .post(baseURL, {
              method: "database/newRec",
              params: [],
            })
            .then(
                (response) => {
                  data = response.data.result.records[0];
                },
                (error) => {
                  const store = useUserStore();
                  let {setUserName} = store;
                  setUserName("");
                  let msg = error.message;
                  if (error.response)
                    msg = this.$t(error.response.data.error.message);
                  notifyError(msg);
                }
            )
            .finally(() => {
              this.loading = ref(false);
            });
      } else {
        data = {
          id: rec.id,
          cod: rec.cod,
          modelName: rec.modelName,
          dataBaseType: rec.dataBaseType,
          name: rec.name,
          fullName: rec.fullName,
          cmt: rec.cmt,
        };
      }

      this.$q
          .dialog({
            component: UpdateDataBase,
            componentProps: {
              data: data,
              mode: mode,
              // ...
            },
          })
          .onOk((r) => {
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
                  method: "database/delete",
                  params: [rec],
                })
                .then(
                    () => {
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      notifyInfo(error.message);
                    }
                );
          });
    },
  },

  created() {
    this.cols = this.getColumns();
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_DataBaseType"}],
        })
        .then((response) => {
          this.FD_DataBaseType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_DataBaseType.set(it["id"], it["text"]);
          });
        });

    this.load();
  },

  watch: {
    cols: function () {
      return this.getColumns();
    },
  },

  setup() {
  },
});
</script>
