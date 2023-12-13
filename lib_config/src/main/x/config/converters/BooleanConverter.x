/**
 * A `Converter` to convert a `Config` value to a `Boolean`.
 */
const BooleanConverter
        extends AbstractConverter<Boolean> {

    /**
     * A singleton instance of a `BooleanConverter`.
     */
    static BooleanConverter Instance = new BooleanConverter();

    @Override
    conditional Boolean convertValue(Config config) {
        if (String value := config.value(String).get()) {
            return True, "true" == value.toLowercase();
        }
        return False;
    }
}
