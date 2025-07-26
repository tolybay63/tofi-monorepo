<template>
  <q-page class="q-pa-md">
    <q-inner-loading :showing="loading"/>
    <q-btn dense no-caps color="primary" label="Загрузка объектов" @click="loadObjectServed"/>
    <div> {{ this.rows }}</div>
    <hr class="q-mb-xl"/>

    <q-btn dense no-caps label="Добавить" color="primary" @click="saveObjectServed('ins')"/>
    <div> {{ this.row }}</div>
    <hr class="q-mb-xl"/>

    <div class="row">
      <q-input :model-value="id" v-model="id" label="id записи" type="number" :disable="rows.length===0"></q-input>
      <q-btn no-caps :disable="rows.length===0" dense class="q-ml-md" label="Редактировать" color="primary" @click="saveObjectServed('upd')"/>
    </div>
    <div> {{ this.row }}</div>

    <hr class="q-mb-xl"/>

    <div class="row">
      <q-input :model-value="id" v-model="id" label="id записи" type="number" :disable="rows.length===0"></q-input>
      <q-btn dense no-caps class="q-ml-md" label="Удалить" color="primary" @click="deleteObjectServed()" :disable="rows.length===0"/>
    </div>

  </q-page>
</template>

<script>
import {defineComponent} from 'vue'
import {api, baseURL} from "boot/axios.js";
import UpdaterRow from "pages/UpdaterRow.vue";
import {extend} from "quasar";
import {notifyError} from "src/utils/jsutils.js";

export default defineComponent({
  name: 'IndexPage',

  data() {
    return {
      loading: false,
      rows: [],
      id: 0,
      indexRow: -1,
      row: [],

    }
  },

  methods: {
    findIndex(id) {
      this.indexRow = -1
      this.rows.forEach(((item, index) => {
        if (item.id === parseInt(id, 10)) {
          this.id = item.id
          this.indexRow = index
        }
      }))
    },

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
            if (this.rows.length > 0)
              this.id = this.rows[0].id
          })
        .catch(error => {
          console.error(error.message)
        })
        .finally(() => {
          this.loading = false
        })

    },

    saveObjectServed(mode) {
      let data = {}
      if (mode === "upd") {
        if (parseInt(this.id, 10) > 0) {
          this.findIndex(this.id)
          if (this.indexRow < 0) {
            notifyError("Нет такой записи")
            return
          }
          extend(true, data, this.rows[this.indexRow])
        } else {
          notifyError("id не указан")
          return;
        }
      }
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
          //console.log("Ok! updated", r);
          this.row = []
          this.row.push(r)
          this.loadObjectServed()
        })
    },

    deleteObjectServed() {
      this.findIndex(this.id)
      if (this.indexRow===-1) {
        notifyError("Нет такой записи")
        return
      }
      let row = this.rows[this.indexRow]

      this.$q
        .dialog({
          title: "Удаление записи",
          message:
            "Удалить запись?" +
            '<div style="color: plum">(' + row.name + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.$axios
            .post(baseURL, {
              method: "data/deleteObjWithProperties",
              params: [row.id],
            })
            .then(() => {
              this.loadObjectServed()
            })
            .catch(error => {
              console.log(error.message)
              notifyError(error.message)
            })
        })
    }
  },


})
</script>
