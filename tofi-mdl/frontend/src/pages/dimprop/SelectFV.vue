<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin full-height" style="width: 600px">
      <q-bar class="text-white bg-primary">
        {{ $t("select") }}
      </q-bar>

      <q-card>
        <q-banner dense inline-actions class="bg-orange-1 no-scroll">
          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand()"
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
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 200px); width: 100%">
          <div
            class="q-table-container q-table--dense wrap scroll"
            style="height: 100%"
          >
            <table
              class="q-table q-table--cell-separator q-table--bordered wrap"
            >
              <thead class="text-bold text-white bg-blue-grey-13">
                <tr class style="text-align: left">
                  <th style="font-size: 1.3em; width: 80%">
                    {{ cols[0].label }}
                  </th>
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
                      />
                      <q-btn
                        dense
                        flat
                        :color="item.parent === undefined ? 'gray' : 'blue'"
                        :icon="
                          item.checked ? 'check_box' : 'check_box_outline_blank'
                        "
                        @click.stop="selectedRow(item)"
                      >
                      </q-btn>

                      <q-icon
                        :name="getIcon(item)"
                        :color="getIconColor(item)"
                      ></q-icon>
                      {{ item.name }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validFV()"
        />
        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, pack} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  props: ["dimPropItem", "lg", "dense"],

  data() {
    return {
      cols: [],
      rows: [],
      separator: ref("cell"),
      isExpanded: true,
      itemId: null,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    getIconColor(row) {
      if (row.isOwn === 1) return "green";
      else return "gray";
    },

    getIcon(row) {
      if (row.parent) return "brightness_1";
      else return "square";
    },

    selectedRow(item) {
      let v = this;
      if (!item.parent) return;
      let parent = v.rows.filter((row) => {
        return row.id === item.parent;
      });
      if (item.checked === 1) {
        item.checked = 0;
        parent[0].checked = 0;
      } else {
        let { children } = parent[0];
        children.map((row) => {
          row.checked = 0;
        });
        parent[0].checked = 1;
        item.checked = 1;
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
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          style: "font-size: 1.3em; width: 80%",
        },
      ];
    },

    validFV() {
      let i = 0;
      let flt = [];
      while (i < this.rows.length) {
        let { children } = this.rows[i];
        flt = children.filter((ch) => ch.checked === 1);
        if (flt.length > 0) {
          return false;
        }
        i = i + 1;
      }
      return true;
    },

    /////////////////////////////////
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

    fetchData() {
      api
        .post(baseURL, {
          method: "dimprop/loadForFvSelect",
          params: [this.dimPropItem],
        })
        .then((response) => {
          this.rows = pack(response.data.result.records, "ord");
        });
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

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

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      let ids = [];
      let names = [];
      this.rows.forEach((row) => {
        const { children } = row;
        children.forEach((it) => {
          if (it.checked === 1) {
            ids.push(it.id.toString());
            names.push(it.name.toString());
          }
        });
      });
      this.$emit("ok", { fvs: ids.join(","), nms: names.join("; ") });
      this.hide();
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  created() {
    this.cols = this.getColumns();

    this.fetchData();
  },
};
</script>
