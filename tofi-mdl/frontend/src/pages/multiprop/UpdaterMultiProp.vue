<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card>
        <q-tabs v-model="tab" class="text-teal">
          <q-tab name="main" :label="$t('mainProp')"/>
          <q-tab name="dop" :label="$t('dopProp')"/>
        </q-tabs>

        <q-separator/>

        <q-tab-panels
            v-model="tab"
            animated
            swipeable
            vertical
            transition-prev="jump-up"
            transition-next="jump-up"
        >
          <q-tab-panel name="main">
            <q-item-label
                style="color: orange; text-underline: orange; margin-top: 10px"
            >Свойства:
            </q-item-label
            >
            <!-- name -->
            <q-input
                autofocus
                :dense="dense"
                :model-value="form.name"
                v-model="form.name"
                @blur="onBlurName"
                :label="$t('fldName')"
                :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            >
            </q-input>
            <!-- fullName-->
            <q-input
                :dense="dense"
                :model-value="form.fullName"
                v-model="form.fullName"
                :label="$t('fldFullName')"
                :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            >
            </q-input>

            <!-- Cod -->
            <q-input
                :dense="dense"
                v-model="form.cod"
                :model-value="form.cod"
                :label="$t('code')"
                :placeholder="$t('msgCodeGen')"
            />

            <!-- AccessLevel -->
            <q-select
                :dense="dense"
                v-model="al"
                :options="optionsLevel"
                :label="$t('accessLevel')"
                option-value="id"
                option-label="text"
                map-options
                :model-value="al"
                @update:model-value="fnSelectDict()"
            />

            <q-item-label
                style="color: orange; text-underline: orange; margin-top: 10px"
            >
              {{ $t("statusFactor") }}:
            </q-item-label>

            <q-select
                v-model="status"
                map-options
                :model-value="status"
                use-input
                input-debounce="0"
                :label="$t('factor')"
                :options="optionsStatus"
                option-value="id"
                option-label="name"
                :dense="dense"
                :options-dense="dense"
                clearable
                @clear="fnClearStatus"
                @update:model-value="inputValueStatus"
                @filter="filterFnStatus"
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-item-label
                style="color: orange; margin-top: 10px; text-underline: orange"
            >
              {{ $t("providerTyp") }}:
            </q-item-label>

            <q-select
                v-model="provider"
                :model-value="provider"
                use-input
                map-options
                input-debounce="0"
                :label="$t('typ')"
                :options="optionsProvider"
                option-value="id"
                option-label="name"
                :dense="dense"
                :options-dense="dense"
                @update:model-value="inputValueProvider"
                @filter="filterFnProvider"
                clearable
                @clear="fnClearProvider"
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>
          </q-tab-panel>

          <q-tab-panel name="dop">

            <div>
              <q-toggle
                  style="margin-left: 10px; margin-top: 10px" :dense="dense"
                  v-model="form['isDependValueOnPeriod']" :model-value="form['isDependValueOnPeriod']"
                  :label="$t('dependValueOnPeriod')"
              />
            </div>

            <q-toggle
                style="margin-left: 10px; margin-top: 10px" :dense="dense"
                v-model="form.isUniq" :model-value="form.isUniq" :label="$t('isUniq')"
            />

            <q-input
                class="q-mt-lg"
                v-model="form.fillMore" :model-value="form.fillMore" type="number" :dense="dense"
                :label="$t('fillMore')" :rules="[(val) => val > 0 || $t('minFillMore')]"
            />

            <!-- cmt -->
            <q-input
                :dense="dense" :model-value="form.cmt" v-model="form.cmt" type="textarea" :label="$t('fldCmt')"
            >
            </q-input>

            <q-input
                :dense="dense" :model-value="form.propTag" v-model="form.propTag" :label="$t('propTag')"
            />
          </q-tab-panel>
        </q-tab-panels>
      </q-card>

      <q-card-actions align="right">
        <q-btn
            :dense="dense" color="primary" icon="save" :label="$t('save')"
            @click="onOKClick" :disable="validSave()"
        />
        <q-btn
            :dense="dense" color="primary" icon="cancel" :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>

import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";

export default {

  props: ["rec", "mode", "lg", "dense"],

  data() {
    //console.log("data 2", this.data)

    return {
      form: this.rec,
      visible: ref(false),

      optionsLevel: [],
      al: this.rec.accessLevel,

      status: this.rec.statusFactor,
      optionsStatus: [],
      optionsStatusOrg: [],

      provider: this.rec.providerTyp,
      optionsProvider: [],
      optionsProviderOrg: [],

      tab: ref("main"),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnClearStatus() {
      this.form.statusFactor = null;
    },

    fnClearProvider() {
      this.form.providerTyp = null;
    },

    inputValueStatus(val) {
      if (val) {
        this.status.id = val.id;
        this.form.statusFactor = val.id;
      }
    },

    filterFnStatus(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsStatus = this.optionsStatusOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsStatusOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsStatus = this.optionsStatusOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    inputValueProvider(val) {
      if (val) {
        this.provider.id = val.id;
        this.form.providerTyp = val.id;
      }
    },

    filterFnProvider(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsProvider = this.optionsProviderOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsProviderOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsProvider = this.optionsProviderOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
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


    fnSelectDict() {
      this.form.accessLevel = this.al.id
/*
      if (dict === "accessLevel") this.form.accessLevel = this.al.id;
      else if (dict === "visualFormat") this.form.visualFormat = this.vf.id;
*/
    },

    validSave() {
      return !(
          this.form.name !== null &&
          this.form.name !== undefined &&
          this.form.name.length > 2 &&
          this.form.fullName !== null &&
          this.form.fullName !== undefined &&
          this.form.fullName.length > 2 &&
          this.form.fillMore > 0
      );
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
      //console.log("form", this.form);
      const method =
          this.mode === "ins" ? "insertMultiProp" : "updateMultiProp";

      let err = false;
      api
          .post(baseURL, {
            method: "multiProp/" + method,
            params: [{rec: this.form}],
          })
          .then(
              (response) => {
                err = false;
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true;
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    getDict(dictName) {
      api
          .post(baseURL, {
            method: "dict/load",
            params: [{dict: dictName}],
          })
          .then((response) => {
            //if (dictName === "FD_AccessLevel")
              this.optionsLevel = response.data.result.records;
            //else if (dictName === "FD_VisualFormat")
              //this.optionsFV = response.data.result.records;
          });
    },
  },
  created() {
    //console.info("create:", this.rec);

    this.getDict("FD_AccessLevel");
    //this.getDict("FD_VisualFormat");
    this.visible = ref(true);
    api
        .post(baseURL, {
          method: "factor/loadForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsStatusOrg = response.data.result.records;
          this.optionsStatus = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false);
        });

    this.visible = ref(true);
    api
        .post(baseURL, {
          method: "typ/loadTypForSelect",
          params: [{}],
        })
        .then((response) => {
          this.optionsProviderOrg = response.data.result.records;
          this.optionsProvider = response.data.result.records;
          //this.optionsProvider.unshift({id: 0, name: this.$t('notChosen')})
        })
        .finally(() => {
          this.visible = ref(false);
        });

  },
};
</script>
