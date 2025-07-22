<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary" v-if="mode==='upd'">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>
      <q-bar class="text-white bg-primary" v-else>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary" />

      <!-- cod -->
      <q-card-section>

<!--        <q-item-label class="q-ma-md" v-if="parentName">
          <span class="text-blue">Родитель:</span> {{ parentName }}
        </q-item-label>-->


        <q-input
          dense autofocus
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <!-- cls -->
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("cls") }}</q-item-label
        >
        <treeselect
          :options="optCls"
          v-model="cls"
          :normalizer="normalizer"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @close="fnCloseCls" :disabled="mode==='upd'"
        />

        <!-- name -->
        <q-input
          dense
          :model-value="form.name"
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
          dense
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- dbeg-->
        <q-input
          dense
          type="date"
          stack-label
          :model-value="form.dbeg"
          v-model="form.dbeg"
          :label="$t('fldDbeg')"
        />
        <!-- dend-->
        <q-input
          dense
          type="date"
          stack-label
          :model-value="form.dend"
          v-model="form.dend"
          :label="$t('fldDend')"
        />
        <!-- accessLevel -->
        <q-select
          dense options-dense
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="al"
          @update:model-value="fnSelectAL()"
          style="margin-bottom: 5px"
        />

        <!-- objParent -->
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("parentObj") }}</q-item-label
        >
        <treeselect
          :options="optParents"
          v-model="objParent"
          :model-value="objParent"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @close="fnCloseObj"
        >
        </treeselect>

        <!-- cmt -->
        <q-input
          dense
          :model-value="form['cmt']"
          v-model="form['cmt']"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense
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
import { notifyError, notifySuccess, pack } from "src/utils/jsutils";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
// import the component
import treeselect from "vue3-treeselect";
// import the styles
import "vue3-treeselect/dist/vue3-treeselect.css";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  props: ["data", "mode", "typOrCls", "typ", "isMultiProp", "level", "parentName"],
  components: { treeselect },
  data() {
    console.info("data()", this.data)
    return {
      form: this.data,
      optAL: [],
      al: this.data.accessLevel,
      //
      optCls: [],
      cls: this.level===0 ? null : this.data.cls,
      loading: false,

      optParents: [],
      objParent: this.data.parent,

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fetchDataObj(typ, cls, obj) {
      this.loading = true
      api
        .post(baseURL, {
          method: "data/loadObjTreeParent",
          params: [typ, cls, obj, getModel.value, getMetaModel.value],
        })
        .then(
          (response) => {
            this.optParents = pack(response.data.result.records, "ord");
          },
          (error) => {
            console.info("fetchDataObj")
            let msg
            if (error.response) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = false
        });
    },

    fnCloseObj(v) {
      this.form.parent = v;
    },

    fnCloseCls(v) {
      this.form.cls = v;
      this.fetchDataObj(this.typ, v, this.form.id || 0);
    },

    normalizer(node) {
      return {
        id: node["ent"],
        label: node.name,
      };
    },

    onBlurName() {
      if (this.form.name) {
        this.form.fullName = this.form.name.trim();
      }
    },

    fnSelectAL() {
      //console.log("select", this.al)
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      if (!this.form.name || !this.form.fullName || !this.cls) return true
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


      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al
      this.form.cls = typeof this.cls === "object" ? this.cls.id : this.cls
      this.form.model = getModel.value
      this.form.metamodel = getMetaModel.value
      this.form.mode = this.mode
      this.form.isObj = true
      this.form.isMultiProp = this.isMultiProp
      this.form.typ = this.typ
      let err = false
      api
        .post(baseURL, {
          method: "data/createOwn",
          params: [this.form],
        })
        .then(
          (response) => {
            console.info("response", response.data.result.records[0])
            this.$emit("ok", response.data.result.records[0]);
            //this.$emit("ok", { res: true });
            notifySuccess(this.$t("success"));
          })
        .catch(error => {
          err = true;
          notifyError(error.response.data.error.message);
        })
        .finally(() => {
          if (!err) this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    console.info("data create", this.data)

    this.loading = +true
    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      })
      .catch((error)=> {
        if (error.response.data)
          notifyError(this.$t(error.response.data.error.message))
        else
          notifyError(this.$t(error.message))
      })
    //
    api
      .post(baseURL, {
        method: "data/loadClsTree",
        params: [{typOrCls: this.typOrCls, level: this.level, typNodeVisible: false}],
      })
      .then((response) => {
        this.optCls = pack(response.data.result.records, "ord");
      })
      .catch((error)=> {
        console.error(error.message);
        if (error.response.data)
          notifyError(this.$t(error.response.data.error.message))
        else
          notifyError(this.$t(error.message))
      })
      .finally(() => {
        this.loading = false
      })

    let cls = this.data.cls ? this.data.cls : 0
    if (this.mode==="upd")
      this.fetchDataObj(this.typ, this.data.cls, this.data.id)
    else
      this.fetchDataObj(this.typ, cls, 0)

  },
};
</script>
