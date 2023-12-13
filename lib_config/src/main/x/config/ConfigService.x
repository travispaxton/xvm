/**
 * A `Service` that is an implementation of a `Config`.
 *
 * This service represents the root of a `Config` hierarchy made
 * up of `ConfigNode`s.
 */
service ConfigService
        implements Config {

    import converters.ConverterFunction;
    import converters.ConverterRegistry;
    import converters.ArrayConverter;
    import converters.StringConverter;
    import nodes.ConfigNodeConfig;

    /**
     * Create a `ConfigService`.
     *
     * @param rootNode  the root `ConfigNode` in the config hierarchy
     */
    construct (MapNode rootNode) {
        this.rootNode = rootNode;
        this.nodeMap  = nodes.createNodeMap(key, rootNode);
    }

    @Override
    ConfigKey key = ConfigKey.Root;

    @Override
    ConfigType type = Map;

    /**
     * The root node of the configuration hierarchy.
     */
    ConfigNode rootNode;

    /**
     * The map of `ConfigNode`s that make up this configuration.
     */
    Map<ConfigKey, ConfigNode> nodeMap;

    /**
     * A registry of converters that can convert configuration values into other types.
     */
    private ConverterRegistry registry = new ConverterRegistry();

    @Override
    Config! get(ConfigKey subKey) {
        return lookupConfig(key.child(subKey));
    }

    @Override
    <Value> ConfigValue<Value> value(Type<Value> type, ConfigConverter<Value>? converter = Null) {
        if (converter.is(ConfigConverter)) {
            return new DelegatingConfigValue<Value>(key, k -> this.get(k), converter);
        }
        if (nodes.isTypeA(type, Config)) {
            return new ConfigConfigValue(key, this).as(ConfigValue<Value>);
        }
        return lookupvalue(key, type).as(ConfigValue<Value>);
    }

    <Value> ConfigValue lookupvalue(ConfigKey key, Type<Value> type) {
        return switch (type.is(_)) {
            case Type<Config[]>: new ConfigArrayConfigValue(key, asConfigList);
            case Type<String>:   new StringConfigValue(key, getValue);
            case Type<Array>:    asArray(key, type);
            default:             lookupValueWithConverter(key, type);
        };
    }

    <Value> ConfigValue lookupValueWithConverter(ConfigKey key, Type<Value> type) {
        if (ConfigConverter<Value> converter := registry.findConfigConverter(type)) {
            return new DelegatingConfigValue<Value>(key, k -> this.get(k), converter);
        }
        return new EmptyConfigValue<Value>(key);
    }

    <Value> ConfigValue<Value> asArray(ConfigKey key, Type<Value> type) {
        assert Type[] innerTypes := type.parameterized();
        Type inner = innerTypes[0];
        if (ConfigConverter<inner.DataType> innerConverter := registry.findConfigConverter(inner)) {
            StringConverter stringConverter = new StringConverter();
            ArrayConverter<inner.DataType> arrayConverter = new ArrayConverter<inner.DataType>(stringConverter);
            return new DelegatingConfigValue<Value>(key, lookupConfig, arrayConverter.as(ConfigConverter<Value>));
        }
        throw new IllegalArgument($"No converter registered to convert a Config to a {type}");
    }

    /**
     * Return the `Config` for the specified key.
     *
     * @param key  the `ConfigKey` for the `Config` to find
     *
     * @return the `Config` for the specified key.
     */
    Config! lookupConfig(ConfigKey key) {
        if (key == this.key) {
            return this;
        }
        if (ConfigNode node := findNode(key)) {
            return createConfig(key, node);
        }
        return new EmptyConfig(key);
    }

    /**
     * Create a `Config` for a `ConfigNode`.
     *
     * @param key   the `ConfigKey` for the `Config` to create
     * @param node  the `ConfigNode` to create the `Config` for
     *
     * @return a `Config` for a `ConfigNode`
     */
    protected Config! createConfig(ConfigKey key, ConfigNode node) {
        return switch (node.is(_)) {
            case MapNode: new ConfigNodeConfig(key, Map, this);
            case ListNode: new ConfigNodeConfig(key, List, this);
            case ValueNode: new ConfigNodeConfig(key, Value, this);
            default: assert;
        };
    }

    /**
     * Return the direct child `Config` items for a given `ConfigKey`
     * as a `Config` array.
     *
     * @param key  the `ConfigKey` to return the children of
     *
     * @return the direct child `Config` items for the `ConfigKey`
               as a `Config` array
     */
    Config[] asConfigList(ConfigKey key) {
        if (ConfigNode node := findNode(key)) {
            Array<Config> configs = new Array();
            if (node.is(ListNode)) {
                for (Int i : 0 ..< node.size) {
                    Config config = createConfig(key.child(i.toString()), node[i]);
                    configs.add(config);
                }
            }
            return configs.freeze(True);
        }
        return [];
    }

    /**
     * Return the configured `String` value for a given `ConfigKey`.
     *
     * @param key  the `ConfigKey` to return the `String` value of
     *
     * @return `True` iff a `ConfigNode` exists for the key and that node has a `String` value,
     *         otherwise `False` if the node does not exists or has no value
     * @return the `String` value from the `ConfigNode` iff it exists and has a `String` value
     */
    conditional String getValue(ConfigKey key) {
        if (ConfigNode node := findNode(key)) {
            return node.getValue();
        }
        return False;
    }

    /**
     * Find a `ConfigNode` for a given `ConfigKey`.
     *
     * @param key  the key of the node to find
     *
     * @return `True` if a node exists with the specified key
     * @return the `ConfigNode` for the key
     */
    conditional ConfigNode findNode(ConfigKey key) {
        if (ConfigNode node := nodeMap.get(key)) {
            return True, node;
        }
        return False;
    }
}