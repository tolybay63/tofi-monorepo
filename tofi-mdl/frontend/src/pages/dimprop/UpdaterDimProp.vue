<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- cod -->
        <q-input
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')" dense
          :placeholder="$t('msgCodeGen')"
        />

        <!-- dimPropType -->
        <q-select
          :disable="mode==='upd'"
          v-model="dpt"
          :model-value="dpt"
          :options="optDPT"
          :label="$t('dimPropType')"
          option-value="id"
          option-label="text"
          map-options dense options-dense
          @update:model-value="fnSelectDimPropType"
        />

        <!-- dimMultiProp -->
        <div v-if="form.dimPropType===consDimPropTypeDMP">
          <q-item-label
            :class="form.dimMultiProp===0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row">
            {{ $t("dimMultiProp") }}
            <q-space></q-space>
            <q-icon name="error" v-if="form.dimMultiProp===0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
            :options="optDimMP"
            v-model="form.dimMultiProp"
            :normalizer="normalizerDimMP"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            defaultExpandLevel="2"
            autofocus
            @select="fnSelectDimMP"
          />

          <q-item-label v-if="form.dimMultiProp===0"
                        class="text-red-10" style="font-size: 0.8em"
          >
            {{ $t('chooseDimMultiProp') }}
          </q-item-label>
        </div>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          @blur="onBlurName" dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')" dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- accessLevel -->
        <q-select
          v-model="al"
          :model-value="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options dense options-dense
          @update:model-value="fnSelectAL()"
        />

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea" dense
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel" dense
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import treeselect from "vue3-treeselect";
import allConsts from "pages/all-consts.js";

export default {
  components: {treeselect},
  props: ["data", "mode", "lg"],

  data() {
    return {
      loading: false,
      form: this.data,
      lang: this.lg,
      optAL: [],
      al: this.data.accessLevel,
      optDPT: [],
      dpt: this.data.dimPropType,
      optDimMP: [],
      consDimPropTypeDMP: allConsts.FD_DimPropType.dimMultiProp,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectDimPropType(v) {
      console.log("fnSelectDimPropType", v);
      this.form.dimPropType = v.id
    },

    fnSelectDimMP(v) {
      console.log("fnSelectDimMP", v);
      this.form.dimMultiProp = v.id
    },

    normalizerDimMP(node) {
      return {
        id: node.dimMultiProp,
        label: node.name,
      };
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
      //console.log("select", this.al)
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      if (!this.form.name) return true;
      else if (this.form.name.trim().length === 0) return true;
      return false;
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

      const method = this.mode === "ins" ? "insert" : "update";
      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al;

      this.form.dimPropType =
        typeof this.dpt === "object" ? this.dpt.id : this.dpt;

      api
        .post(baseURL, {
          id: this.form.id,
          method: "dimprop/" + method,
          params: [{rec: this.form}],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            notifySuccess(this.$t("success"));
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    console.info("UpdaterDimProp");
    this.loading = true;
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      })
      .finally(() => {
        this.loading = false;
      });
    //
    this.loading = true;
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_DimPropType"}],
      })
      .then((response) => {
        this.optDPT = response.data.result.records;
      })
      .finally(() => {
        this.loading = false;
      });
    //
    this.loading = true;
    api
      .post(baseURL, {
        method: "dimprop/loadAllDimMultiPropForSelect",
        params: [],
      })
      .then((response) => {
        console.info("optDimMP", response.data.result.records)
        this.optDimMP = pack(response.data.result.records, "id");
      })
      .finally(() => {
        this.loading = false;
      });
  },
};
</script>
