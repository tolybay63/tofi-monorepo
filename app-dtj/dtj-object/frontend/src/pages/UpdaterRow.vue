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
        <div>Добавить</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>Редактировать</div>
      </q-bar>

      <q-card-section>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('Наименоваие')"
          class="q-ma-md" dense autofocus
          @blur="fnBlur()"
        />
        <!-- fullName -->
        <q-input
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="fmReqLabel('полное наименование')"
          class="q-ma-md" dense autofocus
        />

        <!-- objObjectType -->
        <q-select
          v-model="form['objObjectType']"
          :model-value="form['objObjectType']"
          :label="fmReqLabel('Объект')"
          :options="optObj"
          dense class="q-ma-md"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectObj"
          @filter="filterObj"
        />

        <!-- fvSide -->
        <q-select
          v-model="form['fvSide']"
          :model-value="form['fvSide']"
          :label="fmReqLabel('Значение фактора')"
          :options="optFv" clearable
          dense options-dense map-options
          option-label="name" option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectFv"
        />

        <!-- StartKm -->
        <q-input
          :model-value="form['StartKm']"
          v-model="form['StartKm']"
          class="q-ma-md" dense
          :label="fmLabel('Начало, км')"
        />

        <!-- FinishKm -->
        <q-input
          :model-value="form['FinishKm']"
          v-model="form['FinishKm']"
          class="q-ma-md" dense
          :label="fmLabel('Конец, км')"
        />

        <!-- StartPicket -->
        <q-input
          :model-value="form['StartPicket']"
          v-model="form['StartPicket']"
          class="q-ma-md" dense
          :label="fmLabel('Начало, пк')"
        />

        <!-- FinishPicket -->
        <q-input
          :model-value="form['FinishPicket']"
          v-model="form['FinishPicket']"
          class="q-ma-md" dense
          :label="fmLabel('Конец, пк')"
        />

        <!-- PeriodicityReplacement -->
        <q-input
          :model-value="form['PeriodicityReplacement']"
          v-model="form['PeriodicityReplacement']"
          class="q-ma-md" dense
          :label="fmLabel('Периодичность замены, год')"
        />

        <!-- Number -->
        <q-input
          :model-value="form['Number']"
          v-model="form['Number']"
          class="q-ma-md" dense
          :label="fmLabel('Номер')"
        />


        <!-- InstallationDate -->
        <q-input
          :model-value="form['InstallationDate']"
          v-model="form['InstallationDate']"
          class="q-ma-md" dense type="date"
          :label="fmLabel('Дата установки')"
        />

        <!-- CreatedAt -->
        <q-input
          :model-value="form['CreatedAt']"
          v-model="form['CreatedAt']"
          class="q-ma-md" dense type="date"
          :label="fmLabel('Дата создания записи')"
        />

        <!-- UpdatedAt -->
        <q-input
          :model-value="form['UpdatedAt']"
          v-model="form['UpdatedAt']"
          class="q-ma-md" dense type="date"
          :label="fmLabel('Дата последнего обновления записи')"
        />

        <!-- Description -->
        <q-input
          :model-value="form.Description"
          v-model="form.Description"
          type="textarea" class="q-ma-md"
          :label="fmLabel('Описание')"
        />


      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          label="Сохранить"
          @click="onOKClick"
          :loading="loading"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>

        </q-btn>
        <q-btn
          color="primary"
          icon="cancel" dense
          label="Отмена"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "data"],

  data() {
    return {
      loading: false,
      form: this.data,
      optFv: [],
      optFvOrg: [],
      optObj: [],
      optObjOrg: [],


    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnBlur() {
      if (this.form.fullName === "") {
        this.form.fullName = this.form.name;
      }
    },

    fmReqLabel(label) {
      return label + "*";
    },

    fmLabel(label) {
      return label;
    },

    fnSelectObj(v) {
      this.form.objObjectType = v.id
      this.form.cls = v.cls
      this.form.pvobjObjectType = v["pv"]
    },

    filterObj(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optObj = this.optObjOrg
        })
        return
      }
      update(() => {
        if (this.optObjOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optObj = this.optObjOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectFv(v) {
      if (v) {
        this.form.fvSide = v.id
        this.form.pvSide = v["pv"]
      } else {
        this.form.fvSide = null
        this.form.pvSide = null
      }
    },


    validSave() {
      if (!this.form.name || !this.form.fullName || !this.form.objObjectType) return true
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

      this.loading = true
      let err = false
      this.form.linkCls = this.form.cls
      this.$axios
        .post(baseURL, {
          method: "data/saveObjectServed",
          params: [this.mode, this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result["records"][0])
          },
          (error) => {
            err = true
            if (error.response)
              notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.loading = false
          if (!err)
            this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadFactorValForSelect',
        params: ['Prop_Side'],
      })
      .then(
        (response) => {
          this.optFv = response.data.result["records"]
          this.optFvOrg = response.data.result["records"]
        })
      .then(() => {
        api
          .post(baseURL, {
            method: "data/loadObjList",
            params: ["Typ_ObjectTyp", "Prop_ObjectType", "nsidata"],
          })
          .then(
            (response) => {
              this.optObj = response.data.result["records"]
              this.optObjOrg = response.data.result["records"]
            })
          .catch(error => {
            console.error(error.message)
            notifyError(error.message)
          })
      })
      .catch(error => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = false
      })
    //


  },
};
</script>
