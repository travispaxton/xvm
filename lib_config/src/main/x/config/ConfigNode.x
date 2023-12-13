/**
 * A node in a configuration hierarchy.
 */
interface ConfigNode
    extends Freezable {

    /**
     * The type of this node.
     */
    @RO ConfigType type;

    /**
     * Obtain the `String` value from the node.
     *
     * @return True iff this node has a value.
     * @return the value from the node.
     */
    conditional String getValue();
}
