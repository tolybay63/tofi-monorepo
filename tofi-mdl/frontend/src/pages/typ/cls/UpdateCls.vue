<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="height: 600px; width: 1200px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card>
        <q-tabs v-model="tab" class="text-teal" no-caps>
          <q-tab name="tabCls" :label="$t('clsProp')"/>
          <q-tab name="tabClsFV" :label="$t('cfvProp')"/>
        </q-tabs>

        <q-separator/>

        <q-tab-panels
            v-model="tab"
            animated
            swipeable
            vertical
            transition-prev="jump-up"
            transition-next="jump-up"
        >
          <q-tab-panel name="tabCls">

            <!-- Cod -->
            <q-input
                style="margin-bottom: 10px"
                :dense="dense"
                v-model="form.cod"
                :model-value="form.cod"
                :label="$t('code')"
                :placeholder="$t('msgCodeGen')"
            />

            <!-- name -->
            <q-input
                autofocus
                :dense="dense"
                :model-value="form.name"
                v-model="form.name"
                @blur="onBlurName"
                :label="$t('fldName')"
                :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            >
            </q-input>

            <!-- fullName-->
            <q-input
                :dense="dense"
                :model-value="form.fullName"
                v-model="form.fullName"
                :label="$t('fldFullName')"
                :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            >
            </q-input>

            <!-- isOpenness -->
            <q-toggle
                style="margin-left: 5px; margin-top: 5px; margin-bottom: 10px "
                :dense="dense"
                v-model="form.isOpenness"
                :model-value="form.isOpenness"
                :label="$t('isOpenness')"
            />

            <!-- accessLevel -->
            <q-select
                :dense="dense"
                v-model="al"
                :options="optionsLevel"
                :label="$t('accessLevel')"
                option-value="id"
                option-label="text"
                map-options
                :model-value="al"
                @update:model-value="fnSelectAL()"
            />

            <!-- database -->
            <q-select
                :dense="dense"
                v-model="db"
                :model-value="db"
                :options="optionsDB"
                :label="$t('dbNameLabel')"
                option-value="id"
                option-label="name"
                map-options
                @update:model-value="fnSelectDB()"
            />

            <!-- cmt -->
            <q-input
                :dense="dense"
                :model-value="form.cmt"
                v-model="form.cmt"
                type="textarea"
                :label="$t('fldCmt')"
            />
          </q-tab-panel>

          <q-tab-panel name="tabClsFV">
            <q-banner dense inline-actions class="bg-orange-1 no-scroll">
              <template v-slot:action>
                <q-btn
                    dense
                    icon="expand_more"
                    color="secondary"
                    @click="fnExpand()"
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
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("collapseAll") }}
                  </q-tooltip>
                </q-btn>
              </template>
            </q-banner>

            <div style="height: 395px; width: 100%">
              <div
                  class="q-table-container q-table--dense wrap bg-orange-1"
                  style="height: 100%"
              >
                <div class="q-table-middle scroll">
                  <table
                      class="q-table q-table--cell-separator q-table--bordered wrap"
                  >
                    <thead class="text-bold text-white bg-blue-grey-13">
                    <tr class style="text-align: left">
                      <th style="font-size: 1.2em; width: 80%">
                        {{ cols[0].label }}
                      </th>
                      <th style="font-size: 1.2em; width: 10%">
                        {{ cols[1].label }}
                      </th>
                      <th style="font-size: 1.2em; width: 10%">
                        {{ cols[2].label }}
                      </th>
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
                                style="cursor: pointer"
                                :name="iconName(item)"
                                color="secondary"
                            />
                            <q-btn
                                dense
                                flat
                                :color="
                                item.parent === undefined ? 'gray' : 'blue'
                              "
                                :icon="
                                item.checked
                                  ? 'check_box'
                                  : 'check_box_outline_blank'
                              "
                                @click.stop="selectedRow(item)"
                            >
                            </q-btn>

                            <q-icon
                                :name="getIcon(item)"
                                :color="getIconColor(item)"
                            ></q-icon>
                            {{ item.name }}
                          </span>
                      </td>
                      <!--isReq-->
                      <td :data-th="cols[1].name">{{ fnIsReq(item) }}</td>
                      <!--isUniq-->
                      <td :data-th="cols[2].name">{{ fnIsUniq(item) }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </q-tab-panel>
        </q-tab-panels>
      </q-card>

      <q-card-actions align="right">
        <q-btn
            :loading="loading"
            :dense="dense"
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validFV()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>

        <q-btn
            :dense="dense"
            color="primary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, notifyError, notifySuccess, pack,} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  props: ["data", "mode", "typ", "lg", "dense"],

  data() {
    //console.log("data", this.data)
    return {
      cols: [],
      rows: [],
      form: this.data,
      loading: ref(false),
      separator: ref("cell"),
      optionsLevel: [],
      al: this.data.accessLevel,

      optionsDB: [],
      db: this.data.dataBase,

      isExpanded: true,
      selectedRowID: {},

      itemId: null,
      tab: ref("tabCls"),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    getIconColor(row) {
      if (row.isOwn === 1) return "green";
      else return "gray";
    },

    getIcon(row) {
      if (row.parent) return "brightness_1";
      else return "square";
    },

    selectedRow(item) {
      //console.log("rows", this.rows)
      //    console.log("selectedRow item", item)
      let v = this;
      if (!item.parent) return;
      if (item.checked === 1) {
        if (item.isReq) {
          let parent = v.rows.filter((row) => {
            return row.id === item.parent;
          });
          let {children} = parent[0];
          let flt = children.filter((row) => {
            return row.checked === 1;
          });
          if (flt.length === 1) {
            notifyError("Обязательный фактор");
          } else {
            item.checked = 0;
          }
        } else {
          item.checked = 0;
        }
      } else {
        if (item.isUniq) {
          let parent = v.rows.filter((row) => {
            return row.id === item.parent;
          });
          let {children} = parent[0];
          children.map((row) => {
            row.checked = 0;
          });
          item.checked = 0;
        } else {
          item.checked = 1;
        }

        item.checked = 1;
      }


    },

    fnIsReq(item) {
      if (!item.parent) return item.isReq ? this.$t("yes") : this.$t("no");
      else return null;
    },

    fnIsUniq(item) {
      if (!item.parent) return item.isUniq ? this.$t("yes") : this.$t("no");
      else return null;
    },

    fnExpand() {
      expandAll(this.rows);
    },
    fnCollapse() {
      collapsAll(this.rows);
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          style: "font-size: 1.2em; width: 80%",
        },
        {
          name: "isReq",
          label:
              this.$t("isReq").substring(0, 5) + "-" + this.$t("isReq").substring(5),
          field: "isReq",
          align: "left",
          style: "font-size: 1.2em; width: 10%",
        },
        {
          name: "isUniq",
          label:
              this.$t("isUniq").substring(0, 4) + "-" + this.$t("isUniq").substring(4),
          field: "isUniq",
          align: "left",
          style: "font-size: 1.2em; width: 10%",
        },
      ];
    },

    onBlurName() {
      if (this.form.name) {
        this.form.name = this.form.name.trim();
        if (
            !this.form.fullName ||
            (this.form.fullName && this.form.fullName.trim() === "")
        ) {
          this.form.fullName = this.form.name;
        }
      }
    },

    fnSelectAL() {
      this.form.accessLevel = this.al.id;
    },

    fnSelectDB() {
      this.form.dataBase = this.db.id;
    },

    validFV() {
      let b = true;
      let i = 0;
      let flt = [];
      while (i < this.rows.length) {
        let row = this.rows[i];
        let {children} = this.rows[i];
        if (row.isReq && row.isUniq) {
          flt = children.filter((ch) => ch.checked === 1);
          b = b && flt.length === 1
        } else if (row.isReq && !row.isUniq) {
          flt = children.filter((ch) => ch.checked === 1);
          b = b && flt.length >= 1
        } else if (!row.isReq && row.isUniq) {
          flt = children.filter((ch) => ch.checked === 1);
          b = b && flt.length <= 1
        } else if (!row.isReq && !row.isUniq) {
          flt = children.filter((ch) => ch.checked === 1);
          b = b && flt.length >= 0
        }
        i = i + 1;
      }

      return (
          !(this.form.name !== null &&
              this.form.name !== undefined &&
              this.form.name.trim().length !== 0 &&
              this.form.dataBase !== null && b)
      )
    },

    validName() {
      return (
          this.form.name !== null &&
          this.form.name !== undefined &&
          this.form.name.trim().length !== 0 &&
          this.form.dataBase !== null
      );
    },

    /////////////////////////////////
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

    fetchData(typ, cls) {
      api
          .post(baseURL, {
            method: "typ/loadClsFVforUpd",
            params: [typ, cls],
          })
          .then((response) => {
            //console.log("cfvList", response.data.result.records)
            this.rows = pack(response.data.result.records);
            this.fnExpand();
            //console.log("cfvTree", this.rows)
          });
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

      let ids = [];
      this.rows.forEach((row) => {
        const {children} = row;
        children.forEach((it) => {
          if (it.checked === 1) ids.push(it.id.toString());
        });
      });
      const method = this.mode === "ins" ? "insertCls" : "updateCls";
      let err = false;
      api
          .post(baseURL, {
            method: "typ/" + method,
            params: [{rec: this.form, ids: ids}],
          })
          .then(
              () => {
                err = false;
                //this.$emit("ok", response.data.result.records[0]);
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true;
                //console.log("error.response.data=>>>", error.response.data.error.message)
                let msg = error.message;
                if (error.response) {
                  msg = this.$t(error.response.data.error.message);
                }
                notifyError(msg);
              }
          )
          .finally(() => {
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
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

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optionsLevel = response.data.result.records;
        });

    api
        .post(baseURL, {
          method: "database/loadDbForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsDB = response.data.result.records;
        });


    let cls = this.data.id === null ? 0 : this.data.id;
    this.fetchData(this.typ, cls);

  },
};
</script>
