/**
 * A registry of converters that can convert a string
 * configuration value into another type.
 */
service ConverterRegistry {

    construct () {
    } finally {
        convertersByType.put(Boolean, BooleanConverter.Instance);
        convertersByType.put(String, StringConverter.Instance);
    }

    /**
     * The internal registry-by-type of `ConfigConverter` objects.
     */
    private Map<Type, ConfigConverter> convertersByType = new HashMap();

    /**
     * Obtain the `ConfigConverter` to convert a `Config` instance to a specific `Type`.
     *
     * @param type  the `Type` that the converter should convert a `Config` into
     *
     * @return `True` iff a converter is available that can convert a `Config` to the specified type
     * @return the `ConfigConverter` that can convert a `Config` to the required `Type`
     */
    <ToType> conditional ConfigConverter<ToType> findConfigConverter(Type<ToType> type) {
        if (type.isA(Array)) {
            if (Type<>[] innerTypes := type.parameterized()) {
                Type innerType = innerTypes[0];
                if (ConfigConverter<innerType.DataType> innerConverter := findConfigConverter(innerType)) {
                    ConfigConverter<ToType> ac = new ArrayConverter(innerConverter).as(ConfigConverter<ToType>);
                    return True, ac;
                }
            }
            return False;
        }

        if (ConfigConverter converter := convertersByType.get(type)) {
            return True, converter.as(ConfigConverter<ToType>);
        }
        if (ToType.is(Type<Destringable>)) {
            Type<Destringable> t = type.as(Type<Destringable>);
            DestringableConverter converter = new DestringableConverter<t.DataType>();
            convertersByType.put(type, converter);
            return True, converter.as(ConfigConverter<ToType>);
        }
        return False;
    }
}
