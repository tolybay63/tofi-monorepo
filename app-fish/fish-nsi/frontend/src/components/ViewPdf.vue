<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-down"
    @hide="onDialogHide"
  >
    <q-card
      ref="cardRef"
      :style="cardStyle"
      class="no-scroll pdf-viewer-card"
    >
      <q-bar class="bg-brand-light">
        {{ fileName }}
        <q-space></q-space>
        <q-btn color="white" flat icon="close" @click="hide"></q-btn>
      </q-bar>
      <q-inner-loading :showing="loading" color="secondary"/>
      <div class="fit">
        <q-pdfviewer
          :src="path"
          type="html5"
          content-class="fit container"
          inner-content-class="fit container"
        />
      </div>

    </q-card>
  </q-dialog>
</template>

<script>

import {notifyError} from "src/utils/jsutils";

let saveTimeout = null

export default {
  props: ["id", "fileName"],

  data() {
    return {
      path: "",
      loading: false,
      width: '80vw',
      height: '80vh',
      resizeObserver: null
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
        .post('/fish_loadpdf',
          formData,
          {
            responseType: "blob"
          })
        .then(res => {
          const blob = new Blob([res.data], {type: "application/pdf"})
          this.path = window.URL.createObjectURL(blob)
        })
        .catch(() => {
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
        .finally(() => {
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

    debounceSave() {
      if (saveTimeout) clearTimeout(saveTimeout)
      saveTimeout = setTimeout(() => {
        localStorage.setItem('pdfViewerWidth', this.width)
        localStorage.setItem('pdfViewerHeight', this.height)
      }, 500)
    }

  },

  mounted() {
    const savedWidth = localStorage.getItem('pdfViewerWidth')
    const savedHeight = localStorage.getItem('pdfViewerHeight')

    if (savedWidth) this.width = savedWidth
    if (savedHeight) this.height = savedHeight

    // Отслеживание изменения размера - используем nextTick для доступа к DOM
    this.$nextTick(() => {
      const cardElement = this.$refs.cardRef?.$el
      if (cardElement) {
        this.resizeObserver = new ResizeObserver(() => {
          const rect = cardElement.getBoundingClientRect()
          this.width = `${rect.width}px`
          this.height = `${rect.height}px`
          this.debounceSave()
        })
        this.resizeObserver.observe(cardElement)
      }
    })
  },

  beforeUnmount() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }

    let formData = new FormData();
    formData.append("filename", this.fileName);
    this.$axios
      .post('/deletefile',
        formData,
      )
      .then(() => {
        })
      .catch(() => {
        })
  },

  computed: {
    cardStyle() {
      return {
        minWidth: '400px',
        minHeight: '300px',
        width: this.width,
        height: this.height,
        maxWidth: '95vw',
        maxHeight: '95vh',
        resize: 'both',
        overflow: 'auto'
      }
    }
  },

  created() {
    this.loadPDF()
  },
};
</script>

<style scoped>
.pdf-viewer-card {
  position: relative;
}

.pdf-viewer-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  background: linear-gradient(
    -45deg,
    transparent 0%,
    transparent 30%,
    rgba(0, 0, 0, 0.2) 30%,
    rgba(0, 0, 0, 0.2) 40%,
    transparent 40%,
    transparent 70%,
    rgba(0, 0, 0, 0.2) 70%,
    rgba(0, 0, 0, 0.2) 80%,
    transparent 80%
  );
  cursor: nwse-resize;
  pointer-events: none;
  z-index: 1;
}
</style>
