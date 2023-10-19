
import xunit.assertions;

class AssertionsTest {

    @Test
    void shouldAssertException() {
        ExceptionOne ex = assertions.assertThrows(() -> {
            throw new ExceptionOne("test...");
        });

        assert ex.message == "test...";
        assert ex.cause == Null;
    }

    @Test
    void shouldAssertWrongException() {
        Exception? thrown = Null;
        try {
            ExceptionOne ex = assertions.assertThrows(() -> {
                throw new ExceptionTwo("test...");
            });
        } catch (Exception e) {
            thrown = e;
        }

        assert thrown != Null;
        assert thrown.is(Assertion);
        assert thrown.as(Assertion).message.startsWith("expected api_tests.assertions.AssertionsTest.ExceptionOne to be thrown but was xunit_test:api_tests.assertions.AssertionsTest.ExceptionTwo: test...\n");
        assert thrown.as(Assertion).cause == Null;
    }


    @Test
    void shouldAssertNoException() {
        Exception? thrown = Null;
        try {
            ExceptionOne ex = assertions.assertThrows(() -> {
                return;
            });
        } catch (Exception e) {
            thrown = e;
        }

        assert thrown != Null;
        assert thrown.is(Assertion);
        assert thrown.as(Assertion).message == "expected api_tests.assertions.AssertionsTest.ExceptionOne to be thrown";
        assert thrown.as(Assertion).cause == Null;
    }

    static const ExceptionOne(String? text = Null, Exception? cause = Null)
            extends Exception(text, cause);

    static const ExceptionTwo(String? text = Null, Exception? cause = Null)
            extends Exception(text, cause);
}