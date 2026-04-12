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
            :placeholder="$t('msgCodeGen')"
            dense class="q-pt-sm"
        />
        <!-- name -->
        <q-input
            :model-value="form.name"
            v-model="form.name"
            dense autofocus @blur="onBlurName"
            :label="$t('fldName')"  class="q-pt-sm"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :model-value="form.fullName"
            v-model="form.fullName"
            dense :label="$t('fldFullName')" class="q-pt-sm"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- SourceStock -->
        <q-select
            v-model="ss"
            :model-value="ss"
            :options="optSS"
            :label="$t('stocks')"
            option-value="id"
            option-label="name"
            map-options class="q-pt-sm"
            dense options-dense
            @update:model-value="fnSelectSS()"
        />

        <!-- SysCodingType -->
        <q-select
          v-model="sc"
          :model-value="sc"
          :options="optSC"
          :label="$t('sysCodingType')"
          option-value="id"
          option-label="text"
          map-options class="q-pt-md"
          dense options-dense
          @update:model-value="fnSelectSC()"
        />

        <!-- AccessLevel -->
        <q-select
            v-model="al"
            :options="optAL"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options class="q-pt-md"
            dense options-dense
            :model-value="al"
            @update:model-value="fnSelectAL()"
        />

        <!-- cmt -->
        <q-input
            :model-value="form.cmt"
            v-model="form.cmt"
            dense type="textarea" class="q-pt-md"
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
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
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
import {api} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      al: this.data.accessLevel,
      optAL: [],
      ss: this.data.sourceStock,
      optSS: [],
      sc: this.data.sysCodingType,
      optSC: [],
      loading: false,
    };
  },
  //al: this.form.accessLevel===0 ? 1 : this.form.accessLevel
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

    fnSelectSC() {
      this.form.sysCodingType = this.sc.id;
    },

    fnSelectSS() {
      this.form.sourceStock = this.ss.id;
    },

    validSave() {
      return this.form.name === "" || this.form.fullName === "";
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog["show"]();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog["hide"]();
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
      this.loading = ref(true)
      let err = false

      const method = this.mode === "ins" ? "insert" : "update";
      this.form.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;
      this.form.sourceStock =
          typeof this.ss === "object" ? this.ss.id : this.ss;

      api
          .post('', {
            id: this.form.id,
            method: "syscoding/" + method,
            params: [ this.form ],
          })
          .then(
              (response) => {
                err = false
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            if (!err) this.hide()
            this.loading= ref(false)
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    api
        .post('', {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optAL = response.data.result.records;
        });

    api
      .post('', {
        method: "dict/load",
        params: [{dict: "FD_SysCodingType"}],
      })
      .then((response) => {
        this.optSC = response.data.result.records;
      });

    api
        .post('', {
          method: "stock/loadStockForSelect",
          params: [],
        })
        .then((response) => {
          this.optSS = response.data.result.records;
        });
  },
};
</script>
