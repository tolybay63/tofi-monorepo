<template>
  <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
    <template v-slot:action>
      <q-btn
          v-if="hasTarget('mdl:mn_ds:prop:complex:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("createItemComplex") }}
        </q-tooltip>
      </q-btn>

      <q-btn
          v-if="hasTarget('mdl:mn_ds:prop:complex:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('upd')"
          :disable="
          currentNode == null ||
          (currentNode.parent == null)
        "
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
          v-if="hasTarget('mdl:mn_ds:prop:complex:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null || currentNode.parent == null"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>

<!--
      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:val')"
        dense icon="pan_tool_alt"
        color="secondary"
        class="q-ml-lg"
        :disable="fnDisable(currentNode)"
        @click="propSelect()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("chooseRecord") }}
        </q-tooltip>
      </q-btn>
-->

      <q-space></q-space>

      <!--
            <q-btn
                :dense="dense" icon="check_box" color="secondary" class="q-ml-lg"
                :disable="loading || currentNode==null ||(currentNode !== null && !currentNode.statusFactor)"
                @click="setStatus(currentNode)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("statusFactor") }}
              </q-tooltip>
            </q-btn>
            <q-btn
                :dense="dense" icon="download" color="secondary" class="q-ml-sm"
                :disable="loading || currentNode==null || (currentNode !== null && !currentNode.providerTyp)"
                @click="setProvider(currentNode)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("providerTyp") }}
              </q-tooltip>
            </q-btn>
            <q-btn
                :dense="dense" icon="pan_tool_alt" color="secondary" class="q-ml-lg"
                :disable="loading || currentNode==null" @click="propSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
      -->
    </template>
  </q-banner>

  <QTreeTable
      :cols="cols"
      :rows="rows"
      :icon_leaf="''"
      @updateSelect="onUpdateSelect"
      :meterStruct="meterStruct"
      :FD_PropType="FD_PropType"
      ref="childComp"
      checked_visible="true"
  />
</template>

<script>
import QTreeTable from "components/QTreeTable.vue";
import {ref} from "vue";
import allConsts from "pages/all-consts";
import {api, baseURL} from "boot/axios";
import UpdateProp from "pages/prop/UpdateProp.vue";
import {collapsAll, expandAll, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";

export default {
  name: "ComplexProp",
  components: {QTreeTable},

  props: ["parentNode"],

  data() {
    return {
      cols: [],
      rows: [],
      currentNode: null,
      loading: ref(false),
      dense: true,

      FD_PropType: null,
      meterStruct: 0,
    };
  },

  methods: {
    hasTarget,
    onUpdateSelect(item) {
      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        //console.info("this.currentNode", this.currentNode)
        api
            .post(baseURL, {
              method: "prop/loadRec",
              params: [this.currentNode.id],
            })
            .then((response) => {
              this.currentNode = response.data.result.records[0];
              if (this.currentNode.statusFactor)
                this.checkStatus(this.currentNode);
              if (this.currentNode.providerTyp)
                this.checkProvider(this.currentNode);
            })
            .finally(() => {
              if (
                  this.currentNode.propType === allConsts.FD_PropType.meter ||
                  this.currentNode.propType === allConsts.FD_PropType.rate
              ) {
                api
                    .post(baseURL, {
                      method: "meter/loadRec",
                      params: [{id: this.currentNode.meter}],
                    })
                    .then((response) => {
                      this.meterStruct =
                          response.data.result.records[0].meterStruct;
                    });
              }
            });
      }
    },

    fetchData(prop) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "prop/loadPropComplex",
            params: [prop],
          })
          .then(
              (response) => {
                this.rows = pack(response.data.result.records, "ord");
                this.fnExpand();
              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = ref(false);
          });
    },

    edit(data, mode) {
      if (
          mode === "ins" &&
          (data.propType === allConsts.FD_PropType.typ ||
              data.propType === allConsts.FD_PropType.reltyp ||
              data.propType === allConsts.FD_PropType.factor ||
              data.propType === allConsts.FD_PropType.attr ||
              data.propType === allConsts.FD_PropType.complex)
      ) {
        data.isUniq = true;
        data.allItem = false;
        data.visualFormat = allConsts.FD_VisualFormat.short;
      }
      const lg = this.lang;
      this.$q
          .dialog({
            component: UpdateProp,
            componentProps: {
              rec: data,
              mode: mode,
              parentName: this.parentNode.name,
              isComplexProp: true,
              lg: lg,
            },
          })
          .onOk((data) => {
            if (data.res) {
              console.info("edit, fetch, mod", mode, this.parentNode.id)
              this.fetchData(this.parentNode.id);
            }
          });
    },

    fnIns(mode) {
      if (mode === "ins") {
        api
            .post(baseURL, {
              method: "prop/newRecComplex",
              params: [this.parentNode],
            })
            .then((response) => {
              this.edit(response.data.result.records[0], mode);
            });
      } else {
        console.log("this.currentNode", this.currentNode);
        this.edit(this.currentNode, mode);
      }
    },

    fnDel(rec) {
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.cod +
                ": " +
                rec.name +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            //let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "prop/delete",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      this.$refs.childComp.clrAny();
                      this.fetchData(this.parentNode.id);
                    })
              .catch(error=> {
                console.log(error.message)
                if (error.response.data.error.message.includes("@")) {
                  let msgs = error.response.data.error.message.split("@")
                  let m1 = this.$t(`${msgs[0]}`)
                  let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
                  let msg = m1 + m2
                  notifyError(msg)
                } else {
                  notifyError(error.response.data.error.message)
                }
              })
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    checkStatus(item) {
      let prop = item.id;
      api
          .post(baseURL, {
            method: "prop/checkStatus",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notStatus"));
            }
          });
    },

    checkProvider(item) {
      let prop = item.id;
      api
          .post(baseURL, {
            method: "prop/checkProvider",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notProvider"));
            }
          });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "propType",
          label: this.$t("propType"),
          field: "propType",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
      ];
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },
  },

  created() {
    console.log("create ParentNode", this.parentNode)
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_PropType"}],
        })
        .then((response) => {
          this.FD_PropType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_PropType.set(it["id"], it["text"]);
          });
        });

    this.cols = this.getColumns();
    this.fetchData(this.parentNode.id);
  },

  setup() {
  },
};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}

</style>
