import xunit.ExecutionContext;
import xunit.Extension;
import xunit.ExtensionProvider;

import xunit.annotations.AfterAll;
import xunit.annotations.BeforeAll;
import xunit.annotations.ExtensionMixin;

import xunit.extensions.AfterAllCallback;
import xunit.extensions.BeforeAllCallback;

import xunit_engine.UniqueId;

import xunit_engine.executor.EngineExecutionContext;
import xunit_engine.executor.ExecutionLifecycle;

import xunit_engine.models.BaseModel;

class AnnotationsTest {

    @Test
    void shouldBeAfterAll() {
        Method m = test_packages.before_and_after.packageLevelAfterAll;
        assert m.is(AfterAll);

        ExtensionProvider[] providers = m.as(ExtensionMixin).getExtensionProviders();
        assert providers.size == 1;

        EngineExecutionContext ctx = EngineExecutionContext.create(new ModelStub());
        Extension[] extensions = providers[0].getExtensions(ctx);
        assert extensions.size == 1;
        assert extensions[0].is(AfterAllCallback);
    }

    @Test
    void shouldBeBeforeAll() {
        Method m = test_packages.before_and_after.packageLevelBeforeAll;
        assert m.is(BeforeAll);

        ExtensionProvider[] providers = m.as(ExtensionMixin).getExtensionProviders();
        assert providers.size == 1;

        EngineExecutionContext ctx = EngineExecutionContext.create(new ModelStub());
        Extension[] extensions = providers[0].getExtensions(ctx);
        assert extensions.size == 1;
        assert extensions[0].is(BeforeAllCallback);
    }

    @Test
    void shouldBeBeforeAllThenAfterAll() {
        Method m = test_packages.before_and_after.packageLevelBeforeAndAfterAll;
        assert m.is(BeforeAll);
        assert m.is(AfterAll);

        ExtensionProvider[] providers = m.as(ExtensionMixin).getExtensionProviders();
        assert providers.size == 2;

        EngineExecutionContext ctx = EngineExecutionContext.create(new ModelStub());
        Extension[] extensions = providers[0].getExtensions(ctx);
        assert extensions.size == 1;
        assert extensions[0].is(AfterAllCallback);
        extensions = providers[1].getExtensions(ctx);
        assert extensions.size == 1;
        assert extensions[0].is(BeforeAllCallback);
    }

    /**
     * A stub of a `Model`.
     */
    @MockTest
    static const ModelStub()
            extends BaseModel(UniqueId.forClass(AnnotationsTest), "Stub", False) {
        @Override
        ExecutionLifecycle createExecutionLifecycle() {
            TODO
        }
    }
}
