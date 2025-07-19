<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-width
    full-height
  >
    <q-card class="q-dialog-plugin no-scroll">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("view") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 48px">
        <q-btn
          dense
          icon="expand_more"
          color="secondary"
          @click="fnExpand()"
          style="margin-bottom: 5px"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          dense
          icon="expand_less"
          color="secondary"
          class="q-ml-sm"
          @click="fnCollapse()"
          style="margin-bottom: 5px; margin-left: 5px"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-input
          v-if="cmdDate"
          v-model="dt"
          stack-label
          :model-value="dt"
          type="date"
          dense
          style="width: 100px; margin-left: 20px"
          :label="$t('date')"
          @update:model-value="fnDt"
        />

        <q-space />
        <q-card-actions align="right">
          <q-btn
            :dense="dense"
            color="secondary"
            icon="cancel"
            :label="$t('close')"
            @click="onCancelClick"
          />
        </q-card-actions>
      </q-bar>
      <q-inner-loading :showing="loading" color="secondary" />
      <div
        class="q-table-container q-table--dense wrap bg-orange-1 scroll"
        style="height: 89%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
              <tr class style="text-align: left">
                <th
                  v-for="(col, index) in cols"
                  :key="index"
                  :class="col.headerClass"
                  :style="col.headerStyle"
                >
                  {{ col.label }}
                </th>
              </tr>
            </thead>

            <tbody style="background: aliceblue">
              <tr v-for="(item, index) in arrayTreeObj" :key="index">
                <!--name-->
                <td :data-th="cols[0].name" @click="toggle(item)">
                  <span class="q-tree__node" v-bind:style="setPadding(item)">
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    ></q-icon>

                    <q-icon :name="getIcon(item)"></q-icon>
                    {{ item[cols[0].field] }}
                  </span>
                </td>

                <!--dbeg-->
                <td :data-th="cols[1].name">
                  {{ item[cols[1].field] }}
                </td>

                <!--dend-->
                <td :data-th="cols[2].name">
                  {{ item[cols[2].field] }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <q-bar>{{ $t("countAll") }}: {{ this.sz }}</q-bar>
      </div>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {ref} from "vue";
import {collapsAll, expandAll, notifyError, pack} from "src/utils/jsutils";
import {date} from "quasar";

const expand = (item) => {
  item.expend = ref(true);
  const { children } = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

const collaps = (item) => {
  item.expend = ref(false);
  const { children } = item;
  if (children.length > 0) {
    item.leaf = ref(false);
  } else {
    item.leaf = ref(true);
    item.expend = undefined;
  }
};

export default {
  props: ["dimperiod", "lg", "dense", "cmdDate"],

  data() {
    return {
      lang: this.lg,
      cols: [],
      rows: [],
      loading: false,
      //
      isExpanded: true,
      currentNode: null,
      itemId: null,
      sz: 0,
      dt: this.today(),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnDt(val) {
      //console.info("fnDt", val)
      this.loadData({ dimperiod: this.dimperiod, currDate: val });
    },

    today() {
      let d = new Date();
      let currDate = d.getDate();
      let currMonth = d.getMonth() + 1;
      let currYear = d.getFullYear();
      return (
        currYear +
        "-" +
        (currMonth < 10 ? "0" + currMonth : currMonth) +
        "-" +
        (currDate < 10 ? "0" + currDate : currDate)
      );
    },

    loadData(params) {
      this.loading = true;
      api
        .post(baseURL, {
          method: "dimperioditem/loadTree",
          params: [params],
        })
        .then((response) => {
          this.sz = response.data.result.records.length;
          this.rows = pack(response.data.result.records, "id");
          console.log(this.rows);
        })
        .catch((error)=> {
          notifyError(error.response.data.error.message)

        })
        .finally(() => {
          this.loading = false;
        });
    },

    getIcon() {
      return "date_range";
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 80%",
        },
        {
          name: "dbeg",
          label: this.$t("fldDbeg"),
          field: "dbeg",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
            val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "dend",
          label: this.$t("fldDend"),
          field: "dend",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
            val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {},

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    recursive(obj, newObj, level, itemId, isExpend) {
      let vm = this;
      obj.forEach(function (o) {
        if (o.children && o.children.length !== 0) {
          o.level = level;
          o.leaf = false;
          newObj.push(o);
          if (o.id === itemId) {
            o.expend = isExpend;
          }
          if (o.expend) {
            vm.recursive(o.children, newObj, o.level + 1, itemId, isExpend);
          }
        } else {
          o.level = level;
          o.leaf = true;
          newObj.push(o);
          return false;
        }
      });
    },

    iconName(item) {
      if (item.expend) {
        return "remove_circle_outline";
      }
      if (item.children && item.children.length > 0) {
        return "control_point";
      }
      return "";
    },

    toggle(item) {
      if (item.children && item.children.length > 0) {
        if (item.expend) collaps(item);
        else expand(item);
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },
  },

  created() {
    this.cols = this.getColumns();
    this.loadData({ dimperiod: this.dimperiod });

  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },
};
</script>
