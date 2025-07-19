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

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- prop -->

          <q-item-label
              :class="form.prop===0 ? 'text-red-10' : 'text-grey-7'"
              style="font-size: 0.8em; margin-top: 10px" class="row"
          >{{ $t("prop") }}
            <q-space/>
            <q-icon name="error" v-if="form.prop===0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
              :options="optProp"
              v-model="form.prop"
              :normalizer="normalizerProp"
              :placeholder="$t('select')"
              :noResultsText="$t('noResult')"
              :noChildrenText="$t('noChilds')"
              :noOptionsText="$t('noResult')"
              defaultExpandLevel="1"
              autofocus
              @select="fnSelect"
          />

        <q-item-label v-if="form.prop===0" class="text-red-10" style="font-size: 0.8em">
          {{ $t("chooseProp") }}
        </q-item-label>

        <!-- name -->
        <q-input
            class="q-mt-md"
            dense :model-value="form.name"
            v-model="form.name"
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            dense :model-value="form.fullName"
            v-model="form.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
            dense :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            dense color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
        />
        <q-btn
            dense color="primary"
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
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {extend} from "quasar";

export default {
  components: {treeselect},

  props: [
    "data",
    "mode",
    "dimProp",
    "dimPropName",
    "dimPropType",
    "parentName",
    "lg",
  ],

  data() {
    return {
      form: extend({}, this.data),
      visible: false,
      lang: this.lg,
      optProp: [],

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelect(v) {
      this.form.name = this.dimPropName + " (" + v.name + ")";
      this.form.fullName = this.dimPropName + " (" + v["fullname"] + ")";
    },

    normalizerProp(node) {
      return {
        id: node.prop,
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

    validSave() {
      if (!this.form.prop || !this.form.name) return true;
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

      const method = this.mode === "ins" ? "insertDPI" : "updateDPI";

      api
          .post(baseURL, {
            id: this.form.id,
            method: "dimprop/" + method,
            params: [this.form],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result.records[0]);
                //this.$emit("ok", { res: true });
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
    //console.info("UpdaterDimPropItemProp");
      this.visible = true;
      api
          .post(baseURL, {
            method: "prop/loadAllPropForSelect",
            params: [],
          })
          .then((response) => {
            //console.info("optProp", response.data.result.records)
            this.optProp = pack(response.data.result.records, "id");
          })
          .finally(() => {
            this.visible = false;
          });
    }

}
</script>
