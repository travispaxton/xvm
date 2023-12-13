/**
 * A `ConfigConverter` to convert a `Config` value to a `String`.
 */
const StringConverter
        extends AbstractConverter<String> {

    /**
     * A singleton instance of a `StringConverter`.
     */
    static StringConverter Instance = new StringConverter();

    @Override
    conditional String convertValue(Config config) {
        if (String value := config.value(String).get()) {
            return True, value;
        }
        return False;
    }
}
