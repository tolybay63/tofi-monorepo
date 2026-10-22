<template>
  <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
    <template v-slot:action>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:complex:ins')"
        dense
        icon="post_add"
        color="secondary"
        class="q-ml-sm img-vert"
        @click="fnIns('ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("createItemComplex") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:complex:upd')"
        dense
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        @click="fnIns('upd')"
        :disable="
          currentNode == null ||
          (currentNode.parent == null)
        "
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:complex:del')"
        dense
        icon="delete"
        color="red"
        class="q-ml-sm"
        @click="fnDel(currentNode)"
        :disable="currentNode == null || currentNode.parent == null"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>

      <q-space />
    </template>
  </q-banner>

  <QTreeTable
    :cols="cols"
    :rows="rows"
    :icon_leaf="''"
    @updateSelect="onUpdateSelect"
    :meterStruct="meterStruct"
    :FD_PropType="FD_PropType"
    ref="childComp"
    checked_visible="true"
  />
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useQuasar } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import allConsts from "@/pages/all-consts";
import { api } from "@/boot/axios";
import UpdateProp from "@/pages/prop/UpdateProp.vue";
import { collapsAll, expandAll, hasTarget, notifyError, notifyInfo, pack } from "@/utils/jsutils";

const props = defineProps({
  parentNode: {
    type: Object,
    required: true,
  },
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const loading = ref(false);
const dense = ref(true);

const FD_PropType = reactive(new Map());
const meterStruct = ref(0);
const lang = ref("");
const childComp = ref(null);

const onUpdateSelect = (item) => {
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    api
      .post("", {
        method: "prop/loadRec",
        params: [currentNode.value.id],
      })
      .then((response) => {
        currentNode.value = response.data.result.records[0];
        if (currentNode.value.statusFactor)
          checkStatus(currentNode.value);
        if (currentNode.value.providerTyp)
          checkProvider(currentNode.value);
      })
      .finally(() => {
        if (
          currentNode.value.propType === allConsts.FD_PropType.meter ||
          currentNode.value.propType === allConsts.FD_PropType.rate
        ) {
          api
            .post("", {
              method: "meter/loadRec",
              params: [{ id: currentNode.value.meter }],
            })
            .then((response) => {
              meterStruct.value = response.data.result.records[0].meterStruct;
            });
        }
      });
  }
};

const fetchData = (prop) => {
  loading.value = true;
  api
    .post("", {
      method: "prop/loadPropComplex",
      params: [prop],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "ord");
        fnExpand();
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const edit = (data, mode) => {
  if (
    mode === "ins" &&
    (data.propType === allConsts.FD_PropType.typ ||
      data.propType === allConsts.FD_PropType.reltyp ||
      data.propType === allConsts.FD_PropType.factor ||
      data.propType === allConsts.FD_PropType.attr ||
      data.propType === allConsts.FD_PropType.complex)
  ) {
    data.isUniq = true;
    data.allItem = false;
    data.visualFormat = allConsts.FD_VisualFormat.short;
  }
  const lg = lang.value;
  $q.dialog({
    component: UpdateProp,
    componentProps: {
      rec: data,
      mode: mode,
      parentName: props.parentNode.name,
      isComplexProp: true,
      lg: lg,
    },
  }).onOk((resData) => {
    if (resData.res) {
      fetchData(props.parentNode.id);
    }
  });
};

const fnIns = (mode) => {
  if (mode === "ins") {
    api
      .post("", {
        method: "prop/newRecComplex",
        params: [props.parentNode],
      })
      .then((response) => {
        edit(response.data.result.records[0], mode);
      });
  } else {
    edit(currentNode.value, mode);
  }
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
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "prop/delete",
          params: [{ rec: rec }],
        })
        .then(() => {
          childComp.value?.clrAny();
          fetchData(props.parentNode.id);
        })
        .catch((error) => {
          let errMessage = error.response?.data?.error?.message || error.message;
          if (errMessage.includes("@")) {
            let msgs = errMessage.split("@");
            let m1 = proxy?.$t(`${msgs[0]}`) || msgs[0];
            let m2 = msgs.length > 1 ? " [" + msgs[1] + "]" : "";
            let msg = m1 + m2;
            notifyError(msg);
          } else {
            notifyError(errMessage);
          }
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const checkStatus = (item) => {
  let prop = item.id;
  api
    .post("", {
      method: "prop/checkStatus",
      params: [prop],
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t("notStatus") || "notStatus");
      }
    });
};

const checkProvider = (item) => {
  let prop = item.id;
  api
    .post("", {
      method: "prop/checkProvider",
      params: [prop],
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t("notProvider") || "notProvider");
      }
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "propType",
    label: proxy?.$t("propType") || "propType",
    field: "propType",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PropType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PropType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  fetchData(props.parentNode.id);
});
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
