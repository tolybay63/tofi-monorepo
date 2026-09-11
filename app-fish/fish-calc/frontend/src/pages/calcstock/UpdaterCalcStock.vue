<template>
  <q-dialog
    ref="dialog"
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
    @show="onDialogShow"

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
          v-model="form['name']"
          :label="$t('nameCalc')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          autofocus
          class="q-mx-sm"
          dense
        />

        <div v-if="mode==='ins'">

          <div class="row">
            <!-- CalcStartYear -->
            <q-input
              v-model="form['CalcStartYear']"
              :label="fmReqLabel('CalcStartYear', true)"
              :model-value="form['CalcStartYear']"
              :rules="[
                () => isChild ? checkYear1() || 'Y1 > Y2 || Y1 < YP1' : checkYear1P() || 'Y1 > Y2'
               ]"
              class="q-ml-sm q-mr-lg" dense mask="####"
            />

            <q-space/>

            <!-- CalcEndYear -->
            <q-input
              v-model="form['CalcEndYear']"
              :label="fmReqLabel('CalcEndYear', true)"
              :rules="[
                () => isChild ? checkYear2() || 'Y1 > Y2 || Y2 > YP2' : checkYear2P() || 'Y1 > Y2'
              ]"
              class="q-ml-lg" dense mask="####"
            />
          </div>

          <div v-if="!isChild">
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
              v-model="form['fvCalcFishSpec']"
              :label="fmReqLabel('CalcFishSpec', true)"
              :options="optCalcFishSpec"
              class="q-ma-sm"
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @update:model-value="fnSelectCalcFishSpec"
            />
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validName()"
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
const y1P = props.data["CalcStartYear"];
const y2P = props.data["CalcEndYear"];
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

/*
const checkYear1 = () => {
  if (!form['CalcStartYear'] || !form['CalcEndYear']) return true
  return form['CalcStartYear'] <= form['CalcEndYear'] && form['CalcStartYear'] >= y1P
}
*/

const checkYear1 = () => {
  return (form["CalcStartYear"] && form["CalcStartYear"].length === 4 && form['CalcStartYear'] <= form['CalcEndYear'] && form['CalcStartYear'] >= y1P )
}

const checkYear2 = () => {
  return (form["CalcEndYear"] && form["CalcEndYear"].length === 4 && form['CalcStartYear'] <= form['CalcEndYear'] && form['CalcEndYear'] <= y2P )
}

const checkYear1P = () => {
  return (form["CalcStartYear"] && form["CalcStartYear"].length === 4 && (!form['CalcEndYear'] || (form['CalcEndYear'] && form['CalcStartYear'] <= form['CalcEndYear'])))
}

const checkYear2P = () => {
  return (form["CalcEndYear"] && form["CalcEndYear"].length === 4 && form['CalcStartYear'] <= form['CalcEndYear'])
}

/*const checkYear2 = () => {
  if (!form['CalcStartYear'] || !form['CalcEndYear']) return true
  return form['CalcStartYear'] <= form['CalcEndYear'] && form['CalcEndYear'] <= y2P
}*/

const validName = () => {
  console.info(form)

  if ( !form["name"])
    return true

  if (props.mode==="ins") {
    if (props.isChild) {
      return !checkYear1() || !checkYear2();
    } else {
      return !form["fvCalcFishSpec"] || !form["objReservoirShore"] || !checkYear1P() || !checkYear2P();
    }
  } else {
    return form["name"] === "";
  }
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
  if (!props.isChild) {
    if (props.mode === "ins") {
      const resp1 = await api.post('', {method: 'data/loadReservoirs', params: ['Prop_ReservoirShore']})
      optReservoir.value = resp1.data.result['records']
      optReservoirOrg.value = resp1.data.result['records']
      //
      const resp2 = await api.post('', {method: 'data/loadFVasStore', params: ['Prop_CalcFishSpec']})
      optCalcFishSpec.value = resp2.data.result['records']
    }
  } else {
    console.info("is Child", props.data);
  }
});
</script>
