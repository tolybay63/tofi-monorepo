<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="min-width: 800px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t('FishFecundity') }}</div>
      </q-bar>

      <q-card-section>

        <div class="q-py-md" style="font-size: 24px"> {{this.name}} </div>

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
                <tr >
                  <th style="font-size: 1.2em; width: 60%">
                    {{ cols[0].label }}
                  </th>
                  <th style="font-size: 1.2em; width: 30%">
                    {{ cols[1].label }}
                  </th>
                  <th></th>
                </tr>
                </thead>

                <tbody style="background: aliceblue">
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
                  <!--isReq-->
                  <td :data-th="cols[1].name">
                    {{ item.numberval }}
                  </td>

                  <td :data-th="cols[2].name">

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
      </q-card-section>
      <!---->
      <q-card-actions align="right">
        <q-btn :label="$t('close')" class="q-mt-xl" color="primary" icon="close" @click="onCancelClick"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>

import {api} from 'boot/axios'
import {expandAll, notifyError, notifyInfo, notifySuccess, pack} from 'src/utils/jsutils'
import {ref} from "vue";
import UpdaterFishFecundity from "pages/piscesreservoirs/UpdaterFishFecundity.vue";

export default {
  props: ['relobj', 'name'],

  data() {
    return {
      rows: [],
      cols: [],
      loading: false,

      isExpanded: true,
      itemId: null,

    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {

    fnDelete(row) {
      let nm = row.name
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord") +"</br>("+nm+")",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post('', {
              method: "data/deleteFishFecundity",
              params: [row.idval],
            })
            .then(
              () => {
                if (row.level===0) {
                  this.rows[0].numberval = null;
                } else {
                  let childs = this.rows[0].children;
                  let index = childs.findIndex((rec) => rec.id === row.id);
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
      let rec = {relobj: this.relobj, prop: row.id, numberval: row.numberval || "", name: row.name, idval: row.idval};
      this.$q
        .dialog({
          component: UpdaterFishFecundity,
          componentProps: {
            data: rec,
          },
        })
        .onOk((r) => {
          if (r.res) {
            if (row.level===0) {
              this.rows[0].numberval = r.res;
            } else {
              let childs = this.rows[0].children;
              let index = childs.findIndex((rec) => rec.id === row.id);
              childs[index].numberval = r.res;
            }
          }
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"))
        });

    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog["show"]()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog["hide"]()
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit('hide')
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      this.loading = true
      api
        .post('', {
          method: 'data/savePiscesReservoir',
          params: [this.form],
        })
        .then(
          () => {
            notifySuccess(this.$t('success'))
          },
          () => {
            /*
                        if (error.response.data.error.message.includes('@')) {
                          let msgs = error.response.data.error.message.split('@')
                          let m1 = this.$t(`${msgs[0]}`)
                          let m2 = msgs.length > 1 ? ': [' + msgs[1] + ']' : ''
                          let msg = m1 + m2
                          notifyError(msg)
                        } else {
                          notifyError(this.$t(error.response.data.error.message))
                        }
            */
          }
        )
        .finally(() => {
          this.hide()
          this.loading = false
        })
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
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
          name: "numberval",
          label: this.$t("val"),
          field: "numberval",
          align: "center",
          style: "font-size: 1.2em; width: 30%",
        },
        {
          name: "cmd",
          field: "cmd",
          align: "center",
          style: "font-size: 1.2em; width: 20%",
        }
      ];
    },

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
    this.loading = true
    api
      .post('', {
        method: 'data/loadFishFecundity',
        params: [this.relobj],
      })
      .then(
        (response) => {
          this.rows = pack(response.data.result["records"], "id")
          expandAll(this.rows)
          console.info("rows", this.rows);
        })
      .finally(() => {
        this.loading = false
      })


  },
}
</script>
