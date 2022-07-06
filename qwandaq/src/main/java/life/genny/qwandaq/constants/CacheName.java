package life.genny.qwandaq.constants;

// TODO: Find an appropriate place for this
public enum CacheName {
    BASEENTITY("baseentity"),
    BASEENTITY_ATTRIBUTE("baseentity_attribute"),
    CAPABILITIES("capabilities"),
    ATTRIBUTE("attribute");

    public final String cacheName;
    
    private CacheName(String cacheName) {
        this.cacheName = cacheName;
    }
}
