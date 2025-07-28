<template>
  <div class="q-pa-md" style="height: 100%; width: 100%">
    <q-banner dense inline-actions class="bg-amber-1">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" text-color="white" icon="groups"></q-avatar>
        {{ $t("groupUser") }}
      </div>

      <template v-slot:action>

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
          v-if="hasTarget('adm:tml:upd')"
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
          v-if="hasTarget('adm:tml:upd')"
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
          icon="rule"
          color="secondary"
          class="q-ml-sm"
          @click="fnUpdUsers(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("updateGroup") }}
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
            {{ $t("editGroup") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:upd')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deleteGroup") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div class="q-pa-md" style="height: calc(100vh - 300px)">

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
                  :icon="getIcon(item)"
                  @click.stop="selectedRow(item)"
                  :disable="!hasTarget('adm:tml:upd')"
                />

                <q-icon
                  size="sm" :name="getIcon2(item)"
                  :color="item['group']===1 ? 'orange-5' : 'blue-5'"
                ></q-icon>


                {{ item.name }}
              </span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

    </div>

  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, getParentNode, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";
import UpdaterGroupUserPage from "pages/page/UpdaterGroupUserPage.vue";
import UsersToFromGroup from "pages/page/UsersToFromGroup.vue";


export default {
  name: "GroupUserPage",

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

    getIcon(item) {
      if (item.group !== 1)
        return ""
      else
        return this.currentNode && item.id === this.currentNode.id
          ? 'check_box'
          : 'check_box_outline_blank'
    },

    getIcon2(item) {
      if (item['group'] === 1)
        return "folder"
      else
        return "account_circle"
    },

    fnUpdUsers(rec) {
      this.$q
        .dialog({
          component: UsersToFromGroup,
          componentProps: {
            obj: this.currentNode.id,
            cls: this.currentNode.cls,
            parentName: this.currentNode.name,
          },
        })
        .onOk(() => {
          //console.info("data", data)
          let sel = this.selected[0]
          this.fetchData()
          this.selected = []
          this.selected.push(sel)
        })

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
      if (item.group === 1)
        return `padding-left: ${item.level * 30}px;`;
      else
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
        name: "",
      };

      const lg = this.lang;

      let parent = null;
      let parentName = null;
      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.name;
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          cls: this.currentNode.cls,
          parent: this.currentNode.parent,
          name: this.currentNode.name,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.table, this.currentNode.parent, parentNode);
          parentName = parentNode[0].name;
          isChild = true;
        }
      }

      this.$q
        .dialog({
          component: UpdaterGroupUserPage,
          componentProps: {
            mode: mode,
            isChild: isChild,
            parentName: parentName,
            data: data,
            // ...
          },
        })
        .onOk((data) => {
          //console.info("data", data)
          this.fetchData()
          this.selected = []
          this.selected.push(data)
          //this.selectedRow(data)
        })
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.name +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "entity/deleteGroup",
              params: [rec.id],
            })
            .then(
              (response) => {
                this.fetchData();
                this.currentNode = null;
              },
              (error) => {
                let msg = error.message
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyInfo(msg)
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
          method: "data/loadGroupUsers",
          params: [],
        })
        .then(
          (response) => {
            this.table = pack(response.data.result.records, "ord")
            console.info("table", this.table)
          },
          (error) => {
            //this.$router.push("/");
            let msg = error.message
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg)
          }
        )
        .finally(() => {
          this.fnExpand()
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
          classes: "text-bold text-white bg-blue-grey-13",
          headerStyle:
            "font-size: 1.2em; text-bold text-white bg-blue-grey-13; width:70%",
        },
      ];
    },

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = this.currentNode.name;
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
      var newObj = [];
      vm.recursive(vm.table, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  setup() {
    const store = useUserStore();
    const {isSysAdmin, getTarget} = storeToRefs(store);
    return {
      hasTarget(tg) {
        return true
        if (isSysAdmin.value) return true;
        if (getTarget.value.length === 0) return false;
        return getTarget.value.includes(tg);
      },
    };
  },
};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  filter: FlipV;
  -ms-filter: "FlipV";
}

</style>
