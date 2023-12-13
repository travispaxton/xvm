/**
 * A `ConfigNode` that represents a single value leaf node
 * in a config hierarchy.
 */
interface ValueNode
        extends ConfigNode {

    import nodes.StringValueNode;

    @Override
    @RO ConfigType type = Value;

    @RO String value;

    @Override
    conditional String getValue() {
        return True, value;
    }

    /**
     * Create a `ValueNode`.
     *
     * @param value  the `String` value for the node
     */
    static ValueNode create(String value) {
        return new StringValueNode(value).freeze(True);
    }
}
