/**
 * A `Converter` to convert a `Config` value to an array.
 */
const ArrayConverter<InnerType>(ConfigConverter<InnerType> converter)
        extends AbstractConverter<InnerType[]> {

    @Override
    conditional InnerType[] convertList(Config config) {
        ConfigValue<Config[]> value = config.value(Config[]);
        Array<InnerType> array = new Array();
        if (Config[] configs := value.get()) {
            for (Config c : configs) {
                if (InnerType t := converter.convert(c)) {
                    array.add(t);
                }
            }
        }
        return True, array.freeze(True);
    }
}
