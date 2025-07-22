<template>
  <div class="q-pa-sm">

    <q-table
      style="height: 100%; width: 100%"
      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="horizontal"
      :loading="loading"
      selection="single"
      v-model:selected="selected"
      :rows-per-page-options="[0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="manage_accounts">
          </q-avatar>
          {{ $t("users") }}
        </div>

        <q-space/>

        <q-btn
          icon="delete" dense
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space/>

      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"/>
      </template>
    </q-table>


  </div>

</template>

<script>
import {api, baseURL} from "boot/axios.js";
import {notifyError} from "src/utils/jsutils.js";

export default {
  name: "AccountsPage",

  data: function () {
    return {
      cols: [],
      rows: [],
      selected: [],
      loading: false,
    };
  },

  methods: {
    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + row.fullname + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel"
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "data/deleteOwnerWithProperties",
              params: [row.id, 1],
            })
            .then(() => {
              this.loadUsers()
              this.selected = []
            })
            .catch(error => {
              console.log(error.message)
              notifyError(this.$t(error.response.data.error.message))
            });
        })

    },

    loadUsers() {
      this.loading = true
      api
        .post(baseURL, {
          method: "data/loadUsers",
          params: ["Typ_Users"],
        })
        .then(
          (response) => {
            this.rows = response.data.result.records
            //console.info("rows", this.rows)
          },
          (error) => {
            let msg = error.message
            if (error.response)
              msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = false
        })
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
        {
          name: "fullname",
          label: this.$t("fldFullName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:60%",
        },
      ]

    },

    infoSelected(row) {
      return " " + row.fullname + ")"
    },

  },

  created() {
    this.cols = this.getColumns()
    this.loadUsers()
  }


}
</script>

<style scoped>

</style>
