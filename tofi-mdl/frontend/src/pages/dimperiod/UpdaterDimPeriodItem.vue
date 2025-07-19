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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div v-if="isChild" class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- periodType -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="pt"
          :options="optPT"
          :label="$t('periodType')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="pt"
          :disable="mode === 'upd'"
          @update:model-value="fnSelectPT()"
        />

        <!-- periodNameTml -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="tml"
          :options="optTml"
          :label="$t('periodTml')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="tml"
          @update:model-value="fnSelectPeriodNameTml()"
        />

        <!-- periodIncludeTag -->
        <q-select
          v-if="isChild"
          :dense="dense"
          :options-dense="dense"
          v-model="pit"
          :options="optPIT"
          :label="$t('periodIncludeTag')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="pit"
          @update:model-value="fnSelectPeriodIncludeTag()"
        />

        <q-item-label
          style="color: cornflowerblue; text-align: center; padding-top: 30px"
          >{{ $t("params") }}</q-item-label
        >

        <!-- dbeg-->
        <q-input
          :dense="dense"
          type="date"
          stack-label
          clearable
          :model-value="form.dbeg"
          v-model="form.dbeg"
          :label="$t('fldDbeg')"
          @update:model-value="fnSelectDbeg"
        >
        </q-input>

        <!-- countPeriod-->
        <q-input
          class="q-mt-md"
          :dense="dense"
          type="number"
          stack-label
          clearable
          v-model="form.countPeriod"
          :model-value="form.countPeriod"
          :rules="[
            (val) =>
              val == null ||
              (val && val > 0) ||
              'Количество периодов должно быть положительной',
          ]"
          :label="$t('countPeriod')"
          @update:model-value="fnUpdPC"
        >
        </q-input>

        <!-- dend-->
        <q-input
          class="q-mt-md"
          :dense="dense"
          type="date"
          clearable
          stack-label
          :model-value="form.dend"
          v-model="form.dend"
          :label="$t('fldDend')"
          @update:model-value="fnSelectDend"
        >
        </q-input>

        <q-input
          class="q-mt-md"
          :dense="dense"
          type="number"
          stack-label
          :model-value="form.lagCurrentDate"
          v-model="form.lagCurrentDate"
          clearable
          :label="$t('lagCurrentDate')"
          @update:model-value="fnUpdLCP"
        >
        </q-input>

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
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
import {ref} from "vue";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import allConsts from "pages/all-consts";

export default {
  props: [
    "data",
    "mode",
    "isChild",
    "parentName",
    "parentPeriodType",
    "lg",
    "dense",
  ],

  data() {
    //console.info("data", this.data)
    return {
      form: this.data,
      lang: this.lg,
      visible: ref(false),

      optPT: [],
      pt: this.data.periodType,

      optTml: [],
      tml: this.data.periodNameTml,

      optPIT: [],
      pit: this.data.periodIncludeTag,
      //
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnUpdPC() {
      this.form.dbeg = null;
    },

    fnUpdLCP() {
      this.form.dend = null;
    },

    fnSelectDbeg(value) {
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "dimperiod/getPeriodBeg",
          params: [value, this.form.periodType],
        })
        .then(
          (response) => {
            this.form.dbeg = response.data.result;
            if (this.form.dbeg.startsWith("0000")) {
              this.form.countPeriod = 1;
              this.form.dbeg = null;
            } else this.form.countPeriod = null;
          },
          (error) => {
            let msg = error.message;
            if (error.response.data) msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnSelectDend(value) {
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "dimperiod/getPeriodEnd",
          params: [value, this.form.periodType],
        })
        .then(
          (response) => {
            this.form.dend = response.data.result;
            if (this.form.dend.startsWith("0000")) {
              this.form.lagCurrentDate = 0;
              this.form.dend = null;
            } else this.form.lagCurrentDate = null;
          },
          (error) => {
            let msg = error.message;
            if (error.response.data) msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnSelectPT() {
      this.form.dbeg = null;
      this.form.dend = null;
      this.form.periodType = this.pt.id;
    },

    fnSelectPeriodNameTml() {
      this.form.periodNameTml = this.tml.id;
    },

    fnSelectPeriodIncludeTag() {
      this.form.periodIncludeTag = this.pit.id;
    },

    validSave() {
      if (!this.isChild)
        return (
          (this.form.countPeriod && this.form.countPeriod < 1) ||
          !(
            (this.form.dbeg == null && this.form.countPeriod != null) ||
            (this.form.dbeg != null && this.form.countPeriod == null)
          ) ||
          !(
            (this.form.dend == null && this.form.lagCurrentDate != null) ||
            (this.form.dend != null && this.form.lagCurrentDate == null)
          )
        );
      else return this.form.countPeriod && this.form.countPeriod < 1;
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
      let err = false;

      const method = this.mode === "ins" ? "insertDPI" : "updateDPI";
      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al;

      this.form.periodNameTml =
        typeof this.tml === "object" ? this.tml.id : this.tml;
      this.form.periodIncludeTag =
        typeof this.pit === "object" ? this.pit.id : this.pit;

      api
        .post(baseURL, {
          method: "dimperiod/" + method,
          params: [{ rec: this.form }],
        })
        .then(
          (response) => {
            err = false;
            this.$emit("ok", response.data.result.records[0]);
            notifySuccess(this.$t("success"));
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            err = true;
            let msg = error.message;
            if (error.response.data) msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.visible = ref(false);
          if (!err) this.hide();
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
        params: [{ dict: "FD_PeriodType" }],
      })
      .then((response) => {
        this.optPT = response.data.result.records;
        if (this.isChild) {
          if (this.parentPeriodType === allConsts.FD_PeriodType.year) {
            this.optPT = this.optPT.filter((it) => {
              return [
                allConsts.FD_PeriodType.halfyear,
                allConsts.FD_PeriodType.quarter,
                allConsts.FD_PeriodType.month,
                allConsts.FD_PeriodType.decade,
                allConsts.FD_PeriodType.week,
                allConsts.FD_PeriodType.day,
              ].includes(it.id);
            });
          } else if (
            this.parentPeriodType === allConsts.FD_PeriodType.halfyear
          ) {
            this.optPT = this.optPT.filter((it) => {
              return [
                allConsts.FD_PeriodType.quarter,
                allConsts.FD_PeriodType.month,
                allConsts.FD_PeriodType.decade,
                allConsts.FD_PeriodType.week,
                allConsts.FD_PeriodType.day,
              ].includes(it.id);
            });
          } else if (
            this.parentPeriodType === allConsts.FD_PeriodType.quarter
          ) {
            this.optPT = this.optPT.filter((it) => {
              return [
                allConsts.FD_PeriodType.month,
                allConsts.FD_PeriodType.decade,
                allConsts.FD_PeriodType.week,
                allConsts.FD_PeriodType.day,
              ].includes(it.id);
            });
          } else if (this.parentPeriodType === allConsts.FD_PeriodType.month) {
            this.optPT = this.optPT.filter((it) => {
              return [
                allConsts.FD_PeriodType.decade,
                allConsts.FD_PeriodType.week,
                allConsts.FD_PeriodType.day,
              ].includes(it.id);
            });
          } else if (this.parentPeriodType === allConsts.FD_PeriodType.decade) {
            this.optPT = this.optPT.filter((it) => {
              return [
                allConsts.FD_PeriodType.week,
                allConsts.FD_PeriodType.day,
              ].includes(it.id);
            });
          } else if (this.parentPeriodType === allConsts.FD_PeriodType.week) {
            this.optPT = this.optPT.filter((it) => {
              return [allConsts.FD_PeriodType.day].includes(it.id);
            });
          }
        }
      });
    //
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{ dict: "FD_PeriodNameTml" }],
      })
      .then((response) => {
        this.optTml = response.data.result.records;
      });
    //
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{ dict: "FD_PeriodIncludeTag" }],
      })
      .then((response) => {
        this.optPIT = response.data.result.records;
        if (this.form.periodType !== allConsts.FD_PeriodType.week)
          this.optPIT.splice(-1, 1);
      })
      .finally(() => {
        this.visible = ref(false);
      });
    //
  },
};
</script>
