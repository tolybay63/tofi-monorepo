<!-- DimObjItemProp (lt < 5) -->
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
        <!--  prop -->
        <q-select
          dense options-dense v-model="prop" :model-value="prop"
          option-value="id" :options="optProp" :label="$t('prop')" option-label="name"
          map-options @update:model-value="fnSelectProp()"
        />

        <!-- propStatus -->
        <q-select
          :disable="sDisable"
          dense options-dense v-model="s" :model-value="s"
          option-value="id" :options="optS" :label="$t('statusFactor')" option-label="name"
          map-options @update:model-value="fnSelectSt()"
        />
        <!-- propStatusMissing -->
        <q-select
          :disable="sDisable"
          dense options-dense v-model="sMis" :model-value="sMis"
          option-value="id" option-label="text" :options="optSmis" :label="$t('statusMissing')"
          map-options @update:model-value="fnSelectStMis()"
        />

        <!-- propProvider -->
        <q-select
          :disable="qDisable"
          dense options-dense v-model="q" :model-value="q"
          :options="optQ" :label="$t('providerTyp')"
          option-value="id" option-label="text" map-options
          @update:model-value="fnSelectQ()"
          @click="fnClickQ()"
        />
        <!-- propProviderMissing -->
        <q-select
          :disable="qDisable"
          dense options-dense v-model="qMis" :model-value="qMis"
          :options="optQmis" :label="$t('providerMissing')"
          option-value="id" option-label="text" map-options
          @update:model-value="fnSelectQMis()"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense color="primary" icon="save" :label="$t('save')"
          @click="onOKClick" :disable="validSave()"
        />
        <q-btn
          dense color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "doi", "linkType", "doiType"],

  data() {
    return {
      form: this.data,

      optProp: [],
      prop: this.data.prop,

      optS: [],
      s: this.data.propStatus,

      optSmis: [],
      sMis: this.data.propStatusMissing,

      optQ: [],
      q: this.data.propProvider,

      optQmis: [],
      qMis: this.data.propProviderMissing,

      sDisable: true,
      qDisable: true,

      visible: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnClickQ() {
      notifyInfo("Запрос к другой базе!")
    },


    fnSelectProp() {
      this.form.prop = this.prop.id;
      this.sDisable = !this.prop["statusfactor"]
      this.qDisable = !this.prop["providertyp"]

      if (!this.sDisable) {
        this.loadPropStatus(this.doi, this.form.prop, this.mode);
      } else {
        this.optS = [];
      }
      if (!this.qDisable) {
      } else {
        this.optQ = [];
      }
    },

    fnSelectSt() {
      this.form.propStatus = this.s.id;
    },

    fnSelectStMis() {
      this.form.propStatusMissing = this.sMis.id;
    },

    fnSelectQ() {
      this.form.propProvider = this.q.id;
    },

    fnSelectQMis() {
      this.form.propProviderMissing = this.qMis.id;
    },

    fnSelectAL() {
      this.form.accessLevel = this.al.id;
    },

    loadPropStatus(doi, prop, mode) {
      this.visible = ref(true);
      //
      api
        .post(baseURL, {
          method: "dimobj/loadPropStatus",
          params: [doi, prop, mode],
        })
        .then((response) => {
          this.optS = response.data.result.records;
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
          //
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

/*
    loadPropProvider(prop) {
      this.visible = ref(true);
      //
      api
        .post(baseURL, {
          method: "dimobj/loadPropProvider",
          params: [prop],
        })
        .then((response) => {
          this.optQ = response.data.result.records;
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
          //
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },
  */

    //todo
    validSave() {
      return !(this.form.prop && !this.sDisable && this.form.propStatus && this.form.propStatusMissing ||
        (this.form.prop && this.sDisable));
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
      this.visible = ref(true);

      const method = this.mode === "ins" ? "insertDOIprop" : "updateDOIprop";
      this.form.propStatusMissing =
        typeof this.sMis === "object" ? this.sMis.id : this.sMis;

      this.form.propProviderMissing =
        typeof this.qMis === "object" ? this.qMis.id : this.qMis;

      this.form.dimObjItem = this.doi;

      api
        .post(baseURL, {
          method: "dimobj/" + method,
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
          this.visible = ref(false);
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    this.visible = ref(true);
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_PropStatusMissing"}],
      })
      .then((response) => {
        this.optSmis = response.data.result.records;
      })
      .finally(() => {
        this.visible = ref(false);
      });

    this.visible = ref(true);
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_PropProviderMissing"}],
      })
      .then((response) => {
        this.optQmis = response.data.result.records;
      })
      .finally(() => {
        this.visible = ref(false);
      });
    //
    this.visible = ref(true);
    api
      .post(baseURL, {
        method: "dimobj/loadOptProp",
        params: [this.doi],
      })
      .then((response) => {
        this.optProp = response.data.result.records;
      })
      .catch((error) => {
        let msg = error.message;
        if (error.response) msg = error.response.data.error.message;
        notifyError(msg);
      })
      .finally(() => {
        this.visible = ref(false);
      });

    if (this.mode === 'upd') {
      this.loadPropStatus(this.doi, this.form.prop, this.mode);
    }

  },
};
</script>
