/**
 * The `converters` package contains types that can convert a
 * `Config` to a specific type.
 */
package converters {

    /**
     * A `ConfigConverter` that wraps a function that will
     * perform the conversion.
     */
    const ConverterFunction<Value>(function conditional Value (Config) fn)
            implements ConfigConverter<Value>{

        @Override
        conditional Value convert(Config config) {
            return fn(config);
        }
    }
}
