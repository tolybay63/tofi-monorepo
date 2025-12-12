<template>

<!--        style="margin-bottom: 5px" -->
  <div class="q-pa-md no-scroll">
    <q-banner
      dense
      inline-actions
      class="bg-grey-5"
      style="margin-bottom: -5px"
    >

      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="home_work"></q-avatar>
        {{ $t("typesOfFish") }}
        <span v-if="currentNode">
          <span style="color: #1976d2; margin-left: 5px;font-size: 0.8em;">
            {{ $t("selectedNode") }}:
            <span style="font-size: 0.9em; color: #0f1010; font-weight: bold">
              {{ this.nodeInfo() }}
            </span>
          </span>

        </span>
      </div>
      <template v-slot:action>
        <q-btn
          dense
          icon="expand_more"
          color="secondary"
          @click="fnExpand()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          dense
          icon="expand_less"
          color="secondary"
          class="q-ml-sm"
          @click="fnCollapse()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:vr:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("add") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:vr:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addChild") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:vr:upd')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('upd', false)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mon:vr:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="removeRow(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div style="height: calc(100vh - 280px); width: 100%; margin-top: -5px" class="scroll">

      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
        :FD_FishType="mapFV"
      >
      </QTreeTable>

    </div>

    <q-banner style="background-color: #bdbdbd" >Всего записей: {{this.cnt}} </q-banner>
  </div>

</template>


<script>
import {defineComponent} from 'vue'
import {api} from "boot/axios";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack} from 'src/utils/jsutils'
import QTreeTable from "components/QTreeTable.vue";
import UpdaterTypesFish from 'pages/typesfish/UpdaterTypesFish.vue'
import {extend} from "quasar";

export default defineComponent({
  name: "TypesFishPage",
  props: {},
  components: {QTreeTable},

  data: function () {
    return {
      selected: [],
      cols: [],
      rows: [],
      currentNode: null,
      visible: false,
      cls: null,
      mapFV: new Map(),
      cnt: 0
    };
  },

  methods: {
    hasTarget,

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = this.currentNode.name
          ? this.currentNode.name
          : this.currentNode.cod ? this.currentNode.cod : "";
      }
      return res;
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    onUpdateSelect(data) {
      this.currentNode = data.selected !== undefined ? data.selected : null;
      //console.log("currentNode onUpdateSelect", this.currentNode)
    },

    loadFishFamily() {
      this.visible = true
      api
        .post('', {
          method: 'data/loadFishFamily',
          params: [{ codCls: 'Cls_FishTypes', isRec: false, idObj: 0 }],
        })
        .then((response) => {
          this.cnt = response.data.result.records.length
          this.rows = pack(response.data.result.records, "ord")
        })
        .then(() => {
          api
            .post('', {
              method: 'data/mapFvNameFromId',
              params: [],
            })
            .then((response) => {
              this.mapFV = response.data.result
              //console.info("mapFV", this.mapFV)
            })
        })
        .catch((error) => {
          let msg = error.message
          if (error.response) {
            if (error.response.data.error.message.includes('@')) {
              let msgs = error.response.data.error.message.split('@')
              let m1 = this.$t(`${msgs[0]}`)
              let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
              msg = m1 + m2
              notifyError(msg)
            } else {
              notifyError(this.$t(error.response.data.error.message))
            }
          } else {
            notifyError(msg)
          }
        })
        .finally(() => {
          //setTimeout(()=> {
          this.visible = false
          //}, 5000)
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
          headerStyle: 'font-size: 1.2em; width:30%',
        },
        {
          name: 'fvFishFamily',
          label: this.$t('fishFamily')+"*",
          field: 'fvFishFamily',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%',
          format: (v) => (this.mapFV ? this.mapFV[v] : null),
        },
        {
          name: 'cmt',
          label: this.$t('fldCmt'),
          field: 'cmt',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 50%',
        },
      ];
    },

    fnIns(mode, isChild) {
      let data = { accessLevel: 1, cls: this.cls };

      let parent = null
      let parentName = null
      let fvFishFamily = null
      let pvFishFamily = null

      if (isChild) {
        //console.info("isChild this.currentNode", this.currentNode)
        if (this.currentNode.parent) {
          parent = parseInt(this.currentNode.parent, 10)
          let parentNode = []
          getParentNode(this.rows, parent, parentNode)
          parentName = parentNode[0].name
          fvFishFamily = parentNode[0].fvFishFamily
          pvFishFamily = parentNode[0].pvFishFamily
        } else {
          parent = this.currentNode.id
          parentName = this.currentNode.name
          fvFishFamily = this.currentNode.fvFishFamily
          pvFishFamily = this.currentNode.pvFishFamily

        }
      }
      if (mode === "ins") {
        data.parent = parent
      } else if (mode === "upd") {
        data = extend(true, {}, this.currentNode)
        parent = this.currentNode.parent ? parseInt(this.currentNode.parent, 10) : 0
        if (parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, parent, parentNode)
          parentName = parentNode[0].name
          fvFishFamily = parentNode[0].fvFishFamily
          pvFishFamily = parentNode[0].pvFishFamily
          isChild = true
        }
      }


      if (isChild) {
        data.fvFishFamily = fvFishFamily
        data.pvFishFamily = pvFishFamily
      }

      //console.info(mode, this.currentNode)
      this.$q
        .dialog({
          component: UpdaterTypesFish,
          componentProps: {
            mode: mode,
            isChild: isChild,
            parentName: parentName,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          this.loadFishFamily()
          this.currentNode = r
          this.$refs.childComp.restoreSelect(r)
        })
    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') +
            '<div style="color: plum">(' +
            row.name +
            ' - ' +
            this.mapFV[row.fvFishFamily] +
            ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1],
            })
            .then(() => {
              this.loadFishFamily()
              this.selected = []
            })
            .catch((error) => {
              console.log(error.message)
              if (error.response.data.error)
                  notifyError(error.response.data.error.message)
              else
                  notifyError(this.$t("hasChild"))
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

  },

  created() {
    this.visible = true
    api
      .post('', {
        method: 'data/loadClsId',
        params: ['Cls_FishTypes'],
      })
      .then(
        (response) => {
          this.cls = response.data.result
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.visible = false
      })
    this.cols = this.getColumns()
    this.loadFishFamily()
  },

  setup() {
    return {};
  },

})

</script>

<style scoped>
.img-vert {
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
