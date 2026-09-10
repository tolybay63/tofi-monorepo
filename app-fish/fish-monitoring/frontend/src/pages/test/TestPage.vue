<template>
  <div class="q-pa-md q-gutter-sm">
    <h6 class="page-header text-weight-bolder">Заливка</h6>
    <hr style="margin-top: -50px" />
    <div class="q-pa-md q-gutter-sm q-pt-none">1. Вылов рыбы (Ссылочные данные)</div>
    <div>Формат:</div>
    <q-markup-table bordered class="row q-gutter-sm" style="border-color: #0f1010">
      <q-tr>
        <q-td> cls<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_StartDate<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_FishLocation<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_FishGear<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_AreaOfTon<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_1057</q-td>
        <q-td> Prop_1097</q-td>
        <q-td> Prop_1137</q-td>
        <q-td> Prop_1177</q-td>
        <q-td> Prop_1257</q-td>
        <q-td> Prop_1217</q-td>
        <q-td> Prop_1297</q-td>
        <q-td> Prop_1337</q-td>
        <q-td> Prop_1417</q-td>
        <q-td> Prop_1457</q-td>
        <q-td> Prop_1497</q-td>
        <q-td> Prop_1537</q-td>
        <q-td> Prop_1577</q-td>
        <q-td> Prop_1617</q-td>
        <q-td> Prop_1657</q-td>
        <q-td> Prop_1697</q-td>
        <q-td> Prop_1737</q-td>
      </q-tr>
    </q-markup-table>

    <div class="row">
      <div class="col-9">
        <q-input
          v-model="file"
          :clearable="true"
          accept=".xlsx"
          autofocus
          dense
          type="file"
          @clear="clrFile"
          @update:model-value="updFile"
        />
      </div>
      <q-space></q-space>
      <div class="col-1 text-right">
        <q-btn
          :disable="!file || (file && errTest)"
          color="grey-4"
          icon="file_download"
          label="Залить"
          text-color="black"
          @click="fnGo"
        />
      </div>
    </div>

    <div v-if="file" class="q-pa-md q-gutter-sm">
      <div v-if="!isFill">
        <div v-if="errTest">
          <div>
            Проверка формата: <span class="text-red"> {{ logs[0]["msg"] }} </span>
          </div>
          <div>Количество строк: {{ logs[0].count }}</div>
        </div>
        <div v-else>
          <div>Проверка формата: <span class="text-green"> Успешно </span></div>
          <div>Количество строк: {{ logs.length > 0 ? logs[0].count : '' }}</div>
          <div>Количество значений: {{ logs.length > 0 ? logs[0].countval : '' }}</div>
        </div>
      </div>
      <div v-else>
        <div v-if="errFill">
          <div>Заливка данных: <span class="text-red"> Ошибка </span></div>
        </div>
        <div v-else>
          <div>Заливка данных: <span class="text-green"> Успешно </span></div>
        </div>
      </div>
    </div>

    <!--  Meter  -->

    <hr style="margin-top: 50px" />
    <div class="q-pa-md q-gutter-sm q-pt-none">2. Вылов рыбы (Измеритель)</div>
    <div>Формат:</div>
    <q-markup-table bordered class="row q-gutter-sm" style="border-color: #0f1010">
      <q-tr>
        <q-td> owner <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> isObj <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> prop <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> periodType <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> dte </q-td>
        <q-td> value <span class="text-subtitle2 text-red">*</span></q-td>
      </q-tr>
    </q-markup-table>
    <div class="row">
      <div class="col-9">
        <q-input
          v-model="file2"
          :clearable="true"
          accept=".xlsx"
          autofocus
          dense
          type="file"
          @clear="clrFile2"
          @update:model-value="updFile2"
        />
      </div>
      <q-space></q-space>
<!--      :disable="!file2 || (file2 && errTest2) || (file2 && isFill2)"-->
      <div class="col-1 text-right">
        <q-btn
          :disable="!file2 || (file2 && errTest2)"
          color="grey-4"
          icon="file_download"
          label="Залить"
          text-color="black"
          @click="fnGo2"
        />
      </div>
    </div>
    <div v-if="file2" class="q-pa-md q-gutter-sm">
      <div v-if="!isFill2">
        <div v-if="errTest2">
          <div>
            Проверка формата: <span class="text-red"> {{ logs2[0]["msg"] }} </span>
          </div>
          <div>Количество строк: {{ logs2[0].count }}</div>
        </div>
        <div v-else>
          <div>Проверка формата: <span class="text-green"> Успешно </span></div>
          <div>Количество строк: {{ logs2.length > 0 ? logs2[0].count : '' }}</div>
          <div>Количество значений: {{ logs2.length > 0 ? logs2[0].countval : '' }}</div>
        </div>
      </div>
      <div v-else>
        <div v-if="errFill2">
          <div>Заливка данных: <span class="text-red"> Ошибка </span></div>
        </div>
        <div v-else>
          <div>Заливка данных: <span class="text-green"> Успешно </span></div>
        </div>
      </div>
    </div>

    <div>
      <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
    </div>

    <div>
      <q-inner-loading :showing="loading2" color="secondary"></q-inner-loading>
    </div>

  </div>
</template>

<script setup>
import {ref} from 'vue'
import {api, filldataURL} from "@/boot/axios.js";
import axios from "axios";

const file =  ref(null)
const logs = ref([])
const loading = ref(false)
const errTest = ref(false)
const errFill = ref(false)
const isFill = ref(false)
  //
const file2 =  ref(null)
const logs2 = ref([])
const loading2 = ref(false)
const errTest2 = ref(false)
const errFill2 = ref(false)
const isFill2 = ref(false)

const fnGo = () => {
  toSrv(true)
}

const fnGo2 = () => {
  toSrv2(true)
}

const toSrv = (fill) => {
  loading.value = true
  isFill.value = fill

  let fd = new FormData()
  fd.append('file', file.value)
  fd.append('filename', file.value.name)
  fd.append('fill', fill)
  fd.append('num', 1)

  axios
    .post(filldataURL, fd, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then(() => {
      if (fill) {
        errFill.value = false
      }
    })
    .then(() => {
      loading.value = true
      api
        .post('', {
          method: 'fill/loadLog',
          params: []
        })
        .then((response) => {
          logs.value = response.data.result.records
          errTest.value = logs.value[0].err === 1
          console.log("logs", logs.value)
        })
        .finally(() => {
          loading.value = false
        })
    })
    .catch(() => {
      if (fill) {
        errFill.value = true
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const toSrv2 = (fill) => {
  loading2.value = true
  isFill2.value = fill

  let fd = new FormData()
  fd.append('file', file2.value)
  fd.append('filename', file2.value.name)
  fd.append('fill', fill)
  fd.append('num', 2)
  axios
    .post(filldataURL, fd, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then(() => {
      if (fill) {
        errFill2.value = false
      }
    })
    .then(() => {
      loading2.value = true
      api
        .post('', {
          method: 'fill/loadLog',
          params: []
        })
        .then((response) => {
          logs2.value = response.data.result.records
          errTest2.value = logs2.value[0].err === 1
          console.log("logs2", logs2.value)
        })
        .finally(() => {
          loading2.value = false
        })
    })
    .catch(() => {
      if (fill) {
        errFill2.value = true
      }
    })
    .finally(() => {
      loading2.value = false
    })
}

const clrFile = () => {
  file.value = ref(null)
  errTest.value = false
  errFill.value = false
  logs.value = []
}

const clrFile2= () => {
  file2.value = ref(null)
  errTest2.value = false
  errFill2.value = false
  logs2.value = []
}

const updFile = (val) => {
  if (val !== null) {
    file.value = val[0]
    toSrv(false)
  }
}

const updFile2 = (val) => {
  if (val !== null) {
    file2.value = val[0]
    toSrv2(false)
  }
}

</script>

<style scoped></style>
