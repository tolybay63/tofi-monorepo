<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="height: 600px; width: 1200px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card>
        <q-tabs v-model="tab" class="text-teal" no-caps>
          <q-tab name="tabCls" :label="$t('clsProp')" />
          <q-tab name="tabClsFV" :label="$t('cfvProp')" />
        </q-tabs>

        <q-separator />

        <q-tab-panels
          v-model="tab"
          animated
          swipeable
          vertical
          transition-prev="jump-up"
          transition-next="jump-up"
        >
          <q-tab-panel name="tabCls">
            <!-- Cod -->
            <q-input
              style="margin-bottom: 10px"
              :dense="dense"
              v-model="form.cod"
              :label="$t('code')"
              :placeholder="$t('msgCodeGen')"
            />

            <!-- name -->
            <q-input
              autofocus
              :dense="dense"
              v-model="form.name"
              @blur="onBlurName"
              :label="$t('fldName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />

            <!-- fullName-->
            <q-input
              :dense="dense"
              v-model="form.fullName"
              :label="$t('fldFullName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />

            <!-- isOpenness -->
            <q-toggle
              style="margin-left: 5px; margin-top: 5px; margin-bottom: 10px"
              :dense="dense"
              v-model="form.isOpenness"
              :label="$t('isOpenness')"
            />

            <!-- accessLevel -->
            <q-select
              :dense="dense"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectAL"
            />

            <!-- database -->
            <q-select
              :dense="dense"
              v-model="db"
              :options="optionsDB"
              :label="$t('dbNameLabel')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelectDB"
            />

            <!-- cmt -->
            <q-input
              :dense="dense"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
            />
          </q-tab-panel>

          <q-tab-panel name="tabClsFV">
            <q-banner dense inline-actions class="bg-orange-1 no-scroll">
              <template v-slot:action>
                <q-btn
                  dense
                  icon="expand_more"
                  color="secondary"
                  @click="fnExpand"
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
                  @click="fnCollapse"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("collapseAll") }}
                  </q-tooltip>
                </q-btn>
              </template>
            </q-banner>

            <div style="height: 395px; width: 100%">
              <div
                class="q-table-container q-table--dense wrap bg-orange-1"
                style="height: 100%"
              >
                <div class="q-table-middle scroll">
                  <table
                    class="q-table q-table--cell-separator q-table--bordered wrap"
                  >
                    <thead class="text-bold text-white bg-blue-grey-13">
                    <tr style="text-align: left">
                      <th style="font-size: 1.2em; width: 80%">
                        {{ cols[0]?.label }}
                      </th>
                      <th style="font-size: 1.2em; width: 10%">
                        {{ cols[1]?.label }}
                      </th>
                      <th style="font-size: 1.2em; width: 10%">
                        {{ cols[2]?.label }}
                      </th>
                    </tr>
                    </thead>

                    <tbody style="background: aliceblue">
                    <tr v-for="(item, index) in arrayTreeObj" :key="index">
                      <td
                        :data-th="cols[0]?.name"
                        @click="toggle(item)"
                      >
                          <span
                            class="q-tree-link q-tree-label"
                            :style="setPadding(item)"
                          >
                            <q-icon
                              style="cursor: pointer"
                              :name="iconName(item)"
                              color="secondary"
                            />
                            <q-btn
                              dense
                              flat
                              :color="
                                item.parent === undefined ? 'gray' : 'blue'
                              "
                              :icon="
                                item.checked
                                  ? 'check_box'
                                  : 'check_box_outline_blank'
                              "
                              @click.stop="selectedRow(item)"
                            />

                            <q-icon
                              :name="getIcon(item)"
                              :color="getIconColor(item)"
                            />
                            {{ item.name }}
                          </span>
                      </td>
                      <!--isReq-->
                      <td :data-th="cols[1]?.name">{{ fnIsReq(item) }}</td>
                      <!--isUniq-->
                      <td :data-th="cols[2]?.name">{{ fnIsUniq(item) }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </q-tab-panel>
        </q-tab-panels>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validFV()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>

        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import {
  collapsAll,
  expandAll,
  notifyError,
  notifySuccess,
  pack,
} from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  typ: [Number, String],
  lg: [String, Object],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const form = reactive({ ...props.data });
const loading = ref(false);
const optionsLevel = ref([]);
const al = ref(props.data.accessLevel);

const optionsDB = ref([]);
const db = ref(props.data.dataBase);

const isExpanded = ref(true);
const itemId = ref(null);
const tab = ref("tabCls");

const getIconColor = (row) => {
  if (row.isOwn === 1) return "green";
  else return "gray";
};

const getIcon = (row) => {
  if (row.parent) return "brightness_1";
  else return "square";
};

const selectedRow = (item) => {
  if (!item.parent) return;
  if (item.checked === 1) {
    if (item.isReq) {
      let parent = rows.value.filter((row) => {
        return row.id === item.parent;
      });
      let { children } = parent[0];
      let flt = children.filter((row) => {
        return row.checked === 1;
      });
      if (flt.length === 1) {
        notifyError(proxy?.$t("isReq"));
      } else {
        item.checked = 0;
      }
    } else {
      item.checked = 0;
    }
  } else {
    if (item.isUniq) {
      let parent = rows.value.filter((row) => {
        return row.id === item.parent;
      });
      let { children } = parent[0];
      children.map((row) => {
        row.checked = 0;
      });
      item.checked = 0;
    } else {
      item.checked = 1;
    }
    item.checked = 1;
  }
};

const fnIsReq = (item) => {
  if (!item.parent) return item.isReq ? proxy?.$t("yes") : proxy?.$t("no");
  else return null;
};

const fnIsUniq = (item) => {
  if (!item.parent) return item.isUniq ? proxy?.$t("yes") : proxy?.$t("no");
  else return null;
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    style: "font-size: 1.2em; width: 80%",
  },
  {
    name: "isReq",
    label:
      (proxy?.$t("isReq") || "isReq").substring(0, 5) + "-" + (proxy?.$t("isReq") || "isReq").substring(5),
    field: "isReq",
    align: "left",
    style: "font-size: 1.2em; width: 10%",
  },
  {
    name: "isUniq",
    label:
      (proxy?.$t("isUniq") || "isUniq").substring(0, 4) + "-" + (proxy?.$t("isUniq") || "isUniq").substring(4),
    field: "isUniq",
    align: "left",
    style: "font-size: 1.2em; width: 10%",
  },
];

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectAL = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const fnSelectDB = () => {
  form.dataBase = typeof db.value === "object" && db.value !== null ? db.value.id : db.value;
};

const validFV = () => {
  let b = true;
  let i = 0;
  let flt = [];
  while (i < rows.value.length) {
    let row = rows.value[i];
    let { children } = rows.value[i];
    if (row.isReq && row.isUniq) {
      flt = children.filter((ch) => ch.checked === 1);
      b = b && flt.length === 1;
    } else if (row.isReq && !row.isUniq) {
      flt = children.filter((ch) => ch.checked === 1);
      b = b && flt.length >= 1;
    } else if (!row.isReq && row.isUniq) {
      flt = children.filter((ch) => ch.checked === 1);
      b = b && flt.length <= 1;
    } else if (!row.isReq && !row.isUniq) {
      flt = children.filter((ch) => ch.checked === 1);
      b = b && flt.length >= 0;
    }
    i = i + 1;
  }

  return !(
    form.name !== null &&
    form.name !== undefined &&
    form.name.trim().length !== 0 &&
    form.dataBase !== null &&
    b
  );
};

const recursive = (obj, newObj, level, currentItemId, isExpend) => {
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === currentItemId) {
        o.expend = isExpend;
      }
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, currentItemId, isExpend);
      }
    } else {
      o.level = level;
      o.leaf = true;
      newObj.push(o);
    }
  });
};

const iconName = (item) => {
  if (item.expend) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
  return "";
};

const toggle = (item) => {
  itemId.value = item.id;
  item.leaf = false;
  if (!item.leaf && item.expend === undefined && item.children !== undefined) {
    if (item.children.length !== 0) {
      recursive(item.children, [], item.level + 1, item.id, true);
    }
  }
  if (item.expend && item.children !== undefined) {
    item.children.forEach((o) => {
      o.expend = undefined;
    });
    item.expend = undefined;
    item.leaf = false;
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${item.level * 30}px;`;
};

const fetchData = (typVal, clsVal) => {
  api
    .post("", {
      method: "cls/loadClsFVforUpd",
      params: [typVal, clsVal],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records);
      fnExpand();
    });
};

const onOKClick = () => {
  let ids = [];
  rows.value.forEach((row) => {
    const { children } = row;
    children.forEach((it) => {
      if (it.checked === 1) ids.push(it.id.toString());
    });
  });
  const method = props.mode === "ins" ? "insertCls" : "updateCls";
  let err = false;
  api
    .post("", {
      method: "cls/" + method,
      params: [{ rec: form, ids: ids }],
    })
    .then(
      () => {
        err = false;
        onDialogOK({ res: true });
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.message;
        if (error.response?.data?.error?.message) {
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        }
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      optionsLevel.value = response.data.result.records;
    });

  api
    .post("", {
      method: "database/loadDbForSelect",
      params: [],
    })
    .then((response) => {
      optionsDB.value = response.data.result.records;
    });

  let cls = props.data.id === null ? 0 : props.data.id;
  fetchData(props.typ, cls);
});
</script>

<style scoped></style>
