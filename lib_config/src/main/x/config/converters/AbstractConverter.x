/**
 * A base class for `ConfigConverter` implementations.
 */
const AbstractConverter<ToType>
        implements ConfigConverter<ToType> {

    @Override
    conditional ToType convert(Config config) {
        return switch (config.type) {
        case Map: convertMap(config);
        case List: convertList(config);
        case Value: convertValue(config);
        default: False;
        };
    }

    conditional ToType convertMap(Config config) {
        return False;
    }

    conditional ToType convertList(Config config) {
        return False;
    }

    conditional ToType convertValue(Config config) {
        return False;
    }
}
