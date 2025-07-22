<script>

import {api, baseURL} from "boot/axios.js";

export default {
  name: "TestTypPage",

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: "",
      loading: false,
    };
  },

  methods: {

    getColumns() {
      return [
        {
          name: "id",
          label: "Id",
          field: "id",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:30%",
        },

        {
          name: "name",
          label: "Name",
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
        {
          name: "pv",
          label: "PropVal",
          field: "pv",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
      ]
    },

    load() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadFactorValForSelect",
          params: ["Factor_Defects"],
        })
        .then(
          (response) => {
            this.rows = response.data.result.records
            console.info("rows", this.rows)
          })
        .catch(error => {
          console.error(error.message)
        })
        .finally(() => {
          this.loading = false
        })


    },

  },

  mounted() {
    console.log("Component mounted.")
    this.load();
  },

  created () {
    console.log("Component created")
    this.cols = this.getColumns()

  }

}


</script>

<template>

  <div class="q-pa-md">
    <q-table
      style="height: 100%; width: 100%"
      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :rows-per-page-options="[15, 0]"
    >
      <template v-slot:loading>
        <q-inner-loading showing color="primary"/>
      </template>
    </q-table>

  </div>



</template>

<style scoped>

</style>
