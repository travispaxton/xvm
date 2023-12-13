/**
 * The `nodes` package contains `ConfigNode` implementations.
 */
package nodes {

    /**
     * A `ConfigNode` that can be combined with another `ConfigNode`
     */
    interface CombinableNode
            extends ConfigNode {
        /**
         * Combine this node with the specified node.
         *
         * @param node  the `CombinableNode` to combine with this node
         *
         * @return a new `CombinableNode` that combines the specified noe with this node
         */
        CombinableNode combine(CombinableNode node);
    }

    static Map<ConfigKey, ConfigNode> createNodeMap(ConfigKey key, MapNode node) {
        HashMap<ConfigKey, ConfigNode> result = new HashMap();
        flattenMapNode(key, node).forEach(t -> result.put(t[0], t[1]));
        return result.freeze(True);
    }

    static Map<ConfigKey, ConfigNode> createNodeMap(ConfigKey key, ListNode node) {
        HashMap<ConfigKey, ConfigNode> result = new HashMap();
        flattenListNode(key, node).forEach(t -> result.put(t[0], t[1]));
        return result.freeze(True);
    }

    static Map<ConfigKey, ConfigNode> createNodeMap(ConfigKey key, ValueNode node) {
        HashMap<ConfigKey, ConfigNode> result = new HashMap();
        flattenValueNode(key, node).forEach(t -> result.put(t[0], t[1]));
        return result.freeze(True);
    }

    static Tuple<ConfigKey, ConfigNode>[] flattenNode(ConfigKey key, ConfigNode node) {
        return switch (node.is(_)) {
        case MapNode: flattenMapNode(key, node);
        case ListNode: flattenListNode(key, node);
        case ValueNode: flattenValueNode(key, node);
        default: assert;
        };
    }

    static Array<Tuple<ConfigKey, ConfigNode>> flattenMapNode(ConfigKey key, MapNode node) {
        Array<Tuple<ConfigKey, ConfigNode>> items = new Array();
        items.add(Tuple:(key, node));
        for (Map<String, ConfigNode>.Entry entry : node.as(Map<String, ConfigNode>).entries) {
            items.addAll(flattenNode(key.child(entry.key), entry.value));
        }
        return items;
    }

    static Array<Tuple<ConfigKey, ConfigNode>> flattenListNode(ConfigKey key, ListNode node) {
        Array<Tuple<ConfigKey, ConfigNode>> items = new Array();
        items.add(Tuple:(key, node));
        for (Int i : 0 ..< node.size) {
            ConfigNode child = node[i];
            items.addAll(flattenNode(key.child(i.toString()), child));
        }
        return items;
    }

    static Array<Tuple<ConfigKey, ConfigNode>> flattenValueNode(ConfigKey key, ValueNode node) {
        Array<Tuple<ConfigKey, ConfigNode>> items = new Array();
        items.add(Tuple:(key, node));
        return items;
    }

    /**
     * @return True iff the `type` is a `other`
     */
    static Boolean isTypeA(Type type, Type other) {
       return type.isA(other);
    }

}