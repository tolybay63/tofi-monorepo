<template>
  <div class="q-pa-sm row bg-amber-1" >
    <!--  Date  -->
    <q-input
      v-model="dte"
      :label="$t('date')"
      :model-value="dte"
      class="q-ml-lg"
      dense
      stack-label
      style="width: 100px"
      type="date"
      @update:model-value="fnDt"
    />

    <!--  PeriodType  -->
    <q-select
      class="q-ml-lg"
      v-model="periodType"
      :model-value="periodType"
      dense
      options-dense
      :options="optPeriod"
      :label="fnReqLabel('periodType')"
      option-value="id"
      option-label="text"
      map-options
      @update:model-value="fnSelectPeriodType"
      style="width: 100px"
    />

  </div>

  <div class="q-pt-sm">
    <div
      class="q-table-container q-table--dense wrap bg-orange-1"
      style="height: 100%"
    >
      <div class="q-table-middle scroll">
        <table
          class="q-table q-table--cell-separator q-table--bordered wrap"
        >
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr>
            <th style="font-size: 1.2em; width: 54%">
              {{ cols[0].label }}
            </th>
            <th style="font-size: 1.2em; width: 8%">
              {{ cols[1].label }}
            </th>
            <th style="font-size: 1.2em; width: 8%">
              {{ cols[2].label }}
            </th>
            <th style="font-size: 1.2em; width: 16%">
              {{ cols[3].label }}
            </th>
            <th></th>
          </tr>
          </thead>

          <tbody style="background:  aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td
              :data-th="cols[0].name"
              @click="toggle(item)"
            >
                <span
                  class="q-tree-link q-tree-label"
                  v-bind:style="setPadding(item)"
                >
                  <q-icon
                    :name="iconName(item)"
                    color="secondary"
                    style="cursor: pointer"
                  />

                  {{ item.name }}
                </span>
            </td>
            <!--value-->
            <td :data-th="cols[1].name">
              {{ item.numberval }}
            </td>
            <!--dbeg-->
            <td :data-th="cols[2].name">
              {{ item.numberval }}
            </td>
            <!--dend-->
            <td :data-th="cols[3].name">
              {{ item.numberval }}
            </td>

            <td :data-th="cols[4].name">
              <q-btn
                class="no-padding no-margin" color="blue" dense flat icon="edit" round
                size="sm" @click="fnEdit(item)"
              >
                <q-tooltip
                  transition-hide="rotate" transition-show="rotate"
                >
                  {{ $t("update") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                class="no-padding no-margin" color="red" dense flat icon="delete" round
                size="sm" @click="fnDelete(item)" :disable="!(item.idval>0)"
              >
                <q-tooltip
                  transition-hide="rotate" transition-show="rotate"
                >
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>

</template>

<script>

import {api} from 'boot/axios'
import {expandAll, notifyError, notifyInfo, pack, today} from 'src/utils/jsutils'
import {ref} from "vue";
import {date} from "quasar";

export default {
  props: ['name'],

  data() {
    return {
      rows: [],
      cols: [],
      loading: false,
      isExpanded: true,
      itemId: null,
      obj: 0,

      dte: today(),
      periodType: 41,
      optPeriod: []
    }
  },


  methods: {

    fnSelectPeriodType(v) {
      console.log(v, this.periodType)
      this.periodType = v.id
      this.loadReservoirsMeter(this.obj)
    },

    fnReqLabel(label) {
      return this.$t(label) + "*"
    },

    fnDt(val) {
      //let dt = date.formatDate(val).isWellFormed()
      //console.log(val.length)
      if (val.length === 10 && date.formatDate(val).isWellFormed()) {
        this.dte = val
        this.this.loadReservoirsMeter(this.obj)

      }
    },


    fnDelete(row) {
      let nm = row.name
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord") + "</br>(" + nm + ")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post('', {
              method: "data/deleteReservoirsMeter",
              params: [row.idval],
            })
            .then(
              () => {
                if (row.level === 0) {
                  this.rows[0].idval = null;
                  this.rows[0].numberval = null;
                } else {
                  let childs = this.rows[0].children;
                  let index = childs.findIndex((rec) => rec.id === row.id);
                  childs[index].idval = null;
                  childs[index].numberval = null;
                }
              },
              (error) => {
                notifyError(error.message)
              }
            )
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
        })
    },

    fnEdit(row) {
      let rec = {obj: this.obj, prop: row.id, numberval: row.numberval || "", name: row.name, idval: row.idval};
      this.$q
        .dialog({
          component: UpdaterReservoirsMeter,
          componentProps: {
            data: rec,
          },
        })
        .onOk((r) => {

          if (row.level === 0) {
            this.rows[0].idval = r.idval;
            this.rows[0].numberval = r.numberval;
          } else {
            let childs = this.rows[0].children;
            let index = childs.findIndex((rec) => rec.id === row.id);
            childs[index].idval = r.idval;
            childs[index].numberval = r.numberval;
          }

        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
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
    toggle(item) {
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

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          style: "font-size: 1.2em; width: 50%",
        },
        {
          name: "dbeg",
          label: this.$t("fldDbegShort"),
          field: "dbeg",
          align: "left",
          style: "font-size: 1.2em; width: 8%",
        },
        {
          name: "dend",
          label: this.$t("fldDendShort"),
          field: "dend",
          align: "left",
          style: "font-size: 1.2em; width: 8%",
        },
        {
          name: "numberval",
          label: this.$t("val"),
          field: "numberval",
          align: "center",
          style: "font-size: 1.2em; width: 20%",
        },
        {
          name: "cmd",
          field: "cmd",
          align: "center",
          style: "font-size: 1.2em; width: 14%",
        }
      ];
    },

    clearData() {
      this.rows = [];
    },

    loadReservoirsMeter(obj) {
      this.loading = true
      this.obj = obj
      api
        .post('', {
          method: 'data/loadReservoirsMeter',
          params: [obj, 0],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result["records"], "id")
            expandAll(this.rows)
          })
        .finally(() => {
          this.loading = false
        })
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

  created() {
    this.cols = this.getColumns();
    api
      .post('', {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriod = response.data.result["records"]
        })
      .finally(() => {
        this.loading = false
      })
    //
  },
}
</script>
