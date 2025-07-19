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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-input
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')" dense
        />
        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          dense autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          :model-value="form.fullName"
          v-model="form.fullName"
          dense :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- AccessLevel -->
        <q-select
          v-model="al"
          :model-value="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options dense options-dense
          class="q-mb-md"
          @update:model-value="fnSelectAL()"
        />

        <!-- CubeSType -->
        <q-select
          v-model="ct"
          :model-value="ct"
          :options="optCT"
          :label="$t('cubesType')"
          option-value="id"
          option-label="text"
          map-options dense options-dense
          class="q-mb-md"
          @update:model-value="fnSelectCT()"
        />

        <q-input
          v-model="form.dOrg"
          :model-value="form.dOrg"
          :label="$t('dOrg')"
          dense type="date"
        />

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          dense type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          dense :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
          dense
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      al: this.data.accessLevel === 0 ? 1 : this.data.accessLevel,
      optAL: [],
      ct: this.data.cubeSType === 0 ? 1 : this.data.cubeSType,
      optCT: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
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

    fnSelectCT() {
      this.form.cubeSType = this.ct.id;
    },

    validSave() {
      return this.form.name === "" || this.form.fullName === ""
        || !this.form.accessLevel || !this.form.cubeSType;
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

      const method = this.mode === "ins" ? "insertCube" : "updateCube";
      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al;
      this.form.cubeSType =
        typeof this.ct === "object" ? this.ct.id : this.ct;
      api
        .post(baseURL, {
          id: this.form.id,
          method: "cubes/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            notifySuccess(this.$t("success"));
          },
          (error) => {
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
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      });

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_CubeSType"}],
      })
      .catch(error => {
        console.log(error.message);
        if (error.response && error.response.data && error.response.data.error) {
          notifyError(this.$t(error.response.data.error.message));
        } else {
          notifyError(error.message);
        }
      })
      .then((response) => {
        this.optCT = response.data.result.records;
      });

  },
};
</script>
