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

        <!-- Использование нашего компонента с древовидной структурой -->
        <TreeSelect
          v-model="parent"
          :options="parents"
          :label="$t('parent', true)"
          node-key="id"
          @select="fnCloseParent"
        />

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
import {api,} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";

import {ref} from "vue";
import TreeSelect from "components/TreeSelect.vue";

export default {
  components: {TreeSelect},

  props: ["data", "mode", "isChild", "parentName", "dense"],

  data() {
    return {
      form: this.data,
      optAL: [],
      al: this.data.accessLevel,
      //

      parents: [],
      parent: this.data.parent,
      loading: ref(false)
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnCloseParent(v) {
      console.info("fnCloseParent", v);
      this.form.parent = v;
      this.parent = v;
    },

    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
        children: node.children,
      };
    },

    onBlurName() {
      if (this.form.name) {
        this.form.name = this.form.name.trim();
        this.form.fullName = this.form.name.trim();
      }
    },

    validName() {
      if (!this.form.name) return true;
      else if (this.form.name.trim().length === 0) return true;
      return false;
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog["show"]();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog["hide"]();
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

      this.form.parent =
        typeof this.parent === "object" ? this.parent.id : this.parent;

      const method = this.mode === "ins" ? "insertGr" : "updateGr";
      api
        .post("", {
          id: this.form.id,
          method: "usr/" + method,
          params: [{ rec: this.form }],
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

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {

    this.loading = ref(true)
    api
      .post("", {
        method: "dict/loadDictAsStore",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      })
      .finally(()=> {
        this.loading = ref(false)
      })
    //
    this.loading = ref(true)
    api
      .post("", {
        method: "usr/loadGroupForSelect",
        params: [this.data.id],
      })
      .then((response) => {
        this.parents = pack(response.data.result.records, "ord");
      })
      .finally(()=> {
        this.loading = ref(false)
      })

  },
};
</script>
