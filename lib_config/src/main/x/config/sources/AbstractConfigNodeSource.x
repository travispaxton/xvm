
@Abstract
class AbstractConfigNodeSource
        implements ConfigSource {

    import nodes.ListListNode;
    import nodes.MapMapNode;
    import nodes.StringValueNode;

    ConfigNode nodeFrom(ConfigItem item) {
        return switch (item.is(_)) {
        case String:                          nodeFromValue(item);
        case Map<String, ConfigItem>:         nodeFromMap(item);
        case List<Tuple<String, ConfigItem>>: nodeFromTuples(item);
        case List<ConfigItem>:                nodeFromList(item);
        default: assert;
        };
    }

    MapNode nodeFromMap(Map<String, ConfigItem> map) {
        MapNode mapNode = new @MapMapNode HashMap<String, ConfigNode>();

        // process keys with dots in the name
        Map<String[], ConfigItem> dottedMap = new HashMap();
        for (Map<String, ConfigItem>.Entry entry : map.entries) {
            String[] parts = entry.key.split('.');
            if (parts.size > 1) {
                dottedMap.put(parts, entry.value);
            }
        }

        for (Map<String[], ConfigItem>.Entry entry : dottedMap.entries) {
            String[] parts = entry.key;
            Map<String, ConfigItem> m = map;
            for (Int i : 0 ..< (parts.size - 1)) {
                if (ConfigItem item := map.get(parts[i])) {
                    if (item.is(Map<String, ConfigItem>)) {
                        m = item;
                        continue;
                    }
                }
                Map<String, ConfigItem> child = new HashMap();
                m.put(parts[i], child);
                m = child;
            }
            m.put(parts[parts.size - 1], entry.value);
        }


        for (Map<String, ConfigItem>.Entry entry : map.entries) {
            ConfigNode node = nodeFrom(entry.value);
            mapNode.put(entry.key, node);
        }

        return mapNode.freeze(True);
    }

    MapNode nodeFromTuples(List<Tuple<String, ConfigItem>> list) {
        Map<String, ConfigItem> map = new HashMap();
        for (Tuple<String, ConfigItem> t : list) {
            map.put(t[0], t[1]);
        }
        return nodeFromMap(map);
    }

    ConfigNode nodeFromList(List<ConfigItem> list) {
        ListNode node = new @ListListNode Array<ConfigNode>();
        for (ConfigItem item : list) {
            ConfigNode child = nodeFrom(item);
            node.add(child);
        }
        return node.freeze(True);
    }

    ConfigNode nodeFromValue(String value) {
        return new StringValueNode(value);
    }
}