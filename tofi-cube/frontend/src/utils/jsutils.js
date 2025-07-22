import {Notify} from 'quasar';
import {ref} from 'vue';
import {useUserStore} from 'stores/user-store';
import {storeToRefs} from 'pinia';

// Константы для уведомлений
const NOTIFICATION_DEFAULTS = {
  POSITION: 'top-left',
  TIMEOUTS: {
    SUCCESS: 500,
    ERROR: 3000,
    INFO: 3000
  },
  TYPES: {
    SUCCESS: 'positive',
    ERROR: 'negative',
    INFO: 'info'
  }
};

const store = useUserStore();
const { isSysAdmin, getTarget } = storeToRefs(store);

// Базовая функция для обхода дерева
const traverseTree = (data, callback) => {
  if (!Array.isArray(data)) return;

  data.forEach(node => {
    if (!node) return;
    callback(node);
    if (node.children?.length > 0) {
      traverseTree(node.children, callback);
    }
  });
};

const processTreeState = (data, isExpanded = true) => {
  if (!Array.isArray(data)) return;

  traverseTree(data, (node) => {
    const hasChildren = node.children?.length > 0;
    if (hasChildren) {
      node.expend = ref(isExpanded);
      node.leaf = ref(false);
    } else {
      node.leaf = ref(true);
      node.expend = undefined;
    }
  });
};

const expandAll = (data) => processTreeState(data, true);
const collapsAll = (data) => processTreeState(data, false);

// Улучшенная функция pack
const pack = (arr, orderBy = null) => {
  if (!Array.isArray(arr)) return [];

  const map = arr.reduce((acc, node) => ({
    ...acc,
    [node.id]: { ...node, children: [] }
  }), {});

  const tree = Object.values(map).filter(node => {
    if (node.parent && map[node.parent]) {
      map[node.parent].children.push(node);
      return false;
    }
    return true;
  });

  if (orderBy) {
    const sortNodes = (nodes) => {
      nodes.sort((a, b) => a[orderBy] > b[orderBy] ? 1 : -1);
      nodes.forEach(node => {
        if (node.children?.length) {
          sortNodes(node.children);
        }
      });
    };
    sortNodes(tree);
  }

  return tree;
};

// Функции уведомлений
const notify = (message, type, timeout) => {
  if (!message) return;

  Notify.create({
    type,
    position: NOTIFICATION_DEFAULTS.POSITION,
    timeout,
    message,
  });
};

const notifySuccess = (msg, timeout = NOTIFICATION_DEFAULTS.TIMEOUTS.SUCCESS) =>
  notify(msg, NOTIFICATION_DEFAULTS.TYPES.SUCCESS, timeout);

const notifyError = (msg) =>
  notify(msg, NOTIFICATION_DEFAULTS.TYPES.ERROR, NOTIFICATION_DEFAULTS.TIMEOUTS.ERROR);

const notifyInfo = (msg) =>
  notify(msg, NOTIFICATION_DEFAULTS.TYPES.INFO, NOTIFICATION_DEFAULTS.TIMEOUTS.INFO);

const hasTarget = (tg) => {
  if (isSysAdmin.value) return true;
  return getTarget.value?.includes(tg) ?? false;
};

export {
  notifySuccess,
  notifyError,
  notifyInfo,
  pack,
  expandAll,
  collapsAll,
  hasTarget
};
