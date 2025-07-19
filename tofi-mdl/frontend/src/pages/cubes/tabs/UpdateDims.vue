<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>

        <!-- CubeSDimType -->
        <q-select
          autofocus
          v-model="form.cubeSDimType"
          :model-value="form.cubeSDimType"
          :options="optCDT"
          :label="$t('cubeSDimType')"
          option-value="id"
          option-label="text"
          map-options dense options-dense
          class="q-mb-md" :disable="mode==='upd'"
          @update:model-value="fnSelectCDT"
        />

        <!-- dimPeriod -->
        <div v-if="form.cubeSDimType === dimTypePeriod">
          <q-select
            autofocus
            v-model="form.dimPeriod"
            :model-value="form.dimPeriod"
            :options="optDimPeriod"
            :label="$t('dimPeriod')"
            option-value="id"
            option-label="name"
            map-options dense options-dense
            class="q-mb-md"
            @update:model-value="fnSelectPeriod"
          />
        </div>

        <!-- DimPropType -->
        <div v-if="form.cubeSDimType===2">
          <q-select
            v-model="dpt"
            :model-value="dpt"
            :options="optDPT"
            :label="$t('dimPropType')"
            option-value="id"
            option-label="text"
            map-options dense options-dense
            class="q-mb-md" :disable="mode==='upd'"
            @update:model-value="fnSelectDPT"
          />
        </div>


        <div v-if="form.cubeSDimType === dimTypeProp">
          <q-item-label
            :class="form.dimProp===0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row"
          >{{ $t("dimProp") }}
            <q-space/>
            <q-icon name="error" v-if="form.dimProp===0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
            :options="optDimProp"
            v-model="form.dimProp"
            :normalizer="normalizerDimProp"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            defaultExpandLevel="1"
            autofocus :disabled="mode==='upd'"
            @select="fnSelectDimProp"
          />

          <q-item-label v-if="form.dimProp===0" class="text-red-10" style="font-size: 0.8em">
            {{ $t("chooseDimProp") }}
          </q-item-label>
        </div>

        <div v-if="form.cubeSDimType === dimTypeObj">
          <q-item-label
            :class="form.dimObj===0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row"
          >{{ $t("dimObj") }}
            <q-space/>
            <q-icon name="error" v-if="form.dimObj===0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
            :options="optDimObj"
            v-model="form.dimObj"
            :normalizer="normalizerDimObj"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            defaultExpandLevel="1"
            autofocus :disabled="mode==='upd'"
            @select="fnSelectDimObj"
          />

          <q-item-label v-if="form.dimObj===0" class="text-red-10" style="font-size: 0.8em">
            {{ $t("chooseDimObj") }}
          </q-item-label>
        </div>


        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <q-input
          v-model="form.dOrg"
          :model-value="form.dOrg"
          :label="$t('dOrg')"
          dense type="date"
        />

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          dense :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
          dense
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import allConsts from "pages/all-consts.js";
import treeselect from "vue3-treeselect";

export default {
  components: {treeselect},
  props: ["data", "mode", "typeProp"],

  data() {
    return {
      form: this.data,
      optCDT: [],
      optDimPeriod: [],
      optDimProp: [],
      optDimObj: [],
      dpt: this.data.dimPropType ? this.data.dimPropType : undefined,
      optDPT: [],
      dimTypePeriod: allConsts.FD_CubeSDimType.period,
      dimTypeProp: allConsts.FD_CubeSDimType.prop,
      dimTypeObj: allConsts.FD_CubeSDimType.obj,
      loading: false,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectDPT() {
      console.log("fnSelectDPT", this.dpt);
      this.form.dimPropType = this.dpt.id
      this.loadDimPropForSelect(this.form.cubeS, this.dpt.id, this.mode)
    },

    fnSelectCDT(v) {
      console.log("fnSelectCDT", v);
      this.form.cubeSDimType = v.id;
    },

    fnSelectPeriod(v) {
      console.log("fnSelectPeriod", v);
      this.form.dimPeriod = v.id;
      this.form.name = v.name;
    },

    fnSelectDimProp(v) {
      this.form.name = v.name;
    },

    normalizerDimProp(node) {
      return {
        id: node.dimProp,
        label: node.name,
      };
    },

    fnSelectDimObj(v) {
      this.form.name = v.name;
    },

    normalizerDimObj(node) {
      return {
        id: node.dimObj,
        label: node.name,
      };
    },

    validSave() {
      return this.form.name === "" || !this.form.cubeSDimType
        || (this.form.cubeSDimType === this.dimTypeProp && !this.form.dimProp)
        || (this.form.cubeSDimType === this.dimTypeObj && this.form.dimObj);
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      let err = false
      this.form.mode = this.mode
      //const method = this.mode === "ins" ? "insertCubeDim" : "updateCubeDim";
      /*      this.form.cubeSDimType =
              typeof this.cdt === "object" ? this.cdt.id : this.cdt;*/
      api
        .post(baseURL, {
          method: "cubes/saveCubeDim",
          params: [this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            notifySuccess(this.$t("success"));
          })
        .catch((error) => {
          err = true
          notifyError(this.$t(error.response.data.error.message))
        })
        .finally(() => {
          if (!err) this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    loadDimPropForSelect(cubeS, dimPropType, mode) {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/loadDimPropForSelect",
          params: [cubeS, dimPropType, mode],
        })
        .then((response) => {
          this.optDimProp = pack(response.data.result.records, "id");
          console.info("this.optDimProp", this.optDimProp)
        })
        .finally(() => {
          this.loading = false
        })
      //

    }
  },
  created() {

    this.loading = true
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_CubeSDimType"}],
      })
      .then((response) => {
        this.optCDT = response.data.result.records;
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_DimPropType"}],
      })
      .then((response) => {
        let notProp1 = 0
        let notProp2 = 0
        if (this.typeProp===1) {
          notProp1 = 3
          notProp2 = 4
        }
        if (this.typeProp===3) {
          notProp1 = 1
          notProp2 = 0
        }
        if (this.typeProp===4) {
          notProp1 = 1
          notProp2 = 0
        }

        this.optDPT = response.data.result.records.filter(it=> {
          return (it["id"] !== notProp1 && it["id"] !== notProp2)
        });
        //console.info("this.typeProp", this.typeProp)
        //console.info("notProp1, notProp2", notProp1, notProp2)
        //console.info("this.optDPT", this.optDPT)
      })
      .finally(() => {
        this.loading = false
      })
    //

    this.loading = true
    api
      .post(baseURL, {
        method: "cubes/loadDimPeriod",
        params: [],
      })
      .then((response) => {
        this.optDimPeriod = response.data.result.records;
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post(baseURL, {
        method: "cubes/loadDimObj",
        params: [],
      })
      .then((response) => {
        this.optDimObj = pack(response.data.result.records, "id");
      })
      .finally(() => {
        this.loading = false
      })
    //

      console.log("create form", this.form);
      if (this.form.dimPropType > 0) {
        this.loadDimPropForSelect(this.form.cubeS, this.form.dimPropType, this.mode)
      }

  },
};
</script>
