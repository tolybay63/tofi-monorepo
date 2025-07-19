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
        :table-colspan="2"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :filter="filter"
        :loading="loading"
        :dense="dense"
        :rows-per-page-options="[0]"
        selection="single"
        v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0" class="text-bold">
          {{ this.infoSelected(selected[0]) }}
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("notextended") }}</div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:typ:sel:notext:ins')"
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
            v-if="hasTarget('mdl:mn_ds:typ:sel:notext:upd')"
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
            v-if="hasTarget('mdl:mn_ds:typ:sel:notext:del')"
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
        <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateNotExtended from "pages/typ/notextended/UpdateNotExtended.vue";

export default {
  name: "NotExtended",
  props: ["typParent"],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: ref(""),
      loading: ref(false),
      separator: ref("cell"),
      selected: ref([]),
      dense: true,

      typ: this.typParent,
    };
  },
  methods: {
    hasTarget,
    infoSelected(row) {
      let o =
          row["nameObj"] !== undefined
              ? " - " + this.$t("obj") + ": " + row["nameObj"]
              : "";
      return this.$t("cls") + ": " + row["nameObj"] + o;
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        typ: this.typParent,
        clsOrObjCls: null,
        obj: null,
      };

      if (mode === "upd") {
        data = {
          id: rec.id,
          typ: rec.typ,
          clsOrObjCls: rec.clsOrObjCls,
          obj: rec.obj,
        };
      }
      this.$q
          .dialog({
            component: UpdateNotExtended,
            componentProps: {
              mode: mode,
              data: data,
              typ: this.typParent,
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
                rec["nameClass"] +
                rec["nameObj"]
                    ? "; " + rec["nameObj"]
                    : "" + ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "typ/deleteNotExtended",
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

    fetchData(typ) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "typ/loadNotExtended",
            params: [typ],
          })
          .then((response) => {
            this.rows = response.data.result.records;
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
            //}, 1000)
          });
    },

    getColumns() {
      return [
        {
          name: "nameClass",
          label: this.$t("cls"),
          field: "nameClass",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "text-align: left; width:50%",
        },
        {
          name: "nameObj",
          label: this.$t("obj"),
          field: "nameObj",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "text-align: left; width:50%",
        },
      ];
    },
  },

  mounted() {
    this.typ = parseInt(this.$route["params"].typ, 10);
    //console.log("typ", this.typ)
    //console.log("typParent", this.typParent)
    this.fetchData(this.typParent);
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
    //
  },

  setup() {
  }

};
</script>

<style scoped></style>
