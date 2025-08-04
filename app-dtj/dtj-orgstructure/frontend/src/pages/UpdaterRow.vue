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
        />
        <!-- cls -->
        <q-select
          v-model="form['cls']"
          :model-value="form['cls']"
          :label="fmReqLabel('Вид деятельности')"
          :options="optCls"
          dense class="q-ma-md"
          map-options
          option-label="name"
          option-value="id"
          @update:model-value="fnSelectCls"
        />

        <!-- objObjectTypeMulti -->
        <q-select
          v-model="form['objObjectTypeMulti']"
          :model-value="form['objObjectTypeMulti']"
          :label="fmReqLabel('Объект')"
          :options="optObjMulti"
          dense class="q-ma-md"
          map-options
          option-label="name"
          option-value="id"
          multiple
        />

        <!-- fvRegion -->
        <q-select
          v-model="form['fvRegion']"
          :model-value="form['fvRegion']"
          :label="fmReqLabel('Значение фактора')"
          :options="optFvRegion"
          dense options-dense map-options
          option-label="name" option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectFvRegion"
        />

        <!-- fvIsActive -->
        <q-select
          v-model="form['fvIsActive']"
          :model-value="form['fvIsActive']"
          :label="fmReqLabel('Значение фактора')"
          :options="optFvIsActive"
          dense options-dense map-options
          option-label="name" option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectFvIsActive"
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

        <!-- StageLength -->
        <q-input
          :model-value="form['StageLength']"
          v-model="form['StageLength']"
          class="q-ma-md" dense
          :label="fmLabel('Протяженность')"
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
      optCls: [],

      optFvRegion: [],
      optFvRegionOrg: [],

      optFvIsActive: [],
      optFvIsActiveOrg: [],

      optObjMulti: [],
      optObjMultiOrg: [],


    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fmReqLabel(label) {
      return label + "*";
    },

    fmLabel(label) {
      return label;
    },

    fnSelectCls(v) {
      this.form.cls = v.id
      this.form.nameCls = v["name"]
    },

    fnSelectFvRegion(v) {
      if (v) {
        this.form.fvRegion = v.id
        this.form.pvRegion = v["pv"]
      } else {
        this.form.fvRegion = null
        this.form.pvRegion = null
      }
    },

    fnSelectFvIsActive(v) {
      if (v) {
        this.form.fvIsActive = v.id
        this.form.pvIsActive = v["pv"]
      } else {
        this.form.fvIsActive = null
        this.form.pvIsActive = null
      }
    },

    validSave() {
      if (!this.form.name || !this.form.cls) return true
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
      this.$axios
        .post(baseURL, {
          method: "data/saveLocation",
          params: [this.mode, this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result["records"][0])
          },
          (error) => {
            error = true
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
        params: ['Prop_Region'],
      })
      .then(
        (response) => {
          this.optFvRegion = response.data.result["records"]
          this.optFvRegionOrg = response.data.result["records"]
        })
      .then(() => {
        api
          .post(baseURL, {
            method: 'data/loadFactorValForSelect',
            params: ['Prop_IsActive'],
          })
          .then(
            (response) => {
              this.optFvIsActive = response.data.result["records"]
              this.optFvIsActiveOrg = response.data.result["records"]
            })
        //
        api
          .post(baseURL, {
            method: 'data/loadClsForSelect',
            params: ['Typ_Location'],
          })
          .then(
            (response) => {
              this.optCls = response.data.result["records"]
            })
        //

        api
          .post(baseURL, {
            method: "data/loadObjList",
            params: ["Typ_ObjectTyp", "Prop_ObjectType", "nsidata"],
          })
          .then(
            (response) => {
              this.optObjMulti = response.data.result["records"]
              this.optObjMultiOrg = response.data.result["records"]
            })
          .then(()=> {

            if (this.mode==="upd") {
              let ObjectTypeMulti = this.data["objObjectTypeMulti"].split(",")
              this.form["objObjectTypeMulti"] = []

              ObjectTypeMulti.forEach((obj) => {
                let o = parseInt(obj, 10)
                let ind = this.optObjMulti.findIndex((row)=> row.id===o)
                this.form["objObjectTypeMulti"].push(this.optObjMulti[ind])
              })
            }
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
