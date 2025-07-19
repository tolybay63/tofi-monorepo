<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      full-width
      full-height
  >
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("deleteGroupRecords") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 48px">
        <q-space/>
        <q-card-actions align="right">
          <q-btn
              :loading="loading"
              dense
              color="secondary"
              icon="delete"
              :label="$t('delete')"
              :disable="validSave()"
              @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>

          <q-btn
              dense
              color="secondary"
              icon="cancel"
              :label="$t('cancel')"
              @click="onCancelClick"
          />
        </q-card-actions>
      </q-bar>

      <div>
      <q-table
          style="height: calc(100vh - 200px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          dense
          selection="multiple"
          v-model:selected="selected"
          :rows-per-page-options="[20, 15, 0]"
      />
      </div>


    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["relTyp", "lg"],

  data() {
    return {
      lang: this.lg,
      loading: ref(false),
      cols: [],
      rows: [],
      selected: ref([])
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    validSave() {
      return this.selected.length === 0
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

      this.loading = ref(true);
      let err = false


      api
          .post(baseURL, {
            method: "relcls/deleteGroupRelCls",
            params: [this.relTyp, this.selected],
          })
          .then(
              (response) => {
                err = false;
                this.$emit("ok", response.data.result);
                //notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                err = true;
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

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:35%;",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em;width:50%;",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:15%;",
        },
      ];
    },

    loadData() {

      this.loading = ref(true);

      api
          .post(baseURL, {
            method: "relcls/load",
            params: [this.relTyp],
          })
          .then((response) => {
            this.rows = response.data.result.records;
            //
          })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });



    }
  },

  created() {
    this.cols = this.getColumns();
    this.loadData()

  },

};
</script>
