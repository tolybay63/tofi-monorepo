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
            <q-radio
              v-model="face"
              checked-icon="task_alt"
              unchecked-icon="panorama_fish_eye"
              val="face2"
              label="2-х мерные"
              @update:model-value="updFace"
            />
            <q-radio
              v-if="dimCount > 2"
              v-model="face"
              checked-icon="task_alt"
              unchecked-icon="panorama_fish_eye"
              val="face3"
              label="3-х мерные"
              @update:model-value="updFace"
            />
            <q-radio
              v-if="dimCount > 3"
              v-model="face"
              checked-icon="task_alt"
              unchecked-icon="panorama_fish_eye"
              val="face4"
              label="4-х мерные"
              @update:model-value="updFace"
            />
          </div>
          <div class="q-ma-sm">
            <!-- DoX -->
            <q-select
              autofocus
              readonly
              v-model="doX"
              :options="optDoX"
              label="Измерение по строкам"
              option-value="id"
              option-label="name"
              map-options
              dense
              options-dense
              class="q-mb-md"
            />
            <!-- DoY -->
            <q-select
              readonly
              v-model="doY"
              :options="optDoY"
              label="Измерение по столбцам"
              option-value="id"
              option-label="name"
              map-options
              dense
              options-dense
              class="q-mb-md"
            />
          </div>

          <div style="text-align: center">
            <q-btn label="<" class="q-mx-lg" @click="fnPrev" :disable="fnPrevDisable()" />
            <span class="q-mx-md" v-if="face === 'face2'"> {{ face2 }} из {{ face2count }}</span>
            <span class="q-mx-md" v-if="face === 'face3'"> {{ face3 }} из {{ face3count }}</span>
            <span class="q-mx-md" v-if="face === 'face4'"> {{ face4 }} из {{ face4count }}</span>
            <q-btn label=">" class="q-mx-lg" @click="fnNext" :disable="fnNextDisable()" />
          </div>

          <!--  fixed dims  -->
          <div class="q-ma-md text-center text-blue" v-if="(face === 'face3' || face === 'face4') && dimCount >= 3">
            Фиксированные измерения
            <hr />
            <!-- Fixed 1 -->
            <div v-if="(face === 'face3' && dimCount === 3) || ((face === 'face4' || face === 'face3') && dimCount === 4)">
              <q-select
                v-model="cl3"
                :options="optDOI3"
                :label="labelDOI3"
                option-value="id"
                option-label="name"
                map-options
                dense
                options-dense
                class="q-mb-md"
                @update:model-value="fnSelectDOI3"
              />
            </div>
            <!-- Fixed 2 -->
            <div v-if="face === 'face4' && dimCount === 4">
              <q-select
                v-model="cl4"
                :options="optDOI4"
                :label="labelDOI4"
                option-value="id"
                option-label="name"
                map-options
                dense
                options-dense
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

      <q-inner-loading :showing="loading" color="secondary" />

      <template v-slot:after>
        <div v-if="!showGrid">
          <h3> Грани прокуба объектов</h3>
        </div>
        <div v-else>
          <edjes-obj
            :cols="cols"
            :rows="rows"
            :cubeSFace="cubeSFace"
            :do1="doX"
            :do2="doY"
            :optRC="optRC"
            :dimCount="dimCount"
            :doi3="doi3"
            :doi4="doi4"
            :cl3="cl3"
            :cl4="cl4"
            :face="face"
            @updateDelete="onUpdateDelete"
            @updateBind="onUpdateBind"
          />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from "vue";
import { api } from "@/boot/axios.js";
import EdjesObj from "@/pages/cubes/tabs/obj/EdjesObj.vue";
import { notifyError, notifySuccess } from "@/utils/jsutils.js";

const props = defineProps({
  cubeS: [Number, String],
  dOrg: String,
});

const { proxy } = getCurrentInstance();

const loading = ref(false);
const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const dimCount = ref(2);
const face2 = ref(1);
const face2count = ref(1);
const face3 = ref(1);
const face3count = ref(0);
const face4 = ref(1);
const face4count = ref(0);
const face = ref("face2");

const doX = ref(null);
const doY = ref(null);
const optDimObj = ref([]);
const optDoX = ref([]);
const optDoY = ref([]);

const cl3 = ref(null);
const cl4 = ref(null);

const doi3 = ref(null);
const optDOI3 = ref([]);
const optDOI3org = ref([]);
const labelDOI3 = ref("Измерение 3");
const labelDOI4 = ref("Измерение 4");

const doi4 = ref(null);
const optDOI4 = ref([]);
const optDOI4org = ref([]);
const fixedDim = ref(false);
const showGrid = ref(false);
const optRC = ref([]);
const cubeSFace = ref(0);

const setCombo = () => {
  optDoX.value = [];
  optDoY.value = [];
  if (dimCount.value === 3) {
    if (face.value === 'face2') {
      if (face2.value === 1) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;
      } else if (face2.value === 2) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[2]);
        doY.value = optDimObj.value[2].id;
      } else if (face2.value === 3) {
        optDoX.value.push(optDimObj.value[1]);
        doX.value = optDimObj.value[1].id;
        optDoY.value.push(optDimObj.value[2]);
        doY.value = optDimObj.value[2].id;
      }
    }
    if (face.value === 'face3') {
      if (face3.value === 1) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;
      }
    }
  }

  if (dimCount.value === 4) {
    if (face.value === 'face2') {
      if (face2.value === 1) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;
      } else if (face2.value === 2) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[2]);
        doY.value = optDimObj.value[2].id;
      } else if (face2.value === 3) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[3]);
        doY.value = optDimObj.value[3].id;
      } else if (face2.value === 4) {
        optDoX.value.push(optDimObj.value[1]);
        doX.value = optDimObj.value[1].id;
        optDoY.value.push(optDimObj.value[2]);
        doY.value = optDimObj.value[2].id;
      } else if (face2.value === 5) {
        optDoX.value.push(optDimObj.value[1]);
        doX.value = optDimObj.value[1].id;
        optDoY.value.push(optDimObj.value[3]);
        doY.value = optDimObj.value[3].id;
      } else if (face2.value === 6) {
        optDoX.value.push(optDimObj.value[2]);
        doX.value = optDimObj.value[2].id;
        optDoY.value.push(optDimObj.value[3]);
        doY.value = optDimObj.value[3].id;
      }
    }
    if (face.value === 'face3') {
      cl4.value = null;
      doi4.value = null;
      if (face3.value === 1) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;

        optDOI3.value = optDOI3org.value;
        labelDOI3.value = optDimObj.value[2].name;
      }
      if (face3.value === 2) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;

        optDOI3.value = optDOI4org.value;
        labelDOI3.value = optDimObj.value[3].name;
        cl3.value = null;
        doi3.value = null;
      }
      if (face3.value === 3) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[2]);
        doY.value = optDimObj.value[2].id;

        optDOI3.value = optDOI4org.value;
        labelDOI3.value = optDimObj.value[3].name;
        cl3.value = null;
        doi3.value = null;
      }
      if (face3.value === 4) {
        optDoX.value.push(optDimObj.value[2]);
        doX.value = optDimObj.value[2].id;
        optDoY.value.push(optDimObj.value[3]);
        doY.value = optDimObj.value[3].id;

        optDOI3.value = optDOI4org.value;
        labelDOI3.value = optDimObj.value[3].name;
        cl3.value = null;
        doi3.value = null;
      }
    }
    if (face.value === 'face4') {
      if (face4.value === 1) {
        optDoX.value.push(optDimObj.value[0]);
        doX.value = optDimObj.value[0].id;
        optDoY.value.push(optDimObj.value[1]);
        doY.value = optDimObj.value[1].id;

        optDOI3.value = optDOI3org.value;
        labelDOI3.value = optDimObj.value[2].name;
        optDOI4.value = optDOI4org.value;
        labelDOI4.value = optDimObj.value[3].name;
      }
    }
  }
};

const loadGridDO = () => {
  if (!fixedDim.value) return;
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadGridDO",
      params: [props.cubeS, dimCount.value, doX.value, doY.value, cl3.value ? cl3.value : 0, cl4.value ? cl4.value : 0],
    })
    .then((response) => {
      cols.value = response.data.result.cols;
      rows.value = response.data.result.rows.records;

      for (let key in response.data.result.sel) {
        optRC.value.push(key);
        optRC.value[key] = [];
        response.data.result.sel[key].records.forEach((item) => {
          optRC.value[key].push(item);
        });
      }
    })
    .then(() => {
      showGrid.value = true;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const updFixedDim = (v) => {
  if (v) {
    loadGridDO();
  } else {
    showGrid.value = false;
    cols.value = [];
    rows.value = [];
  }
};

const updFace = (v) => {
  fixedDim.value = false;
  showGrid.value = false;
  if (v === "face2") {
    doi3.value = null;
    cl3.value = null;
    doi4.value = null;
    cl4.value = null;
  }
  if (v === "face3") {
    doi4.value = null;
    cl4.value = null;
    face2.value = 1;
    setCombo();
  }
  if (dimCount.value === 4) {
    if (v === "face4") {
      face4.value = 1;
      setCombo();
    }
  }
};

const fnSelectDOI3 = (v) => {
  updFace(face.value);
  cl3.value = v.id;
  doi3.value = v.doi;
};

const fnSelectDOI4 = (v) => {
  updFace(face.value);
  cl4.value = v.id;
  doi4.value = v.doi;
};

const onUpdateDelete = () => {
  fixedDim.value = false;
  rows.value = [];
  setTimeout(() => {
    fixedDim.value = true;
    updFixedDim(fixedDim.value);
  }, 100);
};

const onUpdateBind = () => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/bindingDO",
      params: [
        cubeSFace.value,
        dimCount.value,
        doX.value,
        doY.value,
        doi3.value ? doi3.value : 0,
        doi4.value ? doi4.value : 0,
        cl3.value ? cl3.value : 0,
        cl4.value ? cl4.value : 0,
      ],
    })
    .then(() => {
      notifySuccess(proxy?.$t("success"));
    })
    .then(() => {
      fixedDim.value = false;
      setTimeout(() => {
        fixedDim.value = true;
        updFixedDim(fixedDim.value);
      }, 100);
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const validAll = () => {
  if (dimCount.value === 2) {
    if (doX.value && doY.value) return false;
  }
  if (dimCount.value === 3) {
    if (face.value === "face2" && doX.value && doY.value) return false;
    if (face.value === "face3" && doX.value && doY.value && doi3.value) return false;
  }
  if (dimCount.value === 4) {
    if (
      (face.value === "face2" && doX.value && doY.value) ||
      (face.value === "face2" && doX.value && doi3.value) ||
      (face.value === "face2" && doX.value && doi4.value) ||
      (face.value === "face2" && doY.value && doi3.value) ||
      (face.value === "face2" && doY.value && doi4.value) ||
      (face.value === "face2" && doi3.value && doi4.value)
    )
      return false;
    if (
      (face.value === "face3" && doX.value && doY.value && doi3.value) ||
      (face.value === "face3" && doX.value && doY.value && doi4.value) ||
      (face.value === "face3" && doX.value && doi3.value && doi4.value) ||
      (face.value === "face3" && doY.value && doi3.value && doi4.value)
    )
      return false;
    if (face.value === "face4" && doX.value && doY.value && doi3.value && doi4.value) return false;
  }
  return true;
};

const fnPrev = () => {
  if (dimCount.value > 2) {
    if (face.value === 'face2') face2.value = face2.value > 1 ? face2.value - 1 : 1;
    if (face.value === 'face3') face3.value = face3.value > 1 ? face3.value - 1 : 1;
    if (face.value === 'face4') {
      face4.value = face4.value > 1 ? face4.value - 1 : 1;
      updFace(face.value);
    }
  }
  setCombo();
  loadGridDO();
};

const fnNext = () => {
  if (dimCount.value === 3) {
    if (face.value === 'face2') face2.value = face2.value < 3 ? face2.value + 1 : 3;
  }
  if (dimCount.value === 4) {
    if (face.value === 'face2') face2.value = face2.value < 6 ? face2.value + 1 : 6;
    if (face.value === 'face3') {
      face3.value = face3.value < 4 ? face3.value + 1 : 4;
      updFace(face.value);
    }
  }
  setCombo();
  loadGridDO();
};

const fnPrevDisable = () => {
  if (dimCount.value === 2) return true;
  return (
    (face.value === 'face2' && face2.value === 1) ||
    (face.value === 'face3' && face3.value === 1) ||
    (face.value === 'face4' && face4.value === 1)
  );
};

const fnNextDisable = () => {
  if (dimCount.value === 2) return true;
  if (dimCount.value === 3) {
    return (face.value === 'face2' && face2.value >= 3) || (face.value === 'face3' && face3.value === 1);
  } else {
    return (
      (face.value === 'face2' && face2.value >= 6) ||
      (face.value === 'face3' && face3.value >= 4) ||
      (face.value === 'face4' && face4.value >= 1)
    );
  }
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimObjForSelect",
      params: [props.cubeS],
    })
    .then((response) => {
      optDimObj.value = response.data.result.store.records;
      dimCount.value = optDimObj.value.length;
      cubeSFace.value = response.data.result.cubeSFace;
    })
    .then(() => {
      if (dimCount.value === 2) {
        face2count.value = 1;
        face3count.value = 0;
        face4count.value = 0;
      } else if (dimCount.value === 3) {
        face2count.value = 3;
        face3count.value = 1;
        face4count.value = 0;
      } else if (dimCount.value === 4) {
        face2count.value = 6;
        face3count.value = 4;
        face4count.value = 1;
      }
      optDoX.value.push(optDimObj.value[0]);
      doX.value = optDimObj.value[0].id;
      optDoY.value.push(optDimObj.value[1]);
      doY.value = optDimObj.value[1].id;
    })
    .then(() => {
      if (dimCount.value > 2) {
        loading.value = true;
        let param = { cubeS: props.cubeS, dimObj3: optDimObj.value[2].id };
        if (dimCount.value === 4) {
          param.dimObj4 = optDimObj.value[3].id;
        }
        api
          .post("", {
            method: "cubes/loadAllDimObjForFixed",
            params: [param],
          })
          .then((response) => {
            optDOI3.value = response.data.result.doi3.records;
            optDOI3org.value = response.data.result.doi3.records;
            labelDOI3.value = optDimObj.value[2].name;
            if (dimCount.value === 4) {
              optDOI4.value = response.data.result.doi4.records;
              optDOI4org.value = response.data.result.doi4.records;
              labelDOI4.value = optDimObj.value[3].name;
            }
          })
          .catch((error) => {
            console.error(error);
          })
          .finally(() => {
            loading.value = false;
          });
      }
    })
    .catch((error) => {
      console.error(error);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
