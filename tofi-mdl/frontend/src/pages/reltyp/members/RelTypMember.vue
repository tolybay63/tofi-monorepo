<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 250px); width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :dense="dense"
      selection="single"
      v-model:selected="selected"
      @update:selected="updSelected"
      :rows-per-page-options="[0]"
    >
      <template v-slot:body="props">
        <q-tr :props="props">
          <td style="width: 5px">
            <q-btn
              :dense="dense"
              flat
              color="blue"
              :icon="
                selected.length === 1 && props.row.id === selected[0].id
                  ? 'check_box'
                  : 'check_box_outline_blank'
              "
              @click="selectedRow(props.row)"
            />
          </td>
          <td key="name" :props="props">
            {{ props.row.name }}
          </td>

          <td key="fullName" :props="props">
            {{ props.row.fullName }}
          </td>

          <td key="memberType" :props="props">
            <q-icon
              size="24px"
              :name="getIcon(props.row)"
              :color="getIconColor()"
            />
            {{ getMemberType(props.row.memberType) }}
          </td>

          <td key="memberName" :props="props">
            {{ props.row.memberName }}
          </td>

          <td key="cmt" :props="props">
            {{ props.row.cmt }}
          </td>
        </q-tr>
      </template>

      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td
          colspan="100%"
          v-else-if="rows.length > 0"
          class="text-bold"
        >
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("membersRel") }}</div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
          :dense="dense"
          icon="post_add"
          color="secondary"
          :disable="loading"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
          :dense="dense"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:del')"
          :dense="dense"
          icon="delete"
          color="red"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:up')"
          :dense="dense"
          icon="swipe_up_alt"
          color="secondary"
          class="q-ml-lg"
          @click="fnUp(true)"
          :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:down')"
          :dense="dense"
          icon="swipe_down_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(false)"
          :disable="onoffDown()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("down") }}
          </q-tooltip>
        </q-btn>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary" />
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import FD_Consts from "@/pages/all-consts.js";
import UpdateRelTypMember from "@/pages/reltyp/members/UpdateRelTypMember.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const FD_MemberType = reactive(new Map());
const dense = ref(true);
const maxLen = ref(0);
const reltypId = ref(0);

const getIconColor = () => {
  return "orange";
};

const getIcon = (row) => {
  if (row.memberType === 1) return "square";
  else if (row.memberType === 2) return "group_work";
};

const getMemberType = (val) => {
  return FD_MemberType ? FD_MemberType.get(val) : null;
};

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
    updSelected();
  }
};

const fnUp = (up) => {
  api
    .post("", {
      method: "reltypmember/changeOrdMember",
      params: [{ rec: selected.value[0], up: up }],
    })
    .then(
      () => {
        fetchData(reltypId.value);
      },
      (error) => {
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    );
};

const indexOf = (id) => {
  let rez = -1;
  rows.value.forEach((row, index) => {
    if (row.id === id) {
      rez = index;
    }
  });
  return rez;
};

const onoffUp = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) <= 0;
  }
};

const onoffDown = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) >= maxLen.value - 1;
  }
};

const updSelected = () => {};

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "reltypmember/deleteRelTypMember",
          params: [rec],
        })
        .then(() => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    name: "",
    fullName: "",
    relTyp: reltypId.value,
    memberType: FD_Consts.FD_MemberType.typ,
    card: 0,
    typ: 0,
    relTypMemb: 0,
    role: 0,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      name: rec.name,
      fullName: rec.fullName,
      relTyp: rec.relTyp,
      memberType: rec.memberType,
      card: rec.card,
      typ: rec.typ,
      relTypMemb: rec.relTypMemb,
      role: rec.role,
      ord: rec.ord,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateRelTypMember,
    componentProps: {
      data: data,
      mode: mode,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [];
      selected.value.push(r);
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    }
  });
};

const fetchData = (reltyp) => {
  loading.value = true;

  api
    .post("", {
      method: "reltypmember/loadRelTypMember",
      params: [reltyp],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      maxLen.value = rows.value.length;

      if (selected.value.length > 0) {
        let curId = selected.value[0].id;
        selected.value = [];
        let index = rows.value.findIndex((row) => row.id === curId);
        if (index > -1) {
          selected.value[0] = rows.value[index];
        }
      }
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
  {
    name: "memberType",
    label: proxy?.$t("memberType") || "memberType",
    field: "memberType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) =>
      FD_MemberType ? FD_MemberType.get(val) : null,
  },
  {
    name: "memberName",
    label: proxy?.$t("memberName") || "memberName",
    field: "memberName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
];

const infoSelected = (row) => {
  return " " + row.name;
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_MemberType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_MemberType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();

  reltypId.value = parseInt(route.params.reltyp, 10);
  fetchData(reltypId.value);
});
</script>

<style scoped></style>
