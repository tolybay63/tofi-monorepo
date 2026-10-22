<template>
  <div class="q-pa-sm-sm bg-green-1">
    <q-tabs dense v-model="tab" class="text-teal no-scroll">
      <div style="margin-left: 20px">
        {{ $t("prop") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ infoProp() }} </strong>
        </span>
      </div>

      <q-space />

      <q-btn dense glossy round color="secondary" icon="arrow_back" @click="toBack">
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>

      <q-tab
        name="status"
        no-caps
        icon="check_box"
        :label="$t('statusFactor')"
        v-if="prop.statusFactor"
      />

      <q-tab
        name="provider"
        no-caps
        icon="download"
        :label="$t('providerTyp')"
        v-if="prop.providerTyp"
      />

      <q-tab
        name="refval"
        no-caps
        :icon="getIcon()"
        :label="$t(getTitle())"
        v-if="isFactor() || isTyp() || isRel()"
      />

      <q-tab
        name="refvalMeasure"
        no-caps
        :icon="getIcon()"
        :label="$t(getTitle())"
        v-if="isMeasure()"
      />

      <q-tab
        name="meterval"
        no-caps
        :icon="getIconM()"
        :label="$t('subProps')"
        class="no-scroll"
        v-if="isMeter() || isRate()"
      />

      <q-tab
        name="entityValList"
        no-caps
        :icon="getIconEntity()"
        :label="getEntityTitle()"
        class="no-scroll"
        v-if="isEntityList()"
      />

      <q-tab
        name="entityValTreeProp"
        no-caps
        :icon="getIconEntity()"
        :label="getEntityTitle()"
        class="no-scroll"
        v-if="isEntityTreeProp()"
      />

      <q-tab
        name="entityValTree"
        no-caps
        :icon="getIconEntity()"
        :label="getEntityTitle()"
        class="no-scroll"
        v-if="isEntityTree()"
      />

      <q-tab
        name="complexProp"
        no-caps
        :icon="getIconEntity()"
        label="Элементы комплексного свойства"
        class="no-scroll"
        v-if="isComplex()"
      />

      <q-tab
        name="periodType"
        no-caps
        icon="date_range"
        :label="$t('periodTypes')"
        v-if="dependPeriod()"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="status" class="no-scroll">
        <status-page
          :act="'prop'"
          :fk="prop.id"
          :factor="prop.statusFactor"
        />
      </q-tab-panel>

      <q-tab-panel name="provider" class="no-scroll">
        <provider-page
          :act="'prop'"
          :fk="prop.id"
          :typ="prop.providerTyp"
        />
      </q-tab-panel>

      <q-tab-panel name="meterval" class="no-scroll">
        <prop-meter-val
          :meter="prop.meter"
          :meterRate="prop.meterRate"
          :propGr="prop.propGr"
          :prop="prop.id"
          :meterStruct="prop.meterStruct"
        />
      </q-tab-panel>

      <q-tab-panel name="refval">
        <prop-ref-val :propType="prop.propType" :allItem="prop.allItem" />
      </q-tab-panel>

      <q-tab-panel name="refvalMeasure" class="no-scroll">
        <prop-ref-val-measure :prop="prop.id" />
      </q-tab-panel>

      <q-tab-panel name="entityValList">
        <prop-entity-val-list :entityType="prop.entityType" />
      </q-tab-panel>

      <q-tab-panel name="entityValTree">
        <prop-entity-val-tree :entityType="prop.entityType" />
      </q-tab-panel>

      <q-tab-panel name="entityValTreeProp">
        <prop-entity-val-tree-prop :entityType="prop.entityType" />
      </q-tab-panel>

      <q-tab-panel name="complexProp">
        <complex-prop :parentNode="prop" />
      </q-tab-panel>

      <q-tab-panel name="periodType" v-if="dependPeriod()">
        <prop-period-type />
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios";
import allConsts from "@/pages/all-consts";
import PropPeriodType from "@/pages/prop/values/PropPeriodType.vue";
import PropRefVal from "@/pages/prop/values/PropRefVal.vue";
import PropRefValMeasure from "@/pages/prop/values/PropRefValMeasure.vue";
import PropMeterVal from "@/pages/prop/values/PropMeterVal.vue";
import PropEntityValList from "@/pages/prop/values/PropEntityValList.vue";
import PropEntityValTree from "@/pages/prop/values/PropEntityValTree.vue";
import PropEntityValTreeProp from "@/pages/prop/values/PropEntityValTreeProp.vue";
import ComplexProp from "@/pages/prop/ComplexProp.vue";
import StatusPage from "@/pages/prop/StatusPage.vue";
import ProviderPage from "@/pages/prop/ProviderPage.vue";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

const tab = ref(null);
const propGrId = ref(0);
const propId = ref(0);
const prop = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "propPage",
    params: {
      propGr: propGrId.value,
      prop: propId.value,
    },
  });
};

const infoProp = () => {
  let inf = (prop.value.cod || "") + " - " + (prop.value.name || "");
  if (
    [allConsts.FD_PropType.meter, allConsts.FD_PropType.rate].includes(
      prop.value.propType
    )
  ) {
    if (prop.value.meterStruct === allConsts.FD_MeterStruct.soft)
      inf = inf + " (" + (proxy?.$t("soft") || "soft") + ")";
    else if (prop.value.meterStruct === allConsts.FD_MeterStruct.hard)
      inf = inf + " (" + (proxy?.$t("hard") || "hard") + ")";
  }
  return inf;
};

const dependPeriod = () => {
  return prop.value["isDependValueOnPeriod"] === true;
};

const getIcon = () => {
  if (isFactor()) return "account_tree";
  else if (isTyp()) return "view_quilt";
  else if (isRel()) return "view_column";
  else if (isMeasure()) return "square_foot";
};

const getTitle = () => {
  if (isFactor()) return "propValFactor";
  else if (isTyp()) return "propValTyp";
  else if (isRel()) return "propValRelTyp";
  else if (isMeasure()) return "propValMeasure";
};

const getIconM = () => {
  if (isMeter()) return "scale";
  else if (isRate()) return "speed";
};

const getIconEntity = () => {
  return "settings";
};

const getEntityTitle = () => {
  if (prop.value.entityType === allConsts.FD_EntityType.Factor)
    return proxy?.$t("propValFactor") || "propValFactor";
  else if (prop.value.entityType === allConsts.FD_EntityType.FactorVal)
    return proxy?.$t("propValFactorVal") || "propValFactorVal";
  else if (prop.value.entityType === allConsts.FD_EntityType.Type)
    return proxy?.$t("propValTyp") || "propValTyp";
  else if (prop.value.entityType === allConsts.FD_EntityType.RelTyp)
    return proxy?.$t("propValRelTyp") || "propValRelTyp";
  else if (prop.value.entityType === allConsts.FD_EntityType.Cls)
    return proxy?.$t("propValCls") || "propValCls";
  else if (prop.value.entityType === allConsts.FD_EntityType.Measure)
    return proxy?.$t("propValMeasure") || "propValMeasure";
  else if (prop.value.entityType === allConsts.FD_EntityType.Prop)
    return proxy?.$t("propValProp") || "propValProp";
};

const isEntityList = () => {
  return [
    allConsts.FD_EntityType.Factor,
    allConsts.FD_EntityType.Type,
    allConsts.FD_EntityType.RelTyp,
  ].includes(prop.value.entityType);
};

const isEntityTree = () => {
  return [
    allConsts.FD_EntityType.Cls,
    allConsts.FD_EntityType.FactorVal,
  ].includes(prop.value.entityType);
};

const isEntityTreeProp = () => {
  return allConsts.FD_EntityType.Prop === prop.value.entityType;
};

const isFactor = () => {
  return prop.value.propType === allConsts.FD_PropType.factor;
};

const isTyp = () => {
  return prop.value.propType === allConsts.FD_PropType.typ;
};

const isRel = () => {
  return prop.value.propType === allConsts.FD_PropType.reltyp;
};

const isMeasure = () => {
  return prop.value.propType === allConsts.FD_PropType.measure;
};

const isMeter = () => {
  return prop.value.propType === allConsts.FD_PropType.meter;
};

const isRate = () => {
  return prop.value.propType === allConsts.FD_PropType.rate;
};

const isComplex = () => {
  return prop.value.propType === allConsts.FD_PropType.complex;
};

const setTab = () => {
  if (prop.value.statusFactor) tab.value = "status";
  else if (prop.value.providerTyp) tab.value = "provider";
  else if (isFactor() || isTyp() || isRel()) tab.value = "refval";
  else if (isMeasure()) tab.value = "refvalMeasure";
  else if (isMeter() || isRate()) tab.value = "meterval";
  else if (isEntityList()) tab.value = "entityValList";
  else if (isEntityTree()) tab.value = "entityValTree";
  else if (isEntityTreeProp()) {
    tab.value = "entityValTreeProp";
  } else if (isComplex()) {
    tab.value = "complexProp";
  } else {
    tab.value = "periodType";
  }
};

onMounted(() => {
  propGrId.value = parseInt(route.params.propGr, 10) || 0;
  propId.value = parseInt(route.params.prop, 10) || 0;

  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "prop/loadRec",
      params: [propId.value],
    })
    .then((response) => {
      prop.value = response.data.result.records[0];
    })
    .catch((error) => {
      console.log(error);
    })
    .finally(() => {
      setTab();
      loading.value = false;
    });
});
</script>

<style scoped></style>
