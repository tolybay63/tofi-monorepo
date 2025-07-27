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
        <!-- login -->
        <q-input
          autofocus dense
          v-model="form['login']"
          :model-value="form['login']"
          type="text"
          :label="fnReqLabel('Логин')"
        />

          <!-- passwd -->
          <q-input
            v-show="mode === 'ins'"
            dense
            v-model="form['passwd']"
            :model-value="form['passwd']"
            :label="fnLabel('Пароль')"
          >
            <template v-slot:append>
              <q-icon
                dense
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>
          <!-- psw2 -->
          <q-input
            v-show="mode === 'ins'"
            dense
            v-model="form['psw2']"
            :model-value="form['psw2']"
            :label="fnReqLabel('Подтверждение')"
            :type="isPwd ? 'password' : 'text'"
            :rules="[(val) => pswTest(val) || fnLabel('Ошибка')]"
          >
          </q-input>

          <q-toggle
            class="q-mt-lg"
            dense
            :model-value="form.locked"
            v-model="form.locked"
            :label="$t('locked')"
          />
           Prop_UserSecondName Prop_UserFirstName Prop_UserMiddleName


          <!-- Prop_TabNumber -->
          <q-input
            :model-value="form['TabNumber']"
            v-model="form['TabNumber']"
            label="Табельный номер"
            class="q-ma-md" dense
          />

          <!-- Prop_UserSecondName -->
          <q-input
            :model-value="form['UserSecondName']"
            v-model="form['UserSecondName']"
            label="Фамилия"
            class="q-ma-md" dense
          />

          <!-- Prop_UserFirstName -->
          <q-input
            :model-value="form['UserFirstName']"
            v-model="form['UserFirstName']"
            label="Имя"
            class="q-ma-md" dense
          />
          <!-- Prop_UserFirstName -->
          <q-input
            :model-value="form['UserMiddleName']"
            v-model="form['UserMiddleName']"
            label="Отчество"
            class="q-ma-md" dense
          />





          <!-- objLocation -->
          <q-select
            v-model="form['objLocation']"
            :model-value="form['objLocation']"
            :label="fnReqLabel('Объект')"
            :options="optObj"
            dense class="q-ma-md"
            map-options
            option-label="name"
            option-value="id"
            @update:model-value="fnSelectObj()"
          />

          <!-- fvSex -->
          <q-select
            v-model="form['fvSex']"
            :model-value="form['fvSex']"
            :label="fnReqLabel('Значение фактора')"
            :options="optFvSex"
            dense options-dense map-options
            option-label="name" option-value="id"
            class="q-ma-md"
            @update:model-value="fnSelectFvSex"
          />

          <!-- fvPosition -->
          <q-select
            v-model="form['fvPosition']"
            :model-value="form['fvPosition']"
            :label="fnReqLabel('Значение фактора')"
            :options="optFvPosition"
            dense options-dense map-options
            option-label="name" option-value="id"
            class="q-ma-md"
            @update:model-value="fnSelectFvPosition"
          />

          <!-- StartKm -->
          <q-input
            :model-value="form['StartKm']"
            v-model="form['StartKm']"
            class="q-ma-md" dense
            :label="fnLabel('Начало, км')"
          />

          <!-- FinishKm -->
          <q-input
            :model-value="form['FinishKm']"
            v-model="form['FinishKm']"
            class="q-ma-md" dense
            :label="fnLabel('Конец, км')"
          />

          <!-- StageLength -->
          <q-input
            :model-value="form['StageLength']"
            v-model="form['StageLength']"
            class="q-ma-md" dense
            :label="fnLabel('Протяженность')"
          />

          <!-- CreatedAt -->
          <q-input
            :model-value="form['CreatedAt']"
            v-model="form['CreatedAt']"
            class="q-ma-md" dense type="date"
            :label="fnLabel('Дата создания записи')"
          />

          <!-- UpdatedAt -->
          <q-input
            :model-value="form['UpdatedAt']"
            v-model="form['UpdatedAt']"
            class="q-ma-md" dense type="date"
            :label="fnLabel('Дата последнего обновления записи')"
          />

          <!-- Description -->
          <q-input
            :model-value="form.Description"
            v-model="form.Description"
            type="textarea" class="q-ma-md"
            :label="fnLabel('Описание')"
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
      optFvSex: [],
      optFvPosition: [],
      optObj: [],
      optObjOrg: [],
      isPwd: true,


    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
        v
      );
    },


    pswTest(val) {
      return val === this.form["passwd"];
    },

    fnReqLabel(label) {
      return label + "*";
    },

    fnLabel(label) {
      return label;
    },

    fnSelectObj(v) {
      this.form.objLocation = v.id
      this.form.pvLocation = parseInt(v["pv"], 10)
    },

    fnSelectFvSex(v) {
      this.form.fvSex = v.id
      this.form.pvSex = parseInt(v["pv"], 10)
    },

    fnSelectFvPosition(v) {
      this.form.fvPosition = v.id
      this.form.pvPosition = parseInt(v["pv"], 10)
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
        params: ['Prop_UserSex'],
      })
      .then(
        (response) => {
          this.optFvSex = response.data.result["records"]
        })
      .then(() => {
        api
          .post(baseURL, {
            method: 'data/loadFactorValForSelect',
            params: ['Prop_Position'],
          })
          .then(
            (response) => {
              this.optFvPosition = response.data.result["records"]
            })
        //
        api
          .post(baseURL, {
            method: "data/loadObjList",
            params: ["Typ_Location", "Prop_Location", "orgstructuredata"],
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
