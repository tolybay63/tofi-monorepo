<template>
  <div class="q-pa-md" style="height: calc(100vh - 190px)">
    <q-banner dense inline-actions class="bg-amber-1">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" text-color="white" icon="code"></q-avatar>
        {{ $t("tml_permis") }}
      </div>

      <template v-slot:action>
        <q-btn dense icon="save" color="secondary" @click="fnSave()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("save") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          dense
          icon="expand_more"
          color="secondary"
          class="q-ml-sm"
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

        <q-btn
          v-if="hasTarget('adm:tml:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("create1level") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("createChild") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('upd', null)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
      {{ this.nodeInfo() }}
    </div>

    <div
      class="q-table-container q-table--dense wrap bg-amber-1 scroll"
      style="height: 100%; width: 100%"
    >
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
          <tr>
            <th :style="columns[0].headerStyle">{{ columns[0].label }}</th>
            <th :style="columns[0].headerStyle">{{ columns[1].label }}</th>
          </tr>
        </thead>

        <tbody style="background: aliceblue; height: 100%">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="columns[0].name" @click="toggle(item, index)">
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
                  dense
                  flat
                  color="blue"
                  :icon="
                    selected.length === 1 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click.stop="selectedRow(item)"
                  :disable="!hasTarget('adm:tml:upd')"
                >
                </q-btn>

                {{ item.text }}
              </span>
            </td>
            <!--id-->
            <td :data-th="columns[1].id">{{ item.id }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import UpdaterPermis from "pages/permis/UpdaterPermis.vue";
import {api, baseURL} from "boot/axios";
import {exportFile} from "quasar";

export default {
  name: "PermisPage",

  data: function () {
    return {
      isExpanded: true,
      selected: ref([]),
      currentNode: null,
      itemId: null,
      columns: [],
      table: [],
      separator: "cell",
      loading: ref(false),
    };
  },

  methods: {
    hasTarget,
    fnSave() {
      const data = JSON.stringify(this.table);
      //console.info(data)
      const status = exportFile("important.txt", data);

      if (status === true) {
        // browser allowed it
        console.log("Ok: " + status);
      } else {
        // browser denied it
        console.log("Error: " + status);
      }
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

    toggle(item, index) {
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

    selectedRow(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
      this.currentNode = vm.selected[0] !== undefined ? vm.selected[0] : null;
    },

    fnExpand() {
      expandAll(this.table);
    },

    fnCollapse() {
      collapsAll(this.table);
    },

    fnIns(mode, isChild) {
      let data = {
        id: "",
        text: "",
      };

      //const lg = this.lang;

      let parent = null;
      let parentName = null;
      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.text;
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          parent: this.currentNode.parent,
          text: this.currentNode.text,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.table, this.currentNode.parent, parentNode);
          parentName = parentNode[0].text;
          isChild = true;
        }
      }

      this.$q
        .dialog({
          component: UpdaterPermis,
          componentProps: {
            mode: mode,
            isChild: isChild,
            parentName: parentName,
            data: data,
            // ...
          },
        })
        .onOk((data) => {
          this.fetchData();
          this.selected.push(data);
          this.selectedRow(data);
          this.fnExpand();
        });
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.text +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "permis/delete",
              params: [{ rec: rec }],
            })
            .then(
              () => {
                this.fetchData();
                this.currentNode = null;
              },
              () => {
                notifyInfo(this.$t("hasChild"));
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    fetchData() {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "permis/load",
          params: [{}],
        })
        .then(
          (response) => {
            this.table = pack(response.data.result.records, "ord");
          },
          (error) => {
            this.$router.push("/");
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          }
        )
        .finally(() => {
          this.fnCollapse();
          this.loading = ref(false);
        });
    },
    getColumns() {
      return [
        {
          name: "text",
          label: this.$t("fldName"),
          field: "text",
          align: "left",
          classes: "text-bold text-white bg-blue-grey-13",
          headerStyle:
            "font-size: 1.2em; text-bold text-white bg-blue-grey-13; width:70%",
        },
        {
          name: "id",
          label: this.$t("target"),
          field: "id",
          classes: "text-bold text-white bg-blue-grey-13",
          headerStyle:
            "font-size: 1.2em; text-bold text-white bg-blue-grey-13; width:30%",
        },
      ];
    },

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = this.currentNode.text;
      }
      return res;
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.columns = this.getColumns();
    this.fetchData();
  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.table, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  setup() {
    return {};
  },
};
</script>

<style scoped>

.img-vert {
  transform: scaleY(-1);
  filter: "FlipV";
  -ms-filter: "FlipV";
}

</style>
