/**
 * A `ConfigNode` that is a map of child nodes.
 */
interface MapNode
        extends Map<String, ConfigNode>
        extends ConfigNode {

    import nodes.MapMapNode;

    @Override
    @RO ConfigType type = Map;

    /**
     * Obtain a `Builder` that can build a `MapNode`.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A builder that provides a fluent API to build a `MapNode`.
     */
    static class Builder {
        /**
         * The `ConfigNode` map to use to build the `MapNode`.
         */
        private Map<String, ConfigNode> nodes = new HashMap();

        /**
         * An optional value for the node.
         */
        private String? value = Null;

        /**
         * Add a `ConfigNode` to the map.
         *
         * @param node  the `ConfigNode` to add to the map
         *
         * @return this `Builder`
         */
        @Op("[]=") Builder add(String key, ConfigNode node) {
            String keyTrimmed = key.trim();
            if (keyTrimmed == "") {
                throw new IllegalArgument("key cannot be an empty or blank string");
            }
            if (&node == &this) {
                throw new IllegalArgument("a node cannot be added to itself");
            }
            nodes.put(key, node);
            return this;
        }

        /**
         * Add a `String` value to the map.
         *
         * @param value  the `String` value to add
         *
         * @return this `Builder`
         */
        @Op("[]=") Builder add(String key, String value) {
            return add(key, ValueNode.create(value));
        }

        Builder withValue(String value) {
            this.value = value;
            return this;
        }

        /**
         * Build a `MapNode` from the state in this `Builder`.
         */
        MapNode build() {
            MapNode map = new @MapMapNode(this.value) HashMap<String, ConfigNode>();
            map.putAll(nodes);
            return map.freeze(True);
        }
    }

}