/**
 * The XUnit test framework module.
 *
 * Applications can import this module to add XUnit tests to their
 * application code.
 */
module xunit.xtclang.org {

    /**
     * A mixin that marks a module as a test suite.
     *
     * ToDo: we should eventually be able to remove this when there is a proper "xtest"
     * Ecstasy test executable that will execute tests for a given module in the same
     * way that "xec" executes a module.
     */
    mixin Suite
            into Module {
        /**
         * Discover and execute all the test fixtures in the `Module`.
         */
        void test() {
        }
    }

    /**
     * The valid targets for a Test annotation.
     */
    typedef Module | Package | Class | Property | Method | Function as TestTarget;

    /**
     * An `PreconditionFailed` exception is raised when a test precondition fails.
     *
     * This is typically used in tests to indicate that a the preconditions for running
     * the test cannot be met so the test is marked as skipped rather than failed.
     */
    const PreconditionFailed(String? text = Null, Exception? cause = Null)
            extends Exception(text, cause);

    /**
     * An class that determines whether a test should be skipped.
     */
    interface TestExecutionPredicate
            extends Const
        {
        /**
         * Returns whether a test should be skipped.
         *
         * @return `True` if the test should be skipped, otherwise `False`
         * @return the reason the test should be skipped
         */
        conditional String shouldSkip(ExecutionContext context);
        }

    /**
     * A `Method` or a `Function`.
     */
    typedef Method<Object, Tuple<>, Tuple<>> | Function<Tuple, Tuple> | Function<<>, <Object>> as MethodOrFunction;

    /**
     * A function that performs a predicate check on a test fixture.
     */
    typedef function Boolean (Object) as FixturePredicate;

    /**
     * A `FixturePredicate` that tests a value is a `Method`.
     */
    static FixturePredicate MethodFixturePredicate = o -> o.is(Method);

    /**
     * A `FixturePredicate` that tests a value is a `Class`.
     */
    static FixturePredicate ClassFixturePredicate = o -> o.is(Class);

    /**
     * A `FixturePredicate` that tests a value is a `Package`.
     */
    static FixturePredicate PackageFixturePredicate = o -> o.is(Package);

   /**
     * The result for a possibly skipped test,
     *
     * @param skipped  a flag that is `True` if the test was skipped, or `False` if the test was executed.
     * @param reason   an optional message to describe why the test was skipped
     */
	const SkipResult(Boolean skipped, String reason = "unknown") {
	    /**
	     * A singleton not skipped `SkipResult`.
	     */
	    static SkipResult NotSkipped = new SkipResult(False);
    }
}
