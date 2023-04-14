package life.genny.bootq.models;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.ECacheRef;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.validation.Validation;

@ApplicationScoped
public class Validator {
    public static final String KEY_PARENT = "parent";
    public static final String KEY_CHILD = "child";
    
    protected static final String[] DEF_PREFIXES = new String[] {
        Prefix.ATT_,
        Prefix.SER_,
        Prefix.DFT_,
        Prefix.DEP_,
        Prefix.UNQ_
    };

    @Inject
    CacheManager cm;

    /**
     * Validate a Capability Requirements String from the google sheets
     * @param capabilityString - string of requirements to interpret
     * @return - a valid capability set if no error occurred
     * @throws BadDataException if the capability string could not be parsed properly
     * 
     * @see {@link CapabilityConverter#convertToEa} to see where BadDataExceptions are thrown
     */
    public Set<Capability> validateCapabilities(String capabilityString) 
        throws BadDataException {
            if(StringUtils.isBlank(capabilityString))
                return new HashSet<>();
            try {
                return CapabilityConverter.convertToEA(capabilityString);
            } catch (BadDataException e) {
                throw new BadDataException("Exception occurred when validating Capability Requirements: " + capabilityString + ". " + e.getMessage(), e);
            }
    }

    /**
     * Validate a Question Question by first finding its dependencies (source question and child question )
     * @param row the corresponding google sheets row
     * @param realmName the product these questions belong in
     * @return - a map containing the valid dependencies
     * <pre>e.g
     * {
     * "parent": [parent question object],
     * "child": [child question object]
     * }
     * </pre>
     * @throws BadDataException if the parent question or child question could not be found in the given product
     */
    public Map<String, Object> validateQuestionQuestion(Map<String, String> row, String realmName) 
        throws BadDataException {

            String parentCode = row.get("parentcode");
            String childCode = row.get("targetcode");

            Question parentQuestion = cm.getQuestion(realmName, parentCode);
            if(parentQuestion == null) {
                throw new BadDataException("Parent Question: " + parentCode + " of QuestionQuestion: " + parentCode + ":" + childCode + " does not exist/has not been persisted. Please check the question table");
            }

            Question childQuestion = cm.getQuestion(realmName, childCode);
            if(childQuestion == null) {
                throw new BadDataException("Child Question: " + childCode + " of QuestionQuestion: " + parentCode + ":" + childCode + " does not exist/has not been persisted. Please check the question table");
            }

        return Map.of
        (KEY_PARENT, parentQuestion,
        KEY_CHILD, childQuestion);
    }

    /**
     * Validate a single question (find its Attribute dependeny in cache)
     * @param row - the google sheets row pertaining to the Question
     * @param realmName - the product the Question belongs in
     * @return - a Map of the question's dependencies (in this case just the Attribute)
     * @throws BadDataException if the dependent attribute cannot be found
     */
    public Map<Class<?>, Object> validateQuestion(Map<String, String> row, String realmName)
        throws BadDataException {

            String attributeCode = row.get("attributecode");

            Attribute attribute = cm.getAttribute(realmName, attributeCode);
            if(attribute == null) {
                throw new BadDataException("Attribute: " + attributeCode + " cannot be found in product " + realmName);
            }

            return Map.of
            (
                Attribute.class, attribute
            );
        }

    /**
     * Validate a given Attribute (find its dependent DataType)
     * @param row - the google sheets row pertaining to the DataType
     * @param realmName - the product the Attribute lives in
     * @return a map of the Attributes dependencies (in this case containing 1 entry for the DataType)
     * @throws BadDataException if the DataType could not be found
     */
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

    /**
     * Validate an Entity Attribute or a DEF_EntityAttribute
     * @param row - the Entity Attribute row in the google sheets
     * @param realmName - the product the EntityAttribute lives in
     * @return the base entity and attribute dependencies in a map (if they exist)
     * 
     * @throws BadDataException if the base entity or attribute corresponding to the entity attribute does not exist
     * <p> this will throw a bad data exception for a DEF attribute if its base attribute does not exist. E.g DEF_USER:ATT_PRI_NAME will throw
     *  an exception if the attribute: PRI_NAME does not exist</p>
     */
    public Map<Class<?>, Object> validateEntityAttribute(Map<String, String> row, String realmName) 
        throws BadDataException {
        String baseEntityCode = row.get("baseentitycode");

        BaseEntity baseEntity = (BaseEntity) cm.getPersistableEntity(ECacheRef.BASEENTITY, new BaseEntityKey(realmName, baseEntityCode));
        if(baseEntity == null)
            throw new BadDataException("No Persisted BaseEntity found for code: " + baseEntityCode + " in product " + realmName);

        String attributeCode = row.get("attributecode");

        boolean isDefAttr = baseEntityCode.startsWith(Prefix.DEF_);
        
            // Strip off any of the DEF_PREFIXES
        if(isDefAttr && CommonUtils.isInArray(DEF_PREFIXES, attributeCode.substring(0, 4))) {
            attributeCode = attributeCode.substring(4);
        }

        Attribute attribute = cm.getAttribute(realmName, attributeCode);
        if(attribute == null) {
            if(isDefAttr) {
                throw new BadDataException("Missing Base Attribute: " + attributeCode + " for entity attribute: " + baseEntityCode + ":" + row.get("attributecode") + " in product: " + realmName);
            } else {
                throw new BadDataException("Missing Attribute: " + attributeCode + " for entity attribute: " + baseEntityCode + ":" + attributeCode + " in product: " + realmName);
            }
        }


        return Map.of(
            BaseEntity.class, baseEntity,
            Attribute.class, attribute
        );
    }
}
