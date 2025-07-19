<template>
  <div class="no-padding no-margin">

    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading.value"
      dense
      selection="single"
      v-model:selected="selected.value"
      @update:selected="updSelected"
      :rows-per-page-options="[0]"
    >
      <template v-slot:body="props">
        <q-tr :props="props">
          <td style="width: 5px">
            <q-btn
              dense
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
          <q-td key="name" :props="props">
            {{ props.row.name }}
          </q-td>

          <q-td key="cmt" :props="props">
            {{ props.row.cmt }}
          </q-td>
        </q-tr>
      </template>

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
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("membersRelObj") }}</div>

        <q-space/>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>

      </template>


      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>

  </div>
</template>

<script>
import {ref} from "vue";
import {baseURL} from "boot/axios";
import {hasTarget, notifyError} from "src/utils/jsutils";
import UpdaterObj from "pages/types_objects/UpdaterObj.vue";
import {extend} from "quasar";


export default {
  name: "RelObjMember",
  props: ["relobj"],

  data() {
    //console.log("data")
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      selected: ref([]),

    };
  },

  emits: [
    "updateSelect"
  ],

  methods: {
    hasTarget,

    updSelected(sel) {
    },

    selectedRow(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id) {
        vm.selected = [];
      } else {
        //console.info("item", item)
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    editRow(rec) {
      //console.log("rec Obj", rec)
      let data = {}
      this.$q
        .dialog({
          component: UpdaterObj,
          componentProps: {
            data: extend(true, data, rec),
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          for (let key in r) {
            if (r.hasOwnProperty(key)) {
              rec[key] = r[key];
            }
          }
        })
    },

    fetchData(relobj) {
      this.loading = ref(true);

      this.$axios
        .post(baseURL, {
          method: "data/loadRelObjMember",
          params: [relobj],
        })
        .then((response) => {
          this.rows = response.data.result["records"];
          //console.info("rows", this.rows);
          //
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = this.$t(error.response.data.error.message);

          notifyError(msg);
        })
        .finally(() => {
          this.loading = ref(false);
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
          style: "width: 65%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 35%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.name;
    },

  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    this.cols = this.getColumns();
  },

  mounted() {
    //console.log("mounted")

    this.fetchData(this.relobj);
  },

  setup() {

  },
};
</script>

<style scoped></style>
