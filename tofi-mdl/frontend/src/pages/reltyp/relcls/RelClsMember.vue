<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: 100%; width: 100%"
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
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("membersRelCls") }}</div>

        <q-space />

        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
          :dense="dense"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
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
import { ref, reactive, onMounted, getCurrentInstance, defineExpose } from "vue";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import UpdateRelClsMember from "@/pages/reltyp/relcls/UpdateRelClsMember.vue";

const props = defineProps({
  relcls: [Number, String],
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const FD_MemberType = reactive(new Map());
const dense = ref(true);
const lang = ref("");

const getIconColor = () => {
  return "orange";
};

const getIcon = (row) => {
  if (row.memberType === 3) return "square";
  else if (row.memberType === 4) return "group_work";
};

const getMemberType = (val) => {
  return FD_MemberType ? FD_MemberType.get(val) : null;
};

const updSelected = () => {};

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
};

const editRow = (rec) => {
  let data = {
    id: rec.id,
    relCls: rec.relCls,
    name: rec.name,
    fullName: rec.fullName,
    memberType: rec.memberType,
    card: rec.card,
    cls: rec.cls,
    relClsMemb: rec.relClsMemb,
    cmt: rec.cmt,
  };

  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateRelClsMember,
    componentProps: {
      data: data,
      mode: "upd",
      lg: lg,
    },
  }).onOk((r) => {
    for (let key in r) {
      if (Object.prototype.hasOwnProperty.call(r, key)) {
        rec[key] = r[key];
      }
    }
  });
};

const fetchData = (relclsVal) => {
  loading.value = true;

  api
    .post("", {
      method: "relcls/loadRelClsMember",
      params: [relclsVal],
    })
    .then((response) => {
      rows.value = response.data.result.records;

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
    style: "width: 25%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 35%",
  },
  {
    name: "memberType",
    label: proxy?.$t("memberType") || "memberType",
    field: "memberType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) =>
      FD_MemberType ? FD_MemberType.get(val) : null,
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
];

const infoSelected = (row) => {
  return " " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

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
  fetchData(props.relcls);
});

defineExpose({
  selected,
  fetchData,
});
</script>

<style scoped></style>
