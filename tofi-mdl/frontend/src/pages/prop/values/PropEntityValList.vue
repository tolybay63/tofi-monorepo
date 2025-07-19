<template>
  <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="cod"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :dense="dense"
  >
    <template v-slot:top>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:prop:val:edit')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="editData()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-table>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {hasTarget, notifyError} from "src/utils/jsutils";
import UpdaterPropEntityVal from "pages/prop/values/UpdaterPropEntityVal.vue";

export default {
  name: "PropEntityValList",

  props: ["entityType"],
  data() {
    return {
      rows: [],
      cols: [],

      loading: ref(false),
      propId: null,
      dense: true,
    };
  },

  methods: {
    hasTarget,
    editData() {
      this.$q
          .dialog({
            component: UpdaterPropEntityVal,
            componentProps: {
              prop: this.propId,
              entityType: this.entityType,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.fetchData(this.propId);
            }
          });
      //}
    },

    fetchData(prop) {
      this.loading = ref(true);

      api
          .post(baseURL, {
            method: "prop/loadPropValEntity",
            params: [prop, this.entityType],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 35%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 45%",
        },
      ];
    },
  },

  mounted() {
    console.log("mounted");
    this.propId = parseInt(this.$route["params"].prop, 10);

    this.fetchData(this.propId);
  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  setup() {
  }

};
</script>

<style scoped></style>
