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
        <div> {{form['objObjectTypeMulti']}}</div>

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
          @update:model-value="fnSelectObjMulti"
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

    fnSelectObjMulti(v) {
      console.info("fnSelectObjMulti", v)
      console.info("objObjectTypeMulti", this.form['objObjectTypeMulti'])
      //this.form.objObjectTypeMulti = v.id
      //this.form.cls = v.cls
      //this.form.pvobjObjectType = v["pv"]
    },

    filterObjMulti(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optObjMulti = this.optObjMultiOrg
        })
        return
      }
      update(() => {
        if (this.optObjMultiOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optObjMulti = this.optObjMultiOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectFvRegion(v) {
      this.form.fvRegion = v.id
      this.form.pvRegion = v["pv"]
    },

    fnSelectFvIsActive(v) {
      this.form.fvIsActive = v.id
      this.form.pvIsActive = v["pv"]
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
          console.info("FV", this.optFvRegion)
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
              console.info("FV", this.optFvIsActive)
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
              console.info("Cls", this.optCls)
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
              console.info("Obj", this.optObjMulti)
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
    if (this.mode==="upd") {
      //let objObjectTypeMulti = JSON.parse( (this.data["objObjectTypeMulti"]))
      let objObjectTypeMulti = JSON.stringify(this.data["objObjectTypeMulti"])


      console.info("objObjectTypeMulti", objObjectTypeMulti)

    }


  },
};
</script>
