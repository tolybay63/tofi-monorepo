<template>
  <q-table
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :dense="dense"
      :rows-per-page-options="[0]"
      selection="single"
      v-model:selected="selected"
  >
    <template v-slot:top>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:dim:edit')"
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

      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:dim:ord')"
          :dense="dense"
          icon="swipe_up_alt"
          color="secondary"
          class="q-ml-lg"
          @click="fnUp(true)"
          :disable="onoffUp()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("up") }}
        </q-tooltip>
      </q-btn>

      <q-btn
          v-if="hasTarget('mdl:mn_ds:pm:sel:dim:ord')"
          :dense="dense"
          icon="swipe_down_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(false)"
          :disable="onoffDown()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("down") }}
        </q-tooltip>
      </q-btn>
    </template>

    <template v-slot:body="props">
      <q-tr :props="props">
        <td style="width: 5px">
          <q-btn
              :dense="dense"
              flat
              color="blue"
              :icon="
              selected.length === 1 && props.row.id === selected[0].id
                ? 'check_box'
                : 'check_box_outline_blank'
            "
              @click="selectedRow(props.row)"
          >
          </q-btn>
        </td>

        <q-td key="cod" :props="props">
          {{ props.row.cod }}
        </q-td>

        <q-td key="name" :props="props">
          {{ props.row.name }}
        </q-td>

        <q-td key="fullName" :props="props">
          {{ props.row.fullName }}
        </q-td>

        <q-td key="isFilled" :props="props">
          <q-icon
              :dense="dense"
              flat
              color="blue"
              size="24px"
              :name="
              props.row.isFilled === 1 ? 'check_box' : 'check_box_outline_blank'
            "
          >
          </q-icon>
        </q-td>

        <q-td key="dimNumber" :props="props">
          {{ props.row.dimNumber }}
        </q-td>


      </q-tr>
    </template>

    <template #loading>
      <q-inner-loading showing color="secondary"></q-inner-loading>
    </template>
  </q-table>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {hasTarget, notifyError} from "src/utils/jsutils";
import UpdaterMultiPropDim from "pages/multiprop/UpdaterMultiPropDim.vue";

export default {
  props: ["lg", "dense"],

  data() {
    return {
      lang: this.lg,
      cols: [],
      rows: [],
      loading: ref(false),
      multiPropId: null,
      selected: ref([]),
      maxLen: 0,
      //diSZ: 0,
    };
  },

  methods: {
    hasTarget,
    selectedRow(item) {
      if (this.selected.length > 0 && item.id === this.selected[0].id) {
        this.selected = [];
      } else {
        this.selected = [];
        this.selected.push(item);
        //this.updSelected(this.selected)
      }
    },

    fnUp(up) {
      api
          .post(baseURL, {
            method: "multiProp/changeOrdMultiPropDim",
            params: [{rec: this.selected[0], up: up}],
          })
          .then(
              () => {
                //reload...
                this.load(this.multiPropId);
              },
              (error) => {
                let msg = error.response.data.error.message
                    ? error.response.data.error.message
                    : error.message;
                notifyError(msg);
              }
          );
    },
    onoffUp() {
      //console.log("selected[0]", this.selected[0])

      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) <= 0;
      }
    },
    onoffDown() {
      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) >= this.maxLen - 1;
      }
    },

    indexOf: function (id) {
      let rez = -1;
      this.rows.forEach((row, index) => {
        //console.log(row)
        if (row.id === id) {
          rez = index;
        }
      });
      return rez;
    },

    load(multiProp) {
      //console.log("multiProp", multiProp);
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadMultiPropDim",
            params: [multiProp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            this.maxLen = this.rows.length;
            //
            if (this.selected.length > 0) {
              let curId = this.selected[0].id;
              this.selected = ref([]);
              let index = this.rows.findIndex((row) => row.id === curId);
              this.selected[0] = this.rows[index];
            }
          })
          .catch((error) => {

            this.$router["push"]("/");

            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    editData() {
      this.$q
          .dialog({
            component: UpdaterMultiPropDim,
            componentProps: {
              multiPropId: this.multiPropId,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.load(this.multiPropId);
            }
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
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 50%",
        },
        {
          name: "isFilled",
          label: this.$t("isFilled"),
          field: "isFilled",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },
        {
          name: "dimNumber",
          label: "Номер измерения",
          field: "dimNumber",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },

      ];
    },

  },

  mounted() {
    this.multiPropId = parseInt(this.$route["params"].mp, 10);
    this.load(this.multiPropId);
  },

  created() {
    console.log("created!!!");
    this.cols = this.getColumns();
  },

  setup() {
  }

};
</script>
