<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-down"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin full-width">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("updateAccount") }}</div>
      </q-bar>

      <div class="q-col-gutter-y-md">
        <q-card-section>

          <!-- secondName -->
          <q-input
            v-model="form.UserSecondName"
            :label="$t('secondName')"
            :model-value="form.UserSecondName"
            :rules="[(val) => textTest(val) || $t('req')]"
            type="text"
          >
          </q-input>
          <!-- firstName -->
          <q-input
            v-model="form.UserFirstName"
            :label="$t('firstName')"
            :model-value="form.UserFirstName"
            :rules="[(val) => textTest(val) || $t('req')]"
            type="text"
          >
          </q-input>
          <!-- middleName -->
          <q-input
            v-model="form.UserMiddleName"
            :label="$t('middleName')"
            :model-value="form.UserMiddleName"
            type="text"
          >
          </q-input>
          <!-- sex -->
          <q-select
            v-model="sex"
            :label="$t('sex')"
            :model-value="sex"
            :options="optSex"
            dense
            map-options
            option-label="name"
            option-value="id" options-dense
            @update:model-value="fnSelectSex()"
          />
          <!-- Birth Day-->
          <q-input
            v-model="form['UserDateBirth']" :label="$t('birthDay')"
            :model-value="form['UserDateBirth']" class="q-mt-md"
            clearable dense stack-label type="date"
          >
          </q-input>

          <!-- email -->
          <q-input
            v-model="form.UserEmail"
            :label="$t('email')"
            :model-value="form.UserEmail"
            :rules="[(val) => emailTest(val) || $t('req')]"
            type="email"
          >
          </q-input>

          <!-- phone -->
          <q-input
            v-model="form.UserPhone"
            :label="$t('phone')"
            :model-value="form.UserPhone"
            :rules="[(val) => val.length > 9 || $t('req')]"
            clearable dense
            fill-mask="_"
            mask="### ### ####"
            prefix="+7"
            unmasked-value
          />

        </q-card-section>
      </div>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          :loading="loading"
          color="primary"
          icon="save"
          @click="onOKClick"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
        <q-btn
          :label="$t('cancel')"
          color="primary"
          icon="cancel"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>


import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";
import {useUserStore} from "stores/user-store";


export default {
  props: ["lg", "userId"],

  data() {
    return {
      //
      form: {UserId: parseInt(this.userId, 10)},
      loading: ref(false),
      //
      sex: null,
      optSex: [],
    };
  },

  emits: ["ok", "hide"],

  methods: {

    fnSelectSex() {
      this.form.UserSex = this.sex.id
    },

    validSave() {
      return !(
        this.textTest(this.form.UserFirstName) &&
        this.textTest(this.form.UserSecondName) &&
        this.form.UserSex && this.emailTest(this.form.UserEmail) &&
        this.form.UserPhone.length > 9
      )

    },

    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
        v
      );
    },

    textTest(val) {
      return !!val && !!val.trim();
    },

    show() {
      this.$refs.dialog.show();
    },
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      this.$emit("hide");
    },

    onOKClick() {
      const store = useUserStore();
      const {setUserName} = store;

      //console.log("Form", this.form);

      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "entity/updateProfile",
          params: [this.form],
        })
        .then(
          () => {
            let nm = this.form.UserSecondName + " " + this.form.UserFirstName + " " + this.form.UserMiddleName
            setUserName(nm)

          },
          (error) => {
            let msgs = error.response.data.error.message.split("@")
            let msg = this.$t(`${msgs[0]}`) + (msgs.size>1) ? " ["+msgs[1]+"]" : ""
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false)
          this.hide()
        });
    },

    onCancelClick() {
      this.hide();
    },
  },

  created() {
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "data/loadProfile",
        params: [this.userId],
      })
      .then(
        (response) => {
          this.form = response.data.result.records[0]
          this.sex = this.form.UserSex
        },
        (error) => {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length>1) ? " ["+msgs[1]+"]" : ""
          let msg = m1 + m2
          notifyError(msg);
          //notifyError(error.message);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      });
    //
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "meta/loadFactorSexValues",
        params: [],
      })
      .then(
        (response) => {
          this.optSex = response.data.result.records;
        },
        (error) => {
          let msgs = error.response.data.error.message.split("@")
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = (msgs.length>1) ? " ["+msgs[1]+"]" : ""
          let msg = m1 + m2
          notifyError(msg);
        }
      )
      .finally(() => {
        this.loading = ref(false);
      });


  },
};
</script>
