<template>

  <div class="q-pa-md bg-amber-1 no-scroll">
    <q-banner
      dense
      inline-actions
      class="bg-orange-1"
      style="margin-bottom: 5px"
    >
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="home_work"></q-avatar>
        {{ $t("kato") }}
      </div>
      <template v-slot:action>
        <q-btn
          v-if="hasTarget('nsi:kato:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addRegion") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('nsi:kato:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addDistrict") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('nsi:kato:upd')"
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
          v-if="hasTarget('nsi:kato:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
        <q-inner-loading :showing="visible" color="secondary"/>
      </template>
    </q-banner>

    <div style="height: calc(100vh - 250px); width: 100%" class="scroll">
      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
      />

    </div>
  </div>

</template>


<script>
import {defineComponent} from "vue";
import {api} from "boot/axios";
import {collapsAll, getParentNode, hasTarget, notifyError, pack,} from "src/utils/jsutils";
import QTreeTable from "components/QTreeTable.vue";
import UpdaterKATO from "pages/kato/UpdaterKATO.vue";

export default defineComponent({
  name: "KatoPage",
  props: {},
  components: {QTreeTable},

  data: function () {
    return {
      selected: [],
      cols: [],
      rows: [],
      currentNode: null,
      visible: false,
    };
  },

  methods: {
    hasTarget,
    clearAny() {
      this.$refs.childComp.clrAny();
    },

    onUpdateSelect(data) {
      this.currentNode = data.selected !== undefined ? data.selected : null;
      //console.log("currentNode onUpdateSelect", this.currentNode)
    },

    fetchData() {
      this.visible = true;
      api
        .post('', {
          method: "data/loadKATO",
          params: [],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result["records"], "ord")
            collapsAll(this.rows)
          })
        .catch((error) => {
          let msg = error.message
          if (error.response)
            msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        })
        .finally(() => {
          this.visible = false;
        });
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },

      ];
    },

    fnIns(mode, isChild) {
      let data = {
        accessLevel: 1,
      };

      let parent = null
      let parentName = null

      if (isChild) {
        if (this.currentNode.parent) {
          parent = this.currentNode.parent
          let parentNode = []
          getParentNode(this.rows, this.currentNode.parent, parentNode)
          parentName = parentNode[0].fullName
        } else {
          parent = this.currentNode.id
          parentName = this.currentNode.fullName
        }
      }
      if (mode === "ins") {
        data.parent = parent
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          cls: this.currentNode.cls,
          parent: this.currentNode.parent,
          cod: this.currentNode.cod,
          accessLevel: this.currentNode.accessLevel,
          name: this.currentNode.name,
          fullName: this.currentNode.fullName,
          cmt: this.currentNode.cmt,
        }
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode)
          parentName = parentNode[0].fullName
          isChild = true
        }
      }
      this.$q
        .dialog({
          component: UpdaterKATO,
          componentProps: {
            mode: mode,
            isChild: isChild,
            parentName: parentName,
            data: data,
            // ...
          },
        })
        .onOk((data) => {
          //console.log("Ok! updated", data);
          this.fetchData()
          this.currentNode = data
          this.$refs.childComp.restoreSelect(data)
        })
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + rec.fullName + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: "data/deleteOwner",
              params: [rec.id, 1],
            })
            .then(
              () => {
                this.fetchData()
                this.clearAny()
                this.selected = []
                this.currentNode = null
              })
            .catch((error) => {
              if (error.response.data.error.message.includes("@")) {
                let msgs = error.response.data.error.message.split("@")
                let m1 = this.$t(`${msgs[0]}`)
                let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
                let msg = m1 + m2
                notifyError(msg)
              } else {
                notifyError(this.$t("hasChild"))
              }
            })
        })
    },

  },

  created() {
    this.cols = this.getColumns()
    this.fetchData()
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
