import {Notify} from "quasar";
import {ref} from "vue";
import {useUserStore} from "stores/user-store";
import {storeToRefs} from "pinia";

/**
 * @typedef {Object} NotifyOptions
 * @property {'positive' | 'negative' | 'warning' | 'info'} type
 * @property {string} position
 * @property {number} timeout
 * @property {string} message
 */

const store = useUserStore();
const { isSysAdmin, getTarget } = storeToRefs(store);

/**
 * Capitalizes the first letter of a string
 * @param {string} str - Input string
 * @returns {string} - String with first letter capitalized
 */
const ucFirst = (str) => {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
};

/**
 * Shows a success notification
 * @param {string} msg - Message to display
 * @param {number} [timeout=1000] - Notification timeout in milliseconds
 */
const notifySuccess = (msg, timeout = 1000) => {
  Notify.create({
    type: "positive",
    position: "bottom-right",
    timeout,
    message: msg,
  });
};

/**
 * Shows an error notification
 * @param {string} msg - Error message to display
 */
const notifyError = (msg) => {
  Notify.create({
    type: "negative",
    position: "bottom-right",
    timeout: 5000,
    message: msg,
  });
};

/**
 * Shows an info notification
 * @param {string} msg - Info message to display
 */
const notifyInfo = (msg) => {
  Notify.create({
    type: "info",
    position: "bottom-right",
    timeout: 5000,
    message: msg,
  });
};

/**
 * Creates a tree structure from flat array
 * @param {Array} arr - Input array
 * @param {string} ord - Order property
 * @returns {Array} - Tree structure
 */
const pack = (arr, ord) => {
  try {
    const map = arr.reduce((acc, v) => ({
      ...acc,
      [v.id]: { ...v, children: [] }
    }), {});

    const tree = Object.values(map).filter(
      (v) => !(v.parent && map[v.parent]?.children.push(v))
    );

    if (ord) {
      tree.forEach(node => {
        node.children.sort((a, b) => (a[ord] > b[ord] ? 1 : -1));
      });
    }

    return tree;
  } catch (error) {
    console.error('Error in pack:', error);
    return [];
  }
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

  const { children = [] } = node;
  children.forEach(child => traverseTree(child, curNodeParent, parentNode));
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
 * Expands all nodes in tree
 * @param {Array} data - Tree data
 */
const expandAll = (data) => {
  data.forEach(node => {
    node.expend = ref(true);
    const { children = [] } = node;

    if (children.length > 0) {
      node.leaf = ref(false);
      expandAll(children);
    } else {
      node.leaf = ref(true);
      node.expend = undefined;
    }
  });
};

/**
 * Converts node IDs in tree
 * @param {Array} data - Tree data
 */
const cnvData = (data) => {
  data.forEach(node => {
    node.id = node.id.split("_").pop();
    const { children = [] } = node;
    if (children.length > 0) {
      cnvData(children);
    }
  });
};

/**
 * Collapses all nodes in tree
 * @param {Array} data - Tree data
 */
const collapsAll = (data) => {
  data.forEach(node => {
    node.expend = ref(false);
    const { children = [] } = node;

    if (children.length > 0) {
      node.leaf = ref(false);
      collapsAll(children);
    } else {
      node.leaf = ref(true);
      node.expend = undefined;
    }
  });
};

/**
 * Checks if user has target permission
 * @param {string} tg - Target to check
 * @returns {boolean} - Whether user has target permission
 */
const hasTarget = (tg) => {
  if (isSysAdmin.value) return true;
  if (!getTarget.value?.length) return false;
  return getTarget.value.includes(tg);
};

export {
  ucFirst,
  notifySuccess,
  notifyError,
  notifyInfo,
  getParentNode,
  pack,
  expandAll,
  collapsAll,
  cnvData,
  hasTarget
};
