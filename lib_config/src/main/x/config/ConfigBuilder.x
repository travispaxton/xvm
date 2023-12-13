
class ConfigBuilder {

    private Array<ConfigSource> sources = new Array();

    ConfigBuilder addSource(ConfigSource source) {
        sources.add(source);
        return this;
    }


    Config build() {
        TODO
    }
}
