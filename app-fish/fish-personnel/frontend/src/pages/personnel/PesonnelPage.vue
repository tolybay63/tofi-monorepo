<template>
  <h6 class="q-mx-lg">Calculation A: Данные из [Meta]</h6>

  <div class="no-padding no-margin">
    <q-table

      color="primary" dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :rows-per-page-options="[0]"
    >
    </q-table>

  </div>


</template>


<script>
import {api} from "../../boot/axios.js";

export default {
  name: "PesonnelPage",

  data: function() {
    return {
      rows: [],
      cols: []
    }
  },


  methods: {

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:60%",
        },

        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
      ]
    },


    },

  mounted() {

  },

  created() {
    this.cols = this.getColumns();


  }


}
</script>

<style scoped>

</style>
