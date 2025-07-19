<template>

        <q-table
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            dense
            :rows-per-page-options="[0]"
        >
          <template v-slot:top>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:s:edit')"
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

          <template v-slot:body="props">
            <q-tr :props="props">
              <q-td key="cod" :props="props">
                {{ props.row.cod }}
              </q-td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>

              <q-td key="isDefault" :props="props">
                <q-btn
                    dense
                    flat
                    color="blue"
                    :icon="
                    props.row.isDefault
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                >
                </q-btn>
              </q-td>
            </q-tr>
          </template>

          <template #loading>
            <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
          </template>
        </q-table>


</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import UpdaterStatus from "pages/prop/UpdaterStatus.vue";
import {hasTarget} from "src/utils/jsutils.js";

export default {
  props: ["act", "fk", "factor"],

  data() {
    return {
      //form: this.data,
      //lang: this.lg,
      cols: [],
      rows: [],
      loading: ref(false),
    };
  },


  methods: {
    hasTarget,
    loadStatus() {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: this.act + "/loadStatus",
            params: [this.fk],
          })
          .then((response) => {
            this.rows = response.data.result.records;
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    editData() {
      this.$q
          .dialog({
            component: UpdaterStatus,
            componentProps: {
              data: {act: this.act, fk: this.fk, factor: this.factor},
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.loadStatus();
            }
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
          headerStyle: "font-size: 1.2em; width: 25%",
          style: "width: 25%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 70%",
          style: "width: 70%",
        },
        {
          name: "isDefault",
          label: this.$t("isDefault"),
          field: "isDefault",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
          style: "width: 5%",
        },
      ];
    },


  },
  created() {
    this.cols = this.getColumns();
    this.loadStatus();
  },

  setup() {
  }

};
</script>
