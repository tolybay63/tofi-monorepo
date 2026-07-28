<template>
  <q-page class="q-pa-md bg-amber-1">
    <q-banner
      dense
      inline-actions
      class="bg-orange-1"
      style="margin-bottom: 5px"
    >
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="apartment"></q-avatar>
        {{ $t("struct_enterprise") }}
      </div>
      <template v-slot:action>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:mea:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addEnt") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:mea:ins')"
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
          v-if="hasTarget('mdl:mn_ds:mea:upd')"
          dense
          icon="edit"
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
          v-if="hasTarget('mdl:mn_ds:mea:del')"
          dense
          icon="delete"
          color="red"
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

    <div style="height: calc(100vh - 250px); width: 100%">
      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
      />
    </div>

  </q-page>
</template>

<script>
import {ref} from "vue";
import {api} from "../..//boot/axios";
import {expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "../../utils/jsutils";
import QTreeTable from "../../components/QTreeTable.vue";
import UpdaterStructEnterprise2 from "./UpdaterStructEnterprise2.vue";

export default {
  name: "StructEnterprisePage",
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
        .post("", {
          method: "data/loadEnterprise",
          params: ["Typ_Enterprise"],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result.records, "ord");
            expandAll(this.rows);
          },
          (error) => {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            console.error(msg);
          }
        )
        .finally(() => {
          //setTimeout(() => {
          this.visible = false;
          //}, 3000)
        });
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em;",
          headerClass: "text-bold text-white bg-blue-grey-13 ",
          style: "text-align: left; width:20%",
        },
        {
          name: "fullname",
          label: this.$t("fldFullName"),
          field: "fullname",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:30%",
        },
        {
          name: "namecls",
          label: this.$t("cls"),
          field: "namecls",
          headerStyle: "font-size: 1.3em;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: right; width:20%;",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:30%",
        },
      ];
    },

    fnIns(mode, isChild) {
      let data = {
      };

      let parent = null;
      let parentName = null;

      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.fullname;


/*
        if (this.currentNode.parent > 0) {
          parent = this.currentNode.parent
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          console.log("ParentNode-----", parentNode)
          parentName = parentNode[0].fullname;

        } else {
          parent = this.currentNode.id;
          parentName = this.currentNode.fullname;
        }
*/
      }
      console.log("ParentNode-----2", parentName)
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          parent: this.currentNode.parent,
          name: this.currentNode.name,
          cls: this.currentNode.cls,
          nameCls: this.currentNode.namecls,
          fullName: this.currentNode.fullname,
          cmt: this.currentNode.cmt,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          parentName = parentNode[0].fullname;
          isChild = true;
        }
      }
      this.$q
        .dialog({
          component: UpdaterStructEnterprise2,
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
          this.fetchData();
          this.currentNode = data
          this.$refs.childComp.restoreSelect(data)

        });
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.cod +
            ": " +
            rec.name +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post("", {
              method: "data/deleteEnterprise",
              params: [rec.id],
            })
            .then(
              () => {
                this.fetchData();
                this.clearAny();
              })
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },
  },

  created() {
    this.cols = this.getColumns();

    this.fetchData();
  },

  setup() {}

};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}

</style>
