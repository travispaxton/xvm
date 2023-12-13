/**
 * The key of an entry in a `Config`.
 *
 * A `Config` is a hierarchical map of configuration keys and values.
 * This key represents a specific entry in that hierarchy.
 */
const ConfigKey {

    private construct (String name, ConfigKey? parent = Null) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * The name of the config item represented by this key.
     */
    String name;

    /**
     * An optional parent key if this key is a child in a hierarchy.
     */
    ConfigKey? parent;

    /**
     * The full path of this key in the config hierarchy.
     */
    @Lazy
    String[] path.calc() {
        Array<String> path = new Array();
        ConfigKey? p = parent;
        if (p.is(ConfigKey)) {
            path.addAll(p.path);
        }
        if (name != "") {
            path.add(name);
        }
        return path.freeze(True);
    }

    /**
     * @returns `True` if this key is the root in a config hierarchy.
     */
    @Lazy
    Boolean isRoot.calc() {
        return parent == Null;
    }

    /**
     * The default root `ConfigKey`.
     */
    static ConfigKey Root = new ConfigKey("");

    /**
     * The default Ecstasy `ConfigKey`.
     */
    static ConfigKey Ecstasy = new ConfigKey("xtclang");

    /**
     * Create a `ConfigKey`.
     *
     * @param name    the name of the configuration item
     * @param parent  the parent key
     */
    static ConfigKey create(String name, ConfigKey parent = Root) {
        return parent.child(name);
    }

    /**
     * Create a `ConfigKey` that is a child of this key.
     */
    ConfigKey! child(String name) {
        return child(name.split('.'));
    }

    ConfigKey! child(ConfigKey key) {
        return child(key.path);
    }

    protected ConfigKey! child(String[] names) {
        ConfigKey result = this;
        for (String name : names) {
            if (name == "") {
                continue;
            }
        result = new ConfigKey(name, result);
        }
        return result;
    }
}