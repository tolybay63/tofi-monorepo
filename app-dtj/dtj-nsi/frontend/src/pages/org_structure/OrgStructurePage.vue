<template>

  <div class="q-pa-sm">

    <q-banner dense inline-actions  class="bg-orange-1">

      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="dashboard"></q-avatar>
        {{ $t("org_structure") }}
      </div>
      <template v-slot:action>

        <q-btn
          dense class="q-ml-sm" icon="expand_more" color="secondary" @click="fnExpand()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          dense icon="expand_less" color="secondary" class="q-ml-sm" @click="fnCollapse()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          :disable="loading"
          @click="addNode(false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          :disable="loading || selected.length === 0"
          @click="addNode(true)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addSubNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editNode(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="deleteNode()"
          :disable="loading || selected.length === 0"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deleteNode") }}
          </q-tooltip>
        </q-btn>

        <q-space></q-space>

        <q-inner-loading :showing="loading" color="secondary"/>

      </template>

    </q-banner>


    <div style="height: calc(100vh - 330px); width: 100%" class="no-scroll">
      <div
        class="q-table-container q-table--dense scroll"
        style="height: 100%"
      >

        <table
          class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle"
        >
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th :style="fnColStyle(0)">{{ fnColLabel(0) }}</th>
            <th :style="fnColStyle(1)">{{ fnColLabel(1) }}</th>
            <th :style="fnColStyle(2)">{{ fnColLabel(2) }}</th>
            <th :style="fnColStyle(3)">{{ fnColLabel(3) }}</th>
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
                    dense flat
                    color="blue"
                    :icon="
                      selected.length > 0 && item.id === selected[0].id
                        ? 'check_box'
                        : 'check_box_outline_blank'
                    "
                    @click.stop="selectedCheck(item)"
                  >
                  </q-btn>

                  {{ item.name }}
                </span>
            </td>
            <td :data-th="cols[1].name">{{ item.StartStation }}</td>
            <td :data-th="cols[2].name">{{ item.FinishStation }}</td>
            <td :data-th="cols[3].name">{{ item.StageLength }}</td>
          </tr>
          </tbody>
        </table>

        <div>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td
            v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
            {{ $t("infoRow") }}
          </q-td>
        </div>


      </div>

    </div>

  </div>

</template>

<script>

import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack} from "src/utils/jsutils";
import UpdaterOrgStructure from "pages/org_structure/UpdaterOrgStructure.vue";

const expand = (item) => {
  item.expend = ref(true);
  const {children} = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

export default {
  name: "OrgStructurePage",
  data() {
    return {
      cols: [],
      rows: [],
      loading: false,
      isExpanded: true,
      itemId: null,
      selected: [],
    };
  },

  methods: {
    hasTarget,

    fnColStyle(ind) {
      return this.cols[ind] ? this.cols[ind].headerStyle : ""
    },

    fnColLabel(ind) {
      return this.cols[ind] ? this.cols[ind].label : "";
    },

    selectedCheck(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    addNode(isChild) {
      let data = {accessLevel: 1};
      let parentName = "Нет";
      if (isChild) {
        data.parent = this.selected[0].id;
        parentName = this.selected[0].name;
      } else {
        if (this.selected.length > 0) {
          let parentNode = [];
          data.parent = this.selected[0].parent;
          getParentNode(this.rows, this.selected[0].parent, parentNode);
          parentName = parentNode.length > 0 ? parentNode[0].name : "Нет";
        }
      }

      let selRow = this.selected[0];

      this.$q
        .dialog({
          component: UpdaterOrgStructure,
          componentProps: {
            data: data,
            mode: "ins",
            parentName: parentName,
            dense: true,
          },
        })
        .onOk((r) => {
          this.loadData();
          this.selected = []
          this.selected.push(r)
          if (isChild)
            expand(selRow)
        });
    },

    editNode(rec) {
      let parentNode = [];
      let parentName = "Нет";
      let parent = 0;
      if (rec.parent > 0) {
        getParentNode(this.rows, rec.parent, parentNode);
        parentName = parentNode[0].name;
        parent = parentNode[0].id;
      }

      this.$q
        .dialog({
          component: UpdaterOrgStructure,
          componentProps: {
            data: rec,
            mode: "upd",
            parentName: parentName,
            parent: parent,
            hasChild: this.hasChild,
            lg: this.lang,
          },
        })
        .onOk((res) => {
          this.selected = [];
          this.loadData();
          this.selected.push(res)
          //this.selectedCheck(res)
        });


    },

    deleteNode() {
      let rec = this.selected[0];
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + rec.name + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.$axios
            .post(baseURL, {
              method: "data/deleteOwnerWithProperties",
              params: [rec.id, 1],
            })
            .then(
              () => {
                this.selected = []
                this.loadData();
              },
              () => {
                notifyInfo(this.$t("hasChild"));
              }
            );
        });

    },

    loadData() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadOrgStructure",
          params: [0],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result["records"], "id")
            this.fnExpand()
          })
        .catch(error => {
          if (error.response.data.error.message.includes("@")) {
            let msgs = error.response.data.error.message.split("@")
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        })
        .finally(() => {
          this.loading = false
        })

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

    infoSelected(row) {
      return " " + row.name
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
        {
          name: "StartStation",
          label: this.$t("StartStation_"),
          field: "StartStation",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },
        {
          name: "FinishStation",
          label: this.$t("FinishStation_"),
          field: "FinishStation",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },
        {
          name: "StageLength",
          label: this.$t("StageLength"),
          field: "StageLength",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:20%",
        },
      ]
    }

  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  mounted() {
    console.info("mounted")
    this.cols = this.getColumns()
    this.loadData()
  },

  created() {
    console.info("created")
  }


}
</script>

<style scoped>

</style>
