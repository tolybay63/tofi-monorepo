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
    <q-card class="q-dialog-plugin no-scroll">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
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
              :dense="dense"
              color="secondary"
              icon="save"
              :label="$t('save')"
              @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>

          <q-btn
              :dense="dense"
              color="secondary"
              icon="cancel"
              :label="$t('cancel')"
              @click="onCancelClick"
          />
        </q-card-actions>
      </q-bar>

      <q-inner-loading :showing="showing" color="secondary"/>

      <div
          class="q-table-container q-table--dense wrap bg-orange-1 scroll"
          style="height: 90%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
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
              <!--name-->
              <td :data-th="cols[0].name" @click="toggle(item)">
                  <span class="q-tree__node" v-bind:style="setPadding(item)">
                    <q-icon
                        style="cursor: pointer"
                        :name="iconName(item)"
                        color="secondary"
                    ></q-icon>

                    <q-btn
                        dense
                        flat
                        :color="item.children.length > 0 ? 'gray' : 'blue'"
                        :icon="
                        item.checked === 1
                          ? 'check_box'
                          : 'check_box_outline_blank'
                      "
                        @click.stop="selectedCheck(item)"
                    >
                    </q-btn>

                    <q-icon :name="getIcon(item)"></q-icon>
                    {{ item[cols[0].field] }}
                  </span>
              </td>

              <!--name-->
              <td :data-th="cols[1].name">
                {{ item[cols[1].field] }}
              </td>
              <!--fullName-->
              <td :data-th="cols[1].fullName">
                {{ item[cols[1].field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <q-bar>{{ $t("countAll") }}: {{ this.sz }}</q-bar>
      </div>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {checkChilds, collapsAll, expandAll, notifyError, pack, uncheckChilds} from "src/utils/jsutils";

const expand = (item) => {
  item.expend = ref(true);
  const {children} = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

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
  props: ["prop", "entityType", "dense"],

  data() {
    return {
      lang: this.lg,
      cols: [],
      rows: [],
      separator: ref("cell"),
      loading: ref(false),
      showing: ref(false),
      //
      isExpanded: true,
      currentNode: null,
      itemId: null,
      sz: 0,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    selectedCheck(item) {
      if (item.children.length > 0) {
        if (item.checked === false) checkChilds(item);
        else uncheckChilds(item);
      } else {
        item.checked = !item.checked;
      }

      //console.info("item", item)
    },

    getIcon(row) {
      if (!row["isEntity"]) return "folder";
      else return "";
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
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
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 45%",
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {
      //this.loading = ref(true)
      let dta = [];

      const tt = (node, chks) => {
        if (node.checked && node["isEntity"]) {
          chks.push(node);
        }
        let children = node.children;
        if (children.length > 0) {
          children.forEach((ch) => tt(ch, chks));
        } else {

        }
      };

      const getCheckeds = (data, chks) => {
        for (let i = 0; i < data.length; i++) {
          tt(data[i], chks);
        }
      };

      getCheckeds(this.rows, dta);

      let d0 = [];
      dta.forEach((d) => {
        let {...o} = d;
        o.children = null;
        d0.push(o);
      });

      api
          .post(baseURL, {
            method: "prop/savePropEntityVal",
            params: [this.prop, d0], //this.entityType,
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
              },
              (error) => {
                let msg = error.message;
                if (error.response.data.error.message)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = ref(false);
            this.hide();
          });
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

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
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
      if (item.children && item.children.length > 0) {
        if (item.expend) collaps(item);
        else expand(item);
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },
  },

  created() {
    this.cols = this.getColumns();

    this.showing = ref(true);
    api
        .post(baseURL, {
          method: "prop/loadPropValEntityForUpd",
          params: [this.prop, this.entityType],
        })
        .then((response) => {
          this.sz = response.data.result.records.length;
          this.rows = pack(response.data.result.records, "ord");
          expandAll(this.rows);
        })
        .finally(() => {
          this.showing = ref(false);
        });

    return {};
  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },
};
</script>
