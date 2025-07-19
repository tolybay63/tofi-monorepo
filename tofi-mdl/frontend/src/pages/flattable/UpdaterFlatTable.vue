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

      <q-inner-loading :showing="loading" color="secondary"/>

      <q-card>
        <q-card-section>

          <!-- cod -->
          <q-input dense v-model="form.cod" :model-value="form.cod" :label="$t('code')"
                   :placeholder="$t('msgCodeGen')"
          />

          <!-- nameTable -->
          <q-input
              autofocus dense v-model="form.nameTable" :model-value="form.nameTable" :label="$t('tableName')"
              :rules="[(val) => !validTableName(val) || $t('validTableName')]"
          />


          <!-- accessLevel -->
          <q-select
              dense v-model="al" :model-value="al" :options="optAL" :label="$t('accessLevel')"
              option-value="id" option-label="text" map-options
              @update:model-value="fnAL()"
          />

          <!-- name -->
          <q-input
              dense v-model="form.name" :model-value="form.name" @blur="onBlurName" :label="$t('fldName')"
              :rules="[(val) => validName(val) || $t('req')]"
          />
          <!-- fullName-->
          <q-input
              dense v-model="form.fullName" :model-value="form.fullName" :label="$t('fldFullName')"
              :rules="[(val) => validName(val) || $t('req')]"
          />
          <!-- cls -->
          <q-item-label
              :class="form.cls < 0 ? 'text-red-10' : 'text-grey-7'"
              style="font-size: 0.8em" class="row">
            {{ $t("objCls") }}
            <q-space/>
            <q-icon name="error" v-if="form.cls < 0" color="red-10" size="24px"></q-icon>
          </q-item-label>

          <treeselect
              :disabled="!!form.relCls"
              :options="optCls"
              :max-height="600"
              v-model="form.cls"
              :flat="true"
              :default-expand-level="1"
              :show-count="true"
              :normalizer="normalizerCls"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
          />
          <q-item-label v-if="form.cls < 0"
                        class="text-red-10" style="font-size: 0.8em"
          >
            {{ $t("chooseCls") }}
          </q-item-label>

          <!-- relCls -->
          <div :class="form.relCls===0 ? 'text-red-10' : 'text-grey-7'"
               style="margin-top: 10px; font-size: 0.8em" class="row">
            {{ $t("relCls") }}
            <q-space/>
            <q-icon name="error" v-if="form.relCls===0" color="red-10" size="24px"></q-icon>
          </div>

          <treeselect
              :disabled="!!form.cls"
              :options="optRelCls"

              v-model="form.relCls"
              :default-expand-level="1"
              :normalizer="normalizerRelCls"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
          />
          <q-item-label v-if="form.relCls===0"
                        class="text-red-10" style="font-size: 0.8em"
          >
            {{ $t('chooseRelCls') }}
          </q-item-label>


          <!-- cmt -->
          <q-input
              dense v-model="form.cmt" :model-value="form.cmt" type="textarea" :label="$t('fldCmt')"
          />

        </q-card-section>
      </q-card>

      <q-card-actions align="right">
        <q-btn
            dense color="primary" icon="save" :label="$t('save')" @click="onOKClick" :disable="validSave()"
        />
        <q-btn dense color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import treeselect from 'vue3-treeselect'
import 'vue3-treeselect/dist/vue3-treeselect.css'
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  components: {treeselect},

  props: ["rec", "mode", "lg"],

  data() {
    return {
      form: this.rec,
      loading: ref(false),

      optAL: [],
      al: this.rec.accessLevel,

      optCls: [],

      optRelCls: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnAL() {
      this.form.accessLevel = this.al.id
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

    validName(val) {
      return !!val && !!val.trim()
    },

    valid(val) {
      let reg = /^[a-zA-Z0-9_]+$/;
      return reg.test(val);
    },

    validTableName(v) {
      return !this.valid(v);
    },

    validSave() {
      return !(!this.validTableName(this.form.nameTable) &&
          this.validName(this.form.name) && this.validName(this.form.fullName) &&
          (this.form.cls > 0 || this.form.relCls > 0));
    },

    normalizerCls(node) {
      return {
        id: node.cls,
        label: node.name,
      };
    },

    normalizerRelCls(node) {
      return {
        id: node.relcls,
        label: node.name,
      };
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
      //console.log("form", this.form)
      const method =
          this.mode === "ins" ? "insertFlatTable" : "updateFlatTable";

      this.loading = ref(true);
      let err = false;
      api
          .post(baseURL, {
            method: "flatTable/" + method,
            params: [this.form],
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
            this.loading = ref(false);
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optAL = response.data.result.records;
        })
        .finally(() => {
          this.loading = ref(false)
        });


    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dimMultiProp/loadClsForSelect",
          params: [],
        })
        .then((response) => {
              this.optCls = pack(response.data.result.records, "id")
            },
            (error) => {
              notifyError("class" + error.message)
            })
        .finally(() => {
          this.loading = ref(false)
        });

    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dimMultiProp/loadRelClsForSelect",
          params: [],
        })
        .then((response) => {
              this.optRelCls = pack(response.data.result.records, "id");
            },
            (error) => {
              notifyError(error.message)
            })
        .finally(() => {
          this.loading = ref(false);
        });


  },
};
</script>
