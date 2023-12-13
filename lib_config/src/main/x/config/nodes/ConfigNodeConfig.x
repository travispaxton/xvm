/**
 * An implementation of a `Config` that refers to a specific node in
 * a parent configuration hierarchy.
 *
 * @param key     the key to the node this config refers to
 * @param type    the type of this config
 * @param parent  the parent `ConfigService`
 */
const ConfigNodeConfig(ConfigKey key, ConfigType type, ConfigService parent)
        implements Config {

    @Override
    Config! get(ConfigKey subKey) {
        if (type == Value) {
            return new EmptyConfig(key);
        }
        return parent.lookupConfig(key.child(subKey));
    }

    @Override
    <Value> ConfigValue<Value> value(Type<Value> type, ConfigConverter<Value>? converter = Null) {
        if (converter.is(ConfigConverter)) {
            return new DelegatingConfigValue<Value>(key, k -> this, converter);
        }
        if (nodes.isTypeA(type, Config)) {
            return new ConfigConfigValue(key, this).as(ConfigValue<Value>);
        }
        return parent.lookupvalue(key, type).as(ConfigValue<Value>);
    }
}