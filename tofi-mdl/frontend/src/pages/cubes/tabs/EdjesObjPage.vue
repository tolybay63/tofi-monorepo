<template>
  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[20, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
    >

      <template v-slot:before>
        <div style="height: calc(100vh - 310px); width: 100%">

          <div>
            <span class="q-ma-sm text-blue">Граны:</span>
            <q-radio v-model="face" checked-icon="task_alt" unchecked-icon="panorama_fish_eye" val="face2"
                     label="2-х мерные" @update:model-value="updFace"/>
            <q-radio v-if="dimCount > 2" v-model="face" checked-icon="task_alt" unchecked-icon="panorama_fish_eye"
                     val="face3" label="3-х мерные" @update:model-value="updFace"/>
            <q-radio v-if="dimCount > 3" v-model="face" checked-icon="task_alt" unchecked-icon="panorama_fish_eye"
                     val="face4" label="4-х мерные" @update:model-value="updFace"/>
          </div>
          <div class="q-ma-sm">

            <!-- DoX -->
            <q-select
              autofocus
              readonly
              v-model="doX"
              :model-value="doX"
              :options="optDoX"
              label="Измерение по строкам"
              option-value="id"
              option-label="name"
              map-options dense options-dense
              class="q-mb-md"
            />
            <!-- DoY -->
            <q-select
              readonly
              v-model="doY"
              :model-value="doY"
              :options="optDoY"
              label="Измерение по столбцам"
              option-value="id"
              option-label="name"
              map-options dense options-dense
              class="q-mb-md"
            />
          </div>

          <div style="text-align: center">
            <q-btn label="<" class="q-mx-lg" @click="fnPrev()" :disable="fnPrevDisable()"></q-btn>
            <span class="q-mx-md" v-if="face==='face2'"> {{ face2 }} из {{ face2count }}</span>
            <span class="q-mx-md" v-if="face==='face3'"> {{ face3 }} из {{ face3count }}</span>
            <span class="q-mx-md" v-if="face==='face4'"> {{ face4 }} из {{ face4count }}</span>
            <q-btn label=">" class="q-mx-lg" @click="fnNext()" :disable="fnNextDisable()"></q-btn>
          </div>

          <!--  fixed dims  -->
          <div class="q-ma-md text-center text-blue" v-if="(face==='face3' || face==='face4') && dimCount>=3">
            Фиксированные измерения
            <hr/>
            <!-- Fixed 1 -->
            <div v-if="(face==='face3' && dimCount===3) || ((face==='face4' || face==='face3') && dimCount===4)">
              <q-select
                v-model="cl3"
                :model-value="cl3"
                :options="optDOI3"
                :label="labelDOI3"
                option-value="id"
                option-label="name"
                map-options dense options-dense
                class="q-mb-md"
                @update:model-value="fnSelectDOI3"
              />
            </div>
            <!-- Fixed 2 -->
            <div v-if="face==='face4' && dimCount===4">
              <q-select
                v-model="cl4"
                :model-value="cl4"
                :options="optDOI4"
                :label="labelDOI4"
                option-value="id"
                option-label="name"
                map-options dense options-dense
                class="q-mb-md"
                @update:model-value="fnSelectDOI4"
              />
            </div>
          </div>
          <!--  Show Grid  -->
          <q-toggle
            v-model="fixedDim"
            color="green"
            label="Показать"
            :disable="validAll()"
            @update:model-value="updFixedDim"
          />


        </div>


      </template>

      <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>


      <template v-slot:after>
        <div v-if="!showGrid">
          <h3> Грани прокуба объектов</h3>
        </div>
        <div v-else>
          <edjes-obj :cols="cols" :rows="rows" :cubeSFace="cubeSFace" :do1="doX" :do2="doY"
                     :optRC="optRC" :dimCount="dimCount" :doi3="doi3" :doi4="doi4" :cl3="cl3" :cl4="cl4" :face="face"
                     @updateDelete="onUpdateDelete" @updateBind="onUpdateBind"
          />

        </div>

      </template>


    </q-splitter>


  </div>
</template>

<script>
import {api, baseURL} from "boot/axios.js";
import {ref} from "vue";
import EdjesProp from "pages/cubes/tabs/prop/EdjesProp.vue";
import EdjesObj from "pages/cubes/tabs/obj/EdjesObj.vue";
import {notifyError, notifySuccess} from "src/utils/jsutils.js";

export default {
  name: "EdjesObjPage",
  components: {EdjesObj, EdjesProp},
  props: ["cubeS", "dOrg"],

  data() {
    return {
      loading: false,
      splitterModel: 30,
      cols: [],
      rows: [],
      dimCount: 2,
      face2: 1,
      face2count: 1,
      face3: 1,
      face3count: 0,
      face4: 1,
      face4count: 0,
      face: ref("face2"),

      doX: null,
      doY: null,
      optDimObj: [],
      optDoX: [],
      optDoY: [],

      cl3: null,
      cl4: null,

      doi3: null,
      optDOI3: [],
      optDOI3org: [],
      labelDOI3: "Измерение 3",
      labelDOI4: "Измерение 4",

      doi4: null,
      optDOI4: [],
      optDOI4org: [],
      fixedDim: ref(false),
      showGrid: ref(false),
      optRC: [],
      cubeSFace: 0

    }
  },

  methods: {
    fnSelectDOI3(v) {
      //console.log("fnSelectDOI3", v)
      this.updFace(this.face)
      this.cl3 = v.id
      this.doi3 = v.doi
    },

    fnSelectDOI4(v) {
      //console.log("fnSelectDOI4", v)
      this.updFace(this.face)
      this.cl4 = v.id
      this.doi4 = v.doi
    },

    onUpdateDelete(data) {
      //console.log("onUpdateDelete", data)
      this.fixedDim = false
      this.rows = []
      setTimeout(() => {
        this.fixedDim = true
        this.updFixedDim(this.fixedDim)
      }, 100)
    },

    onUpdateBind() {
      //console.log("onUpdateBind")
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/bindingDO",
          params: [this.cubeSFace, this.dimCount, this.doX, this.doY, this.doi3 ? this.doi3 : 0, this.doi4 ? this.doi4 : 0,
            this.cl3 ? this.cl3 : 0, this.cl4 ? this.cl4 : 0],
        })
        .then(() => {
          notifySuccess(this.$t("success"))
        })
        .then(() => {
          this.fixedDim = false
          setTimeout(() => {
            this.fixedDim = true
            this.updFixedDim(this.fixedDim)
          }, 100)
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
          console.error(msg)
        })
        .finally(() => {
          this.loading = false
        })
    },

    validAll() {
      if (this.dimCount === 2) {
        if (this.doX && this.doY)
          return false
      }
      if (this.dimCount === 3) {
        if (this.face === "face2" && this.doX && this.doY)
          return false
        if (this.face === "face3" && this.doX && this.doY && this.doi3)
          return false
      }
      if (this.dimCount === 4) {
        if ((this.face === "face2" && this.doX && this.doY) ||
          (this.face === "face2" && this.doX && this.doi3) ||
          (this.face === "face2" && this.doX && this.doi4) ||
          (this.face === "face2" && this.doY && this.doi3) ||
          (this.face === "face2" && this.doY && this.doi4) ||
          (this.face === "face2" && this.doi3 && this.doi4))
          return false
        if ((this.face === "face3" && this.doX && this.doY && this.doi3) ||
          (this.face === "face3" && this.doX && this.doY && this.doi4) ||
          (this.face === "face3" && this.doX && this.doi3 && this.doi4) ||
          (this.face === "face3" && this.doY && this.doi3 && this.doi4))
          return false
        if ((this.face === "face4" && this.doX && this.doY && this.doi3 && this.doi4))
          return false
      }

      return true
    },

    loadGridDO() {
      if (!this.fixedDim)
        return
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/loadGridDO",
          params: [this.cubeS, this.dimCount, this.doX, this.doY, this.cl3 ? this.cl3 : 0, this.cl4 ? this.cl4 : 0],
        })
        .then((response) => {
          //console.info(`response`, response.data.result)

          this.cols = response.data.result.cols
          this.rows = response.data.result.rows.records

          for (let key in response.data.result.sel) {
            this.optRC.push(key)
            this.optRC[key] = []
            response.data.result.sel[key].records.forEach((item) => {
              this.optRC[key].push(item)
            })
          }
          //console.info(`optRC`, this.optRC)
        })
        .then((response) => {
          this.showGrid = true
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })

    },

    updFixedDim(v) {
      //console.info(v)
      if (v) {
        this.loadGridDO()
      } else {
        this.showGrid = false
        this.cols = []
        this.rows = []
      }
    },

    updFace(v, e) {
      this.fixedDim = ref(false)
      this.showGrid = ref(false)
      //console.info("v", v)
      //console.info("e", e)
      if (v === "face2") {
        this.doi3 = null
        this.cl3 = null
        this.doi4 = null
        this.cl4 = null
      }
      if (v === "face3") {
        this.doi4 = null
        this.cl4 = null
        this.face2 = 1
        this.setCombo()
      }
      if (this.dimCount === 4) {
        if (v === "face4") {
          this.face4 = 1
          this.setCombo()
        }
      }

    },

    fnPrev() {
      if (this.dimCount > 2) {
        if (this.face === 'face2')
          this.face2 = this.face2 > 1 ? this.face2 - 1 : 1;
        if (this.face === 'face3')
          this.face3 = this.face3 > 1 ? this.face3 - 1 : 1;
        if (this.face === 'face4') {
          this.face4 = this.face4 > 1 ? this.face4 - 1 : 1;
          this.updFace(this.face)
        }
      }
      this.setCombo()
      this.loadGridDO()
    },

    fnNext() {
      if (this.dimCount === 3) {
        if (this.face === 'face2')
          this.face2 = this.face2 < 3 ? this.face2 + 1 : 3;
      }
      if (this.dimCount === 4) {
        if (this.face === 'face2')
          this.face2 = this.face2 < 6 ? this.face2 + 1 : 6;
        if (this.face === 'face3') {
          this.face3 = this.face3 < 4 ? this.face3 + 1 : 4;
          this.updFace(this.face)
        }
      }
      this.setCombo()
      this.loadGridDO()
    },

    fnPrevDisable() {
      if (this.dimCount === 2)
        return true
      return this.face === 'face2' && this.face2 === 1 ||
        this.face === 'face3' && this.face3 === 1 ||
        this.face === 'face4' && this.face4 === 1;
    },

    fnNextDisable() {
      if (this.dimCount === 2)
        return true
      if (this.dimCount === 3) {
        return this.face === 'face2' && this.face2 >= 3 || this.face === 'face3' && this.face3 === 1;
      } else {
        return this.face === 'face2' && this.face2 >= 6 || this.face === 'face3' && this.face3 >= 4 || this.face === 'face4' && this.face4 >= 1;
      }
    },

    setCombo() {
      this.optDoX = []
      this.optDoY = []
      if (this.dimCount === 3) {
        if (this.face === 'face2') {
          if (this.face2 === 1) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id
          } else if (this.face2 === 2) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[2])
            this.doY = this.optDimObj[2].id
          } else if (this.face2 === 3) {
            this.optDoX.push(this.optDimObj[1])
            this.doX = this.optDimObj[1].id
            this.optDoY.push(this.optDimObj[2])
            this.doY = this.optDimObj[2].id
          }
        }
        if (this.face === 'face3') {
          if (this.face3 === 1) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id
          }
        }
      }

      if (this.dimCount === 4) {
        if (this.face === 'face2') {
          if (this.face2 === 1) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id
          } else if (this.face2 === 2) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[2])
            this.doY = this.optDimObj[2].id
          } else if (this.face2 === 3) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[3])
            this.doY = this.optDimObj[3].id
          } else if (this.face2 === 4) {
            this.optDoX.push(this.optDimObj[1])
            this.doX = this.optDimObj[1].id
            this.optDoY.push(this.optDimObj[2])
            this.doY = this.optDimObj[2].id
          } else if (this.face2 === 5) {
            this.optDoX.push(this.optDimObj[1])
            this.doX = this.optDimObj[1].id
            this.optDoY.push(this.optDimObj[3])
            this.doY = this.optDimObj[3].id
          } else if (this.face2 === 6) {
            this.optDoX.push(this.optDimObj[2])
            this.doX = this.optDimObj[2].id
            this.optDoY.push(this.optDimObj[3])
            this.doY = this.optDimObj[3].id
          }
        }
        if (this.face === 'face3') {

          this.cl4 = null
          this.doi4 = null
          if (this.face3 === 1) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id

            this.optDOI3 = this.optDOI3org
            this.labelDOI3 = this.optDimObj[2].name
          }
          if (this.face3 === 2) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id

            this.optDOI3 = this.optDOI4org
            this.labelDOI3 = this.optDimObj[3].name
            this.cl3 = null
            this.doi3 = null
          }
          if (this.face3 === 3) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[2])
            this.doY = this.optDimObj[2].id

            this.optDOI3 = this.optDOI4org
            this.labelDOI3 = this.optDimObj[3].name
            this.cl3 = null
            this.doi3 = null
          }
          if (this.face3 === 4) {
            this.optDoX.push(this.optDimObj[2])
            this.doX = this.optDimObj[2].id
            this.optDoY.push(this.optDimObj[3])
            this.doY = this.optDimObj[3].id

            this.optDOI3 = this.optDOI4org
            this.labelDOI3 = this.optDimObj[3].name
            this.cl3 = null
            this.doi3 = null
          }
        }
        if (this.face === 'face4') {
          if (this.face4 === 1) {
            this.optDoX.push(this.optDimObj[0])
            this.doX = this.optDimObj[0].id
            this.optDoY.push(this.optDimObj[1])
            this.doY = this.optDimObj[1].id

            this.optDOI3 = this.optDOI3org
            this.labelDOI3 = this.optDimObj[2].name
            this.optDOI4 = this.optDOI4org
            this.labelDOI4 = this.optDimObj[3].name
          }
        }
      }
    }


  },

  created() {

    this.loading = true
    api
      .post(baseURL, {
        method: "cubes/loadDimObjForSelect",
        params: [this.cubeS],
      })
      .then((response) => {
        this.optDimObj = response.data.result.store.records;
        this.dimCount = this.optDimObj.length
        this.cubeSFace = response.data.result.cubeSFace
        //console.info("optDimObj", this.optDimObj)
      })
      .then(() => {
        if (this.dimCount === 2) {
          this.face2count = 1
          this.face3count = 0
          this.face4count = 0
        } else if (this.dimCount === 3) {
          this.face2count = 3
          this.face3count = 1
          this.face4count = 0
        } else if (this.dimCount === 4) {
          this.face2count = 6
          this.face3count = 4
          this.face4count = 1
        }
        this.optDoX.push(this.optDimObj[0])
        this.doX = this.optDimObj[0].id
        this.optDoY.push(this.optDimObj[1])
        this.doY = this.optDimObj[1].id
      })
      .then(() => {
        if (this.dimCount > 2) {
          this.loading = true
          let param = {cubeS: this.cubeS, dimObj3: this.optDimObj[2].id}
          if (this.dimCount === 4) {
            param.dimObj4 = this.optDimObj[3].id
          }
          api
            .post(baseURL, {
              method: "cubes/loadAllDimObjForFixed",
              params: [param],
            })
            .then((response) => {
              this.optDOI3 = response.data.result.doi3.records;
              this.optDOI3org = response.data.result.doi3.records;
              this.labelDOI3 = this.optDimObj[2].name
              if (this.dimCount === 4) {
                this.optDOI4 = response.data.result.doi4.records;
                this.optDOI4org = response.data.result.doi4.records;
                this.labelDOI4 = this.optDimObj[3].name
              }
            })
            .catch(error => {
              console.log(error)
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
      .catch(error => {
        console.log(error)
      })
      .finally(() => {
        this.loading = false
      })
    //

  }


}
</script>

<style scoped>

</style>
