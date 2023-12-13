class MapConfigSource
        extends AbstractConfigNodeSource {

    construct (Map<String, ConfigItem> map) {
        items = map;
    }

    private Map<String, ConfigItem> items;

    @Override
    conditional MapNode createConfig() {
        if (items.empty) {
            return False;
        }
        return True, nodeFromMap(items);
    }
}
