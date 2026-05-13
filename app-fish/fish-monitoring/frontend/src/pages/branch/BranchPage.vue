<template>
  <div class="q-pa-sm">
    <q-table
      style="height: calc(100vh - 140px); width: 100%"
      class="sticky-header-table"
      color="primary"
      dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="horizontal"
      :filter="filter"
      :loading="loading"
      selection="single"
      v-model:selected="selected"
      :rows-per-page-options="[25, 0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t('selectedRow') }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t('infoRow') }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="business"> </q-avatar>
          {{ $t('Branches') }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mon:vr:ins')"
          icon="post_add"
          dense
          color="secondary"
          :disable="loading"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('newRecord') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mon:vr:upd')"
          icon="edit"
          dense
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('editRecord') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mon:vr:del')"
          icon="delete"
          dense
          color="red"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('deletingRecord') }}
          </q-tooltip>
        </q-btn>

        <q-space />

        <q-input
          dense
          debounce="300"
          color="primary"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script>
import {extend} from 'quasar'
import {api} from 'boot/axios'
import {hasTarget, notifyInfo} from 'src/utils/jsutils'
import UpdaterBranch from "pages/branch/UpdaterBranch.vue";

export default {
  name: 'BranchPage',
  props: [],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      loading: false,
    }
  },

  methods: {
    hasTarget,

    editRow(row, mode) {
      let data = { accessLevel: 1 }
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterBranch,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
          } else {
            for (let key in r) {
              row[key] = r[key]
              /*
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
*/
            }
          }
        })
    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">(' + row.name + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteBranch',
              params: [row.id],
            })
            .then(() => {
              this.loadBranch()
              this.selected = []
            })
            .catch(() => {
              //console.log(error.message)

/*
              if (error.response.data.error.message.includes('@')) {
                let msgs = error.response.data.error.message.split('@')
                let m1 = msgs[0]
                let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
                let msg = ''
                if (m1 === 'existsSampling') {
                  msg = `
                  Заборы проб:
                  Существует - ${m2}
                  `
                }
                notifyError(msg)
              } else {
                notifyError(this.$t(error.response.data.error.message))
              }
*/
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    loadBranch: function () {
      this.loading = true
      api
        .post('', {
          method: 'data/loadBranch',
          params: [{ codTyp: 'Typ_Enterprise', idObj: 0 }],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]
          })
        .finally(() => {
          this.loading = false
        })
    },

    getColumns() {
      return [
        {
          name: 'name',
          label: this.$t('fldName')+"*",
          field: 'name',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%',
        },
        {
          name: 'fullName',
          label: this.$t('fldFullName')+"*",
          field: 'fullName',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
        },
        {
          name: 'nameCls',
          label: this.$t('cls')+"*",
          field: 'nameCls',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:20%',
        },
        {
          name: 'cmt',
          label: this.$t('description'),
          field: 'cmt',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
        },
      ]
    },

    infoSelected(row) {
      return ' ' + row.name
    },
  },

  created() {
    this.cols = this.getColumns()
    //
    this.loadBranch()

  },
}
</script>

<!--<style lang="sass">
.my-sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 190px)

  .q-table__top,
  .q-table__bottom,
  thead tr:first-child th
    /* bg color is important for th; just specify one */
    background-color: #bdbdbd

  thead tr th
    position: sticky
    z-index: 1
  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */
  &.q-table&#45;&#45;loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */
  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>-->

<style scoped>
.sticky-header-table {
  /* Ограничиваем высоту контейнера, чтобы появилась прокрутка */
  max-height: 100%;
  overflow: auto;
}

.sticky-header-table table {
  /* Убираем схлопывание границ, чтобы sticky работал корректно в некоторых браузерах */
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  /* Делаем заголовок липким */
  position: sticky;
  top: 0;
  /* Z-index нужен, чтобы содержимое body не перекрывало заголовок */
  z-index: 1;
  /* Фон обязателен, иначе заголовок будет прозрачным */
  background-color: #607d8b; /* Аналог bg-blue-grey-13 */
}

/* Опционально: если у таблицы есть границы, фиксируем их отображение */
.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
