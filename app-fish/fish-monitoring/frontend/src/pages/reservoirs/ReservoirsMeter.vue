<template>
  <div class="q-pa-sm row bg-amber-1">
    <!--  Date  -->
    <q-input
      v-model="dte"
      :label="$t('date')"
      :model-value="dte"
      class="q-mr-lg"
      dense
      stack-label
      style="width: 100px"
      type="date"
      @update:model-value="fnDt"
    />

    <!--  PeriodType  -->
    <q-select
      v-model="periodType"
      :label="fnReqLabel('periodType')"
      :model-value="periodType"
      :options="optPeriod"
      class="q-ml-lg"
      dense
      map-options
      option-label="text"
      option-value="id"
      options-dense
      style="width: 100px"
      @update:model-value="fnSelectPeriodType"
    />
  </div>


  <div
    class="q-table-container q-table--dense wrap bg-orange-1"
    style="height: 100%"
  >
    <div class="q-pa-sm-sm bg-orange-1 sticky-header-table">
      <table
        class="q-table q-table--cell-separator q-table--bordered wrap"
      >
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr>
          <th style="font-size: 1.2em; width: 50%">
            {{ cols[0].label }}
          </th>
          <th style="font-size: 1.2em; width: 14%">
            {{ cols[1].label }}
          </th>
          <th style="font-size: 1.2em; width: 14%">
            {{ cols[2].label }}
          </th>
          <th style="font-size: 1.2em; width: 12%">
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
          <!--dbeg-->
          <td :data-th="cols[2].name">
            {{ dtFormat(item.dbeg) }}
          </td>
          <!--dend-->
          <td :data-th="cols[3].name">
            {{ dtFormat(item.dend) }}
          </td>
          <!--value-->
          <td :data-th="cols[1].name">
            {{ item.numberval }}
          </td>
          <!--cmd-->
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
              :disable="!(item.idval>0)" class="no-padding no-margin" color="red" dense flat icon="delete"
              round size="sm" @click="fnDelete(item)"
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


</template>

<script>

import {api, tofi_dbeg, tofi_dend} from 'boot/axios'
import {notifyError, notifyInfo, pack, today} from 'src/utils/jsutils'
import {ref} from "vue";
import {date} from "quasar";
import UpdaterReservoirMeter from "pages/reservoirs/UpdaterReservoirMeter.vue";

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
      periodType: 11,
      optPeriod: []
    }
  },


  methods: {
    dtFormat(v) {
      return (v <= tofi_dbeg || v >= tofi_dend) ? "..." : date.formatDate(v, "DD.MM.YYYY")
    },

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
      console.log("fbDt", val)
      if (val.length === 10 && date.formatDate(val).isWellFormed()) {
        this.dte = val
        this.loadReservoirsMeter(this.obj)

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
                  let index = this.rows.findIndex((rec) => rec.id === row.id);
                  if (index > -1) {
                    this.rows[index].idval = null;
                    this.rows[index].numberval = null;
                    this.rows[index].dbeg = null;
                    this.rows[index].dend = null;
                  }
                } else {
                  let index = this.rows.findIndex((rec) => rec.id === row.parent);
                  let child = this.rows[index].children;
                  let index2 = child.findIndex((rec) => rec.id === row.id);
                  child[index2].idval = null;
                  child[index2].numberval = null;
                  child[index2].dbeg = null;
                  child[index2].dend = null;
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

    updateRowValue(rows, targetRec) {
      for (let row of rows) {
        // Ищем элемент по id (можно изменить условие под ваши нужды)
        if (row.id === targetRec.id) {
          row.idval = targetRec.idval;
          row.numberval = targetRec.numberval;
          row.dbeg = targetRec.dbeg;
          row.dend = targetRec.dend;
          return true; // Успешно найдено и обновлено, прерываем поиск
        }
        // Если у узла есть дети, ищем рекурсивно в глубину
        if (row.children && Array.isArray(row.children) && row.children.length > 0) {
          if (this.updateRowValue(row.children, targetRec)) {
            return true;
          }
        }
      }
      return false; // Элемент не найден
    },

    fnEdit(row) {
      let rec = {
        obj: this.obj, prop: row.id,
        name: row.name, idval: row.idval,
        numberval: row.numberval || "",
        dependperiod: row.dependperiod,
        dt: this.dte, pt: this.periodType,
        level: row.level,
      };

      this.$q
        .dialog({
          component: UpdaterReservoirMeter,
          componentProps: {
            data: rec,
          },
        })
        .onOk((r) => {
          this.updateRowValue(this.rows, r)
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
          style: "font-size: 1.2em; width: 54%",
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
          style: "font-size: 1.2em; width: 16%",
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
          params: [obj, 0, this.dte, this.periodType],
        })
        .then(
          (response) => {
            //console.info("rows", response.data.result["records"]);
            this.rows = pack(response.data.result["records"], "id")
            //expandAll(this.rows)
            console.log(this.rows)
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

<style scoped>
.sticky-header-table {
  /* Ограничиваем высоту контейнера, чтобы появилась прокрутка */
  max-height: 95%;
  overflow: auto;
}

.sticky-header-table table {
  /* Убираем схлопывание границ, чтобы sticky работал корректно в некоторых браузерах */
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  /* Делаем заголовок липким */
  position: sticky;
  top: 0;
  /* Z-index нужен, чтобы содержимое body не перекрывало заголовок */
  z-index: 1;
  /* Фон обязателен, иначе заголовок будет прозрачным */
  background-color: #607d8b; /* Аналог bg-blue-grey-13 */
}

/* Опционально: если у таблицы есть границы, фиксируем их отображение */
.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>

