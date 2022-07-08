package life.genny.qwandaq.constants;

// TODO: Find an appropriate place for this
public enum CacheName {
    GENERIC("generic"), // Avoid this where possible
    METADATA("metadata"),
    BASEENTITY("baseentity"),
    BASEENTITY_ATTRIBUTE("baseentity_attribute"),
    CAPABILITIES("capabilities"),
    ATTRIBUTE("attribute");

    public final String cacheName;
    
    private CacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * Case-insensitive fetching of an {@link CacheName}
     * @param name
     * @return
     */
    public static CacheName getCacheName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch(NullPointerException e) {
            System.err.println("[!] CacheName: Could not find cache with name: " + name.toUpperCase());
            return null;
        }
    }
}
