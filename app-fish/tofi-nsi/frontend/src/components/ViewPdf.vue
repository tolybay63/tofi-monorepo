<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="no-scroll" style="min-width: 80vw; min-height: 80vh; width: 100%; height: 90%;">
      <q-bar class="bg-brand-light">
        {{fileName}}
        <q-space></q-space>
        <q-btn @click="hide" color="white" flat icon="close"></q-btn>
      </q-bar>
      <q-inner-loading :showing="loading" color="secondary"/>
        <div class="fit">
        <q-pdfviewer
          type="html5"
          :src="path"
        />
        </div>

    </q-card>
  </q-dialog>
</template>
<script>


import {notifyError} from "src/utils/jsutils";

export default {
  props: ["id", "fileName"],

  data() {
    return {
      path: "",
      loading: false,
    };
  },

  emits: ["ok", "hide"],

  methods: {

    loadPDF() {
      console.log("Load", this.path);
      this.loading = true;
      let formData = new FormData();
      formData.append("id", this.id);
      formData.append("model", "nsidata");
      formData.append("filename", this.fileName);
      this.$axios
        .post('/loadpdf',
        formData,
        {
          responseType: "blob"
        })
        .then(res => {
          const blob = new Blob([res.data], {type: "application/pdf"})
          this.path  = window.URL.createObjectURL(blob)
          //console.log("path", this.path);
        })
        .catch(err => {
          notifyError({
            message: 'Ошибка загрузки PDF',
            type: 'negative',
            textColor: 'white',
            color: 'negative',
            icon: 'error',
            closeBtn: 'close',
            position: 'top'
          })
        })
        .finally(()=> {
          this.loading = false;
        })
    },

    show() {
      this.$refs.dialog.show();
    },

    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      this.$emit("hide");
    },

  },

  beforeUnmount() {
    let formData = new FormData();
    formData.append("filename", this.fileName);
    this.$axios
      .post('/deletefile',
        formData,
        )
      .then(
        (resp) => {
        })
      .catch((err)=> {
      })
  },

  created() {

/*
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "data/getPathFile",
        params: [this.id],
      })
      .then(
        (response) => {
          //this.path = "/pdf/"+this.fileName
          this.path = response.data.result;
          console.log("path", this.path);
        })
      .catch(error => {
        notifyError(error.message);
      })
      .finally(() => {
        this.loading = ref(false);
      })
*/

    this.loadPDF()

  },
};
</script>
