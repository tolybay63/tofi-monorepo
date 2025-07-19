<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      full-width
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary" dense>
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-card-section>

        <q-table
            style="height: 100%; width: 100%"
            dense color="primary"
            card-class="bg-amber-1"
            row-key="val"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading"
            selection="single"

        >

        <template v-slot:body="props">
          <q-tr :props="props">
            <td style="width: 5px">
              <q-btn
                  dense flat color="blue"
                  :icon="props.row.checked ? 'check_box' : 'check_box_outline_blank'"
                  @click="selectedRow(props.row)"
              >
              </q-btn>
            </td>

            <q-td key="val" :props="props">
              {{ props.row.val }}
            </q-td>

            <q-td key="name" :props="props">
              {{ props.row.name }}
            </q-td>

            <q-td key="fullName" :props="props">
              {{ props.row.fullName }}
            </q-td>

            <q-td key="scaleValColor" :props="props">
              <q-avatar size="28px" :style="fnStyle(props.row)" ></q-avatar>
              {{ props.row.scaleValColor }}
            </q-td>

            <q-td key="cmd" :props="props">
              <q-btn
                  :disable="!props.row.checked"
                  round
                  size="sm"
                  icon="edit"
                  color="blue"
                  flat
                  dense
                  @click="fnEdit(props.row)"
                  class="no-padding no-margin"
              >
                <q-tooltip
                    transition-show="rotate"
                    transition-hide="rotate"
                >
                  {{ $t("update") }}
                </q-tooltip>
              </q-btn>

            </q-td>

          </q-tr>

        </template>

        </q-table>

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save" dense
            :label="$t('save')"
            @click="onOKClick"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";
import UpdateScaleValAsgn from "pages/scale/UpdateScaleValAsgn.vue";

export default {
  props: ["scale", "scaleAsgn", "nmAsgn"],

  data() {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnStyle(row) {
      return "background:" + row["scaleValColor"]

    },

    selectedRow(row) {
      row.checked = !row.checked
    },

    fnEdit(row) {
      this.$q
          .dialog({
            component: UpdateScaleValAsgn,
            componentProps: {
              data: row,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  row[key] = r[key];
                }
              }

          });

    },

    getColumns() {
      return [
        {
          name: "val",
          label: this.$t("val"),
          field: "val",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 35%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 45%",
        },
        {
          name: "scaleValColor",
          label: this.$t("scaleColor"),
          field: "scaleValColor",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },
        {
          name: "cmd",
          label: "",
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },

      ];
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
      this.loading = ref(true)
      let err = false
      let data = []
      this.rows.forEach(r=> {
        if (r.checked) {
          data.push(r)
        }
      })

      api
          .post(baseURL, {
            method: "scale/updateScaleValAsgn",
            params: [ this.scale, this.scaleAsgn, data],
          })
          .then(
              () => {
                err = false
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            if (!err) this.hide()
            this.loading= ref(false)
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.cols = this.getColumns()

    api
        .post(baseURL, {
          method: "scale/loadScaleValAsgnUpd",
          params: [this.scale, this.scaleAsgn, this.nmAsgn],
        })
        .then((response) => {
          this.rows = response.data.result.records;
        });

  },
};
</script>
