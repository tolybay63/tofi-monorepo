<template>
  <q-banner :dense="dense" inline-actions class="bg-amber-1">
    <div style="font-size: 1.2em">{{ $t("itemsMultiPropCharGr") }}</div>
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
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:matrix:edit')"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:matrix:upd')"
          :dense="dense"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="!!(this.loading || this.selected.length === 0)"
          @click="editRow(selected[0])"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-banner>

  <div class="q-table-container q-table--dense wrap bg-orange-1">
    <div class="q-pa-sm-sm bg-orange-1">
      <div class="q-table-middle scroll">
        <table class="q-table q-table--cell-separator q-table--bordered wrap">
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
            <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
            <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
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
                      :color="item.children.length > 0 ? 'gray' : 'blue'"
                      :icon="
                      selected.length > 0 && item.id === selected[0].id
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
            <td :data-th="cols[2].name">{{ getVal(item.storageType) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, hasTarget, notifyError, pack} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import UpdaterRelTypCharGrMultiProp from "pages/chargr/relchargr/UpdaterRelTypCharGrMultiProp.vue";
import UpdaterRelTypCharGrMultiPropUpd from "pages/chargr/relchargr/UpdaterRelTypCharGrMultiPropUpd.vue";

export default {
  name: "RelTypCharGrMultiProp",

  props: ["relTypCharGr", "dense"],

  data() {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      //
      FD_StorageType: null,
      isExpanded: true,
      selectedRowID: {},
      itemId: null,
      selected: ref([]),
    };
  },

  methods: {
    hasTarget,
    getVal(val) {
      if (val===undefined) return null;
      return this.FD_StorageType.get(val);
    },

    editRow(rec) {
      this.$q
          .dialog({
            component: UpdaterRelTypCharGrMultiProp,
            componentProps: {
              data: rec,
              lg: this.lang,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.loadData(this.relTypCharGr);
            }
          });
    },

    editData() {
      this.$q
          .dialog({
            component: UpdaterRelTypCharGrMultiPropUpd,
            componentProps: {
              relTypCharGr: this.relTypCharGr,
              lg: this.lang,
              dense: true,
            },
          })
          .onOk((data) => {
            if (data.res) {
              this.loadData(this.relTypCharGr);
            }
          });
    },

    selectedCheck(item) {
      if (!item.id.includes("p_")) return;
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    loadData(relTypCharGr) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "reltyp/loadRelTypCharGrMultiProp",
            params: [{relTypCharGr: relTypCharGr}],
          })
          .then((response) => {
            this.rows = pack(response.data.result.records, "ord");
            this.fnExpand();
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

    getIcon(row) {
      if (row.multiProp === undefined) return "folder";
      return "view_in_ar";
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
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "storageType",
          label: this.$t("fldStorageType"),
          field: "storageType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
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
          !item.leaf &&
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
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

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

    this.cols = this.getColumns();
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
