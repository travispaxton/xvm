/**
 * The Config framework module.
 */
module config.xtclang.org {

    /**
     * A provider of a configuration.
     */
    interface ConfigSource {
        /**
         * Return the `MapNode` representing the configuration from this source.
         *
         * @return `True` iff this source can produces a config
         * @return the `MapNode` representing the configuration
         */
        conditional MapNode createConfig();
    }

    /**
     * A `ConfigItem` represents the types of items present in a `Config`.
     *
     * In a `Config` all underlying values are ultimately `String` values,
     * but a `Config` itself may be a single value, or a list of configs or
     * a hierarchical map of configs.
     */
    typedef Map<String, ConfigItem> | List<ConfigItem> | ConfigItem[] | String as ConfigItem;

    /**
     * A `ConfigConverter` can convert a `Config` into a different type.
     *
     * @param ConvertTo the type this converter converts a config to
     */
    interface ConfigConverter<ToType> {
        /**
         * Convert a `Config` to an instance of `ToType`.
         *
         * @param config  the `Config` to use to create an instance of `ToType`
         *
         * @returns `True` iff the `Config` can be converted to the required type
         * @returns an instance of `ConvertTo` created from the configuration items
         *          in the specified `Config`
         */

        conditional ToType convert(Config config);
    }

    /**
     * An enum representing the different types of `Config`.
     */
    enum ConfigType(Boolean exists, Boolean leaf) {
        /**
         * A `Map` type is a hierarchical map of configuration items,
         * where each item has a unique key.
         */
        Map(True, False),
        /**
         * A `List` type is an indexed list of configuration items.
         */
        List(True, False),
        /**
         * A `Value` type is a `Config` with a single value.
         */
        Value(True, True),
        /**
         * A `Missing` type is a `Config` representing a `ConfigKey` that is
         * not present in the parent `Config`, also an empty `Config`.
         */
        Missing(False, False)
    }

    /**
     * An empty `Config`.
     */
    const EmptyConfig(ConfigKey key)
            implements Config {

        @Override
        public/private ConfigType type = Missing;

        @Override
        Config! get(ConfigKey subKey) {
            return new EmptyConfig(key.child(subKey));
        }

        @Override
        <Value> ConfigValue<Value> value(Type<Value> type, ConfigConverter<Value>? converter = Null) {
            return new EmptyConfigValue(key);
        }
    }

    /**
     * A `ConfigValue` that returns a `Config` as its value.
     */
    const ConfigConfigValue(ConfigKey key, Config value)
            implements ConfigValue<Config> {

        @Override
        conditional Config get() {
            return True, value;
        }
    }

    /**
     * A `ConfigValue` that returns a `Config` array as its value.
     */
    const ConfigArrayConfigValue(ConfigKey key, GetConfigList getConfigList)
            implements ConfigValue<Config[]> {

        /**
         * A function that returns a `Config` array for a given `ConfigKey`.
         */
        typedef function Config[] (ConfigKey) as GetConfigList;

        @Override
        conditional Config[] get() {
            return True, getConfigList(key);
        }
    }
}