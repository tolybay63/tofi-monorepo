<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      full-height
      full-width
  >
    <q-card class="q-dialog-plugin no-scroll">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-bar style="height: 45px">
        <q-btn
            dense
            icon="expand_circle_down"
            color="secondary"
            class="q-ml-sm q-mr-sm"
            @click="onAll()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("onAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            dense
            icon="unpublished"
            color="secondary"
            class="q-ml-sm"
            @click="offAll()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("offAll") }}
          </q-tooltip>
        </q-btn>

        <q-space></q-space>

        <q-btn
            class="q-mr-sm"
            :loading="loading"
            :dense="dense"
            color="secondary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>

        <q-btn
            class="q-ml-sm"
            :dense="dense"
            color="secondary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-bar>

      <div
          class="q-table-container q-table--dense wrap bg-orange-1 scroll"
          style="height: 90%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
          <q-card-section>
            <q-table
                virtual-scroll
                color="primary"
                card-class="bg-amber-1"
                table-class="text-grey-8"
                row-key="id"
                :columns="cols"
                :rows="rows"
                :wrap-cells="true"
                :table-colspan="4"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="ref"
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

                  <q-td key="cod" :props="props">
                    {{ props.row.cod }}
                  </q-td>

                  <q-td key="name" :props="props">
                    {{ props.row.name }}
                  </q-td>

                  <q-td key="fullName" :props="props">
                    {{ props.row.fullName }}
                  </q-td>
                </q-tr>
              </template>
            </q-table>
          </q-card-section>
          <q-banner class="bg-orange-1"></q-banner>
          <q-banner class="bg-orange-1"></q-banner>
        </div>
      </div>

      <q-bar></q-bar>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["prop", "dense"],

  data() {
    return {
      propId: this.prop,
      cols: [],
      rows: [],
      separator: ref("cell"),
      loading: ref(false),
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

    onAll() {
      this.rows.forEach((r) => {
        r.checked = true;
      });
    },

    offAll() {
      this.rows.forEach((r) => {
        r.checked = false;
      });
    },

    validSave() {
      return false;
      /*      let i = 0
            this.rows.forEach(r => {
              if (r.checked) i++
            })
            return !(i > 0)*/
    },

    getColumns() {
      return [
        {
          name: "checked",
          field: "checked",
          style: "width: 3%",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em;",
          style: "width: 10%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 37%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 50%",
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
          dta.push({id: r.id});
        }
      });

      api
          .post(baseURL, {
            method: "prop/savePropRefVal",
            params: [this.prop, dta],
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
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
          method: "prop/loadPropValForUpd",
          params: [this.prop],
        })
        .then((response) => {
          this.rows = response.data.result.records;
        });

    return {};
  },
};
</script>
