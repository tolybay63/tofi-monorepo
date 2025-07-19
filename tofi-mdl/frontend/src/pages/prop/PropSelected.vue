<template>
  <div class="q-pa-sm-sm bg-green-1">
    <q-tabs dense v-model="tab" class="text-teal no-scroll">
      <div style="margin-left: 20px">
        {{ $t("prop") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ this.infoProp() }} </strong>
        </span>
      </div>

      <q-space/>

      <q-btn dense glossy round color="secondary" icon="arrow_back" @click="toBack()">
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
        <prop-ref-val :propType="prop.propType" :allItem="prop.allItem"/>
      </q-tab-panel>

      <q-tab-panel name="refvalMeasure" class="no-scroll">
        <prop-ref-val-measure :prop="prop.id"/>
      </q-tab-panel>

      <q-tab-panel name="entityValList">
        <prop-entity-val-list :entityType="prop.entityType"/>
      </q-tab-panel>

      <q-tab-panel name="entityValTree">
        <prop-entity-val-tree :entityType="prop.entityType"/>
      </q-tab-panel>

      <q-tab-panel name="entityValTreeProp">
        <prop-entity-val-tree-prop :entityType="prop.entityType"/>
      </q-tab-panel>

      <q-tab-panel name="complexProp">
        <complex-prop :parentNode="prop"/>
      </q-tab-panel>

      <q-tab-panel name="periodType" v-if="dependPeriod()">
        <prop-period-type/>
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import allConsts from "pages/all-consts";
import PropPeriodType from "pages/prop/values/PropPeriodType.vue";
import PropRefVal from "pages/prop/values/PropRefVal.vue";
import PropRefValMeasure from "pages/prop/values/PropRefValMeasure.vue";
import PropMeterVal from "pages/prop/values/PropMeterVal.vue";
import PropEntityValList from "pages/prop/values/PropEntityValList.vue";
import PropEntityValTree from "pages/prop/values/PropEntityValTree.vue";
import PropEntityValTreeProp from "pages/prop/values/PropEntityValTreeProp.vue";
import ComplexProp from "pages/prop/ComplexProp.vue";
import StatusPage from "pages/prop/StatusPage.vue";
import ProviderPage from "pages/prop/ProviderPage.vue";

export default {
  name: "PropSelected",
  components: {
    ProviderPage,
    StatusPage,
    PropMeterVal,
    PropRefVal,
    PropPeriodType,
    PropEntityValList,
    PropRefValMeasure,
    PropEntityValTree,
    PropEntityValTreeProp,
    ComplexProp,
  },

  data() {
    return {
      tab: ref(null),
      propGrId: 0,
      propId: 0,
      prop: {},
    };
  },
  methods: {
    toBack() {
      this.$router["push"]({
        name: "propPage",
        params: {
          propGr: this.propGrId,
          prop: this.propId,
        },
      });
    },

    infoProp() {
      let inf = this.prop.cod + " - " + this.prop.name;
      if (
          [allConsts.FD_PropType.meter, allConsts.FD_PropType.rate].includes(
              this.prop.propType
          )
      ) {
        if (this.prop.meterStruct === allConsts.FD_MeterStruct.soft)
          inf = inf + " (" + this.$t("soft") + ")";
        else if (this.prop.meterStruct === allConsts.FD_MeterStruct.hard)
          inf = inf + " (" + this.$t("hard") + ")";
      }
      return inf;
    },

    dependPeriod() {
      return this.prop["isDependValueOnPeriod"] === true;
    },

    getIcon() {
      if (this.isFactor()) return "account_tree";
      else if (this.isTyp()) return "view_quilt";
      else if (this.isRel()) return "view_column";
      else if (this.isMeasure()) return "square_foot";
    },

    getTitle() {
      if (this.isFactor()) return "propValFactor";
      else if (this.isTyp()) return "propValTyp";
      else if (this.isRel()) return "propValRelTyp";
      else if (this.isMeasure()) return "propValMeasure";
    },

    getIconM() {
      if (this.isMeter()) return "scale";
      else if (this.isRate()) return "speed";
    },

    getIconEntity() {
      return "settings";
      /*
      if (this.prop.entityType == allConsts.FD_EntityType.Factor)
        return '';
      else if (this.prop.entityType == allConsts.FD_EntityType.FactorVal)
        return '';
*/
    },

    getEntityTitle() {
      if (this.prop.entityType === allConsts.FD_EntityType.Factor)
        return this.$t("propValFactor");
      else if (this.prop.entityType === allConsts.FD_EntityType.FactorVal)
        return this.$t("propValFactorVal");
      else if (this.prop.entityType === allConsts.FD_EntityType.Type)
        return this.$t("propValTyp");
      else if (this.prop.entityType === allConsts.FD_EntityType.RelTyp)
        return this.$t("propValRelTyp");
      else if (this.prop.entityType === allConsts.FD_EntityType.Cls)
        return this.$t("propValCls");
      else if (this.prop.entityType === allConsts.FD_EntityType.Measure)
        return this.$t("propValMeasure");
      else if (this.prop.entityType === allConsts.FD_EntityType.Prop)
        return this.$t("propValProp");
    },

    isEntityList() {
      return [
        allConsts.FD_EntityType.Factor,
        allConsts.FD_EntityType.Type,
        allConsts.FD_EntityType.RelTyp,
      ].includes(this.prop.entityType);
    },

    isEntityTree() {
      return [
        allConsts.FD_EntityType.Cls,
        allConsts.FD_EntityType.FactorVal,
        //allConsts.FD_EntityType.RelObj,
        //allConsts.FD_EntityType.Obj,
      ].includes(this.prop.entityType);
    },

    isEntityTreeProp() {
      return allConsts.FD_EntityType.Prop === this.prop.entityType;
    },

    ////////////////////

    isFactor() {
      return this.prop.propType === allConsts.FD_PropType.factor;
    },
    isTyp() {
      return this.prop.propType === allConsts.FD_PropType.typ;
    },
    isRel() {
      return this.prop.propType === allConsts.FD_PropType.reltyp;
    },
    isMeasure() {
      return this.prop.propType === allConsts.FD_PropType.measure;
    },

    isMeter() {
      return this.prop.propType === allConsts.FD_PropType.meter;
    },

    isRate() {
      return this.prop.propType === allConsts.FD_PropType.rate;
    },

    isComplex() {
      return this.prop.propType === allConsts.FD_PropType.complex;
    },

    setTab() {
      if (this.prop.statusFactor)
        this.tab = ref("status");
      else if (this.prop.providerTyp)
        this.tab = ref("provider");
      else if (this.isFactor() || this.isTyp() || this.isRel())
        this.tab = ref("refval");
      else if (this.isMeasure()) this.tab = ref("refvalMeasure");
      else if (this.isMeter() || this.isRate()) this.tab = ref("meterval");
      else if (this.isEntityList()) this.tab = ref("entityValList");
      else if (this.isEntityTree()) this.tab = ref("entityValTree");
      else if (this.isEntityTreeProp()) {
        this.tab = ref("entityValTreeProp");
      } else if (this.isComplex()) {
        this.tab = ref("complexProp");
      } else {
        this.tab = ref("periodType");
      }
    },
  },

  mounted() {
    //console.log("mounted!", this.$route.params.prop);
    this.propGrId = parseInt(this.$route["params"].propGr, 10);
    this.propId = parseInt(this.$route["params"].prop, 10);

    // load prop
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "prop/loadRec",
          params: [this.propId],
        })
        .then((response) => {
          //console.log("prop info", response.data.result.records)
          this.prop = response.data.result.records[0];
        })
        .catch((error) => {
          console.log(error);
        })
        .finally(() => {
          this.setTab();
          this.loading = ref(false);
        });
    //
  },
};
</script>

<style scoped></style>
