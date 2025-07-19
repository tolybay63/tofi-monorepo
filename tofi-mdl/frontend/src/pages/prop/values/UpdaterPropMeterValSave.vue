<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      full-width
  >
    <q-card class="q-dialog-plugin" no-scroll>
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 45px">
        <q-btn
            dense
            icon="expand_more"
            color="secondary"
            @click="fnExpand()"
            style="margin-bottom: 3px"
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
            style="margin-bottom: 3px; margin-left: 5px"
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
              :disable="validSave()"
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

      <div
          class="q-table-container q-table--dense wrap bg-orange-1 scroll"
          style="height: 100%"
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
                    {{ item[cols[0].field] }}
                  </span>
              </td>

              <td :data-th="cols[1].name">
                {{ item[cols[1].field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {collapsAll, expandAll, notifyError, pack} from "src/utils/jsutils";

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
  props: [
    "checkeds",
    "prop",
    "meter",
    "meterRate",
    "meterStruct",
    "lg",
    "dense",
  ],
  //components: {QTreeTable},

  data() {
    return {
      lang: this.lg,
      cols: [],
      rows: [],
      separator: ref("cell"),
      loading: ref(false),
      //
      isExpanded: true,
      currentNode: null,
      itemId: null,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    validSave() {
      return false; //this.rows.length===0
    },

    loadData() {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "prop/loadPropMeterForUpdSave",
            params: [
              {
                meter: this.meter,
                meterStruct: this.meterStruct,
                checkeds: this.checkeds,
              },
            ],
          })
          .then((response) => {
            this.rows = pack(response.data.result.records, "id");
            expandAll(this.rows);
          })
          .finally(() => {
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
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 60%",
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {
      this.loading = ref(true);

      let dta = [];

      const tt = (node, dta) => {
        dta.push(node);
        let children = node.children;
        if (children.length > 0) {
          children.forEach((ch) => tt(ch, dta));
        } else {

        }
      };

      const data2dta = (data, dta) => {
        for (let i = 0; i < data.length; i++) {
          tt(data[i], dta);
        }
      };
      data2dta(this.rows, dta);

      let d0 = [];
      dta.forEach((d) => {
        let {...o} = d;
        o.children = null;
        d0.push(o);
      });

      api
          .post(baseURL, {
            method: "prop/savePropMeter",
            params: [
              {prop: this.prop, meterStruct: this.meterStruct, data: d0},
            ],
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
    this.loadData();

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
