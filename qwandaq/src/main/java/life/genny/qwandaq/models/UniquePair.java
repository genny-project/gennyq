package life.genny.qwandaq.models;

public class UniquePair {

    String attributeCode;
    String value;

    public UniquePair(final String attributeCode, final String value) {
        this.attributeCode = attributeCode;
        this.value = value;
    }

    public String getAttributeCode() {
        return attributeCode;
    }

    public void setAttributeCode(String attributeCode) {
        this.attributeCode = attributeCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "UniquePair [attributeCode=" + attributeCode + ", value=" + value + "]";
    }

    
    
}
