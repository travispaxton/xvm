/**
 * A value for a given key in a `Config`.
 *
 * @param Value  the `Type` of the value represented.
 */
interface ConfigValue<Value>
        extends Const {
    /**
     * The key in the `Config` that this `ConfigValue` represents
     */
    @RO ConfigKey key;

    /**
     * Returns the actual value from the `Config` that this `ConfigValue`
     * represents.
     *
     * @return `True` iff the underlying `Config` contains a value for the `key`.
     * @return the value from the `Config` converted to the `Value` type.
     */
    conditional Value get();

    /**
     * Returns the actual value from the `Config` that this `ConfigValue`
     * represents, or if the `Config` has no value returns the specified
     * default value.
     *
     * @param dflt  the default value to return if this value is empty
     */
    Value orDefault(Value dflt) {
        if (Value value := get()) {
            return value;
        }
        return dflt;
    }

    /**
     * If this `ConfigValue` has a value, pass that value to the specified consumer.
     * If this `ConfigValue` has no value and a default value has been provided, then
     * the default value will be passed to the consumer.
     *
     * @param consumer  a function that consumes the value
     * @param dflt      an optional default value to pass to the consumer
     */
    void apply(function void (Value) consumer, Value? dflt = Null) {
        if (Value value := get()) {
            consumer(value);
        } else if (dflt.is(Value)) {
            consumer(dflt);
        }
    }
}
