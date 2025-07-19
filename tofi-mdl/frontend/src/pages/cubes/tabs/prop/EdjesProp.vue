<template>

  <q-bar class="q-pa-md bg-amber-1" style="height: 50px">
    <q-btn
      dense icon="expand_more" color="secondary" @click="fnExpand()" style="margin-bottom: 5px" size="16px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("expandAll") }}
      </q-tooltip>
    </q-btn>
    <q-btn
      dense icon="expand_less" color="secondary"
      class="q-ml-sm q-mr-lg" @click="fnCollapse()" style="margin-bottom: 5px" size="16px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("collapseAll") }}
      </q-tooltip>
    </q-btn>

    <div v-if="showAction">
      <q-btn
        dense icon="save" color="secondary" class="q-ml-lg" :loading="loading" :disable="btnDisabled()"
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
        dense icon="delete" color="secondary" class="q-ml-lg" :loading="loading" :disable="!btnDisabled()"
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

  </q-bar>

  <div class="q-pa-sm-sm bg-grey-2 scroll" style="height: calc(100vh - 350px); width: 100%">
    <table class="q-table q-table--cell-separator q-table--bordered wrap">
      <thead class="text-bold text-white bg-blue-grey-13">
      <tr class>
        <th
          v-for="(col, index) in cols"
          :key="index"
          :class="col.headerClass"
          :style="col.headerStyle"
        >
          {{ col.label }}
        </th>
      </tr>
      </thead>

      <tbody style="background: aliceblue">
      <tr v-for="(item, index) in arrayTreeObj" :key="index">
        <td :data-th="cols0[0].name" @click="toggle(item, index)">
          <div class="row">
            <div class="q-tree__node q-ma-xs-xs" v-bind:style="setPadding(item)">
              <q-icon
                :name="iconName(item)" color="secondary" size="16px" style="cursor: pointer"
              />
            </div>
            <div class="q-mb-xs" style="align-content: end">
              {{ item[cols0[0].field] }}
            </div>
          </div>
        </td>
        <!--other cols without 0-->
        <td v-for="(col, i) in cols_" :key="i" :data-th="col.name">
          <div v-if="col.name==='val'">
            <treeselect
              :disabled="!showSelect"
              :options="optProp"
              v-model="item['val']"
              :normalizer="normalizerProp"
              :placeholder="$t('select')"
              :noResultsText="$t('noResult')"
              :noChildrenText="$t('noChilds')"
              :noOptionsText="$t('noResult')"
              defaultExpandLevel="1"
              @select="fnSelectProp(item)"
              @close="fnClose"
              before-list="Open..."
              after-list="Close..."
            />
            <q-item-label v-if="item['val'] < 0" class="text-red-10" style="font-size: 0.8em">
              <q-icon name="error" color="red-10" size="24px"></q-icon>
              {{ $t("chooseProp") }}
            </q-item-label>
          </div>
          <div v-else>
            {{ item[col.field] }}
          </div>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import {collaps, collapsAll, expand, expandAll, notifyError, notifySuccess, pack} from "src/utils/jsutils.js";
import {api, baseURL} from "boot/axios.js";
import treeselect from "vue3-treeselect";


export default {
  name: "EdjesProp",
  components: {treeselect},
  props: ["cols", "rows", "propField", "cubeS", "showAction", "showSelect", "mapping"],
  emits: [
    "updateDelete",
    "updateBind"
  ],

  data() {
    return {
      loading: false,
      isExpanded: false,
      itemId: null,
      optProp: [],
      selDPI: 0,
    }
  },

  methods: {
    fnClose(v) {
      if (v === undefined)
        v = 0
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/selectValue",
          params: [this.cubeS, this.selDPI, v, this.mapping, this.propField],
        })
        .then(() => {
          notifySuccess("success")
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

    fnSelectProp(item) {
      //console.log("fnSelectProp", item)
      this.selDPI = item["id"]
    },

    normalizerProp(node) {
      return {
        id: node.prop,
        label: node.name,
      };
    },

    fnBind() {
      this.$emit("updateBind", {});
    },

    fnUnBind() {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/unbinding",
          params: [this.cubeS, this.mapping],
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

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    recursive(obj, newObj, level, itemId, isExpend) {
      let vm = this;
      obj.forEach(function (o) {
        if (o.children && o.children.length !== 0) {
          o.level = level;
          o.leaf = false;
          newObj.push(o);
          if (o.id === itemId) {
            o.expend = isExpend;
          }
          if (o.expend) {
            vm.recursive(o.children, newObj, o.level + 1, itemId, isExpend);
          }
        } else {
          o.level = level;
          o.leaf = true;
          newObj.push(o);
          return false;
        }
      });
    },

    iconName(item) {
      if (item.expend) {
        return "remove_circle_outline";
      }
      if (item.children && item.children.length > 0) {
        return "control_point";
      }
      return "";
    },

    toggle(item) {
      if (item.children && item.children.length > 0) {
        if (item.expend) collaps(item);
        else expand(item);
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },

    btnDisabled() {
      let isBined = false;
      this.arrayTreeObj.forEach((item) => {
        if (item["val"])
          isBined = true;
      })
      return isBined
    }

  },

  computed: {
    cols0() {
      return this.cols.slice(0, 1);
    },

    cols_() {
      return this.cols.slice(1);
    },

    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  mounted() {
    //console.log("Component mounted.");
    //console.info("propField", this.propField)
    this.visible = true;
    api
      .post(baseURL, {
        method: "prop/loadPropForSelect",
        params: [this.propField],
      })
      .then((response) => {
        this.optProp = pack(response.data.result.records, "id");
        console.info("this.optProp", this.optProp);
      })
      .finally(() => {
        this.visible = false;
      });

  },

  created() {
    console.log("Component created.");
  }

}
</script>

<style scoped>

</style>
