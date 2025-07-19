<template>
  <div class="q-pa-sm-sm" style="height: calc(100vh - 270px)">
    <q-banner dense inline-actions class="bg-amber-1">
      <div style="font-size: 1.2em; font-weight: bold">
        {{ $t("role_privileges") }}
      </div>

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
          v-if="hasTarget('adm:role:sel:priv')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="fnEdit()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("update") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div
      class="q-table-container q-table--dense wrap bg-amber-1"
      style="height: 100%; width: 100%"
    >
      <div class="q-pa-sm-sm">
        <div class="q-table-middle scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
              <tr class style="text-align: left">
                <th :style="columns[0].headerStyle">{{ columns[0].label }}</th>
                <th :style="columns[1].headerStyle">{{ columns[1].label }}</th>
              </tr>
            </thead>

            <tbody style="background: aliceblue; height: 100%">
              <tr v-for="(item, index) in arrayTreeObj" :key="index">
                <td
                  :data-th="columns[0].name"
                  style="width: 20%"
                  @click="toggle(item, index)"
                >
                  <span
                    class="q-tree-link q-tree-label"
                    v-bind:style="setPadding(item)"
                  >
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    ></q-icon>

                    {{ item.text }}
                  </span>
                </td>
                <td :data-th="columns[1].name">
                  {{ fnAL(item.accessLevel) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, hasTarget, pack,} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import UpdaterRolePermis from "pages/roles/UpdaterRolePermis.vue";


export default {
  name: "RolePermis",

  data: function () {
    return {
      role_id: 0,
      FD_AccessLevel: null,
      isExpanded: true,
      itemId: null,
      columns: [],
      table: [],
      separator: "cell",
      loading: ref(false),
    };
  },

  methods: {
    hasTarget,
    fnAL(val) {
      return this.FD_AccessLevel ? this.FD_AccessLevel[val] : null;
    },

    fetchData(role) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "role/loadRolePermis",
          params: [role],
        })
        .then((response) => {
          this.table = pack(response.data.result.records, "ord");
        })
        .finally(() => {
          this.fnExpand();
          this.loading = ref(false);
        });
    },

    fnEdit() {
      this.$q
        .dialog({
          component: UpdaterRolePermis,
          componentProps: {
            role: this.role_id,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk(() => {
          this.fetchData(this.role_id);
        });
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

    fnExpand() {
      expandAll(this.table);
    },

    fnCollapse() {
      collapsAll(this.table);
    },

    getColumns() {
      return [
        {
          name: "text",
          label: this.$t("fldName"),
          field: "text",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.2em; background: bg-blue-grey-13; text-align: left; width:70%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.2em; background: bg-blue-grey-13; text-align: left; width:30%",
          format: (val) => this.FD_AccessLevel.get(val),
        },
      ];
    },
  },

  mounted() {
    this.role_id = this.$route.params.role;
    this.fetchData(this.role_id);
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.columns = this.getColumns();

    this.loading = ref(true)
    api
      .post(baseURL, {
        method: "dict/loadDict",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.FD_AccessLevel = response.data.result
      })
      .finally(()=> {
        this.loading = ref(false)
      })
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

<style scoped></style>
