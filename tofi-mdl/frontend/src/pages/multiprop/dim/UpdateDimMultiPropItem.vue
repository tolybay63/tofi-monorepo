<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>

      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("parent") }}: {{ parentName }}
        </q-item-section>

        <q-input
          class="q-my-md" dense
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <q-select
          dense options-dense
          v-model="et"
          :model-value="et"
          :options="optET"
          :label="$t('multiValEntityType')"
          option-value="id"
          option-label="text"
          map-options
          class="q-mb-md"
          @update:model-value="fnSelectET()"
        />


        <div v-if="dimMultiPropType===2">
          <!--  prop -->
          <q-item-label
            :class="form.prop === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row q-mt-lg">
            {{ $t("prop") }}
            <q-space></q-space>
            <q-icon name="error" v-if="form.prop === 0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
            class="q-mb-lg"
            :options="optProp"
            v-model="form.prop"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @close="fnCloseProp"
            @select="fnSelectProp"
          />
          <q-item-label v-if="form.prop === 0"
                        class="text-red-10" style="font-size: 0.8em"
          >
            {{ $t("chooseProp") }}
          </q-item-label>

        </div>


        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          class="q-mb-md"
          @blur="onBlurName" dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          :model-value="form.fullName"
          v-model="form.fullName"
          class="q-mb-md"
          :label="$t('fldFullName')" dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <div v-for="tit in titles">
          <q-input
            :model-value="form['col_'+tit.id]"
            v-model="form['col_'+tit.id]"
            :label="tit['title']"
            class="q-mb-md"
            dense
          />
        </div>

        <q-input
          :model-value="form['multiPropItemTag']"
          v-model="form['multiPropItemTag']"
          class="q-mb-md"
          :label="$t('propTag')" dense
        />

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea" dense
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick" dense
          :disable="validName()"
        />
        <q-btn
          color="primary"
          icon="cancel" dense
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>


<script>
import {extend} from "quasar";
import {api, baseURL} from "boot/axios.js";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils.js";
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";

export default {
  name: "UpdateDimMultiPropItem",
  props: ["data", "mode", "lg", "isChild", "parentName",
    "dimMultiPropName", "dimMultiPropType", "titles"],
  components: {treeselect},

  data() {
    let dta= {}
    extend(true, dta, this.data)
    return {
      form: dta,
      lang: this.lg,
      visible: false,
      et: this.data.multiEntityType === undefined ? 1 : this.data.multiEntityType,
      optET: [],
      entityName: "",
      prop: this.data.prop,
      optProp: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    normalizer(node) {
      return {
        id: node.prop,
        label: node.name,
      };
    },

    fnCloseProp(v) {
      //console.log("fnCloseProp", v);
      this.form.prop = v;
    },

    fnSelectProp(v) {
      //console.log("fnSelectProp", v);
      this.form.name = v.name;
      this.form.fullName = v.name;
    },

    fnSelectET() {
      this.form.multiEtityType = this.et.id
    },

    onBlurName() {
      if (this.form.name) {
        this.form.name = this.form.name.trim();
        if (
            !this.form.fullName ||
            (this.form.fullName && this.form.fullName.trim() === "")
        ) {
          this.form.fullName = this.form.name;
        }
      }
    },

    validName() {
      if (this.dimMultiPropType===1) {
        if (!this.form.name || !this.form.fullName) return true;
        else if (this.form.name.trim().length === 0) return true;
      } else {
        if (!this.form.name || !this.form.fullName || !this.form.prop) return true;
        else if (this.form.name.trim().length === 0) return true;
      }
      return false;
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
      this.form.name = this.form.name + this.entityName;
      this.form.fullName = this.form.fullName + this.entityName;

      const method = this.mode === "ins" ? "insertDimItem" : "updateDimItem";
      this.form.multiEntityType =
          typeof this.et === "object" ? this.et.id : this.et
      api
          .post(baseURL, {
            id: this.form.id,
            method: "dimMultiProp/" + method,
            params: [this.form],
          })
          .then(
              (response) => {
                //this.$emit("ok", {res: true});
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    this.visible = true
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_MultiValEntityType"}],
        })
        .then((response) => {
          //console.log("FD_AccessLevel", response.data.result.records)
          this.optET = response.data.result.records;
        })
        .finally(() => {
          this.visible = false
        });

    //
    this.visible = true
    api
        .post(baseURL, {
          method: "dimMultiProp/loadPropForMultiPropItem",
          params: [],
        })
        .then((response) => {
          this.optProp = pack(response.data.result.records, "id");
          //console.log("optProp", this.optProp);
        })
        .finally(() => {
          this.visible = false
        });

  },


}
</script>

<style scoped>

</style>
