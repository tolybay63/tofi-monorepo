import {Notify} from 'quasar';
import {ref} from 'vue';
import {useUserStore} from 'stores/user-store';
import {storeToRefs} from 'pinia';

// Константы для уведомлений
const NOTIFICATION_DEFAULTS = {
  POSITION: 'bottom-right',
  TIMEOUTS: {
    SUCCESS: 1000,
    ERROR: 5000,
    INFO: 5000
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

// Базовая функция для обхода одного узла
const traverseNode = (node, callback) => {
  if (!node) return;
  callback(node);
  node.children?.forEach(child => traverseNode(child, callback));
};

// Функции для работы с чекбоксами
const checkChilds = (node) => {
  if (!node) return;
  traverseNode(node, (n) => {
    n.checked = true;
  });
};

const uncheckChilds = (node) => {
  if (!node) return;
  traverseNode(node, (n) => {
    n.checked = false;
  });
};

const checkParents = (node) => {
  if (!node?.parent) return;

  const siblings = node.parent.children;
  node.parent.checked = siblings?.every(child => child.checked) ?? false;

  checkParents(node.parent);
};

const uncheckParents = (node) => {
  if (!node?.parent) return;
  node.parent.checked = false;
  uncheckParents(node.parent);
};

// Функции для работы с деревом
const findNodeById = (tree, id) => {
  if (!tree || !id) return null;
  if (tree.id === id) return tree;

  return tree.children?.reduce((found, child) =>
    found || findNodeById(child, id), null) ?? null;
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

const convertNodeIds = (data) => {
  if (!Array.isArray(data)) return;

  traverseTree(data, (node) => {
    if (node.id) {
      const arr = node.id.split("_");
      node.id = arr[arr.length - 1];
    }
  });
};

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

// Улучшенные функции поиска родительского узла
const findParentNodeRecursive = (node, targetParentId, parentNode) => {
  if (!node || !targetParentId) return;

  if (node.id === targetParentId) {
    parentNode.push(node);
    return;
  }

  node.children?.forEach(child =>
    findParentNodeRecursive(child, targetParentId, parentNode)
  );
};

const getParentNode = (data, targetParentId, parentNode = []) => {
  if (!Array.isArray(data) || !targetParentId) return;

  for (const node of data) {
    findParentNodeRecursive(node, targetParentId, parentNode);
    if (parentNode.length > 0) break;
  }
};

// Вспомогательные функции
const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj;
  try {
    return structuredClone(obj);
  } catch {
    const clone = Array.isArray(obj) ? [] : {};
    Object.entries(obj).forEach(([key, value]) => {
      clone[key] = deepClone(value);
    });
    return clone;
  }
};

const ucFirst = (str) => {
  if (!str?.length) return str;
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
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
  checkChilds,
  uncheckChilds,
  ucFirst,
  notifySuccess,
  notifyError,
  notifyInfo,
  getParentNode,
  pack,
  expandAll,
  collapsAll,
  convertNodeIds,
  hasTarget
};
