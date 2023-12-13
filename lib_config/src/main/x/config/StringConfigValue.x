/**
 * A `ConfigValue` that contains a fixed `String` as its value.
 *
 * @param key    the key for this value in the underlying `Config`
 * @param value  the `GetValue` function to look up the 1String` value for the key
 */
const StringConfigValue(ConfigKey key, GetValue value)
        implements ConfigValue<String> {

    /**
     * A function that can return a `String` value for a given `ConfigKey`.
     */
    typedef function conditional String (ConfigKey) as GetValue;

    @Override
    conditional String get() {
        return value(key);
    }
}
