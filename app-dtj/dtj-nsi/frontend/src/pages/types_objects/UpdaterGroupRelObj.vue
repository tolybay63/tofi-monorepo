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
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("createGroupRecords") }}</div>
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


        <q-space/>
        <q-card-actions align="right">
          <q-btn
              :loading="loading"
              dense
              color="secondary"
              icon="save"
              :label="$t('save')"
              :disable="validSave()"
              @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>

          <q-btn
              dense
              color="secondary"
              icon="cancel"
              :label="$t('cancel')"
              @click="onCancelClick"
          />
        </q-card-actions>
      </q-bar>

      <div class="q-ma-sm q-table--dense wrap bg-orange-1 scroll" style="height: 90%">
        <q-card-section>
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
              <td :data-th="cols0[0].name" @click="toggle(item, index)">
                  <span class="q-tree__node" v-bind:style="setPadding(item)">
                    <q-icon
                        style="cursor: pointer"
                        :name="iconName(item)"
                        color="secondary"
                    ></q-icon>
                    <q-btn
                      v-if="item.checked!==null"

                        dense flat
                        :color="item['level']===2 ? 'blue' : 'gray'"
                        :icon="item.checked ? 'check_box' : 'check_box_outline_blank'"
                        @click.stop="updateCheck(item)"
                    >
                    </q-btn>

                    <q-icon v-if="item.isOwn !== -1"
                            :name= "getIconName(item)"
                            :color="getIconColor(item)"
                    ></q-icon>

                    {{ item[cols0[0].field] }}
                  </span>
              </td>

              <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
                {{ item[col.field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </q-card-section>
      </div>
      <div>
        <q-bar style="height: 16px"/>
      </div>

    </q-card>
  </q-dialog>
</template>

<script>
import {baseURL} from "boot/axios";
import {ref} from "vue";
import {
  checkChilds,
  collapsAll,
  expandAll,
  getParentNode,
  notifyError,
  notifySuccess,
  pack,
  uncheckChilds
} from "src/utils/jsutils";

/////////////////////////////////////////////////////////////
let checkeds = new Set()
const checkCheckets = (node) => {
  if (node.level===2 && node.checked)
    checkeds.add(node["membs"])
  const children = node.children
  children.forEach(checkCheckets)
  return checkeds.size === 2
}

let checkedMembers = []
let checkedMembers1 = []
let checkedMembers2 = []
const getCheckets = (node) => {
  for (let childKey of node.children[0].children) {
    if (childKey.checked) {
      checkedMembers1.push({cls: childKey.cls, relcls: childKey.relcls, rcm: childKey.rcm,
        ent: childKey.ent, name: childKey.name})
    }
  }
  for (let childKey of node.children[1].children) {
    if (childKey.checked) {
      checkedMembers2.push({cls: childKey.cls, relcls: childKey.relcls, rcm: childKey.rcm,
        ent: childKey.ent, name: childKey.name})
    }
  }
  checkedMembers.push(checkedMembers1)
  checkedMembers.push(checkedMembers2)
}

/////////////////////////////////////////////////////////////

const expand = (item) => {
  item.expend = ref(true);
  const {children} = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
}

const collaps = (item) => {
  item.expend = ref(false);
  const {children} = item;
  if (children.length > 0) {
    item.leaf = ref(false);
  } else {
    item.leaf = ref(true);
    item.expend = undefined;
  }
};

export default {
  props: ["data", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      loading: ref(false),
      cols: [],
      rows: [],
      isExpanded: false,
      currentNode: null,
      itemId: null,
      //
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {


    updateCheck(item) {
      if (item.level<2) {
        if (item.level===0)
          if (item.children.length
              && item.children[0].children.length> 0
                && item.children[1].children.length> 0) {
            if (!item.checked) checkChilds(item)
            else uncheckChilds(item)
          }
        if (item.level===1)
          if (item.children.length) {
            if (!item.checked) checkChilds(item)
            else uncheckChilds(item)
          }

      } else {
        item.checked = !item.checked
        let parentNode = []
        getParentNode(this.rows, item.parent, parentNode)
        if (parentNode.length > 0)
          if (item.checked)
            parentNode[0].checked = true
          else {
            let all_unch = true
            for (let child of parentNode[0].children) {
              if (child.checked)
                all_unch = false
            }
            if (all_unch)
              parentNode[0].checked = false
          }
      }
    },

    getIconName(row) {
      if (row.isOwn === 1)
        return "brightness_1";
      else if (row.isOwn === -2)
        return "brightness_7"
    },

    getIconColor(row) {
      if (row.isOwn === 1)
        return "green";
      else if (row.isOwn === 0)
        return "dark-gray";
      else
        return 'orange'
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
          if (o.expend === true) {
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
      if (item.expend === true) {
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

    validSave() {
      let rez = false
      for (let i = 0; i < this.rows.length; i++) {
        checkeds = new Set();
        rez = checkCheckets(this.rows[i])
        if (rez) break
      }
      return !rez
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

      this.loading = ref(true);
      let err = false
      let rez = []
      for (let i = 0; i < this.rows.length; i++) {
        checkeds = new Set();
        let ch = checkCheckets(this.rows[i])
        if (ch) {
          checkedMembers = [];
          checkedMembers1 = [];
          checkedMembers2 = [];
          getCheckets(this.rows[i])
          if (checkedMembers.length > 0)
            rez.push(checkedMembers)
        }
      }

      this.$axios
          .post(baseURL, {
            method: "data/createGroupRelObj",
            params: [this.form.relTyp, rez],
          })
          .then(
              (response) => {
                err = false;
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true;
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            this.loading = ref(false);
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width:35%;",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width:15%;",
        },
      ];
    },
  },

  created() {
    this.cols = this.getColumns();

    this.loading = ref(true);
    this.$axios
        .post(baseURL, {
          method: "data/loadAllMembers",
          params: [this.form],
        })
        .then((response) => {
          this.rows = pack(response.data.result["records"], "ord");
        })
        .finally(() => {
          this.loading = ref(false);
        });

  },

  computed: {
    cols0() {
      return this.cols.slice(0, 1);
    },

    cols_() {
      return this.cols.slice(1);
    },
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },
};
</script>
