import libconfig.Config;
import libconfig.ConfigItem;
import libconfig.ConfigService;
import libconfig.MapNode;
import libconfig.sources.MapConfigSource;

service RTConfig {

    Config createConfigFromMap(Tuple<String, ConfigItem>[] items) {
        Map<String, ConfigItem> map = createMap(items);
        return createConfig(map);
    }

    Config createConfig(Map<String, ConfigItem> map) {
        MapConfigSource source = new MapConfigSource(map);
        assert MapNode node := source.createConfig();
        return new ConfigService(node);
    }

    private Map<String, ConfigItem> createMap(Tuple<String, ConfigItem>[] items) {
        Map<String, ConfigItem> map = new HashMap();
        for (Tuple<String, ConfigItem> t : items) {
            map.put(t[0], createItem(t[1]));
        }
        return map;
    }

    private ConfigItem[] createArray(ConfigItem[] items) {
        Array<ConfigItem> a = new Array();
        for (ConfigItem item : items) {
            a.add(createItem(item));
        }
        return a;
    }

    private ConfigItem createItem(ConfigItem item) {
        return switch (item.is(_)) {
            case Array<Tuple<String, ConfigItem>>: createMap(item);
            case Array<ConfigItem>:                createArray(item);
            default:                               item;
        };
    }




}