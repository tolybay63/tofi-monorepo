<template>
  <q-page class="q-pa-md bg-amber-1">
    <q-banner
      dense
      inline-actions
      class="bg-orange-1"
      style="margin-bottom: 5px"
    >
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="square_foot"></q-avatar>
        {{ $t("measures") }}
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
            {{ $t("createBaseMeasure") }}
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
            {{ $t("createDerivedMeasure") }}
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

    <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
        :FD_AccessLevel="FD_AccessLevel"
      />
    </div>

  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import UpdateMeasure from "pages/measure/UpdateMeasure.vue";
import QTreeTable from "components/QTreeTable.vue";

export default {
  name: "MeasurePage",
  components: {QTreeTable},

  data: function () {
    return {
      selected: ref([]),
      FD_AccessLevel: null,
      cols: [],
      rows: [],
      currentNode: null,
      visible: ref(false),
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
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "measure/load",
          params: [{}],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result.records, "ord");
            expandAll(this.rows);
          },
          (error) => {
            let msg
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            else msg = error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          //setTimeout(() => {
          this.visible = ref(false);
          //}, 3000)
        });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          headerStyle:
            "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:15%;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:15%;",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em;width:15%;",
          headerClass: "text-bold text-white bg-blue-grey-13 ",
          style: "text-align: left; width:15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:25%;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:25%",
        },

        {
          name: "kFromBase",
          label: this.$t("kfcMeasure"),
          field: "kFromBase",
          headerStyle:
            "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:10%;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: right; width:10%;",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:10%;",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:10%;",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:30%",
          headerClass: "text-bold text-white bg-blue-grey-13",
          style: "text-align: left; width:30%",
        },
      ];
    },

    fnIns(mode, isChild) {
      let data = {
        id: 0,
        cod: "",
        accessLevel: 1,
        kFromBase: 1,
        name: "",
        fullName: "",
        cmt: null,
      };

      let parent = null;
      let parentName = null;

      if (isChild) {
        if (this.currentNode.parent > 0) {
          parent = this.currentNode.parent
          //parentName = findParentNode(this.currentNode, this.rows).fullName
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          //console.log("ParentNode-----", parentNode)
          parentName = parentNode[0].fullName;
        } else {
          parent = this.currentNode.id;
          parentName = this.currentNode.fullName;
        }
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          parent: this.currentNode.parent,
          cod: this.currentNode.cod,
          accessLevel: this.currentNode.accessLevel,
          kFromBase: this.currentNode.kFromBase,
          name: this.currentNode.name,
          fullName: this.currentNode.fullName,
          cmt: this.currentNode.cmt,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          //console.log("ParentNode-----", parentNode)
          parentName = parentNode[0].fullName;

          isChild = true;
        }
      }
      this.$q
        .dialog({
          component: UpdateMeasure,
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
            .post(baseURL, {
              method: "measure/delete",
              params: [{rec: rec}],
            })
            .then(
              () => {
                this.fetchData();
                this.clearAny();
              },
              () => {
                notifyInfo(this.$t("hasChild"));
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },
  },

  created() {
    this.cols = this.getColumns();

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        this.FD_AccessLevel = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_AccessLevel.set(it["id"], it["text"]);
        });
      });

    this.fetchData();
  },

  setup() {}

};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}

</style>
