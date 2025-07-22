import { Notify } from "quasar";
import { ref } from "vue";
import {useUserStore} from 'stores/user-store';
import {storeToRefs} from 'pinia';

const store = useUserStore();
const { isSysAdmin, getTarget } = storeToRefs(store);
/**
 * Capitalizes the first letter of a string
 * @param {string} str - The input string
 * @returns {string} The string with first letter capitalized
 */
const ucFirst = (str) => {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
};

/**
 * Configuration object for notifications
 */
const NOTIFY_CONFIG = {
  position: "bottom-right",
  SUCCESS_TIMEOUT: 1000,
  ERROR_TIMEOUT: 5000,
  INFO_TIMEOUT: 5000,
};

/**
 * Shows a success notification
 * @param {string} message - The message to display
 * @param {number} [timeout] - Custom timeout in milliseconds
 */
const notifySuccess = (message, timeout = NOTIFY_CONFIG.SUCCESS_TIMEOUT) => {
  Notify.create({
    type: "positive",
    position: NOTIFY_CONFIG.position,
    timeout,
    message,
  });
};

/**
 * Shows an error notification
 * @param {string} message - The error message to display
 */
const notifyError = (message) => {
  Notify.create({
    type: "negative",
    position: NOTIFY_CONFIG.position,
    timeout: NOTIFY_CONFIG.ERROR_TIMEOUT,
    message,
  });
};

/**
 * Shows an info notification
 * @param {string} message - The info message to display
 */
const notifyInfo = (message) => {
  Notify.create({
    type: "info",
    position: NOTIFY_CONFIG.position,
    timeout: NOTIFY_CONFIG.INFO_TIMEOUT,
    message,
  });
};

/**
 * Converts an array of nodes into a tree structure
 * @param {Array} arr - Array of nodes
 * @param {string|null} [ord] - Optional ordering field
 * @returns {Array} Tree structure
 */
const pack = (arr, ord = null) => {
  if (!Array.isArray(arr)) {
    throw new Error("Input must be an array");
  }

  const map = arr.reduce(
    (acc, v) => ({
      ...acc,
      [v.id]: { ...v, children: [] },
    }),
    {}
  );

  const tree = Object.values(map).filter(
    (v) => !(v.parent && map[v.parent]?.children.push(v))
  );

  if (ord) {
    tree.forEach((node) => {
      node.children.sort((a, b) => (a[ord] > b[ord] ? 1 : -1));
    });
  }

  return tree;
};

/**
 * Traverses tree to find parent node
 * @param {Object} node - Current node
 * @param {string|number} curNodeParent - Parent ID to find
 * @param {Array} parentNode - Array to store found parent
 */
const traverseTree = (node, curNodeParent, parentNode) => {
  if (node.id === curNodeParent) {
    parentNode.push(node);
    return;
  }

  node.children?.forEach((child) =>
    traverseTree(child, curNodeParent, parentNode)
  );
};

/**
 * Gets parent node from tree data
 * @param {Array} data - Tree data
 * @param {string|number} curNodeParent - Parent ID to find
 * @param {Array} parentNode - Array to store found parent
 */
const getParentNode = (data, curNodeParent, parentNode) => {
  for (const node of data) {
    traverseTree(node, curNodeParent, parentNode);
    if (parentNode.length > 0) break;
  }
};

/**
 * Expands all nodes in the tree
 * @param {Array} data - Tree data
 */
const expandAll = (data) => {
  data.forEach((node) => {
    node.expend = ref(true);

    if (node.children?.length) {
      node.leaf = ref(false);
      expandAll(node.children);
    } else {
      node.leaf = ref(true);
      node.expend = undefined;
    }
  });
};

/**
 * Converts node IDs in the tree
 * @param {Array} data - Tree data
 */
const cnvData = (data) => {
  data.forEach((node) => {
    const arr = node.id.split("_");
    node.id = arr[arr.length - 1];

    if (node.children?.length) {
      cnvData(node.children);
    }
  });
};

/**
 * Collapses all nodes in the tree
 * @param {Array} data - Tree data
 */
const collapsAll = (data) => {
  data.forEach((node) => {
    node.expend = ref(false);

    if (node.children?.length) {
      node.leaf = ref(false);
      collapsAll(node.children);
    } else {
      node.leaf = ref(true);
      node.expend = undefined;
    }
  });
};

const hasTarget = (tg) => {
  if (isSysAdmin.value) return true;
  return getTarget.value?.includes(tg) ?? false;
};

export {
  hasTarget,
  ucFirst,
  notifySuccess,
  notifyError,
  notifyInfo,
  getParentNode,
  pack,
  expandAll,
  collapsAll,
  cnvData,
};
