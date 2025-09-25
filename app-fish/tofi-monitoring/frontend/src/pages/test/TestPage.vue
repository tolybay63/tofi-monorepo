<template>
  <div class="q-pa-md q-gutter-sm">
    <h6 class="page-header text-weight-bolder">Заливка</h6>
    <hr style="margin-top: -50px" />
    <div class="q-pa-md q-gutter-sm q-pt-none">1. Водные объекты и их свойства (Ссылочные данные)</div>
    <div>Формат:</div>
    <q-markup-table bordered class="row q-gutter-sm" style="border-color: #0f1010">
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

    <!--  Meter  -->

    <hr style="margin-top: 50px" />
    <div class="q-pa-md q-gutter-sm q-pt-none">2. Водные объекты и их свойства (Измеритель)</div>
    <div>Формат:</div>
    <q-markup-table bordered class="row q-gutter-sm" style="border-color: #0f1010">
      <q-tr>
        <q-td> owner <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> isObj <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> prop <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> status</q-td>
        <q-td> provider</q-td>
        <q-td> periodType <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> dbeg <span class="text-subtitle2 text-red">*</span></q-td>
        <q-td> dend <span class="text-subtitle2 text-red">*</span></q-td>
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
      <div class="col-1 text-right">
        <q-btn
          :disable="!file2 || (file2 && errTest2) || (file2 && isFill2)"
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
            Проверка формата: <span class="text-red"> Заголовок файла не соответствует шаблону </span>
          </div>
          <div>Количество строк: {{ logs2.cnt }}</div>
        </div>
        <div v-else>
          <div>Проверка формата: <span class="text-green"> Успешно </span></div>
          <div>Количество строк: {{ logs2.cnt }}</div>
        </div>
      </div>
      <div v-else>
        <div>
          <div>Заливка данных: <span class="text-black"> {{cnt2}} </span></div>
        </div>

        <div v-if="logs2Err.length > 0" class="text-black">
          <div>Пропущенные строки:</div>

          <div v-for="log in logs2Err" :key="log.index" class="text-red" >
            <div>{{log}}</div>
          </div>

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
      //
      file2: ref(null),
      loading2: false,
      errTest2: false,
      errFill2: false,
      isFill2: false,
      cnt2: "",
      logs2: "",
      logs2Err: []
    }
  },

  methods: {
    fnGo() {
      this.toSrv(true)
    },

    fnGo2() {
      this.toSrv2(true)
    },

    clrFile() {
      this.file = ref(null)
      this.errTest = false
      this.errFill = false
      this.logs = []
    },

    clrFile2() {
      this.file2 = ref(null)
      this.errTest2 = false
      this.errFill2 = false
      this.logs2 = ""
      this.logs2Err = []
    },

    updFile(val) {
      if (val !== null) {
        this.file = val[0]
        this.toSrv(false)
      }
    },

    updFile2(val) {
      if (val !== null) {
        this.file2 = val[0]
        this.toSrv2()
      }
    },

    toSrv(fill) {
      this.loading = true
      this.isFill = fill

      let fd = new FormData()
      fd.append('file', this.file)
      fd.append('filename', this.file.name)
      fd.append('fill', fill)
      fd.append('num', 1)

      this.$axios
        .post('/filldata', fd, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
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
              params: []
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

    toSrv2(fill) {
      this.loading2 = true
      this.isFill2 = fill

      let fd = new FormData()
      fd.append('file', this.file2)
      fd.append('filename', this.file2.name)
      fd.append('fill', fill)
      fd.append('num', 2)
      this.$axios
        .post('/filldata', fd, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        .then(() => {
          if (fill) {
            this.errFill2 = false
          }
        })
        .then(() => {
          this.loading2 = true
          this.$axios
            .post(baseURL, {
              method: 'test/loadLog',
              params: []
            })
            .then((response) => {
              this.logs2 = response.data.result.records[0]
              this.errFill2 = this.logs2.err === 1
              if (fill) {
                let arr = this.logs2["msg"].split("@")
                this.cnt2 = arr[0]
                this.logs2Err = arr[1].split(";")

                console.info("fill", arr)
              } else {
                this.errTest2 = this.logs2.err === 1
              }
            })
            .finally(() => {
              this.loading2 = false
            })
        })
        .catch(() => {
          if (fill) {
            this.errFill2 = true
          }
        })
        .finally(() => {
          this.loading2 = false
        })
    },


  }
}
</script>

<style scoped></style>
