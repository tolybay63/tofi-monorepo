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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-sm">
        <div class="col">
          <q-card-section>
            <q-input
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              autofocus
              dense
              v-model="form.login"
              :model-value="form.login"
              type="text"
              :label="$t('login')"
              :rules="[(val) => loginTest(val) || $t('req')]"
            >
            </q-input>
            <q-input
              dense
              v-model="form.email"
              :model-value="form.email"
              type="email"
              :label="$t('email')"
              :rules="[(val) => emailTest(val) || $t('req')]"
            >
            </q-input>
            <q-input
              v-show="mode === 'ins'"
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
            <q-input
              v-show="mode === 'ins'"
              dense
              v-model="form.psw2"
              :model-value="form.psw2"
              :label="$t('confirmation')"
              :type="isPwd ? 'password' : 'text'"
              :rules="[(val) => pswTest(val) || $t('errorPassword')]"
            >
            </q-input>

            <q-select
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              dense
              options-dense
              :model-value="al"
              @update:model-value="fnSelect()"
            />
          </q-card-section>
        </div>

        <div class="col">
          <q-card-section>
            <q-input
              v-model="form.name"
              :model-value="form.name"
              type="text"
              :label="$t('usrName')"
              :rules="[(val) => !!val || $t('req')]"
              @blur="onBlurName"
              dense
            >
            </q-input>

            <q-input
              v-model="form.fullName"
              :model-value="form.fullName"
              type="text"
              :label="$t('usrFullName')"
              :rules="[(val) => !!val || $t('req')]"
              dense
            >
            </q-input>

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
            />

            <q-toggle
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              class="q-mt-lg"
              dense
              :model-value="form.locked"
              v-model="form.locked"
              :label="$t('locked')"
            />
          </q-card-section>
        </div>
      </div>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="how_to_reg"
          :label="$t('save')"
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
import {getCurrentInstance, onMounted, reactive, ref} from "vue";
import {api} from "@/boot/axios";

const props = defineProps({
  rec: Object,
  mode: String,
  dense: Boolean
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();

const dialog = ref(null);
const form = reactive({ ...props.rec });
const isPwd = ref(true);
const loading = ref(false);
const optionsLevel = ref([]);
const al = ref(props.rec?.accessLevel === 0 ? 1 : props.rec?.accessLevel);

const fnSelect = () => {
  form.accessLevel = al.value?.id !== undefined ? al.value.id : al.value;
};

const onBlurName = () => {
  form.name = form.name ? form.name.trim() : null;
  if (form.name) {
    if (form.fullName == null || form.fullName === "")
      form.fullName = form.name.trim();
  }
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
  if (props.mode === "ins")
    return !(
      loginTest(form.login) && emailTest(form.email) &&
      loginTest(form.name) && loginTest(form.fullName) &&
      form.passwd && form.passwd === form.psw2
    );
  else {
    return !(loginTest(form.login) && emailTest(form.email) &&
      loginTest(form.name) && loginTest(form.fullName)
    );
  }
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
  let method = props.mode === "ins" ? "insert" : "update";
  let err = false;

  api
    .post("", {
      method: "usr/" + method,
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
        console.log(msg);
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

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "dict/loadDictAsStore",
      params: ["FD_AccessLevel"],
    })
    .then((response) => {
      optionsLevel.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
});

defineExpose({
  show,
  hide
});
</script>
