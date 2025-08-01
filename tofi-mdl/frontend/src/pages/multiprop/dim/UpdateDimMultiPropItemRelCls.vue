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

        <!-- relcls -->
        <q-item-label
            :class="form.relCls < 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row q-mt-lg">
          {{ $t("relCls") }}
          <q-space></q-space>
          <q-icon name="error" v-if="form.relCls < 0" color="red-10" size="24px"></q-icon>
        </q-item-label>

        <treeselect
            class="q-mb-lg"
            :options="optRelCls"
            v-model="relCls"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @close="fnCloseRelCls"
        />
        <q-item-label v-if="form.relCls < 0"
                      class="text-red-10" style="font-size: 0.8em"
        >
          {{ $t("chooseRelCls") }}
        </q-item-label>

        <!-- format -->
        <q-select
            class="q-my-lg"
            v-model="vf"
            :model-value="vf"
            :options="optVF"
            :label="$t('visualFormat')"
            option-value="id"
            option-label="text"
            map-options
            dense options-dense
            @update:model-value="fnSelectVF"
        />

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
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import {ref} from "vue";
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";

export default {
  components: {treeselect},
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      visible: ref(false),

      vf: this.data.visualFormat,
      optVF: [],

      optRelCls: [],
      relCls: this.data.relCls,

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    normalizer(node) {
      return {
        id: node.relcls,
        label: node.name,
      };
    },

    fnCloseRelCls(v) {
      this.form.relCls = v;
    },

    fnSelectVF() {
      this.form.visualFormat = this.vf.id
    },

    validSave: function () {
      return this.form.relCls < 0;
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

      const method = this.mode === "ins" ? "insDimMultiPropItemRelCls" : "updDimMultiPropItemRelCls";

      this.form.visualFormat =
          typeof this.vf === "object" ? this.vf.id : this.vf;
      api
          .post(baseURL, {
            method: "dimMultiProp/" + method,
            params: [this.form],
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

    this.visible = ref(true)
    api
        .post(baseURL, {
          method: "dimMultiProp/loadRelClsForSelect",
          params: [],
        })
        .then((response) => {
          this.optRelCls = pack(response.data.result.records, "id");
        })
        .finally(() => {
          this.visible = ref(false)
        });

    this.visible = ref(true)
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_VisualFormat"}],
        })
        .then((response) => {
          this.optVF = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false)
        });
  },
};

</script>
