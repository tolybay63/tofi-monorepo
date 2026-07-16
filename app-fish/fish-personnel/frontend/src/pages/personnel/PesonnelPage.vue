<template>
  <q-page class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[80, 100]"
      :model-value="splitterModel"
      after-class="overflow-hidden q-pl-sm"
      before-class="overflow-hidden q-pr-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >

      <template v-slot:before>

        <q-table
          v-model:pagination="pagination"
          v-model:selected="selected"
          :columns="cols"
          :filter="filter"
          :loading="loading"
          :max="pagesNumber"
          :rows="rows"
          :rows-per-page-options="[25, 0]"
          :table-colspan="4"
          :wrap-cells="true"
          card-class="bg-amber-1"
          class="sticky-header-table"
          color="primary"
          dense
          row-key="own"
          selection="single"
          separator="cell"
          table-header-class="text-bold text-white bg-blue-grey-13"
          @request="requestData"
          @update:selected="updateSelected"
        >
          <template #bottom-row>
            <q-td v-if="selected.length > 0" colspan="100%">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td v-else-if="this.rows.length > 0" class="text-bold" colspan="100%">
              {{ $t("infoRow") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <q-avatar color="black" icon="groups" text-color="white"/>
              {{ $t("personnel") }}
            </div>

            <q-space/>
            <q-btn
              v-if="hasTarget('pers:ins')"
              :dense="dense"
              :disable="loading"
              color="secondary"
              icon="post_add"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('pers:upd')"
              :disable="loading || selected.length === 0"
              class="q-ml-sm"
              color="secondary"
              dense
              icon="edit"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('pers:del')"
              :disable="loading || selected.length === 0"
              class="q-ml-sm"
              color="red"
              dense
              icon="delete"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-space/>
            <q-input
              v-model="filter"
              :label="$t('txt_filter')"
              :model-value="filter"
              color="primary"
              debounce="300"
              dense
            >
              <template v-slot:append>
                <q-icon name="search"/>
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading color="secondary" showing></q-inner-loading>
          </template>
        </q-table>

      </template>
      <template v-slot:after>

        <q-card class="bg-amber-1 full-height">
          <q-card-section>

            <q-input v-model="form.UserSecondName" :label="fnLabel('UserSecondName', true)" readonly/>
            <q-input v-model="form.UserFirstName" :label="fnLabel('UserFirstName', true)" readonly/>
            <q-input v-model="form.UserMiddleName" :label="fnLabel('UserMiddleName', false)" readonly/>
            <q-input v-model="form.UserDateBirth" :label="fnLabel('UserDateBirth', false)" readonly/>
            <q-input v-model="form.UserEmail" :label="fnLabel('UserEmail', false)" readonly/>
            <q-input v-model="form.UserPhone" :label="fnLabel('UserPhone', false)" readonly/>
            <q-select
              v-model="form.UserId"
              :label="fnLabel('UserId', false)"
              :options="optUserId"
              map-options
              option-label="name"
              option-value="id"
              readonly
            />

          </q-card-section>

        </q-card>

      </template>

    </q-splitter>

  </q-page>
</template>

<script>
import {ref} from "vue";
import {api,} from "../../boot/axios";
import {hasTarget, notifyInfo, notifySuccess} from "../../utils/jsutils.js";
import UpdaterPersonnel from "./UpdaterPersonnel.vue";
import {extend} from 'quasar'

const requestParam = {
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default {
  name: "PersonnelPage",

  data() {
    return {
      splitterModel: 100,
      cols: [],
      rows: [],
      filter: "",
      loading: false,

      pagination: {
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 25,
        rowsNumber: 0,
      },
      selected: [],
      form: {
        UserSecondName: null, UserFirstName: null, UserMiddleName: null,
        UserDateBirth: null, UserEmail: null, UserPhone: null, UserId: null
      },
      optUserId: [],
    };
  },

  methods: {
    hasTarget,

    updateSelected() {
      this.form = []
      if (this.selected.length > 0) {
        this.splitterModel = 80
        extend(true, this.form, this.selected[0])
        this.form.UserPhone = "+7 " + this.form.UserPhone.substring(0, 3) + " " + this.form.UserPhone.substring(3, 6) + " " + this.form.UserPhone.substring(6, 10)
        if (this.form.UserId)
          this.form.UserId = parseInt(this.form.UserId, 10)
      } else {
        this.splitterModel = 100
      }
    },

    fnLabel(txt, req) {
      if (req)
        return this.$t(txt) + "*";
      else
        return this.$t(txt);
    },

    removeRow(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.name +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post("", {
              method: "data/delete",
              params: [{rec: rec}],
            })
            .then(
              () => {
                this.rows.splice(index, 1);
                this.selected = ref([]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                console.error(msg);
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    editRow(rec, mode) {
      let data = {};
      if (mode === "ins") {
        this.loading = true;
        api
          .post('', {
            method: "data/newRec",
            params: [],
          })
          .then(
            (response) => {
              data = response.data.result.records[0];
            })
          .finally(() => {
            this.loading = false;
          });
      } else {
        extend(true, data, rec)
        if (data.UserId)
          data.UserId = parseInt(data.UserId, 10)
      }

      this.$q
        .dialog({
          component: UpdaterPersonnel,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          this.selected = [];
          if (mode === "ins") {
            this.rows.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                rec[key] = r[key];
              }
            }
          }
          this.selected.push(r);
          this.updateSelected()
        });
    },

    fetchData(requestProps) {
      this.loading = true;

      const page = requestProps.page;
      const rowsPerPage = requestProps.rowsPerPage;
      const orderBy = requestProps.sortBy;
      const filter = requestProps.filter;
      //
      api
        .post("", {
          method: "data/loadPersonnel",
          params: [
            {
              page: page,
              limit: rowsPerPage,
              orderBy: orderBy,
              filter: filter,
            },
          ],
        })
        .then(
          (response) => {
            this.rows = response.data.result.store.records;
            const meta = response.data.result.meta;
            this.pagination.page = meta.page;
            this.pagination.rowsPerPage = meta.limit === meta.total ? 0 : meta.limit;
            this.pagination.rowsNumber = meta.total;
            //
            this.selected = [];
            //
          },
          (error) => {
            //this.$router["push"]("/");
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            console.error(msg);
          }
        )
        .finally(() => {
          this.loading = false;
        });
    },

    pagesNumber: function () {
      return 1;
    },

    requestData(requestProps) {
      const sb = requestProps.pagination.sortBy;
      const des = requestProps.pagination.descending;
      //debugger
      if (sb === null) {
        requestParam.sortBy = null;
      } else {
        if (des === true) requestParam.sortBy = sb + " desc";
        else requestParam.sortBy = sb;
      }
      requestParam.descending = requestProps.pagination.descending;
      requestParam.filter = requestProps.filter;
      requestParam.page = requestProps.pagination.page;
      requestParam.rowsPerPage = requestProps.pagination.rowsPerPage;
      requestParam.rowsNumber = requestProps.pagination.rowsNumber;

      this.pagination.sortBy = requestProps.pagination.sortBy;
      this.pagination.descending = requestProps.pagination.descending;
      //
      this.fetchData(requestParam);
    },

    infoSelected(row) {
      return " " + row.fio;
    },

    getColumns() {
      return [
        {
          name: "fio",
          label: this.$t("fio"),
          field: "fio",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "nameUserSex",
          label: this.$t("UserSex"),
          field: "nameUserSex",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
        },
        {
          name: "nameUserPosition",
          label: this.$t("UserPosition"),
          field: "nameUserPosition",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
        {
          name: "nameUserOrg",
          label: this.$t("UserOrg"),
          field: "nameUserOrg",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 30%",
        },
      ];
    },
  },

  mounted() {
  },

  created() {
    this.loading = true
    api
      .post('', {
        method: 'data/selectUser',
        params: [],
      })
      .then(
        (response) => {
          this.optUserId = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.cols = this.getColumns();
    this.fetchData(requestParam);
  },

  computed: {},

  setup() {
  },
};
</script>

<style lang="sass">
.sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 150px)

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */






  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */






  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>
