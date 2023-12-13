/**
 * A `Converter` to convert a `Config` value to an instance
 * of a specific `Destringable` type.
 */
const DestringableConverter<ToType extends Destringable>
        extends AbstractConverter<ToType> {

    @Override
    conditional ToType convertValue(Config config) {
        if (String value := config.value().get()) {
            return True, new ToType(value.toString());
        }
        return False;
    }
}
