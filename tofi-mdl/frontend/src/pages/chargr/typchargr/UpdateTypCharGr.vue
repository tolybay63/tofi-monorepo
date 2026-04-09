<template>
  <q-dialog
    ref="dialog"
    @show="onDialogShow"
    @hide="onDialogHide"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- name -->
        <q-input
          :dense="dense"
          v-model="form.name"

          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :dense="dense"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- typ -->
        <q-select
          v-model="form.typ"  :options="optTyp"
          option-value="id"
          option-label="name"
          emit-value
          map-options
          :dense="dense"
          :disabled="mode === 'upd'"
          @update:model-value="fnSelectTyp"
          :options-dense="dense"
          :label="$t('typ')"
        />

        <!-- Cluster FV-->
        <q-item-label
          :class="form.factorVal===0 ? 'text-red-10' : 'text-grey-7'"
          style="font-size: 0.8em; margin-top: 10px" class="row"
        >{{ $t("factorVal") }}
          <q-space/>
          <q-icon name="error" v-if="form.factorVal===0" color="red-10" size="24px"></q-icon>
        </q-item-label
        >

        <div v-if="isReady && optionsCFV && optionsCFV.length > 0"
             style="width: 100%; min-height: 50px;"
             :key="form.typ"
        >
          <treeselect
            :options="optionsCFV"
            v-model="form.factorVal"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            max-height="300"
            :default-expand-level="1"
            @select="fnSelectCFV"
            :key="form.typ"
          />
        </div>
        <q-item-label v-else-if="isReady && optionsCFV === null" class="text-grey-5">
          {{ $t('loading') }}...
        </q-item-label>


        <!--

                <div v-if="isRendered && optionsCFV" style="width: 100%; min-height: 50px;">
                  <treeselect
                    :append-to-body="true"
                    :disabled="mode === 'upd'"
                    :options="optionsCFV"
                    v-model="form.factorVal"
                    :default-expand-level="1"
                    max-height="800"
                    :normalizer="normalizer"
                    :placeholder="$t('select')"
                    :noChildrenText="$t('noChilds')"
                    :noResultsText="$t('noResult')"
                    :noOptionsText="$t('noResult')"
                    @select="fnSelectCFV"
                  />
                </div>

        -->
        <q-item-label v-if="form.factorVal===0"
                      class="text-red-10" style="font-size: 0.8em"
        >
          {{ $t('chooseFV') }}
        </q-item-label>


        <!-- cod -->
        <q-input
          :dense="dense"
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <!--accessLevel-->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="optionsAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectAL"
        />

        <!-- cmt -->
        <q-input
          :dense="dense"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {Treeselect} from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {api} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";

export default {
  components: {treeselect: Treeselect},

  props: ["data", "mode", "dense"],

  data() {
// Создаем полную независимую копию данных
    const formCopy = JSON.parse(JSON.stringify(this.data));
    if (!formCopy.factorVal || formCopy.factorVal === 0) formCopy.factorVal = null;

    return {
      form: formCopy,
      isReady: false,     // Флаг полной готовности диалога
      optionsAL: [],
      optionsCFV: null,   // null — значит данных еще нет
      optTyp: [],
    };

/*

    const formData = JSON.parse(JSON.stringify(this.data));
    // Treeselect НЕ ЛЮБИТ 0. Если там 0, ставим null
    if (!formData.factorVal || formData.factorVal === 0) {
      formData.factorVal = null;
    }
    return {
      form: formData,
      al: this.data.accessLevel,
      optionsAL: [],
      optionsCFV: [],
      optTyp: [],
      typ: this.data.typ,
      isRendered: false,

    };
*/
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelectTyp(val) {
      // val теперь — это сразу ID благодаря emit-value в q-select
      this.optionsCFV = null;
      this.form.factorVal = null;
      this.loadclustFactorVal(val, this.mode);

/*

      this.form.typ = this.typ.id
      this.optionsCFV = []
      this.form.factorVal = null;
      this.loadclustFactorVal(this.form.typ, this.mode)
*/
    },

    fnSelectCFV(v) {
      this.form.factorVal = v.key
    },

    normalizer(node) {
      // Проверяем все возможные варианты ID, чтобы не вернуть undefined
      const id = node.key || node.id || node.ID;
      return {
        id: id,
        label: node.name || '---', // Защита от пустых имен
      };
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

    fnSelectAL() {
      //console.log("select", this.form.accessLevel, this.al);
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      return !(
        this.form.name && this.form.fullName && this.form.typ && this.form.factorVal && this.form.factorVal !== 0)
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onDialogShow() {
      // Даем Quasar один тик на завершение всех внутренних процессов
      setTimeout(() => {
        this.isReady = true;
      }, 300);
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

      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al;
      this.form.typ =
        typeof this.typ === "object" ? this.typ.id : this.typ;

      const method =
        this.mode === "ins" ? "insertTypCharGr" : "updateTypCharGr";

      api
        .post("", {
          method: "typ/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
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

    loadclustFactorVal(typ, mode) {
      api
        .post("", {
          method: "typ/loadTypClustFactorVal",
          params: [typ, mode],
        })
        .then((response) => {
          this.optionsCFV = pack(response.data.result.records, "ord");
        });

    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    api
      .post("", {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        //console.log("FD_AccessLevel", response.data.result.records)
        this.optionsAL = response.data.result.records;
      });

    api
      .post("", {
        method: "typ/loadTypForSelect",
        params: [],
      })
      .then((response) => {
        this.optTyp = response.data.result.records;
        this.optTyp.unshift({id: 0, name: this.$t("notChosen")});
      });


    if (this.mode === 'upd') {
      this.loadclustFactorVal(this.form.typ, this.mode)
    }

  },
};
</script>
