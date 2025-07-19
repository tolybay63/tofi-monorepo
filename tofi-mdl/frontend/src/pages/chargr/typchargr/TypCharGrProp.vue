<template>
  <q-banner :dense="dense" inline-actions class="bg-amber-1">
    <div style="font-size: 1.2em">{{ $t("itemsPropCharGr") }}</div>
    <template v-slot:action>
      <q-btn
        :dense="dense"
        icon="expand_more"
        color="secondary"
        @click="fnExpand()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("expandAll") }}
        </q-tooltip>
      </q-btn>
      <q-btn
        :dense="dense"
        icon="expand_less"
        color="secondary"
        class="q-ml-sm"
        @click="fnCollapse()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("collapseAll") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:typ:sel:char:prop:edit')"
        :dense="dense"
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
        v-if="hasTarget('mdl:mn_ds:typ:sel:char:prop:upd')"
        :dense="dense"
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
  </q-banner>

  <div style="height: calc(100vh - 245px); width: 100%" class="no-scroll">
    <div class="q-table-container q-table--dense scroll" style="height: 100%">
      <table class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr class style="text-align: left">
          <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
          <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
          <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
          <th :style="cols[3].headerStyle">{{ cols[3].label }}</th>
          <th :style="cols[4].headerStyle">{{ cols[4].label }}</th>
          <th :style="cols[5].headerStyle">{{ cols[5].label }}</th>
        </tr>
        </thead>

        <tbody style="background: aliceblue">
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td :data-th="cols[0].name" @click="toggle(item)">
              <span
                class="q-tree-link q-tree-label"
                v-bind:style="setPadding(item)"
              >
                <q-icon
                  style="cursor: pointer"
                  :name="iconName(item)"
                  color="secondary"
                ></q-icon>

                <q-btn
                  :dense="dense"
                  flat
                  :color="item.propType === undefined ? 'gray' : 'blue'"
                  :disable="item['isItem']"
                  :icon="
                    item['isItem']
                      ? 'adjust'
                      : selected.length > 0 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click.stop="selectedCheck(item)"
                >
                </q-btn>

                <q-icon :name="getIcon(item)"></q-icon>
                {{ item.cod }}
              </span>
          </td>
          <td :data-th="cols[1].name">{{ item.name }}</td>
          <td :data-th="cols[2].name">
            {{ getVal(item.storageType) }}
          </td>
          <td :data-th="cols[3].name">{{ getTable(item.flatTable) }}</td>
          <td :data-th="cols[4].name">{{ item.p_measure }}</td>
          <td :data-th="cols[5].name">{{ item.pv_measure }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, hasTarget, notifyError, pack} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import allConsts from "pages/all-consts";

import UpdateTypCharGrPropUpd from "pages/chargr/typchargr/UpdateTypCharGrPropUpd.vue";
import UpdateTypCharGrProp from "pages/chargr/typchargr/UpdateTypCharGrProp.vue";

export default {
  name: "TypCharGrProp",

  props: ["typ", "typCharGr", "dense"],

  data() {
    return {
      cols: [],
      rows: [],
      FD_StorageType: null,
      FD_NameTable: null,
      //disableEditProp: true,
      loading: ref(false),
      //
      isExpanded: true,
      itemId: null,
      selected: ref([]),
    };
  },

  methods: {
    hasTarget,
    editRow(rec) {
      this.$q
        .dialog({
          component: UpdateTypCharGrProp,
          componentProps: {
            dta: rec,
            typCharGr: this.typCharGr,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk((data) => {
          if (data.res) {
            this.loadData(this.typCharGr);
          }
        });
    },

    getVal(val) {
      if (val === undefined) return null;
      return this.FD_StorageType.get(val);
    },

    getTable(val) {
      if (val === undefined) return null;
      return this.FD_NameTable.get(val);
    },

    editData() {
      this.$q
        .dialog({
          component: UpdateTypCharGrPropUpd,
          componentProps: {
            typCharGr: this.typCharGr,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk((data) => {
          if (data.res) {
            this.loadData(this.typCharGr);
          }
        });
    },

    selectedCheck(item) {
      if (item.propType === undefined) return;
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    loadData(typCharGr) {
      this.loading = ref(true);
      //
      api
        .post(baseURL, {
          method: "typ/loadTypCharGrProp",
          params: [{typCharGr: typCharGr}],
        })
        .then((response) => {
          //console.info("TypCharGrProp", response.data.result.records)
          this.rows = pack(response.data.result.records, "id");
          this.fnExpand();
        })
        .then(() => {

        })
        .catch((error) => {

          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
          //
        })
        .finally(() => {
          this.loading = ref(false);
        });
    },

    getIcon(row) {
      if (row.propType === undefined) return "folder";
      if (allConsts.FD_PropType.factor === row.propType) {
        return "account_tree";
      } else if (allConsts.FD_PropType.attr === row.propType) {
        return "format_shapes";
      } else if (allConsts.FD_PropType.meter === row.propType) {
        return "scale";
      } else if (allConsts.FD_PropType.rate === row.propType) {
        return "speed";
      } else if (allConsts.FD_PropType.typ === row.propType) {
        return "view_quilt";
      } else if (allConsts.FD_PropType.reltyp === row.propType) {
        return "view_column";
      } else if (allConsts.FD_PropType.measure === row.propType) {
        return "square_foot";
      } else if (allConsts.FD_PropType.complex === row.propType) {
        return "category";
      }
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 18%",
        },
        {
          name: "storageType",
          label: this.$t("fldStorageType"),
          field: "storageType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 18%",
        },
        {
          name: "flatTable",
          label: this.$t("tableName"),
          field: "flatTable",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 6%",
        },
        {
          name: "p_measure",
          label: this.$t("propMeasure"),
          field: "p_measure",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 18%",
        },
        {
          name: "pv_measure",
          label: this.$t("measure"),
          field: "pv_measure",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
      ];
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
      let vm = this;
      vm.itemId = item.id;

      item.leaf = false;
      //show  sub items after click on + (more)
      if (
        item.leaf === false &&
        item.expend === undefined &&
        item.children !== undefined
      ) {
        if (item.children.length !== 0) {
          vm.recursive(item.children, [], item.level + 1, item.id, true);
        }
      }
      if (item.expend && item.children !== undefined) {
        item.children.forEach(function (o) {
          o.expend = undefined;
        });

        item["expend"] = ref(undefined);
        item["leaf"] = ref(false);
        vm.itemId = null;
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },
  },

  created() {
    console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_StorageType"}],
      })
      .then((response) => {
        this.FD_StorageType = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_StorageType.set(it["id"], it["text"]);
        });
      });

    api
      .post(baseURL, {
        method: "flatTable/loadTables",
        params: [{}],
      })
      .then((response) => {
        this.FD_NameTable = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_NameTable.set(it["id"], it["nameTable"]);
        });
      });
    //
    this.cols = this.getColumns();

  },

  mounted() {

  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  setup() {
  }

};
</script>

<style scoped></style>
