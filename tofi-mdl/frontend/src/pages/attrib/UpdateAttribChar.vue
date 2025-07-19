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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" :dense="dense">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" :dense="dense">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- mask-2 -->
        <q-input
            v-if="avt === 2"
            :model-value="form.maskReg"
            v-model="form.maskReg"
            :label="$t('maskVal')"
            :dense="dense"
            autofocus
        >
          <template #append>
            <q-btn
                rounded
                dense
                @click="infoMask()"
                icon="help"
                text-color="primary"
            >
            </q-btn>
          </template>
        </q-input>
        <!-- date-3 -->
        <q-select
            v-if="avt === 3"
            v-model="d"
            :model-value="d"
            :options="optionsDate"
            :label="$t('formatDate')"
            @update:model-value="fnSelectValTyp(avt)"
            :dense="dense"
            :options-dense="dense"
            autofocus
        />
        <q-select
            v-if="avt === 4"
            v-model="d"
            :options="optionsTime"
            :label="$t('formatTime')"
            @update:model-value="fnSelectValTyp(avt)"
            :dense="dense"
            :options-dense="dense"
            autofocus
        />

        <q-select
            v-if="avt === 5"
            v-model="d"
            :options="optionsDateTime"
            :label="$t('formatDateTime')"
            @update:model-value="fnSelectValTyp(avt)"
            :dense="dense"
            :options-dense="dense"
            autofocus
        />
        <q-input
            v-if="avt === 7"
            :model-value="form.fileExt"
            v-model="form.fileExt"
            :label="$t('fileExt')"
            :dense="dense"
            :options-dense="dense"
            autofocus
        >
        </q-input>
        <!-- entityType-8 -->
        <q-select
            v-if="avt === 8"
            autofocus
            v-model="entity"
            :model-value="entity"
            :options="optionsEntity"
            :label="$t('entityType')"
            option-value="id"
            option-label="text"
            map-options
            :dense="dense"
            :options-dense="dense"
            @update:model-value="fnSelectValTyp(avt)"
        />
        <!-- periodType -->
        <q-select
            v-if="avt === 9"
            autofocus
            v-model="period"
            :model-value="period"
            :dense="dense"
            :options-dense="dense"
            :options="optionsPeriod"
            :label="$t('periodType')"
            option-value="id"
            option-label="text"
            map-options
            @update:model-value="fnSelectValTyp(avt)"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validInput(avt)"
            :dense="dense"
        />
        <q-btn
            color="primary"
            icon="cancel"
            :label="$t('cancel')"
            :dense="dense"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["avt", "data", "mode", "attrib", "dense"],

  data() {
    let dt = null;
    let entity = null;
    let period = null;
    if (this.mode === "ins") {
      if (this.avt === 3) dt = "YYYY-MM-DD";
      else if (this.avt === 4) dt = "HH:mm:ss";
      else if (this.avt === 5) dt = "YYYY-MM-DD HH:mm:ss";
      else if (this.avt === 8) {
        entity = 1;
      } else if (this.avt === 9) {
        period = 11;
      }
    } else {
      if ([3, 4, 5].includes(this.avt)) dt = this.data.format;
      else if (this.avt === 8) entity = this.data.entityType;
      else if (this.avt === 9) period = this.data.periodType;
    }
    return {
      form: this.data,
      optionsEntity: [],
      optionsPeriod: [],
      entity: entity,
      period: period,
      d: dt,
      optionsDate: [
        "YYYY-MM-DD",
        "YYYY/MM/DD",
        "DD.MM.YYYY",
        "DD/MM/YYYY",
        "DD-MM-YYYY",
        "YY/M/D",
        "YY-M-D",
        "D/M/YY",
        "D-M-YY",
      ],
      optionsDateTime: [
        "YYYY-MM-DD HH:mm:ss",
        "YYYY/MM/DD HH:mm:ss",
        "DD/MM/YYYY HH:mm:ss",
        "DD-MM-YYYY HH:mm:ss",
        "YY/M/D HH:mm:ss",
        "YY-M-D HH:mm:ss",
        "D/M/YY HH:mm:ss",
        "D-M-YY HH:mm:ss",
      ],
      optionsTime: ["HH:mm:ss", "HH:mm:SSS"],
    };
  },
  //al: this.form.accessLevel===0 ? 1 : this.form.accessLevel
  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    infoMask() {
      //const $q = useQuasar()
      this.$q.dialog({
        message: this.$t("infoMask"),
        html: true,
      });
    },

    fnSelectValTyp(avt) {
      if ([3, 4, 5].includes(avt)) {
        this.form.format = this.d;
      } else if (avt === 8) {
        this.form.entityType = this.entity.id;
        this.form.periodType = null;
      } else if (avt === 9) {
        this.form.periodType = this.period.id;
        this.form.entityType = null;
      }

      //console.log("sel", this.form)
    },

    validInput(avt) {
      if (avt === 2) {
        if (!this.form.maskReg) return true;
        if (this.form.maskReg.trim().length === 0) return true;
      } else if (avt === 7) {
        if (!this.form.fileExt) return true;
        if (this.form.fileExt.trim().length === 0) return true;
      }
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

      //delete this.form.entityType_text
      //delete this.form.periodType_text
      this.form.attrib = this.attrib;
      const method = this.mode === "ins" ? "insertChar" : "updateChar";

      if (this.avt === 2) {
        this.form.format = null;
        this.form.fileExt = null;
        this.form.entityType = null;
        this.form.periodType = null;
      }
      if ([3, 4, 5].includes(this.avt)) {
        this.form.format = this.d;
        this.form.maskReg = null;
        this.form.fileExt = null;
        this.form.entityType = null;
        this.form.periodType = null;
      }
      if (this.avt === 7) {
        this.form.maskReg = null;
        this.form.format = null;
        this.form.periodType = null;
        this.form.entityType = null;
      }
      if (this.avt === 8) {
        this.form.maskReg = null;
        this.form.format = null;
        this.form.fileExt = null;
        this.form.periodType = null;
        this.form.entityType =
            typeof this.entity === "object" ? this.entity.id : this.entity;
      }
      if (this.avt === 9) {
        this.form.maskReg = null;
        this.form.format = null;
        this.form.fileExt = null;
        this.form.entityType = null;
        this.form.periodType =
            typeof this.period === "object" ? this.period.id : this.period;
      }
      //
      api
          .post(baseURL, {
            method: "attrib/" + method,
            params: [{rec: this.form}],
          })
          .then(
              (response) => {
                //console.log("upd 2", response.data.result.records[0])
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
          params: [{dict: "FD_EntityType"}],
        })
        .then((response) => {
          this.optionsEntity = response.data.result.records;
        });

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_PeriodType"}],
        })
        .then((response) => {
          this.optionsPeriod = response.data.result.records;
        });

    return {};
  },
};
</script>
