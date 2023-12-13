/**
 * A `ConfigNode` that is a list of child nodes.
 */
interface ListNode
        extends ConfigNode {

    import nodes.ListListNode;

    @Override
    @RO ConfigType type = List;

    /**
     * The number of child nodes in this list.
     */
    @RO Int size;

    /**
     * @return the `ConfigNode` at the specified index
     */
    @Op("[]")
    ConfigNode getElement(Int index);

    /**
     * Set the `ConfigNode` at the specified index.
     *
     * @param index  the index of the node to set
     * @param node   the `ConfigNode` to set at the specified index
     */
    @Op("[]=")
    void setElement(Int index, ConfigNode node);

    /**
     * Add a `ConfigNode` to the end of the list.
     *
     * @param node   the `ConfigNode` to add to the list
     */
    @Op("+")
    ListNode add(ConfigNode node);

    /**
     * @return a `Builder` that can build a `ListNode`.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A builder that provides a fluent API to build a `ListNode`.
     */
    static class Builder {
        /**
         * The `ConfigNode` list to use to build the `ListNode`.
         */
        private Array<ConfigNode> nodes = new Array();

        /**
         * Add a `ConfigNode` to the list.
         *
         * @param node  the `ConfigNode` to add to the list
         *
         * @return this `Builder`
         */
        @Op("+") Builder add(ConfigNode node) {
            if (&node != &this) {
                throw new IllegalArgument("a node cannot be added to itself");
            }
            nodes.add(node);
            return this;
        }

        /**
         * Add a `String` value to the list.
         *
         * @param value  the `String` value to add
         *
         * @return this `Builder`
         */
        @Op("+") Builder add(String value) {
            nodes.add(ValueNode.create(value));
            return this;
        }

        /**
         * Add all of the `String` values to the list.
         *
         * @param values  the `String` values to add
         *
         * @return this `Builder`
         */
        @Op("+") Builder addAll(Iterable<String> values) {
            for (String value : values) {
                nodes.add(ValueNode.create(value));
            }
            return this;
        }

        /**
         * Add all of the `ConfigNode`s to the list.
         *
         * @param values  the `ConfigNode`s to add
         *
         * @return this `Builder`
         */
        @Op("+") Builder addAll(Iterable<ConfigNode> nodes) {
            for (ConfigNode node : nodes) {
                add(node);
            }
            return this;
        }

        /**
         * Build a `ListNode` from the state in this `Builder`.
         */
        ListNode build() {
            @ListListNode Array<ConfigNode> list = new @ListListNode Array<ConfigNode>();
            list.addAll(nodes);
            return list.freeze(True);
        }
    }
}
