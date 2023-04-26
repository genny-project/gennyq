package life.genny.kogito.common.service;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.attribute.EntityAttribute;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.Optional;

import static life.genny.qwandaq.utils.FailureHandler.optional;
import static life.genny.qwandaq.utils.FailureHandler.required;

@ApplicationScoped
public class BaseEntityService extends KogitoService {

    @Inject
    Logger log;

    /**
     * Send a message to perform an update of a persons summary
     */
    public void updateSummary(String personCode, String summaryCode) {

        BaseEntity person = beUtils.getBaseEntity(personCode);
        Attribute attribute = attributeUtils.getAttribute(Attribute.LNK_SUMMARY, true);
        beaUtils.updateEntityAttribute(new EntityAttribute(person, attribute, 1.0, "[\"" + summaryCode + "\"]"));
        log.info("Summary updated -> " + personCode + " : " + summaryCode);
    }

    /**
     * @param definitionCode
     * @return
     */
    public String commission(String definitionCode) {

        if (definitionCode == null) throw new NullParameterException("definitionCode");
        if (!definitionCode.startsWith(Prefix.DEF_))
            throw new DebugException("Invalid definitionCode: " + definitionCode);

        // fetch the def baseentity
        Definition definition = beUtils.getDefinition(definitionCode);

        // use entity create function and save to db
        BaseEntity entity = beUtils.create(definition);
        log.info("BaseEntity Created: " + entity.getCode());

        entity.setStatus(EEntityStatus.PENDING);
        beUtils.updateBaseEntity(entity);

        return entity.getCode();
    }

    public void decommission(String code) {

        if (code == null) throw new NullParameterException("code");

        BaseEntity baseEntity = beUtils.getBaseEntity(code);
        log.info("Decommissioning entity " + baseEntity.getCode());

        // archive the entity
        baseEntity.setStatus(EEntityStatus.ARCHIVED);
        beUtils.updateBaseEntity(baseEntity);
    }

    public String commission(String definitionCode, String processId, EEntityStatus status) {

        if (definitionCode == null) throw new NullParameterException("definitionCode");
        if (processId == null) throw new NullParameterException("processId");
        if (status == null) throw new NullParameterException("status");
        if (!definitionCode.startsWith(Prefix.DEF_))
            throw new DebugException("Invalid definitionCode: " + definitionCode);

        // fetch the def baseentity
        Definition definition = beUtils.getDefinition(definitionCode);

        // use entity create function and save to db
        String defCode = definition.getCode();
        String defaultName = StringUtils.capitalize(defCode.substring(4));
        EntityAttribute prefixAttr = beaUtils.getEntityAttribute(definition.getRealm(), defCode, Attribute.PRI_PREFIX, false);
        if (prefixAttr == null) {
            throw new ItemNotFoundException(definition.getRealm(), defCode, Attribute.PRI_PREFIX);
        }
        String prefixValue = prefixAttr.getValueString();
        BaseEntity entity = beUtils.create(definition, defaultName, prefixValue + "_" + processId.toUpperCase());
        log.info("BaseEntity Created: " + entity.getCode());

        entity.setStatus(status);
        beUtils.updateBaseEntity(entity);

        return entity.getCode();
    }

    public void delete(String code) {

        if (code == null) throw new NullParameterException("code");

        BaseEntity baseEntity = beUtils.getBaseEntity(code);
        log.info("Deleting entity using EEntityStatus" + baseEntity.getCode());

        // archive the entity
        baseEntity.setStatus(EEntityStatus.DELETED);
        beUtils.updateBaseEntity(baseEntity);
    }

    public void pendingDelete(String code) {

        if (code == null) throw new NullParameterException("code");

        BaseEntity baseEntity = beUtils.getBaseEntity(code);
        log.info("Pending Deleting entity using EEntityStatus" + baseEntity.getCode());

        // archive the entity
        baseEntity.setStatus(EEntityStatus.PENDING_DELETE);
        beUtils.updateBaseEntity(baseEntity);
    }

    public void setActive(String entityCode) {

        BaseEntity entity = beUtils.getBaseEntity(entityCode);
        entity.setStatus(EEntityStatus.ACTIVE);
        beUtils.updateBaseEntity(entity);
    }

    public void setDisabled(String entityCode) {

        BaseEntity entity = beUtils.getBaseEntity(entityCode);
        entity.setStatus(EEntityStatus.DISABLED);
        beUtils.updateBaseEntity(entity);
    }

    public String getDEFPrefix(String definitionCode) {

        BaseEntity definition = beUtils.getBaseEntity(definitionCode);

        Optional<String> prefix = definition.getValue("PRI_PREFIX");
        if (prefix.isEmpty()) {
            throw new NullParameterException(definition.getCode() + ":PRI_PREFIX");
        }

        return prefix.get();
    }

    /**
     * Update the email, firstname and lastname in keycloak
     *
     * @param userCode the UserCode
     */
    public void updateKeycloak(String userCode) {

        String realm = required(() -> userToken.getRealm());
        String email = required(() -> beaUtils.getEntityAttribute(realm, userCode, Attribute.PRI_EMAIL).getValueString());
        String firstName = required(() -> beaUtils.getEntityAttribute(realm, userCode, Attribute.PRI_FIRSTNAME).getValueString());
        String lastName = required(() -> beaUtils.getEntityAttribute(realm, userCode, Attribute.PRI_LASTNAME).getValueString());
        String uuid = required(() -> beaUtils.getEntityAttribute(realm, userCode, Attribute.PRI_UUID).getValueString());

        JsonObject request = Json.createObjectBuilder().add("email", email).add("enabled", true).add("emailVerified", true).add("username", email).add("firstName", firstName).add("lastName", lastName).build();

        keycloakUtils.updateUserDetails(request, uuid, realm);
    }

    public String updatePassword(String userCode) {
        String uuid = required(() -> beaUtils.getEntityAttribute(userToken.getRealm(), userCode, "PRI_UUID").getValueString());
        log.info("uuid: " + uuid);
        return keycloakUtils.updateUserTemporaryPassword(uuid, userToken.getRealm());
    }

    /**
     * Merge a process entity into another entity
     */
    public void mergeFromProcessEntity(String entityCode, ProcessData processData) {

        BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
        BaseEntity entity = beUtils.getBaseEntity(entityCode);

        // iterate our stored process updates and create an answer
        for (EntityAttribute ea : beaUtils.getAllEntityAttributesForBaseEntity(processEntity)) {
            ea.setBaseEntityCode(entity.getCode());
            ea.setBaseEntityId(entity.getId());
            beaUtils.updateEntityAttribute(ea);
        }
        log.info("Saved answers for entity " + entityCode);
    }

    /**
     * Update entityAttributes using updatePairs
     */
    public void updatePairsBaseEntity(String baseEntityCode, String updatePairs) {

        // Now split up the updatePairs

        String[] pairs = updatePairs.split(";");
        for (String pair : pairs) {
            String[] elements = pair.split(":");
            if (elements.length == 1) {
                updateBaseEntity(baseEntityCode, "PRI_PQ_STAGE", elements[0]); // assume valid due to initial
                // construction
            } else if (elements.length == 2) {
                updateBaseEntity(baseEntityCode, elements[0], elements[1]); // assume valid due to initial
                // construction
            } else if (elements.length == 3) {
                updateBaseEntity(elements[0], elements[1], elements[2]); // assume valid due to initial
                // construction
            }
        }

    }

    /**
     * Update entityAttributes
     */
    public void updateBaseEntity(String baseEntityCode, String attributeCode, String value) {

        BaseEntity be = beUtils.getBaseEntity(baseEntityCode);
        String realm = be.getRealm();
        BaseEntity defBe = defUtils.getDEF(be);

        EntityAttribute ea = optional(() -> beaUtils.getEntityAttribute(defBe.getRealm(), defBe.getCode(), Prefix.ATT_ + attributeCode));

        if (ea != null) {
            log.infof("%s found in %s", attributeCode, defBe.getCode());
            EntityAttribute beAttribute = optional(() -> beaUtils.getEntityAttribute(realm, baseEntityCode, attributeCode));

            if (beAttribute == null) log.infof("Adding %s to %s", attributeCode, baseEntityCode);

            if (attributeCode.startsWith(Prefix.LNK_)) {
                // Check if value is in JsonArray format , otherwise wrap it..
                if (value != null && !value.startsWith("[")) {
                    value = "[\"" + value + "\"]";
                }
            }

            be = beUtils.addValue(be, attributeCode, value);

            beUtils.updateBaseEntity(be);

        } else {
            log.error(attributeCode + " is not defined in " + defBe.getCode());
        }
    }

    /**
     * Update baseentity with setup attribute values
     */
    public void setupBaseEntity(String baseEntityCode, String... attributeCodeValue) {

        BaseEntity be = beUtils.getBaseEntity(baseEntityCode);

        for (int i = 0; i < attributeCodeValue.length; i++) {
            String[] split = attributeCodeValue[i].split("=");
            String attributeCode = split[0];
            String value = split[1];
            if (attributeCode.startsWith("LNK_")) {
                // Check if value is in JsonArray format , otherwise wrap it..
                if (value != null) {
                    if (!value.startsWith("[")) {
                        value = "[\"" + value + "\"]";
                    }
                }
            }
            be = beUtils.addValue(be, attributeCode, value);
        }

        beUtils.updateBaseEntity(be);
    }

    /**
     * Set pending status to base entity
     *
     * @param entityCode
     */
    public void setPending(String entityCode) {
        BaseEntity entity = beUtils.getBaseEntity(entityCode);
        entity.setStatus(EEntityStatus.PENDING);
        beUtils.updateBaseEntity(entity);
    }
}
