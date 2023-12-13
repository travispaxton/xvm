/**
 * A `ConfigValue` that delegates to a supplier function to obtain the actual
 * configuration value and then delegates to a converter to convert the actual
 * value to an instance of a `Value`.
 */
const DelegatingConfigValue<Value>(ConfigKey key, GetConfig getConfig, ConfigConverter<Value> converter)
        implements ConfigValue<Value> {

    /**
     * A function that can return a `Config` for a given `ConfigKey`.
     */
    typedef function Config (ConfigKey) as GetConfig;

    @Override
    conditional Value get() {
        Config config = getConfig(key);
        return converter.convert(config);
    }
}
