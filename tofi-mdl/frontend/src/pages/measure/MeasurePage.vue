<template>
  <q-page class="q-pa-md bg-amber-1">
    <q-banner
      dense
      inline-actions
      class="bg-orange-1"
      style="margin-bottom: 5px"
    >
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="square_foot" />
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
          color="red"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-inner-loading :showing="visible" color="secondary" />
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
        :FD_AccessLevel="FD_AccessLevel"
      />
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { expandAll, getParentNode, hasTarget, notifyInfo, pack } from "@/utils/jsutils";
import UpdateMeasure from "@/pages/measure/UpdateMeasure.vue";
import QTreeTable from "@/components/QTreeTable.vue";

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const selected = ref([]);
const FD_AccessLevel = reactive(new Map());
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const visible = ref(false);
const childComp = ref(null);

const clearAny = () => {
  childComp.value?.clrAny();
};

const onUpdateSelect = (data) => {
  currentNode.value = data.selected !== undefined ? data.selected : null;
};

const fetchData = () => {
  visible.value = true;
  api
    .post("", {
      method: "measure/load",
      params: [{}],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "ord");
        expandAll(rows.value);
      },
      (error) => {
        let msg = error.message;
        if (error.response) {
          msg = proxy?.$t(error.response.data.error.message) || msg;
        }
        console.error(msg);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    headerStyle:
      "font-size: 1.3em; color: #1976d2; background: antiquewhite; width:15%;",
    headerClass: "text-bold text-white bg-blue-grey-13",
    style: "text-align: left; width:15%;",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
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
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width:25%;",
    headerClass: "text-bold text-white bg-blue-grey-13",
    style: "text-align: left; width:25%",
  },
  {
    name: "kFromBase",
    label: proxy?.$t("kfcMeasure") || "kfcMeasure",
    field: "kFromBase",
    headerStyle: "font-size: 1.3em; width:10%;",
    headerClass: "text-bold text-white bg-blue-grey-13",
    style: "text-align: right; width:10%;",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width:10%;",
    headerClass: "text-bold text-white bg-blue-grey-13",
    style: "text-align: left; width:10%;",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width:30%",
    headerClass: "text-bold text-white bg-blue-grey-13",
    style: "text-align: left; width:30%",
  },
];

const fnIns = (mode, isChild) => {
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
    if (currentNode.value.parent > 0) {
      parent = currentNode.value.parent;
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0]?.fullName;
    } else {
      parent = currentNode.value.id;
      parentName = currentNode.value.fullName;
    }
  }
  if (mode === "ins") {
    data.parent = parent;
  } else if (mode === "upd") {
    data = {
      id: currentNode.value.id,
      parent: currentNode.value.parent,
      cod: currentNode.value.cod,
      accessLevel: currentNode.value.accessLevel,
      kFromBase: currentNode.value.kFromBase,
      name: currentNode.value.name,
      fullName: currentNode.value.fullName,
      cmt: currentNode.value.cmt,
    };
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0]?.fullName;
      isChild = true;
    }
  }

  $q.dialog({
    component: UpdateMeasure,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  }).onOk((resData) => {
    fetchData();
    currentNode.value = resData;
    childComp.value?.restoreSelect(resData);
  });
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
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
      api
        .post("", {
          method: "measure/delete",
          params: [rec],
        })
        .then(() => {
          fetchData();
          clearAny();
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AccessLevel.set(it.id, it.text);
      });
    });

  fetchData();
});
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
