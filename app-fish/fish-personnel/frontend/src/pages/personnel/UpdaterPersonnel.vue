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

      <q-card-section>
        <!-- UserSecondName -->
        <q-input
          :model-value="form.UserSecondName"
          v-model="form.UserSecondName"
          autofocus dense
          :label="fnLabel('UserSecondName', true)"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- UserFirstName-->
        <q-input
          :model-value="form.UserFirstName"
          v-model="form.UserFirstName"
          :label="fnLabel('UserFirstName', true)"
          dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- UserMiddleName-->
        <q-input
          :model-value="form.UserMiddleName"
          v-model="form.UserMiddleName"
          :label="fnLabel('UserMiddleName', false)"
          dense
        >
        </q-input>
        <!-- UserSex -->
        <q-select
          v-model="form.fvUserSex"
          :label="fnLabel('UserSex', true)"
          :options="optUserSex"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserSex"
          @update:model-value="fnSelectUserSex"
        />
        <!-- UserPosition -->
        <q-select
          v-model="form.fvUserPosition"
          :label="fnLabel('UserPosition', true)"
          :options="optUserPosition"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserPosition"
          @update:model-value="fnSelectUserPosition"
        />
        <!-- UserOrg -->
        <q-select
          v-model="form.objUserOrg"
          :label="fnLabel('UserOrg', true)"
          :options="optUserOrganization"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserOrg"
          @update:model-value="fnSelectUserOrg"
        />

        <!-- UserDateBirth  -->
        <q-input
          v-model="form.UserDateBirth"
          :label="fnLabel('UserDateBirth', false)"
          stack-label
          type="date"
          @update:model-value="fnSelectUserDateBirth"
        />
        <!-- UserEmail -->

        <q-input
          v-model="form.UserEmail"
          type="email"
          :label="fnLabel('UserEmail', false)"
          :rules="[val => emailTest(val) || 'Ошибка формата']"
        >
        </q-input>

        <!-- UserPhone -->
        <q-input
          v-model="form.UserPhone"
          unmasked-value
          :label="fnLabel('UserPhone', false)"
          prefix="+7"
          mask="### ### ####"
          fill-mask="_"
          bottom-slots
          @update:model-value="isValidPhone"
        >
          <template v-slot:error>
            Please use 10 characters.
          </template>
        </q-input>
        <!-- UserId -->
        <q-select
          v-model="form.UserId"
          :label="fnLabel('UserId', false)"
          :options="optUserId"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserId"
          @update:model-value="fnSelectUserId"
        />


        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
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
import {api, } from "../../boot/axios";
import {notifyError, notifySuccess} from "../../utils/jsutils";

export default {
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      optUserSex: [],
      optUserSexOrg: [],
      optUserPosition: [],
      optUserPositionOrg: [],
      optUserOrganization: [],
      optUserOrganizationOrg: [],
      optUserId: [],
      optUserIdOrg: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnLabel(txt, req) {
      if (req)
        return this.$t(txt) + "*";
      else
        return this.$t(txt);
    },

    fnSelectUserDateBirth(v) {
      console.log("UserDateBirth", v)
      if (v.length === 10 && date.formatDate(v).isWellFormed()) {
        this.UserDateBirth = v
      }

    },

    fnSelectUserSex(v) {
      this.form.fvUserSex = v.id
      this.form.pvUserSex = v.pv
    },

    filterUserSex(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optUserSex = this.optUserSexOrg
        })
        return
      }
      update(() => {
        if (this.optUserSexOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optUserSex = this.optUserSexOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectUserPosition(v) {
      this.form.fvUserPosition = v.id
      this.form.pvUserPosition = v.pv
    },

    filterUserPosition(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optUserPosition = this.optUserPositionOrg
        })
        return
      }
      update(() => {
        if (this.optUserPositionOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optUserPosition = this.optUserPositionOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectUserOrg(v) {
      this.form.objUserOrg = v.id
      this.form.pvUserOrg = v.pv
    },

    filterUserOrg(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optUserOrganization = this.optUserOrganizationOrg
        })
        return
      }
      update(() => {
        if (this.optUserOrganizationOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optUserOrganization = this.optUserOrganizationOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectUserId(v) {
      this.form.UserId = v.id
    },

    filterUserId(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optUserId = this.optUserIdOrg
        })
        return
      }
      update(() => {
        if (this.optUserIdOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optUserId = this.optUserIdOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },


    emailTest: function (v) {
      if (!v)
        return true
      else
        return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(v);
    },

    isValidPhone: function (v) {
      return this.form.UserPhone.length === 10
    },

    validSave() {
      return !this.form.UserSecondName || !this.form.UserFirstName || !this.form.fvUserSex ||
        !this.form.fvUserPosition || !this.form.objUserOrg;
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

      api
        .post("", {
           method: "data/savePersonnel",
          params: [this.mode, this.form ],
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
    this.loading = true
    api
      .post('', {
        method: 'data/selectFV',
        params: ['Prop_UserSex'],
      })
      .then(
        (response) => {
          this.optUserSex = response.data.result.records
          this.optUserSexOrg = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/selectFV',
        params: ['Prop_UserPosition'],
      })
      .then(
        (response) => {
          this.optUserPosition = response.data.result.records
          this.optUserPositionOrg = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/selectObj',
        params: ['Prop_UserOrg'],
      })
      .then(
        (response) => {
          this.optUserOrganization = response.data.result.records
          this.optUserOrganizationOrg = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/selectUser',
        params: [],
      })
      .then(
        (response) => {
          console.log("UserId", response.data.result.records);
          this.optUserId = response.data.result.records
          this.optUserIdOrg = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    console.info("data", this.data)



  },
};
</script>
