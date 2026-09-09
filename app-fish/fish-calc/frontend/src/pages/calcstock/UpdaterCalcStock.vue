<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    @show="onDialogShow"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"

  >
    <q-card class="q-dialog-plugin" style="width: 460px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("parent") }}: {{ parentName }}
        </q-item-section>

        <!-- Добавлен ref="inputNameRef" -->
        <q-input
          ref="inputNameRef"
          dense
          autofocus
          v-model="form['name']"
          :label="$t('nameCalc')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-mx-sm"
        />

        <div class="row">
          <!-- CalcStartYear -->
          <q-input
            v-model="form['CalcStartYear']"
            :model-value="form['CalcStartYear']"
            :label="fmReqLabel('CalcStartYear', true)"
            :rules="[(val) => (!!val && val.trim().length===4) || $t('req')]"
            class="q-ml-sm q-mr-lg" dense mask="####"
          />

          <q-space></q-space>

          <!-- CalcEndYear -->
          <q-input
            v-model="form['CalcEndYear']"
            :label="fmReqLabel('CalcEndYear', true)"
            :rules="[
            (val) => (!!val && val.trim().length===4) || $t('req'),
            () => checkYear() || 'Y1 > Y2'
            ]"
            class="q-ml-lg"  dense mask="####"
          />
        </div>

        <!-- Prop_ReservoirShore -->
        <q-select
          v-model="form.objReservoirShore"
          :label="fmReqLabel('reservoir', true)"
          :options="optReservoir"
          class="q-mx-sm"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterReservoir"
          @update:model-value="fnSelectReservoir"
        />
        <!-- Prop_CalcFishSpec -->
        <q-select
          class="q-ma-sm"
          v-model="form['fvCalcFishSpec']"
          dense
          options-dense
          :options="optCalcFishSpec"
          :label="fmReqLabel('CalcFishSpec', true)"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectCalcFishSpec"
        />

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          :loading="loading"
          @click="onOKClick"
          :disable="validName()"
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
import {notifySuccess} from "@/utils/jsutils";

const props = defineProps({
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: Object
});

const emit = defineEmits(["ok", "hide"]);
const {proxy} = getCurrentInstance();

const loading = ref(false);
const dialog = ref(null);
const inputNameRef = ref(null);
const form = reactive({...props.data});
const optReservoir = ref([])
const optReservoirOrg = ref([])
const optCalcFishSpec = ref([])

const fmReqLabel = (label, req) => {
  if (req)
    return proxy?.$t(label) + '*'
  else
    return proxy?.$t(label)
}

const checkYear = () => {
  if (!form['CalcStartYear'] || !form['CalcEndYear']) return true
  return form['CalcStartYear'] <= form['CalcEndYear']
}

const fnSelectCalcFishSpec = (v) => {
  if (v) {
    form.fvCalcFishSpec = v.id
    form.pvCalcFishSpec = v["pv"]
  }
}
const fnSelectReservoir = (v) => {
  if (v) {
    form.objReservoirShore = v.id
    form.pvReservoirShore = v["pv"]
  }
}

const filterReservoir = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optReservoir.value = optReservoirOrg.value
    })
    return
  }
  update(() => {
    if (optReservoirOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optReservoir.value = optReservoirOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
}

const validName = () => {
  let valid = !form["name"] || !form["fvCalcFishSpec"] || !form["objReservoirShore"];
  if (valid) return true
  if (!form["CalcStartYear"] || (form["CalcStartYear"] && form["CalcStartYear"].length !== 4)) return true;
  return !form["CalcEndYear"] || (form["CalcEndYear"] && form["CalcEndYear"].length !== 4);


};

const show = () => {
  dialog.value?.show();
};

const hide = () => {
  dialog.value?.hide();
};

const onDialogShow = () => {
  // Принудительно ставим фокус после полного открытия окна и закрытия q-menu
  inputNameRef.value?.focus();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertCalc" : "updateCalc";
  api
    .post("", {
      method: "data/" + method,
      params: [form],
    })
    .then(
      () => {
        emit("ok", {res: true});
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = error.response.data?.error?.message;
        console.error(msg);
      }
    )
    .finally(() => {
      hide();
    });
};

const onCancelClick = () => {
  hide();
};

defineExpose({
  show,
  hide
});

onMounted(async () => {
  console.info("onMounted upd", props.isChild, props.parentName);

  const resp1 = await api.post('', { method: 'data/loadReservoirs', params: ['Prop_ReservoirShore'] })
  optReservoir.value = resp1.data.result['records']
  optReservoirOrg.value = resp1.data.result['records']
  //
  const resp2 = await api.post('', { method: 'data/loadFVasStore', params: ['Prop_CalcFishSpec'] })
  optCalcFishSpec.value = resp2.data.result['records']



});
</script>
