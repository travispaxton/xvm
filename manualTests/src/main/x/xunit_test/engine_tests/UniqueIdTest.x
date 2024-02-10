import xunit_engine.UniqueId;

class UniqueIdTest {

    @Test
    void shouldBeOrderable() {
        Module   m   = xunit_test;
        UniqueId id1 = UniqueId.forObject(m);
        Package  p   = xunit_test.test_packages;
        UniqueId id2 = UniqueId.forObject(p);
        Class    clz = xunit_test.test_packages.SimpleTest;
        UniqueId id3 = UniqueId.forObject(clz);

        UniqueId id4 = UniqueId.forObject(m);

        assert id1 == id4;
        assert id1 != id2;

        assert id1 <=> id4 == Equal;
        @Inject Console console;
        console.print($"id1 <=> id2 {id1 <=> id2}");
        console.print($"id2 <=> id1 {id2 <=> id1}");

        assert id1 <=> id2 == Lesser;
        assert id2 <=> id1 == Greater;
    }

    @Test
    void shouldGetIdForModule() {
        Module   m  = xunit_test;
        UniqueId id = UniqueId.forObject(m);

        assert id.type == Module;
        assert id.segments.size == 1;
        assert id.parent() == False;
        assert id.path == &m.actualClass.path;
    }

    @Test
    void shouldGetIdForPackage() {
        Package  p  = xunit_test.test_packages;
        UniqueId id = UniqueId.forObject(p);

        assert id.segments.size == 2;
        assert id.type == Package;
        assert id.value == "test_packages";
        UniqueId parent;
        assert parent := id.parent();
        assert parent == UniqueId.forObject(xunit_test);
        assert id.path == &p.actualClass.path;
    }

    @Test
    void shouldGetIdForNestedPackage() {
        Package  p  = xunit_test.test_packages.simple_pkg;
        UniqueId id = UniqueId.forObject(p);

        assert id.segments.size == 3;
        assert id.type == Package;
        assert id.value == "simple_pkg";
        assert id.path == &p.actualClass.path;
        UniqueId parent;
        assert parent := id.parent();
        assert parent == UniqueId.forObject(xunit_test.test_packages);
    }

    @Test
    void shouldGetIdForClass() {
        Class    clz = xunit_test.test_packages.SimpleTest;
        UniqueId id  = UniqueId.forObject(clz);

        assert id.type == Class;
        assert id.value == "SimpleTest";
        assert id.segments.size == 3;
        assert id.path == clz.path;
        UniqueId parent;
        assert parent := id.parent();
        assert parent == UniqueId.forObject(xunit_test.test_packages);
    }

    @Test
    void shouldGetIdForClassInNestedPackage() {
        Class    clz = xunit_test.test_packages.before_and_after.BeforeAndAfterTests;
        UniqueId id  = UniqueId.forObject(clz);

        assert id.type == Class;
        assert id.segments.size == 4;
        assert id.path == clz.path;
        UniqueId parent;
        assert parent := id.parent();
        assert parent == UniqueId.forObject(xunit_test.test_packages.before_and_after);
    }

    @Test
    void shouldGetIdForMethod() {
        Class    clz    = xunit_test.test_packages.before_and_after.BeforeAndAfterTests;
        Method   method = xunit_test.test_packages.before_and_after.BeforeAndAfterTests.testOne;
        UniqueId id     = UniqueId.forObject(method);

        assert id.type == Method;
        assert id.value == "testOne";

        UniqueId parent;
        assert parent := id.parent();
        assert parent == UniqueId.forObject(clz);
    }
}