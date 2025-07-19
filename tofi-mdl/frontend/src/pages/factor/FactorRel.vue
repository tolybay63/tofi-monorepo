<template>
  <div style="height: calc(100vh - 400px); width: 100%">
    <q-splitter
        v-model="splitterModel"
        :model-value="splitterModel"
        :limits="[40, 60]"
        horizontal
    >
      <template v-slot:before>
        <q-table
            color="primary"
            card-class="bg-amber-1 text-brown"
            row-key="cod"
            :dense="value"
            :columns="cols"
            :rows="factorrels"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :filter="filter"
            :loading="loading"
            selection="single"
            @selection="fnSelection"
            v-model:selected="selected"
            :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
                colspan="100%"
                v-else-if="this.factorrels.length > 0" class="text-bold"
            >
              {{ $t("infoFactorValRel") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              {{ $t("relFactors") }}
            </div>

            <q-space/>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:fac:sel:rel:ins')"
                icon="post_add"
                color="secondary"
                :dense="value"
                :disable="loading"
                @click="editVals('ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                :dense="value"
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="editVals('upd')"
                :disable="loading || selected.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-toggle
                style="margin-left: 10px"
                v-model="value"
                :model-value="value"
                :label="$t('isDense')"
            />

            <q-space/>
            <q-input
                :dense="value"
                debounce="300"
                color="primary"
                :model-value="filter"
                v-model="filter"
                :label="$t('txt_filter')"
            >
              <template v-slot:append>
                <q-icon name="search"/>
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <div v-if="factor2() > 0">

          <div style="font-size: 1.2em; font-weight: bold;" class="bg-green-1">
            {{ $t("compatibility") }}
          </div>

          <div>
            <q-table
                style="height: 100%; width: 100%"
                color="primary"
                card-class="bg-amber-1 text-brown"
                table-class="text-grey-8"
                row-key="id"
                :dense="value"
                :columns="propFactors.cols"
                :rows="propFactors.rows"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="cell"
                :loading="loading2.value"
                :rows-per-page-options="[0]"
            >
              <template #body-cell="props">
                <q-td :props="props">
                  <div v-if="isFV(props.value)">
                    <q-btn
                        round
                        :class="
                        props.value !== '0'
                          ? 'bg-white text-red'
                          : 'bg-white text-green'
                      "
                        size="sx"
                        :icon="props.value !== '0' ? 'close' : 'done'"
                        flat
                        :dense="value"
                        label=""
                    >
                      <q-tooltip
                          class="bg-amber-2 text-black shadow-4"
                          :offset="[10, 10]"
                          max-width="300px"
                          v-if="props.value !== '0'"
                      >
                        {{ fnCmt(props) }}
                      </q-tooltip>
                    </q-btn>
                  </div>
                  <div v-else>
                    {{ props.value }}
                  </div>
                </q-td>
              </template>

              <template #loading>
                <q-inner-loading showing color="secondary"></q-inner-loading>
              </template>

              <template #bottom-row>
                <div style="margin-left: 10px">
                  ☑ : <strong>{{ propFactors.cnt_s }}</strong> ☒ :
                  <strong>{{ propFactors.cnt_ns }}</strong>
                </div>
              </template>
            </q-table>
          </div>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError} from "src/utils/jsutils";
import UpdateFactorValRel from "pages/factor/UpdateFactorValRel.vue";

const requestParam = {
  factor: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({
  methods: {
    hasTarget,
    fnCmt(props) {
      let key = props.row.id + "_" + props.row[props.col.name];
      if (this.propFactors.cmt[key]) {
        return this.propFactors.cmt[key];
      } else {
        return this.$t("notComment");
      }
    },

    factor2() {
      if (!this.selected[0]) return 0;
      return this.selected[0].id;
    },

    isFV(val) {
      return /^[0-9]+$/.test(val);
    },

    fnSelection(par) {
      const factor1 = this.factor1_id;
      const factor2 = par.rows[0].id;
      //
      this.loading2 = ref(true);
      api
          .post(baseURL, {
            method: "factorrel/factorValRel",
            params: [{factor1: factor1, factor2: factor2}],
          })
          .then((response) => {
            //console.log("fvs", response.data.result);
            this.propFactors.rows = response.data.result.rows.records;
            let s = 0,
                ns = 0;
            this.propFactors.rows.forEach(function (el) {
              for (let key in el) {
                if (key.startsWith("fv")) {
                  if (el[key] === '0') s = s + 1;
                  else ns = ns + 1;
                }
              }
            });
            this.propFactors.cnt_s = s;
            this.propFactors.cnt_ns = ns;
            //
            this.propFactors.cols = response.data.result.cols;
            this.propFactors.cols[0]["label"] =
                this.$t("factor1") + "/" + this.$t("factor2");
            this.propFactors.cmt = response.data.result.cmt;
          })
          .catch((error) => {
            console.log(error);
            notifyError(error.message);
          })
          .finally(() => {
            this.loading2 = ref(false);
          });
    },

    editVals(mode) {
      let factor1 = this.factor1_id;
      let factor2 = 0;
      if (mode === "upd") factor2 = this.selected[0].id;

      let params = {
        factor1: factor1,
        factor2: factor2,
        mode: mode,
      };
      const lg = {name: this.lang};
      //console.log("data",data)
      this.$q
          .dialog({
            component: UpdateFactorValRel,
            componentProps: {
              params: params,
              lg: lg,
              action: "factorrel",
              data: this.propFactors,
              // ...
            },
          })
          .onOk((rec) => {
            //rec - Factor2
            //console.log("Ok! updated", rec );
            if (mode === "ins") {
              if (rec !== undefined) {
                this.factorrels.push(rec);
                this.selected = ref([]);
                this.selected.push(rec);
                let par = {rows: [{id: this.selected[0].id}]};
                this.fnSelection(par);
              }
            } else {
              let par = {rows: [{id: this.selected[0].id}]};

              if (rec && Object.keys(rec).length > 0) {
                //upd rels
                let tmp = this.selected[0];
                this.selected = [];
                //setTimeout(() => {
                this.selected.push(tmp);
                this.fnSelection(par);
                //}, 200)
              } else {
                // del rels
                let index = this.factorrels.findIndex(
                    (row) => row.id === this.selected[0].id
                );
                this.factorrels.splice(index, 1);
                this.selected = [];
              }
            }
          });
    },

    fetchData(requestProps) {
      this.loading = ref(true);
      //console.log("<<<<<    fetchData")
      //console.log("requestProps", requestProps)
      const orderBy = requestProps.sortBy;
      const filter = requestProps.filter;
      const factor = requestProps.factor;

      api
          .post(baseURL, {
            method: "factorrel/load",
            params: [
              {
                factor: factor,
                orderBy: orderBy,
                filter: filter,
              },
            ],
          })
          .then((response) => {
            //console.log("fvs", response.data.result.records);
            this.factorrels = response.data.result.records;
            this.selected = ref([]);
          })
          .catch((error) => {
            console.log(error);
            notifyError(error.message);
          })
          .finally(() => {
            //setTimeout(() => {
            this.loading = ref(false);
            //}, 500)
          });
    },

    requestData(requestProps) {
      if (requestProps.sortBy === null) {
        requestParam.sortBy = null;
      } else {
        if (requestProps.descending)
          requestParam.sortBy = requestParam.sortBy + " desc";
        else
          requestParam.sortBy = requestParam.sortBy + " asc";
      }
      this.fetchData(requestParam);
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 30%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 30%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 40%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
  },

  data: function () {
    //console.log("<<<<<    data: lg", SetLocale.curLang)

    return {
      splitterModel: ref(40),
      cols: [],
      factorrels: [],
      filter: ref(""),
      loading: ref(false),
      separator: ref("cell"),
      selected: ref([]),
      factor1_id: null,
      factor: {},
      //
      loading2: ref(false),
      separator2: ref("cell"),
      propFactors: {
        rows: [],
        cols: [],
        cmt: ref({}),
        cnt_s: 0,
        cnt_n: 0,
      },
      value: true,
    };
  },

  mounted() {
    //console.log("mounted!");
    this.factor1_id = parseInt(this.$route["params"].factor, 10);
    //
    requestParam.factor = this.factor1_id;
    this.fetchData(requestParam);
  },

  created() {
    //console.log("<<<<<  created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  setup() {}

});
</script>

<style></style>
