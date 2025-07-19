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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>

        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>

      <q-card-section class="no-padding">
        <q-table
          style="height: 600px"
          color="primary"
          card-class="bg-amber-1"
          table-class="text-grey-8"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :dense="dense"
          :rows-per-page-options="[0]"
        >
          <template v-slot:body="props">
            <q-tr :props="props">
              <td style="width: 5px">
                <q-btn
                  :dense="dense"
                  flat
                  color="blue"
                  :icon="
                    props.row.checked === 1
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="selectedRow(props.row)"
                >
                </q-btn>
              </td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["dimPeriodItem", "dense"],

  data() {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      maxCnt: 0,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    selectedRow(row) {
      row.checked = !row.checked;
    },

    validSave() {
      let i = 0;
      this.rows.forEach((r) => {
        if (r.checked) {
          i++;
        }
      });
      return i === this.maxCnt;
    },

    getColumns() {
      return [
        {
          name: "checked",
          field: "checked",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 100%",
        },
      ];
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    onOKClick() {
      this.loading = ref(true);
      let dta = [];

      this.rows.forEach((r) => {
        if (r.checked) {
          dta.push({ id: r.id, numb: r.numb, name: r.name });
        }
      });

      api
        .post(baseURL, {
          method: "dimperiod/updateNotIn",
          params: [this.dimPeriodItem, dta],
        })
        .then(
          () => {
            this.$emit("ok", { res: true });
          },
          (error) => {
            let msg = error.message;
            if (error.response.data.error.message)
              msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
          this.hide();
        });
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

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.cols = this.getColumns();

    api
      .post(baseURL, {
        method: "dimperiod/loadNotInForUpdate",
        params: [this.dimPeriodItem],
      })
      .then((response) => {
        this.rows = response.data.result.records;
        this.maxCnt = this.rows.length;
      });
  },
};
</script>
