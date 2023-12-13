const EmptyConfigValue<Value>(ConfigKey key)
        implements ConfigValue<Value> {

    @Override
    conditional Value get() {
        return False;
    }
}
