<template>

  <q-table
    style="height: calc(100vh - 280px); width: 100%"
    color="primary"
    card-class="bg-amber-1"
    row-key="id"
    dense
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    :table-colspan="4"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="cell"
    :loading="loading"
    :rows-per-page-options="[0]"
    selection="single"
    @update:selected="updSelection"
    v-model:selected="selected"
  >

    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:attr:ins')"
        dense
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
        v-if="hasTarget('mdl:mn_ds:attr:upd')"
        dense icon="edit"
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
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense icon="delete"
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
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense no-caps label="Удалить прокуб свойств"
        color="secondary"
        class="q-ml-xl"
        @click="removeProcubeProp"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense no-caps label="Удалить прокуб объектов"
        color="secondary"
        class="q-ml-sm"
        @click="removeProcubeObj"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>

    </template>

    <template #bottom-row>
      <q-td colspan="100%" v-if="selected.length > 0">
        <span class="text-blue"> {{ $t("selectedRow") }}: </span>
        <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
      </q-td>
      <q-td
        v-else-if="rows.length > 0" colspan="100%" class="text-bold">
        {{ $t("infoRow") }}
      </q-td>
    </template>


  </q-table>


</template>


<script>
import {ref} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios.js";
import {date} from "quasar";
import UpdateDims from "pages/cubes/tabs/UpdateDims.vue";
import {notifyError, notifySuccess} from "src/utils/jsutils.js";

export default {
  name: "DimsPage",
  props: ["cubeS", "dOrg"],

  data() {
    return {
      splitterModel: ref(30),
      cols: [],
      rows: [],
      selected: [],
      loading: false,
      FD_CubeSDimType: new Map(),
      typeProp: 0,
      //
    }
  },

  methods: {
    updSelection() {

    },

    removeProcubeProp() {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteProcubeProp"),
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.loading = true
          api
            .post(baseURL, {
              method: "cubes/deleteProCubeProp",
              params: [this.cubeS],
            })
            .then(
              () => {
                notifySuccess(this.$t("success"))
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
            )
            .finally(()=> {
              this.loading = false
            });
        })
    },

    removeProcubeObj() {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteProcubeObj"),
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.loading = true
          api
            .post(baseURL, {
              method: "cubes/deleteProCubeObj",
              params: [this.cubeS],
            })
            .then(
              () => {
                notifySuccess(this.$t("success"))
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
            )
            .finally(()=> {
              this.loading = false
            });
        })
    },

    calcDimPropType() {
      this.rows.forEach(r=> {
        if (r.dimPropType && r.dimPropType !==2) {
          this.typeProp = r.dimPropType;
        }
      })
    },

    load(cubes) {
      this.loading = ref(true)
      api
        .post(baseURL, {
          method: "cubes/loadDims",
          params: [cubes, 0],
        })
        .then(
          (response) => {
            this.rows = response.data.result.records;
            //console.info("rows", this.rows);
          })
        .then(()=> {
          this.calcDimPropType()
        })
        .catch((error=> {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
        )
        .finally(() => {
          this.loading = ref(false);
        });
    },


    editRow(row, mode) {
      let data = {
        id: 0,
        cubeS: this.cubeS,
        name: null,
        dimPropType: null,
        dimPropTypeName: null,
        dOrg: this.dOrg,
      };

      if (mode === "upd") {
        data = {
          id: row.id,
          cubeS: row.cubeS,
          name: row.name,
          cubeSDimType: row.cubeSDimType,
          dimPeriod: row.dimPeriod,
          dimProp: row.dimProp,
          dimObj: row.dimObj,
          dOrg: row.dOrg,
          dimPropType: row.dimPropType,
          dimPropTypeName: row.dimPropTypeName,
        };
      }
      //console.log("data",data)

      this.$q
        .dialog({
          component: UpdateDims,
          componentProps: {
            data: data,
            mode: mode,
            typeProp: this.typeProp
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
                row[key] = r[key];
              }
            }
          }
          //
          this.calcDimPropType()
        });

    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + row.name + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post(baseURL, {
              method: "cubes/deleteCubeDim",
              params: [row.id],
            })
            .then(
              () => {
                this.selected = [];
                this.load(row.cubeS);
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
            );
        })
    },

    infoSelected(row) {
      return " " + row.name;
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "cubeSDimType",
          label: this.$t("cubeSDimType"),
          field: "cubeSDimType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
          format: (val) => this.FD_CubeSDimType.get(val),
        },
        {
          name: "dimPropTypeName",
          label: this.$t("dimPropType"),
          field: "dimPropTypeName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },

        {
          name: "dOrg",
          label: this.$t("dOrg"),
          field: "dOrg",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) =>
            val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
        },
      ]

    }

  },

  mounted() {
    //console.info("DimPage mounted", this.cubeS, this.dOrg)
  },

  created() {
    //console.info("DimPage", this.cubeS, this.dOrg)

    this.loading = true;
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_CubeSDimType"}],
      })
      .then((response) => {
        response.data.result.records.forEach((it) => {
          this.FD_CubeSDimType.set(it["id"], it["text"]);
        })
      })
      .finally(()=> {
        this.loading = false;
      })

    this.cols = this.getColumns();
    this.load(this.cubeS, 0)

  },

  setup() {}

}
</script>

<style scoped>

</style>
<script setup>
import {hasTarget} from "src/utils/jsutils.js";
</script>
