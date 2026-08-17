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
        <div>{{ $t("myProfile") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-md">
        <div class="col">
          <q-card-section>
            <!-- email -->
            <q-input
              v-model="form.email"
              :model-value="form.email"
              type="email"
              :label="$t('email')"
              autofocus
              :rules="[(val) => emailTest(val) || $t('req')]"
              @change="onChangeEmil"
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
              :rules="[(val) => val?.length > 9 || $t('req')]"
              @change="onChangePhone"
              @update:model-value="isValid"
            />

            <q-toggle
              v-model="chPsw"
              :label="$t('changePsw')"
              :model-value="chPsw"
            />
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
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeNm"
              @blur="onBlur"
            >
            </q-input>

            <!-- fullName -->
            <q-input
              v-model="form.fullName"
              :model-value="form.fullName"
              type="text"
              :label="$t('usrFullName')"
              :rules="[(val) => textTest(val) || $t('req')]"
              @change="onChangeFnm"
            >
            </q-input>
          </q-card-section>
        </div>
      </div>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="save"
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
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api, authURL } from "@/boot/axios.js";
import { notifyError } from "../utils/jsutils.js";
import UpdaterPsw from "@/components/UpdaterPsw.vue";
import { useUserStore } from "@/stores/user-store.js";

const props = defineProps({
  lg: String,
  userId: [String, Number]
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();
const $q = useQuasar();
const router = useRouter();

const dialog = ref(null);
const form = reactive({});
const form2 = reactive({});
const newForm = reactive({});
const loading = ref(false);
const chPsw = ref(false);

const isValid = () => {
  return form.phone?.length !== 10;
};

const onBlur = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (
      !form.fullName ||
      (form.fullName && form.fullName.trim() === "")
    ) {
      form.fullName = form.name;
    }
  }
};

const validSave = () => {
  return !(
    chPsw.value ||
    !(
      isChanged() &&
      form.phone &&
      form.phone.length > 9 &&
      emailTest(form.email) &&
      textTest(form.name) &&
      textTest(form.fullName)
    )
  );
};

const isChanged = () => {
  return !(
    form.email !== form2.email ||
    form.name !== form2.name ||
    form.fullName !== form2.fullName ||
    form.phone !== form2.phone
  );
};

const onChangeEmil = () => {
  newForm.email = form.email;
};
const onChangePhone = () => {
  newForm.phone = form.phone;
};
const onChangeNm = () => {
  newForm.name = form.name;
};
const onChangeFnm = () => {
  newForm.fullName = form.fullName;
};

const emailTest = (v) => {
  return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
    v
  );
};

const textTest = (val) => {
  return !!val && !!val.trim();
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
  const store = useUserStore();
  const { setUserName } = store;

  newForm.id = props.userId;

  loading.value = true;
  api
    .post("", {
      method: "auth/saveProfile",
      params: [newForm],
    })
    .then(
      () => {
        setUserName(form.fullName);
        if (chPsw.value) {
          api
            .post(authURL + "/logout", {
              params: {},
            })
            .then(() => {
              store.setUserStore({});
              router.push("/");
            });

          $q.dialog({
            component: UpdaterPsw,
            componentProps: {
              id: newForm.id,
            },
          })
            .onOk((r) => {
              if (r.res) {
                console.log("Ok! Psw changed!");
                hide();
              }
            });
        } else {
          setUserName(form.fullName);
          hide();
        }
      },
      (error) => {
        notifyError(error.messages);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const onCancelClick = () => {
  hide();
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "auth/loadProfile",
      params: [props.userId],
    })
    .then(
      (response) => {
        const data = response.data.result.records[0];
        Object.assign(form, data);
        Object.assign(form2, data);
      },
      (error) => {
        hide()
        //notifyError(error.messages);
      }
    )
    .finally(() => {
      loading.value = false;
    });
});

defineExpose({
  show,
  hide
});
</script>
