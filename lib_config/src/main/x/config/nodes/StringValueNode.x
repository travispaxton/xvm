
const StringValueNode(String value)
        implements ValueNode
        implements CombinableNode {

    @Override
    immutable ValueNode freeze(Boolean inPlace = False) {
        if (&this.isImmutable) {
            return this.as(immutable ValueNode);
        }
        return makeImmutable();
    }

    @Override
    CombinableNode combine(CombinableNode node) {
        TODO
//        return switch (node.is(_)) {
//        case MapNode:
//        case ListNode: return combineList(node);
//        case ValueNode: return node;
//        }
    }

//    private CombinableNode combineList(CombinableNode node) {
//        if (String s := node.getValue()) {
//            // the ListNode has a value so has priority over this node
//            return node;
//        }
//        // combine this value with the ListNode
//        return node.combine(this);
//    }

//    private CombinableNode combineMap(CombinableNode node) {
//        MapNode.Builder builder = MapNode.builder();
//
//        if (String s := node.getValue()) {
//            // the MapNode has a value so has priority over this node
//            builder.value(s);
//        } else {
//            builder.value(this.value);
//        }
//
//        return builder.build();
//    }
}
