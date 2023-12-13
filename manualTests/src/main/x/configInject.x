
module TestConfig {

    package config import config.xtclang.org;

    import config.Config;
    import config.ConfigValue;

    @Inject ecstasy.io.Console console;

    void run() {
        console.print("TestConfig tests");

        // Injection of the root Config
        @Inject Config config;

        Config cfgOS = config.get("os");

        cfgOS.get("name").value().apply(s -> console.print(s));
        cfgOS.get("arch").value().apply(s -> console.print(s));
        cfgOS.get("version").value().apply(s -> console.print(s));

        Config cfgJava = config.get("java.class.version");
        ConfigValue<Double> value = cfgJava.value(Double);
        assert Double d := value.get();
        console.print($"Class version is {d}");

        // Injection of the Config for the specified key
        @Inject("config", opts="os.name") Config config2;
        config2.value().apply(s -> console.print(s));

        assert String name1 := cfgOS.get("name").value().get();
        assert String name2 := config2.value().get();
        assert name1 == name2;
    }

}