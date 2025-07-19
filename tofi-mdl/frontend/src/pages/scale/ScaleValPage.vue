<!--  <q-page class="q-pa-md">-->
<template>

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
        :max="page"
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
          <q-avatar color="black" text-color="white" icon="device_thermostat"/>
          {{ $t("scaleVal") }}
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

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifySuccess} from "src/utils/jsutils";
import UpdateScaleVal from "pages/scale/UpdateScaleVal.vue";


export default {
  name: 'ScaleValPage',

  emits: [
    "updateSelect"
  ],

  data: function () {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      page: 1,
      selected: ref([]),
      scale: 0,
    };
  },

  methods: {
    hasTarget,

    rowSelected(val) {
      this.$emit("updateSelect", {selected: val})
    },

    requestData() {
      //console.info("reqParams")
    },

    fetchData(scale) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "scale/loadScaleVal",
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
                '<div style="color: plum">(' + rec.name + ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "scale/deleteScaleVal",
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
        scale: this.scale,
        minVal: null,
        isMinValOpen: false,
        maxVal: null,
        isMaxValOpen: false,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          scale: this.scale,
          minVal: rec.minVal,
          isMinValOpen: rec.isMinValOpen,
          maxVal: rec.maxVal,
          isMaxValOpen: rec.isMaxValOpen,
          cmt: rec.cmt,
        };
      }

      this.$q
          .dialog({
            component: UpdateScaleVal,
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
      return " " +   row.name;
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("val"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 60%",
        },
      ];
    },

  },

  created() {
    this.cols = this.getColumns()
  },

  mounted() {
    //console.info("mounted ScaleValPage", this.$route.params)
    this.scale = parseInt(this.$route["params"].scale, 10);
    this.fetchData(this.scale);
  },

  setup() {
  }
}
</script>
