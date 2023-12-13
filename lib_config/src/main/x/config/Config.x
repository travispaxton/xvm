/**
 * A `Config` is a keyed, hierarchical set of configuration items.
 */
interface Config {

    import converters.ConverterFunction;

    /**
     * The root level key for this `Config`.
     */
    @RO ConfigKey key;

    /**
     * The type of this `Config`.
     */
    @RO ConfigType type;

    /**
     * `True` if this `Config` is empty, or `False` if it contains
     * a value or contains other configuration items.
     */
    @RO Boolean empty.get() {
        return type.exists;
    }

    /**
     * Obtain the sub-`Config` this config contains using the specified key.
     * If this `Config` does not contain a configuration item for the key,
     * an empty `Config` will be returned.
     *
     * @param key  the key to use to find the sub-config
     *
     * @return the sub-`Config` this config contains using the specified key
     */
    @Op("[]") Config! get(String key) {
        return get(ConfigKey.create(key));
    }

    /**
     * Obtain the sub-`Config` this config contains using the specified key.
     * If this `Config` does not contain a configuration item for the key,
     * an empty `Config` will be returned.
     *
     * @param key  the key to use to find the sub-config
     *
     * @return the sub-`Config` this config contains using the specified key
     */
    @Op("[]") Config! get(ConfigKey key);

    /**
     * Return a `ConfigValue` representing the `String` value of this configuration item.
     */
    ConfigValue<String> value() {
        return value(String);
    }

    /**
     * Return a `ConfigValue` representing this configuration item converted to the specified type.
     *
     * @param type       the type to convert this `Config` to
     * @param converter  an optional function to convert this `Config` to the specified type
     *
     * @throws IllegalArgument if the `converter` parameter is `Null` and it is not possible to convert
     *                         the `Config` to the requested type
     */
    <Value> ConfigValue<Value> value(Type<Value> type, function conditional Value (Config) converter) {
        return value(type, new ConverterFunction(converter));
    }

    /**
     * Return a `ConfigValue` representing this configuration item converted to the specified type.
     *
     * @param type       the type to convert this `Config` to
     * @param converter  a `Converter` to convert this `Config` to the specified type
     *
     * @throws IllegalArgument if the `converter` parameter is `Null` and it is not possible to convert
     *                         the `Config` to the requested type
     */
    <Value> ConfigValue<Value> value(Type<Value> type, ConfigConverter<Value>? converter = Null);
}
