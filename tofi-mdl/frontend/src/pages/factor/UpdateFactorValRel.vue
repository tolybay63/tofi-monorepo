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
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("addFactor") }}</div>
      </q-bar>
      <q-card-section class="q-pa-md-md">
        <p class="no-padding no-margin">
          <span style="color: #1976d2">{{ $t("factor1") }}:</span>
          {{ name_factor1 }}
        </p>
        <p class="no-padding no-margin" v-if="mode === 'upd'">
          <span style="color: #1976d2">{{ $t("factor2") }}:</span>
          {{ name_factor2 }}
        </p>
        <p class="no-padding no-margin" v-else>
          <q-select
              v-model="factor2"
              @update:model-value="inputValue"
              use-input
              input-debounce="0"
              :label="$t('factor2')"
              :options="options"
              :option-label="dictText()"
              :dense="dense"
              :options-dense="dense"
              @filter="filterFn"
              style="min-width: 350px; max-width: 450px"
              autofocus
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </p>
      </q-card-section>

      <q-card-section>
        <q-table
            style="height: calc(100vh - 300px); width: 100%"
            :dense="dense"
            color="primary"
            card-class="bg-amber-1 text-brown"
            table-class="text-grey-8"
            row-key="id"
            :columns="rels.cols"
            :rows="rels.rows"
            table-header-class="text-bold text-white bg-blue-grey-13"
            table-header-style="size: 3em"
            :separator="separator2"
            :loading="loading"
            :rows-per-page-options="[0]"
        >
          <template #body-cell="props">
            <q-td :props="props">
              <div v-if="isFV(props.value)">
                <q-btn
                    :dense="dense"
                    round
                    :class="
                    props.value !== '0'
                      ? 'bg-white text-red'
                      : 'bg-white text-green'
                  "
                    size="sx"
                    :icon="props.value !== '0' ? 'close' : 'done'"
                    flat
                    label=""
                    @click="fnCell(props)"
                >
                  <q-badge
                      floating
                      rounded
                      color="purple"
                      text-color="white"
                      @click.stop="fnCmt(props)"
                      v-if="props.value !== '0'"
                  >
                    info
                    <q-tooltip
                        class="bg-amber-2 text-black shadow-4"
                        :offset="[10, 10]"
                    >
                      {{ $t("fldCmt") }}
                    </q-tooltip>
                  </q-badge>
                </q-btn>
              </div>
              <div v-else>
                {{ props.value }}
              </div>
            </q-td>
          </template>

          <template v-slot:top>
            <q-btn
                round
                :dense="dense"
                icon="done"
                text-color="green"
                color="white"
                :disable="
                (mode === 'ins' && (loading || factor2.id === 0)) ||
                (mode === 'upd' && loading)
              "
                @click="setOnOff(0)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("allCompatible") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                round
                :dense="dense"
                icon="close"
                color="white"
                text-color="red"
                class="q-ml-sm"
                @click="setOnOff(1)"
                :disable="
                (mode === 'ins' && (loading || factor2.id === 0)) ||
                (mode === 'upd' && loading)
              "
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("allIncompatible") }}
              </q-tooltip>
            </q-btn>
            <q-space/>
            <q-toggle
                style="margin-left: 10px"
                v-model="dense"
                :label="$t('isDense')"
            />
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            :dense="dense"
            :loading="loading"
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
        <q-btn
            color="primary"
            icon="cancel"
            :dense="dense"
            :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  name: "UpdateFactorValRel",

  props: ["params", "lg", "action", "data"],

  data: function () {
    //console.log("************************* data")
    let mode = this.params.mode;
    let rows = JSON.parse(JSON.stringify(this.data.rows));
    let cols = JSON.parse(JSON.stringify(this.data.cols));
    let cmt = JSON.parse(JSON.stringify(this.data.cmt));
    if (mode === "ins") {
      cols = [];
      rows = [];
    }
    return {
      //
      loading: ref(false),
      separator2: ref("cell"),
      name_factor1: "",
      name_factor2: "",
      factor2: ref({id: 0, name: this.$t("notChosen")}),
      rec_factor2: ref({}),
      options: [],
      optionsOrg: [],
      mode: mode,
      lang: this.lg,
      act: this.action,
      rels: {
        rows: rows,
        cols: cols,
        cmt: cmt,
      },
      dense: true,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  mounted() {
    //console.log("************************* mounted")
    //console.log("params", this.params)
    //console.log("data", this.data)
    //console.log("mode", this.mode)

    this.loadFactor("factor1", this.params.factor1);
    if (this.mode === "upd") {
      this.loadFactor("factor2", this.params.factor2);
    } else {
      api
          .post(baseURL, {
            id: 1,
            method: "factorrel/factor2",
            params: [
              {
                factor: this.params.factor1,
              },
            ],
          })
          .then((response) => {
            this.optionsOrg = response.data.result.records;
            this.options = response.data.result.records;
            this.options.unshift({id: 0, name: this.$t("notChosen")});
            //console.log("this.options", this.options)
            //console.log("this.optionsOrg", this.optionsOrg)
          })
          .catch((error) => {
            console.log(error);
            notifyError(error.message);
          });
    }
  },

  methods: {
    setOnOff(val) {
      this.rels.rows.forEach((row) => {
        this.rels.cols.forEach((col) => {
          if (col.name !== "name")
            row[col.name] = val === 0 ? 0 : col.name.substring(2);
        });
      });
    },

    fnCmt(p) {
      let key = p.row.id + "_" + p.row[p.col.name];
      //console.log("key", key)
      //console.log("cmt", this.rels.cmt[key])

      this.$q
          .dialog({
            title: this.$t("fldCmt"),
            message: "",
            prompt: {
              model: this.rels.cmt[key],
              type: "textarea", // optional
            },
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk((data) => {
            this.rels.cmt[key] = data;
          });
    },

    inputValue(val) {
      //console.info("val", val)
      if (val.id > 0) {
        this.factor2.id = val.id;
        this.loadData(this.params.factor1, val.id);
        this.loadFactor("factor2", val.id);
      } else {
        this.rels.rows = [];
        this.rels.cols = [];
      }
    },

    dictText() {
      return "name";
    },

    filterFn(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.options = this.optionsOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.options = this.optionsOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    loadFactor(factor, id) {
      api
          .post(baseURL, {
            method: "factor/loadRec",
            params: [{id: id}],
          })
          .then(
              (response) => {
                //console.log("loadFactor", response.data.result.records)
                if (factor === "factor1")
                  this.name_factor1 = response.data.result.records[0].name;
                if (factor === "factor2") {
                  this.name_factor2 = response.data.result.records[0].name;
                }
              },
              (error) => {
                notifyError(error.response.data.error.message);
              }
          );
    },

    loadData(factor1, factor2) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "factorrel/factorValRel",
            params: [{factor1: factor1, factor2: factor2}],
          })
          .then((response) => {
            //console.log("cols", response.data.result.cols);
            this.rels.rows = response.data.result.rows.records;
            //
            this.rels.cols = response.data.result.cols;
            this.rels.cols[0]["label"] =
                this.$t("factor1") + "/" + this.$t("factor2");
          })
          .catch((error) => {
            console.log(error);
            notifyError(error.message);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    fnCell(p) {
      if (p.row[p.col.name] === '0') {
        p.row[p.col.name] = p.col.name.substring(2);
      } else {
        p.row[p.col.name] = '0';
      }
    },

    isFV(val) {
      return /^[0-9]+$/.test(val);
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

    /*
    // Не понятное поведение
    saveData(f1, f2, data) {
      console.log("f1", f1)
      console.log("f2", f2)
      console.log("data", data)
      api
        .post(baseURL, {
          method: "factorrel/saveFactorValRel",
          params: [
            {factor1: f1, factor2: f2},
            data
          ],
        })
        .then(
          (response) => {
            this.rec_factor2 = response.data.result.records[0]
            console.log("this.rec_factor2", this.rec_factor2)
            //notifySuccess("Успешно!")
         },
          (error) => {
            let msg = error.message
            if (error.response.data.error.message)
              msg = error.response.data.error.message
            notifyError(msg)
          }
        );

    },
*/

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      let data = {};
      let factor1 = this.params.factor1;
      let factor2 = this.params.factor2;
      if (this.params.mode === "ins") {
        factor2 = this.factor2.id;
      }
      this.rels.rows.forEach((row) => {
        this.rels.cols.forEach((col) => {
          if (col.name !== "name") {
            if (row[col.name] !== '0') {
              let key = row.id + "_" + row[col.name];
              data[key] = this.rels.cmt[key] || "";
            }
          }
        });
      });
      //saveData(factor1, factor2, data)
      try {
        this.loading = true;
        api
            .post(baseURL, {
              method: "factorrel/saveFactorValRel",
              params: [{factor1: factor1, factor2: factor2}, data],
            })
            .then(
                (response) => {
                  this.rec_factor2 = response.data.result.records[0];
                  this.$emit("ok", this.rec_factor2);
                  //notifySuccess("Успешно!")
                },
                (error) => {
                  let msg = error.message;
                  if (error.response.data.error.message)
                    msg = error.response.data.error.message;
                  notifyError(msg);
                }
            );
      } finally {
        this.loading = false;
        this.hide();
      }
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
    //...
  },

  created() {
    //console.log("************************* created")

    return {};
  },

  setup() {
    //console.log("************************* setup")

    return {};
  },
};
</script>
