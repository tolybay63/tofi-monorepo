<template>
  <q-banner dense inline-actions class="bg-orange-1" style="font-size: 1.3em">
    <template v-slot:action>
      <q-btn dense icon="expand_more" color="secondary" @click="fnExpand()">
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
                  <q-icon :name="getIcon(item)"></q-icon>
                  {{ item.cod }}
                </span>
            </td>
            <td :data-th="cols[1].name">{{ item.name }}</td>
            <td :data-th="cols[2].name">{{ item.fullName }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {collapsAll, expandAll, hasTarget, notifyError, pack} from "src/utils/jsutils";
import UpdaterPropEntityValTree from "pages/prop/values/UpdaterPropEntityValTree.vue";

export default {
  name: "PropEntityValTree",

  props: ["entityType"],

  data() {
    return {
      rows: [],
      cols: [],
      loading: ref(false),
      separator: ref("cell"),
      isExpanded: true,
      selectedRowID: {},
      currentNode: null,
      itemId: null,

      propId: null,

      dense: true,
    };
  },

  methods: {
    hasTarget,
    getIcon(row) {
      if (row.parent) return "";
      else return "folder";
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    editData() {
      this.$q
          .dialog({
            component: UpdaterPropEntityValTree,
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
            this.rows = pack(response.data.result.records, "ord");
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

  mounted() {
    //console.log("mounted")
    //console.log("params", this.$route.params)
    this.propId = parseInt(this.$route["params"].prop, 10);
    this.fetchData(this.propId);
  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
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
