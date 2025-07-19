<template>
  <div class="q-pa-md q-gutter-sm">
    <h6 class="page-header text-weight-bolder">Заливка</h6>
    <hr style="margin-top: -50px" />
    <div class="q-pa-md q-gutter-sm q-pt-none">1. Водные объекты и их свойства</div>
    <div>Формат:</div>
    <q-markup-table class="row q-gutter-sm" bordered style="border-color: #0f1010">
      <q-tr>
        <q-td> cls<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> name<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_Region<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_District</q-td>
        <q-td> Prop_Branch<span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> Prop_WaterArea</q-td>
        <q-td> Prop_ReservoirType</q-td>
        <q-td> Prop_Location</q-td>
        <q-td> Prop_FishFarmingType</q-td>
      </q-tr>
    </q-markup-table>

    <div class="row">
      <div class="col-9">
        <q-input
          autofocus
          dense
          type="file"
          v-model="file"
          :clearable="true"
          @update:model-value="updFile"
          @clear="clrFile"
          accept=".xlsx"
        />
      </div>
      <q-space></q-space>
      <div class="col-1 text-right">
        <q-btn
          icon="file_download"
          label="Залить"
          color="grey-4"
          text-color="black"
          :disable="!file || (file && errTest)"
          @click="fnGo"
        />
      </div>
    </div>

    <div v-if="file" class="q-pa-md q-gutter-sm">
      <div v-if="!isFill">
        <div v-if="errTest">
          <div>
            Проверка формата: <span class="text-red"> {{ logs[0].msg }} </span>
          </div>
          <div>Количество строк: {{ logs[0].cnt }}</div>
        </div>
        <div v-else>
          <div>Проверка формата: <span class="text-green"> Успешно </span></div>
          <div>Количество строк: {{ logs.length > 0 ? logs[0].cnt : '' }}</div>
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

    <div>
      <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { baseURL } from 'boot/axios'

export default {
  name: 'TestPage',

  data() {
    return {
      file: ref(null),
      loading: false,
      logs: [],

      errTest: false,
      errFill: false,
      isFill: false,
    }
  },

  methods: {
    fnGo() {
      this.toSrv(true)
    },

    clrFile() {
      this.file = ref(null)
      this.errTest = false
      this.errFill = false
      this.logs = []
    },

    updFile(val) {
      if (val !== null) {
        this.file = val[0]
        this.toSrv(false)
      }
    },

    toSrv(fill) {
      this.loading = true
      this.isFill = fill

      let fd = new FormData()
      fd.append('file', this.file)
      fd.append('filename', this.file.name)
      fd.append('fill', fill)

      this.$axios
        .post('/filldata', fd, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        })
        .then(() => {
          if (fill) {
            this.errFill = false
          }
        })
        .then(() => {
          this.loading = true
          this.$axios
            .post(baseURL, {
              method: 'test/loadLog',
              params: [],
            })
            .then((response) => {
              this.logs = response.data.result.records
              this.errTest = this.logs[0].err === 1
            })
            .finally(() => {
              this.loading = false
            })
        })
        .catch(() => {
          if (fill) {
            this.errFill = true
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>

<style scoped></style>
