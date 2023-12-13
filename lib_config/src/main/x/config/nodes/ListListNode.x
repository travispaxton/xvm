/**
 * A mixin that applies to a `List` to make that `List`
 * implement `ListNode`.
 */
mixin ListListNode(String? value = Null)
        into List<ConfigNode>
        implements ListNode {

    @Override
    conditional String getValue() {
        String? s = value;
        if (s.is(String)) {
            return True, s;
        }
        return False;
    }
}
