<template>
  <div class="no-padding no-margin">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      class="no-padding no-margin"
      horizontal
      style="height: calc(100vh - 300px); width: 100%"
      separator-class="bg-red"

    >
      <template v-slot:before>
        <div class="no-padding no-margin">

          <q-banner dense inline-actions class="bg-orange-1">

            <template v-slot:action>

              <q-btn
                dense class="q-ma-sm" icon="expand_more" color="secondary" @click="fnExpand()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("expandAll") }}
                </q-tooltip>
              </q-btn>
              <q-btn
                dense icon="expand_less" color="secondary" class="q-ma-sm" @click="fnCollapse()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("collapseAll") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds')"
                dense class="q-ma-sm"
                icon="edit_note"
                color="secondary"
                :disable="loading"
                @click="createGroup()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("addGroupRecords") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds')"
                dense
                class="q-ma-sm"
                icon="delete_sweep"
                color="secondary"
                :disable="loading"
                @click="deleteGroup()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deleteGroupRecords") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds')"
                dense
                icon="edit"
                color="secondary"
                class="q-ma-sm"
                :disable="loading || selected.length === 0"
                @click="editRow(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:')"
                dense
                icon="delete"
                color="secondary"
                class="q-ma-sm"
                :disable="loading || selected.length === 0"
                @click="removeRow(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-space></q-space>

              <q-inner-loading :showing="loading" color="secondary"/>

            </template>

          </q-banner>


          <div style="height: 100%; width: 100%" class="no-scroll">
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
                    <th :style="fnColStyle(2)">{{ fnColLabel(3) }}</th>
                    <th :style="fnColStyle(2)">{{ fnColLabel(4) }}</th>
                    <th :style="fnColStyle(2)">{{ fnColLabel(5) }}</th>
                  </tr>
                </thead>

                <tbody style="background: aliceblue">


                  <tr v-for="(item, index) in arrayTreeObj" :key="index">

                    <td :data-th="cols0[0].name" @click="toggle(item)">
                      <span class="q-tree-link q-tree-label" v-bind:style="setPadding(item)">
                        <q-icon
                          :style="fnStyleCursor(item)"
                          :name="iconName(item)"
                          color="secondary"
                        />

                        <q-btn
                          dense flat
                          :color="item.level===0 ? 'black' : 'blue'"
                          :icon="item.level===0 ? 'square'
                            : selected.length > 0 && item.id === selected[0].id
                            ? 'check_box'
                            : 'check_box_outline_blank'
                          "
                          @click.stop="selectedCheck(item)"
                        />

                        {{ item.number}}
                      </span>
                    </td>


                    <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">

                      {{ item[col.name] }}
                      <q-btn v-if="item.level>0 && col.name !== 'name' && col.name !== 'cmt'"
                        color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
                      >
                        <q-menu auto-close>
                          <q-btn
                            round size="sm" icon="edit" color="blue" flat dense
                            @click="fnEditCell(item, col)" class="no-padding no-margin"
                          >
                            <q-tooltip
                              transition-show="rotate" transition-hide="rotate"
                            >
                              {{ $t("update") }}
                            </q-tooltip>
                          </q-btn>

                          <q-btn
                            round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                            @click="fnDeleteCell(item, col)"
                            :disable="item[col.name]===undefined"
                          >
                            <q-tooltip
                              transition-show="rotate" transition-hide="rotate"
                            >
                              {{ $t("deletingRecord") }}
                            </q-tooltip>
                          </q-btn>
                        </q-menu>
                      </q-btn>
                    </td>

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

      <template v-slot:after>
        <rel-obj-member :relobj="relobj" ref="childMember"/>
      </template>

    </q-splitter>
  </div>

</template>

<script>
import {ref} from "vue";
import RelObjMember from "pages/types_objects/RelObjMember.vue";
import {collapsAll, expandAll, hasTarget, notifyError, notifySuccess, pack} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import UpdaterGroupRelObj from "pages/types_objects/UpdaterGroupRelObj.vue";
import UpdaterRelObj from "pages/types_objects/UpdaterRelObj.vue";
import DeleteGroupRelObj from "pages/types_objects/DeleteGroupRelObj.vue";
import UpdaterParamsCell from "pages/param_objects/UpdaterParamsCell.vue";

export default {
  props: ["codRelTyp"],
  name: "ParamComponentPage",
  components: {RelObjMember},

  data() {
    return {
      splitterModel: 75,
      cols: [],
      rows: [],
      loading: false,
      selected: [],
      isExpanded: true,
      itemId: null,
      reltyp: 0,
      relobj: 0,
    }
  },

  methods: {
    hasTarget,

    fnStyleCursor(item) {
      if (item.level === 0) {
        return "cursor: default"
      } else {
        return "cursor: pointer"
      }
    },

/*
    fnEditCell(item, col) {
      console.info("owner", item.id)
      console.info("field", col.name)
      console.info("prop", "Prop_" + col.name)
      console.info("value", item[col.name])
    },

    fnDeleteCell() {
    },
*/

    fnEditCell(row, col) {
      //console.info("fnEditCell", row, col)
      //console.info("fnEditCell val", row[col.name])

      let mode = row[col.name] === undefined ? "ins" : "upd"

      let data = {
          name: row["name"],
          codProp: "Prop_" + col.name,
          own: row["id"],
          isObj: 0,
          val: row[col.name],
          idVal: row["id" + col.name]
      }

      this.$q
        .dialog({
          component: UpdaterParamsCell,
          componentProps: {
            mode: mode,
            data: data
          },
        })
        .onOk((val) => {
          row[col.name] = val
          //this.loadSampling()
        })


    },

    fnDeleteCell(row, col) {
      //console.info("fnDeleteCell row", row)
      //console.info("fnDeleteCell col", col)
      let msg = row["name"] + " (" + col.label + ")"

      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">' + msg + '</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          this.$axios
            .post(baseURL, {
              method: 'data/deletePropOfParamComponent',
              params: [row["id" + col.name]],
            })
            .then(() => {
              row[col.name] = null
              //this.loadSampling()
            })
            .catch((error) => {
              console.log(error.message)
              notifyError(error.message)
            })
        })
    },



    fnColStyle(ind) {
      return this.cols[ind] ? this.cols[ind].headerStyle : ""
    },

    fnColLabel(ind) {
      return this.cols[ind] ? this.cols[ind].label : "";
    },

    selectedCheck(item) {
      if (item.level === 0) {
        this.toggle(item)
      } else {
        let vm = this;
        if (vm.selected.length > 0 && item.id === vm.selected[0].id) {
          vm.selected = []
          this.relobj = 0
          this.$refs.childMember.fetchData(0)
        } else {
          vm.selected = []
          vm.selected.push(item)
          this.relobj = vm.selected[0].id
          this.$refs.childMember.fetchData(this.relobj)
        }
      }
    },

    createGroup() {
      const lg = {name: this.lang}
      this.$q
        .dialog({
          component: UpdaterGroupRelObj,
          componentProps: {
            data: {relTyp: this.reltyp},
            lg: lg,
            //dense: this.dense,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (r.res) {
            //reload...
            this.fetchData();
          }
        });
    },

    deleteGroup() {
      const lg = {name: this.lang}
      this.$q
        .dialog({
          component: DeleteGroupRelObj,
          componentProps: {
            relTyp: this.reltyp,
            lg: lg,
            //dense: this.dense,
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          this.fetchData();
        });
    },

    editRow(rec) {
      let data = {}
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadRelObjRec',
          params: [rec.id],
        })
        .then(
          (response) => {
            data = response.data.result["records"][0]
            //console.log("rec", rec)
            //console.log("RelObjRec", data)
            data.name = rec.name
            data.cmt = rec.cmt
          })
        .then(() => {
          this.$q
            .dialog({
              component: UpdaterRelObj,
              componentProps: {
                data: data,
                mode: "upd",
                // ...
              },
            })
            .onOk((r) => {
              //console.log("Ok! updated", r);
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            })
        })
        .finally(() => {
          this.loading = false
        })

    },

    removeRow(rec) {
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
              method: "data/deleteOwner",
              params: [rec.id, 0],
            })
            .then(
              () => {
                this.fetchData()
                this.selected = ref([]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                let msg = error.message;
                if (error.response) msg = error.response.data.error.message;

                notifyError(msg);
              }
            );
        })
    },

    fetchData() {
      this.loading = ref(true);
      this.$axios
        .post(baseURL, {
          method: "data/loadParamsComponent",
          params: [this.reltyp],
        })
        .then((response) => {
          //console.info("rows", response.data.result["records"])
          this.rows = pack(response.data.result["records"], "ord")
          //console.info("rows2", this.rows)
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = this.$t(error.response.data.error.message);

          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })
    },

    getColumns() {
      return [
        {
          name: "number",
          label: this.$t("num"),
          field: "number",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 33%",
        },
        {
          name: "ParamsLimitMax",
          label: this.$t("Prop_ParamsLimitMax"),
          field: "ParamsLimitMax",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },

        {
          name: "ParamsLimitMin",
          label: this.$t("Prop_ParamsLimitMin"),
          field: "ParamsLimitMin",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "ParamsLimitNorm",
          label: this.$t("Prop_ParamsLimitNorm"),
          field: "ParamsLimitNorm",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
      ];
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
      return " " + row.name;
    }

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

  mounted() {
    console.info("mounted")
    this.cols = this.getColumns()
  },

  created() {
    this.loading = true
    this.$axios
      .post(baseURL, {
        method: "data/getIdRelTyp",
        params: [this.codRelTyp],
      })
      .then((response) => {
        this.reltyp = response.data.result;
      })
      .then(() => {
        this.fetchData()
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

  }


}
</script>

<style scoped>

</style>
