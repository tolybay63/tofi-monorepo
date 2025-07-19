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

        <q-select
          autofocus
          v-model="dbType"
          :options="optionsDB"
          :label="$t('db')"
          option-value="id"
          option-label="name"
          map-options
          dense
          options-dense
          :model-value="dbType"
          @update:model-value="fnSelectDB()"
        />



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

      <div class="q-table--dense wrap bg-orange-1 scroll" style="height: 90%">
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
                        dense
                        flat
                        :color="!item.parent ? 'gray' : 'blue'"
                        :icon="
                        item.checked ? 'check_box' : 'check_box_outline_blank'
                      "
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
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {checkChilds, collapsAll, expandAll, notifyError, notifySuccess, pack, uncheckChilds} from "src/utils/jsutils";

/////////////////////////////////////////////////////////////
let checkedMembers = []
const getCheckets = (node) => {
  if (node.parent && node.checked && !checkedMembers.includes(node.ent))
    checkedMembers.push({memType: node.memberType, card: node.card, ent: node.ent});
  const children = node.children;
  children.forEach(getCheckets)
};


/////////////////////////////////////////////////////////////

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
      sizeRTM: 0,
      //
      dbType: null,
      optionsDB: [],

      //
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectDB(val) {
    console.info("dbType, val", this.dbType, val)

    },

    updateCheck(item) {
      if (item.level===0) {
        if (item.children.length > 0) {
          if (!item.checked) checkChilds(item);
          else uncheckChilds(item);
        }
      } else {
        item.checked = !item.checked;
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
      if (this.sizeRTM < 2)
        return true
      let rez = []
      for (let i = 0; i < this.sizeRTM; i++) {
        checkedMembers = [];
        getCheckets(this.rows[i])
        rez.push(checkedMembers)
      }
      let b = false
      rez.forEach(it => {
        if (it.length === 0)
          b = true
      })
      return b && this.dbType.id !== null;
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
      for (let i = 0; i < this.sizeRTM; i++) {
        checkedMembers = [];
        getCheckets(this.rows[i])
        rez.push(checkedMembers)
      }
      //console.info("rez", rez)
      //console.info("rows", this.rows)

      api
          .post(baseURL, {
            method: "relcls/createGroupRelCls",
            params: [this.form.relTyp, rez, this.dbType.id],
          })
          .then(
              () => {
                err = false;
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
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
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em;width:50%;",
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
    api
        .post(baseURL, {
          method: "relcls/loadAllMembers",
          params: [this.form],
        })
        .then((response) => {
          this.rows = pack(response.data.result.records, "ord");
          this.sizeRTM = this.rows.length
          //console.log("rows", this.rows)
        })
        .finally(() => {
          this.loading = ref(false);
        });

    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "database/loadDbForSelect",
        params: [],
      })
      .then((response) => {
        this.optionsDB = response.data.result.records
        this.dbType = this.optionsDB[0]
        //console.log("rows", this.rows)
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
