package life.genny.qwandaq.models;

import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.constants.Prefix;


import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import java.util.Map;

public class SavedSearch {
    static Jsonb jsonb = JsonbBuilder.create();

    private String code;
    private String column;
    private String operator;
    private String value;
    private String valueCode;

    private boolean beingDate;

    public SavedSearch(){}

    /**
     * Constructor
     * @param attributeCode Attribute code
     * @param value Value
     */
    public SavedSearch(String attributeCode,String value) {
        this.code = attributeCode;

        String[] splitted = value.split(FilterConst.SEPARATOR);
        this.operator = "";
        this.value = "";

        if(splitted.length == 2) {
            this.operator = splitted[0].replaceFirst(Prefix.SEL,"");
            this.value = splitted[1].replaceFirst(Prefix.SEL,"");
        }
        this.column = getColumnName(attributeCode);
        this.valueCode = value;

        this.beingDate = false;
        if(this.column.contains(FilterConst.DATETIME)) {
            this.beingDate = true;
        }
    }

    /**
     * Convert map to saved search
     * @param map Map of parameters
     * @return convert to saved search object
     */
    public SavedSearch convertToSavedSearch(Map<String,String> map) {
        String strJson = jsonb.toJson(map);
        SavedSearch savedSearch = jsonb.fromJson(strJson, SavedSearch.class);
        return savedSearch;
    }

    /**
     * Attribtue code
     * @return Attribtue code
     */
    public String getCode() {
        return code;
    }

    /**
     * Set attribtue code
     * @param code Attribute code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Get column name
     * @return Column name
     */
    public String getColumn() {
        return column;
    }

    /**
     * Set column name
     * @param column Column name
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * Return operator
     * @return Operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Set operator
     * @param operator Operator
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * Return value
     * @return Value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set value
     * @param value Value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Return value code
     * @return Value code
     */
    public String getValueCode() {
        return valueCode;
    }

    /**
     * Set value code
     * @param valueCode Value code
     */
    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    /**
     * Being date type
     * @return being date type
     */
    public boolean isBeingDate() {
        return beingDate;
    }

    /**
     * Set date type
     * @param beingDate Being date type
     */
    public void setBeingDate(boolean beingDate) {
        this.beingDate = beingDate;
    }

    /**
     * Return the column name
     * @param value Value
     * @return Return the column name
     */
    public String getColumnName(String value) {
        String fieldName = "";
        int priIndex = -1;
        int fieldIndex = value.lastIndexOf(Prefix.FIELD);
        if(fieldIndex > -1) {
            priIndex = value.indexOf(Prefix.FIELD) + Prefix.FIELD.length();
            fieldName = value.substring(priIndex);
            return fieldName;
        } else {
            priIndex = value.lastIndexOf(Prefix.PRI);
        }
        if(priIndex > -1) {
            fieldName = value.substring(priIndex);
            fieldName = fieldName.replaceFirst("\"]","");
        }
        return fieldName;
    }

}