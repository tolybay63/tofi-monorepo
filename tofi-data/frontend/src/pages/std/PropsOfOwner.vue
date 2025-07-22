<template>
  <div style="height: calc(100vh - 220px); width: 100%" class="no-scroll">
    <q-banner dense inline-actions class="bg-orange-1">
      <strong> {{ $t("itemsPropCharGr") }} </strong>
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
      </template>
    </q-banner>

    <div
      class="q-table-container q-table--dense wrap bg-orange-1 scroll"
      style="height: 95%"
    >
      <table
        class="q-table q-table--cell-separator q-table--bordered wrap scroll"
      >
        <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
            <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
          </tr>
        </thead>

        <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0].name" @click="toggle(item, index)">
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
                  :color="item.propType === undefined ? 'gray' : 'blue'"
                  :icon="
                    item.isItem
                      ? 'adjust'
                      : selected.length > 0 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  :disable="item.isItem"
                  @click.stop="selectedCheck(item)"
                >
                </q-btn>

                <q-icon :name="getIcon(item)"></q-icon>
                {{ item.cod }}
              </span>
            </td>
            <td :data-th="cols[1].name">{{ item.name }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll, pack} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import allConsts from "pages/all-consts";

export default {
  name: "PropsOfOwner",

  data() {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      //
      isExpanded: true,
      selectedRowID: {},
      itemId: null,
      selected: ref([]),
    };
  },

  methods: {
    selectedCheck(item) {
      if (item.propType === undefined) return
      let th = this;
      if (th.selected.length > 0 && item.id === th.selected[0].id)
        th.selected = []
      else {
        th.selected = []
        th.selected.push(item)
      }

      this.$emit("updateSelect", { selected: th.selected[0] })
    },

    loadDataProps(tr, isObj) {
      console.log("loadDataProps", tr, isObj)
      this.loading = ref(true)

      api
        .post(baseURL, {
          method: "data/loadTypCharGrProp",
          params: [ tr, isObj, false ],
        })
        .then((response) => {
          this.rows = pack(response.data.result.records, "ord")
          this.fnExpand()
        })
        .finally(() => {
          this.loading = ref(false)
        })
    },

    getIcon(row) {
      if (row.propType === undefined) return "folder"
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
        return "category"
      }
    },

    fnExpand() {
      expandAll(this.rows)
    },

    fnCollapse() {
      collapsAll(this.rows)
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 40%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 60%",
        },
      ];
    },

    recursive(obj, newObj, level, itemId, isExpend) {
      let th = this;
      obj.forEach(function (o) {
        if (o.children && o.children.length !== 0) {
          o.level = level
          o.leaf = false
          newObj.push(o)
          if (o.id === itemId) {
            o.expend = isExpend
          }
          if (o.expend) {
            th.recursive(o.children, newObj, o.level + 1, itemId, isExpend)
          }
        } else {
          o.level = level
          o.leaf = true
          newObj.push(o)
          return false
        }
      })
    },

    iconName(item) {
      if (item.expend) {
        return "remove_circle_outline"
      }

      if (item.children && item.children.length > 0) {
        return "control_point"
      }

      return ""
    },

    toggle(item, index) {
      let th = this
      th.itemId = item.id

      item.leaf = false
      //show  sub items after click on + (more)
      if (
        !item.leaf &&
        item.expend === undefined &&
        item.children !== undefined
      ) {
        if (item.children.length !== 0) {
          th.recursive(item.children, [], item.level + 1, item.id, true)
        }
      }
      if (item.expend && item.children !== undefined) {
        item.children.forEach(function (o) {
          o.expend = undefined
        })

        item["expend"] = ref(undefined)
        item["leaf"] = ref(false)
        th.itemId = null
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`
    },
  },

  mounted() {},

  created() {
    this.lang = localStorage.getItem("curLang")
    this.lang = this.lang === "en-US" ? "en" : this.lang
    this.cols = this.getColumns()
  },

  computed: {
    arrayTreeObj() {
      let th = this
      var newObj = []
      th.recursive(th.rows, newObj, 0, th.itemId, th.isExpanded)
      return newObj
    },
  },
};
</script>

<style scoped></style>
