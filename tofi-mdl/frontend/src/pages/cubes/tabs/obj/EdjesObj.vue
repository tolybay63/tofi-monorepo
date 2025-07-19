<script>
import {api, baseURL} from "boot/axios.js";
import {notifyError, notifySuccess} from "src/utils/jsutils.js";

export default {
  name: "EdjesObj",
  props: ["cols", "rows", "cubeSFace", "do1", "do2", "optRC", "dimCount", "doi3", "doi4", "cl3", "cl4", "face"],
  emits: [
    "updateDelete",
    "updateBind"
  ],

  data() {
    return {
      loading: false,
      rc: new Map(),
      showAction: true,
      showSelect: true,
    }
  },

  methods: {

    fnColor(cl1, cl2) {
      if (this.dimCount === 2) {
        if (this.optRC[cl1 + "_" + cl2].length === 0)
          return "red-2"
      } else if (this.dimCount === 3) {
        if (this.face === "face2") {
          if (this.optRC[cl1 + "_" + cl2].length === 0)
            return "red-2"
        } else {
          if (this.optRC[cl1 + "_" + cl2 + "_" + this.cl3].length === 0)
            return "red-2"
        }
      } else if (this.dimCount === 4) {
        if (this.face === "face2") {
          if (this.optRC[cl1 + "_" + cl2].length === 0)
            return "red-2"
        } else if (this.face === "face3") {
          if (this.optRC[cl1 + "_" + cl2 + "_" + this.cl3].length === 0)
            return "red-2"
        } else if (this.face === "face4") {
          if (this.optRC[cl1 + "_" + cl2 + "_" + this.cl3 + "_" + this.cl4].length === 0)
            return "red-2"
        }
      } else
        return "red-2"
    },

    fnSelectRC(v, r, c) {
      this.saveCubesFace(v.id, v.reltyp, r.doi, c.name.split('_')[1], r.id, c.name.split('_')[0])
    },

    saveCubesFace(rc, rt, doi1, doi2, cl1, cl2) {
      let params = [this.cubeSFace, rc, rt, doi1, doi2, cl1, cl2, 0, 0, 0, 0]
      if (this.dimCount === 3)
        params = [this.cubeSFace, rc, rt, doi1, doi2, cl1, cl2, this.doi3 ? this.doi3 : 0, this.cl3 ? this.cl3 : 0, 0, 0]
      if (this.dimCount === 4)
        params = [this.cubeSFace, rc, rt, doi1, doi2, cl1, cl2,
          this.doi3 ? this.doi3 : 0, this.cl3 ? this.cl3 : 0, this.doi4 ? this.doi4 : 0, this.cl4 ? this.cl4 : 0]

      api
        .post(baseURL, {
          method: "cubes/saveCubesFace",
          params: params,
        })
        .then(() => {
          notifySuccess(this.$t("success"))
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })
    },

    getOptRC(c1, c2) {
      if (this.dimCount === 2) {
        return this.optRC[c1 + "_" + c2.split("_")[0]]
      } else if (this.dimCount === 3) {
        if (this.face === "face2") {
          return this.optRC[c1 + "_" + c2.split("_")[0]]
        } else {
          return this.optRC[c1 + "_" + c2.split("_")[0] + "_" + this.cl3]
        }
      } else if (this.dimCount === 4) {
        if (this.face === "face2") {
          return this.optRC[c1 + "_" + c2.split("_")[0]]
        } else if (this.face === "face3") {
          return this.optRC[c1 + "_" + c2.split("_")[0] + "_" + this.cl3]
        } else if (this.face === "face4") {
          return this.optRC[c1 + "_" + c2.split("_")[0] + "_" + this.cl3 + "_" + this.cl4]
        }
      }
    },

    isCol(props) {
      return props.col.label !== "";
    },

    fnBind() {
      //setTimeout(()=> {
        this.$emit("updateBind", {});
      //}, 3000)

    },

    fnUnBind() {

      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/unbindingDO",
          params: [this.cubeSFace, this.do1, this.do2, this.cl3 ? this.cl3 : 0, this.cl4 ? this.cl4 : 0],
        })
        .then(() => {
          this.$emit("updateDelete", {res: true});

        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })

    },
  }

}
</script>

<template>
  <div class="q-pa-sm">
    <q-table
      style="height: calc(100vh - 320px); width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      dense
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :rows-per-page-options="[0]"
    >

      <template #body-cell="props">
        <q-td :props="props">
          <div v-if="isCol(props)">
            <q-select
              v-model="props.row[props.col.name]"
              :model-value="props.row[props.col.name]"
              :options="getOptRC(props.row.id, props.col.name.split('_')[0])"
              option-value="id" option-label="name"
              map-options dense options-dense
              class="q-mb-md" :bg-color="fnColor(props.row.id, props.col.name.split('_')[0])"
              @update:model-value="fnSelectRC($event,props.row, props.col)"
            />

          </div>
          <div v-else>
            {{ props.value }}
          </div>
        </q-td>
      </template>

      <template v-slot:top>

        <div v-if="showAction">
          <q-btn
            dense icon="save" color="secondary" class="q-ml-lg" :loading="loading"
            @click="fnBind()" style="margin-bottom: 5px" :label="$t('bind')" no-caps size="16px"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("bind") }}
            </q-tooltip>

            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>

          </q-btn>

          <q-btn
            dense icon="delete" color="secondary" class="q-ml-lg" :loading="loading"
            @click="fnUnBind()" style="margin-bottom: 5px" :label="$t('delbind')" no-caps size="16px"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("delbind") }}
            </q-tooltip>
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>

        </div>

      </template>

    </q-table>

  </div>
</template>

<style scoped>

</style>
