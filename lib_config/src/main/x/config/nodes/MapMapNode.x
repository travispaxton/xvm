
mixin MapMapNode(String? value = Null)
        into Map<String, ConfigNode>
        implements MapNode {

    @Override
    conditional String getValue() {
        String? s = value;
        if (s.is(String)) {
            return True, s;
        }
        return False;
    }
}