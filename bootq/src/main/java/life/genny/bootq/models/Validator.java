package life.genny.bootq.models;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.validation.Validation;

@ApplicationScoped
public class Validator {
    
    protected static final String[] DEF_PREFIXES = new String[] {
        Prefix.ATT_,
        Prefix.SER_,
        Prefix.DFT_,
        Prefix.DEP_,
        Prefix.UNQ_
    };

    @Inject
    CacheManager cm;

    public Map<Class<?>, Object> validateAttribute(Map<String, String> row, String realmName) 
        throws BadDataException {
            String dataTypeCode = row.get("datatype");
            DataType attributeDtt = cm.getDataType(realmName, dataTypeCode);
            if(attributeDtt == null)
                throw new BadDataException("Attribute DataType: " + dataTypeCode + " does not exist in " + realmName + "! Attribute: " + row.get("code"));

            return Map.of(DataType.class, attributeDtt);
    }

    /**
     * Test if a {@link DataType} is a valid DataType (points to at least one {@link Validation} that exists)
     * @param row - row data corresponding to the datatype
     * @param realmName - product the datatype exists in
     * 
     * @returns all valid dependencies (in this case validation list) in an immutable map from Class -> Object
     * 
     * @throws {@link BadDataException} if a corresponding validation cannot be found
     */
    public Map<Class<?>, Object> validateDataType(Map<String, String> row, String realmName) 
        throws BadDataException {
            String validationCode = row.get("validations");
            List<Validation> validations = cm.getValidations(realmName, validationCode);
            if(validations.isEmpty()) {
                throw new BadDataException("No Corresponding Validation for datatype: " + row.get("code") + " in product " + realmName);
            }

            return Map.of(Validation.class.arrayType(), validations);
    }

    public Map<Class<?>, Object> validateEntityAttribute(Map<String, String> row, String realmName) {
        String baseEntityCode = row.get("baseentitycode");

        BaseEntity baseEntity = (BaseEntity) cm.getPersistableEntity(realmName, new BaseEntityKey(realmName, baseEntityCode));
        if(baseEntity == null)
            throw new BadDataException("No Persisted BaseEntity found for code: " + baseEntityCode + " in product " + realmName);

        String attributeCode = row.get("attributecode");

        boolean isDefAttr = baseEntityCode.startsWith(Prefix.DEF_);
        
            // Strip off any of the DEF_PREFIXES
        if(isDefAttr && CommonUtils.isInArray(DEF_PREFIXES, attributeCode.substring(0, 4))) {
            attributeCode = attributeCode.substring(4);
        }

        Attribute attribute = cm.getAttribute(realmName, attributeCode);
        if(attribute == null)
            throw new BadDataException("No Attribute found: " + row.get("attributecode") + " for entity attribute: " + baseEntityCode + ":" + attributeCode + " in product: " + realmName);


        return Map.of(
            BaseEntity.class, baseEntity,
            Attribute.class, attribute
        );
    }
}
