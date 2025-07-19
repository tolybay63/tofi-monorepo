<template>
  <div class="no-padding no-margin" style="height: calc(100vh - 250px)">
    <q-table
      style="max-height: 900px; width: 100%"
      color="primary"
      dense
      card-class="bg-amber-1 text-brown"
      row-key="obj"
      :columns="cols"
      :rows="rows"
      :filter="filter"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      selection="single"
      v-model:selected="selected"
      @update:selected="updateSelect"
      :rows-per-page-options="[0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t('selectedRow') }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t('infoRowExt') }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="free_cancellation"></q-avatar>
          {{ $t('researchResults') }}
        </div>

        <q-space/>
        <q-btn
          v-if="hasTarget('mon:rzp:bio:res:ins')"
          icon="post_add"
          dense
          color="secondary"
          :disable="loading || obj === 0"
          @click="editObjPropsChild(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('newRecord') }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:rzp:bio:res:upd')"
          icon="edit"
          dense
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0 || obj === 0"
          @click="editObjPropsChild(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('editRecord') }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:rzp:bio:res:del')"
          icon="delete"
          dense
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0 || obj === 0"
          @click="removeObjPropsChild(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('deletingRecord') }}
          </q-tooltip>
        </q-btn>

        <q-space/>
      </template>
    </q-table>
  </div>
</template>

<script>
import {api, baseURL} from 'boot/axios'
import {hasTarget, notifyError} from 'src/utils/jsutils'
import {extend} from 'quasar'
import UpdaterResFishingChild from 'pages/samplingresults/UpdaterResFishingChild.vue'

export default {
  name: 'SamplingResTabFishing',

  props: [
    'codCls', //cod класса забора
    'cls', //id класса исследования забора
    'obj',
    'title',
  ],

  data() {
    return {
      cols: [],
      rows: [],
      selected: [],
      loading: false,
      filter: '',
      mapMeasure: new Map(),
      mapTypeOfFish: new Map(),
      mapFV: new Map(),
    }
  },

  methods: {
    hasTarget,

    editObjPropsChild(row, mode) {
      let data = {parent: this.obj, cls: this.cls}
      if (mode === 'upd') {
        extend(true, data, row)
      }

      this.$q
        .dialog({
          component: UpdaterResFishingChild,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
            this.updateSelect()
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

    removeObjPropsChild(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') + '</br>(' + this.mapTypeOfFish.get(row.objFishTyp) + ')',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
            this.$axios
              .post(baseURL, {
                method: 'data/deleteOwnerWithProperties',
                params: [row.obj, 1],
              })
              .then(
                () => {
                  this.selected = []
                  if (this.rows.length > 0) {
                    this.loadChildProps(this.obj)
                  } else {
                    this.selected = []
                    //this.splitterModel = 100
                  }
                },
                (error) => {
                  //console.log(error.message)
                  let msg = error.message
                  if (error.response) msg = error.response.data.error.message
                  notifyError(msg)

                }
              )
          }
        )
    },

    loadChildProps(obj) {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadResultSamplingChild',
          params: [obj, false, 0],
        })
        .then((response) => {
          this.rows = response.data.result.records
          //console.info("rows child", this.rows)
        })
        .catch((error) => {
          if (error.response.data.error.message.includes('@')) {
            let msgs = error.response.data.error.message.split('@')
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        })
        .finally(() => {
          this.loading = false
        })
    }
    ,

    updateSelect() {
    }
    ,

    infoSelected(row) {
      return ' ' + this.mapTypeOfFish.get(row.objFishTyp)
    }
    ,

    getColumns() {
      return [
        {
          name: 'objFishTyp',
          label: this.$t('typeOfFish'),
          field: 'objFishTyp',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:28%',
          format: (v) => (this.mapTypeOfFish ? this.mapTypeOfFish.get(v) : null),
        },
        {
          name: 'FishAge',
          label: this.$t('FishAge') + ', ' + this.mapMeasure['Prop_FishAge']['name'],
          field: 'FishAge',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 18%',
        },
        {
          name: 'FishWeight',
          label: this.$t('FishWeight') + ', ' + this.mapMeasure['Prop_FishWeight']['name'],
          field: 'FishWeight',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:18%',
        },
        {
          name: 'FishLength',
          label: this.$t('FishLength') + ', ' + this.mapMeasure['Prop_FishLength']['name'],
          field: 'FishLength',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 18%',
        },
        {
          name: 'fvFishGender',
          label: this.$t('FishGender'),
          field: 'fvFishGender',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 18%',
          format: (v) => (this.mapFV ? this.mapFV[v] : null),
        },
      ]
    }
    ,

    loadFishTyp() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadTypeOfFish',
          params: ['Cls_FishTypes', 'Prop_FishTyp'],
        })
        .then(
          (response) => {
            response.data.result.records.forEach((it) => {
              this.mapTypeOfFish.set(it["id"], it["name"])
            })
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = false
        })
    }
    ,

    loadFishGender() {
      api
        .post(baseURL, {
          method: 'data/mapFvNameFromId',
          params: [],
        })
        .then((response) => {
          this.mapFV = response.data.result
        })
        .catch((error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        })
    }
    ,
  },

  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/measureInfo',
        params: [],
      })
      .then((response) => {
        this.mapMeasure = response.data.result
      })
      .then(() => {
        this.loadFishTyp()
      })
      .then(() => {
        this.loadFishGender()
      })
      .then(() => {
        this.cols = this.getColumns()
      })
  }
  ,
}
</script>

<style scoped></style>
