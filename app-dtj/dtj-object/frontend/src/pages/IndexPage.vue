<template>
  <q-page class="q-pa-md">
    <q-inner-loading :showing="loading" />
    <q-btn label="Загрузка объектов" @click="loadObjectServed"/>
    <div> {{this.rows}} </div>
    <hr/>

    <q-btn label="Добавить" @click="saveObjectServed('ins')"/>
    <div> {{this.row}} </div>
    <hr/>

    <div class="row">
      <q-input :model-value="indexRow" label="Номер записи" type="number"></q-input>
      <q-btn class="q-ml-md" label="Редактировать" @click="saveObjectServed('upd', indexRow)"/>
    </div>

    <div> {{this.row}} </div>
    <hr/>


  </q-page>
</template>

<script>
import { defineComponent } from 'vue'
import {api, baseURL} from "boot/axios.js";
import UpdaterRow from "pages/UpdaterRow.vue";
import {extend} from "quasar";

export default defineComponent({
  name: 'IndexPage',

  data() {
    return {
      loading: false,
      rows: [],
      row: [],
      indexRow: 0,

    }
  },

  methods: {
    loadObjectServed() {
      this.loading = true;
      api
        .post(baseURL, {
          method: "data/loadObjectServed",
          params: [0],
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

    saveObjectServed(mode, indexRow) {
      this.loading = true;
      let data = {
      }

      if (mode === "upd") {
        extend(true, data, this.rows[indexRow])      }

      this.$q
        .dialog({
          component: UpdaterRow,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Ok! updated", r);
          this.row = []
          this.row.push(r)
        })


    }

  },


})
</script>
