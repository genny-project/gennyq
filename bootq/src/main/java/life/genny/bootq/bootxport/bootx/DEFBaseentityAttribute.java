package life.genny.bootq.bootxport.bootx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DEFBaseentityAttribute {
    private String baseentiyCode;
    private Set<String> attributes;
    private List<String> includeDefBaseentitys;
    boolean hasLnkInclude;

    public DEFBaseentityAttribute(String baseentiyCode) {
        this.baseentiyCode = baseentiyCode;
        this.attributes = new HashSet<>();
        this.includeDefBaseentitys = new ArrayList<>();
        this.hasLnkInclude = false;
    }

    public boolean isHasLnkInclude() {
        return hasLnkInclude;
    }

    public void setHasLnkInclude(boolean hasLnkInclude) {
        this.hasLnkInclude = hasLnkInclude;
    }

    public String getBaseentiyCode() {
        return baseentiyCode;
    }

    public void setBaseentiyCode(String baseentiyCode) {
        this.baseentiyCode = baseentiyCode;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getIncludeDefBaseentitys() {
        return includeDefBaseentitys;
    }

    public void setIncludeDefBaseentitys(List<String> includeDefBaseentitys) {
        this.includeDefBaseentitys = includeDefBaseentitys;
    }
}
