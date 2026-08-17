<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("registration") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-sm">
        <div class="col">
          <q-card-section>
            <!-- login -->
            <q-input
              v-model="form.login"
              :model-value="form.login"
              autofocus
              type="text"
              :label="$t('login')"
              :rules="[(val) => loginTest(val) || $t('req')]"
              dense
            >
            </q-input>
            <!-- email -->
            <q-input
              dense
              v-model="form.email"
              :model-value="form.email"
              type="email"
              :label="$t('email')"
              :rules="[(val) => emailTest(val) || $t('req')]"
            >
            </q-input>
            <!-- passwd -->
            <q-input
              dense
              v-model="form.passwd"
              :model-value="form.passwd"
              :label="$t('passwd')"
              :type="isPwd ? 'password' : 'text'"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
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
              dense
              v-model="form.psw2"
              :model-value="form.psw2"
              :label="$t('confirmation')"
              :type="isPwd ? 'password' : 'text'"
              :rules="[(val) => pswTest(val) || $t('errorPassword')]"
            >
            </q-input>
          </q-card-section>
        </div>

        <div class="col">
          <q-card-section>
            <!-- name -->
            <q-input
              v-model="form.name"
              :model-value="form.name"
              type="text"
              :label="$t('usrName')"
              :rules="[(val) => val?.trim().length > 0 || $t('req')]"
              dense
            >
            </q-input>

            <!-- fullname -->
            <q-input
              v-model="form.fullName"
              :model-value="form.fullName"
              type="text"
              :label="$t('usrFullName')"
              :rules="[(val) => val?.trim().length > 0 || $t('req')]"
              dense
            >
            </q-input>

            <!-- phone -->
            <q-input
              dense
              clearable
              v-model="form.phone"
              :model-value="form.phone"
              unmasked-value
              :label="$t('phone')"
              prefix="+7"
              mask="### ### ####"
              fill-mask="_"
              @update:model-value="isValid"
            />
          </q-card-section>
        </div>
      </div>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="how_to_reg"
          :label="$t('registration')"
          @click="onOKClick"
          :disable="disableReg()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
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

<script setup>
import { ref, reactive, getCurrentInstance } from "vue";
import { api } from "@/boot/axios";

const props = defineProps({
  lg: String
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();

const dialog = ref(null);
const isPwd = ref(true);
const loading = ref(false);

const form = reactive({
  login: "",
  email: "",
  passwd: "",
  psw2: "",
  name: "",
  fullName: "",
  phone: "",
});

const isValid = () => {
  return form.phone?.length === 10;
};

const emailTest = (v) => {
  return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
    v
  );
};

const loginTest = (val) => {
  return !!val && !!val.trim();
};

const pswTest = (val) => {
  return val === form.passwd;
};

const disableReg = () => {
  return !(
    loginTest(form.login) &&
    loginTest(form.name) &&
    loginTest(form.fullName) &&
    emailTest(form.email) &&
    form.passwd && form.passwd === form.psw2 &&
    isValid()
  );
};

const show = () => {
  dialog.value?.show();
};
const hide = () => {
  dialog.value?.hide();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  loading.value = true;
  let err = false;

  api
    .post("", {
      method: "auth/regUser",
      params: [form],
    })
    .then(
      () => {
        emit("ok", { res: true });
      },
      (error) => {
        err = true;
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);

        console.error(msg);
      }
    )
    .finally(() => {
      loading.value = false;
      if (!err) hide();
    });
};

const onCancelClick = () => {
  hide();
};

defineExpose({
  show,
  hide
});
</script>
