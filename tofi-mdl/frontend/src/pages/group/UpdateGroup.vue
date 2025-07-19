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

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("parentGroup") }}: {{ parentName }}
        </q-item-section>
        <!-- cod -->
        <q-input
            :dense="dense"
            v-model="form.cod"
            :model-value="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
        />
        <!-- name -->
        <q-input
            :dense="dense"
            :model-value="form.name"
            v-model="form.name"
            autofocus
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

        <!-- accessLevel -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="al"
            :options="optAL"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="al"
            @update:model-value="fnSelectAL()"
            style="margin-bottom: 5px"
        />

        <!-- parent -->
        <q-item-label
            class="text-grey-7"
            style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("parent") }}
        </q-item-label
        >
        <treeselect
            :normalizer="normalizer"
            :options="parents"
            v-model="parent"
            :model-value="parent"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @close="fnCloseParent"
        >
        </treeselect>

        <!-- cmt -->
        <q-input
            :dense="dense"
            :model-value="form.cmt"
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
            :disable="validName()"
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";

import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";

export default {
  components: {treeselect},

  props: ["data", "mode", "lg", "isChild", "parentName", "tableName", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      optAL: [],
      al: this.data.accessLevel,
      //

      parents: [],
      parent: this.data.parent,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnCloseParent(v) {
      this.form.parent = v;
      this.parent = v;
    },

    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
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
      //console.log("select", this.al)
      this.form.accessLevel = this.al.id;
    },

    validName() {
      if (!this.form.name) return true;
      else if (this.form.name.trim().length === 0) return true;
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

      //delete this.form.accessLevel_text
      //console.log("al:", this.al)
      //console.log("this.form:", this.form)

      const method = this.mode === "ins" ? "insert" : "update";
      this.form.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;

      api
          .post(baseURL, {
            id: this.form.id,
            method: "group/" + method,
            params: [{rec: this.form, tableName: this.tableName}],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result.records[0]);
                //this.$emit("ok", {res: true});
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
    console.log("CREATE DATA", this.data);

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optAL = response.data.result.records;
        });
    //
    api
        .post(baseURL, {
          method: "group/loadGroupForSelect",
          params: [{id: this.data.id, tableName: this.tableName}],
        })
        .then((response) => {
          this.parents = pack(response.data.result.records, "ord");
          if (this.mode === "upd") {
            this.parents.push({id: 0, name: "Выбор...", children: []});
          }
          console.log("PARENTS", this.parents);
        });

    return {};
  },
};
</script>
