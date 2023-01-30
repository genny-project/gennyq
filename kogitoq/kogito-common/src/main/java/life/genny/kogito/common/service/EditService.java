package life.genny.kogito.common.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class EditService extends KogitoService  {

    private static final String EDIT_DEFAULT = "[PCM_EDIT]";

	@Inject
	Logger log;
    
    public String getCurrentPcm(String pcmCodes, Integer currentIndex) {
        String[] pcmCodesArray = CommonUtils.getArrayFromString(pcmCodes, String.class, (str) -> str);
        if(pcmCodesArray.length == 0)
            throw new BadDataException("No PCM Codes");
        // TODO: Replace when kogito workflow errors get revised
        if(currentIndex > pcmCodesArray.length - 1)
            return pcmCodesArray[pcmCodesArray.length - 1];
        if(currentIndex < 0)
            return pcmCodesArray[0];
        return pcmCodesArray[currentIndex].strip();
    }

	/**
	 * Get the edit question groups for a {@link BaseEntity}
	 * <p> will default to <b>QUE_BASEENTITY_GRP</b> if no {@link Attribute#LNK_EDIT_PCMS} attribute exists in the {@link Definition}
	 *     or if the value is blank
	 * </p>
	 * @param baseEntityCode a BaseEntity or Definition code 
	 * @return array of question group codes in order of appearance for editing the BaseEntity
	 */
	public String getEditPcmCodes(String targetCode) {
		if(targetCode == null)
			throw new NullParameterException("targetCode");
		
		// ensure def
		log.info("[!] Attempting to retrieve edit pcms from base entity: " + targetCode);
		Definition baseEntity = defUtils.getDEF(targetCode);
		if(baseEntity == null) {
			log.error("Could not find Definition of be: " + targetCode);
			throw new BadDataException("No definition of BaseEntity: " + targetCode);
		}

		String editCodes = getEditPcmCodes(baseEntity);
		log.debug("Got edit codes for target: " + targetCode);
        return editCodes;
	}

	/**
	 * Get the edit question groups for a {@link BaseEntity}
	 * <p> will default to <b>QUE_BASEENTITY_GRP</b> if no {@link Attribute#LNK_EDIT_PCMS} attribute exists in the {@link Definition}
	 *     or if the value is blank
	 * </p>
	 * @param baseEntityCode a BaseEntity or Definition code 
	 * @return array of question group codes in order of appearance for editing the BaseEntity
	 */
	public String getEditPcmCodes(BaseEntity baseEntity) {
		if(baseEntity == null)
			throw new NullParameterException("baseEntity");
		
		// Convert to DEF
		if(!baseEntity.getCode().startsWith(Prefix.DEF_)) {
			baseEntity = defUtils.getDEF(baseEntity);
		}

		// Look for attached edit ques. If none then default to QUE_BASEENTITY_GRP
		Optional<EntityAttribute> editQuesLnk = baseEntity.findEntityAttribute(Attribute.LNK_EDIT_PCMS);
		if(!editQuesLnk.isPresent()) {
			log.warn("Could not find LNK_EDIT_PCMS in " + baseEntity.getCode() + ". Defaulting to PCM_FORM_EDIT");
			return EDIT_DEFAULT;
		}
		
		String editQues = editQuesLnk.get().getValueString();
        if(CommonUtils.STR_ARRAY_EMPTY.equals(editQues)) {
            log.warn("Found empty array string in LNK_EDIT_PCMS for " + baseEntity.getCode() + "! Defaulting to PCM_EDIT");
            return EDIT_DEFAULT;
        }
		log.debug("FOUND LNK_EDIT_PCMS in BaseEntity: " + baseEntity + ". Edit PCM Codes: " + editQues);

		if(!StringUtils.isBlank(editQues)) {
			return editQues;
		}
		
		log.warn("Edit pcm was blank. Defaulting to PCM_FORM_EDIT");
		return EDIT_DEFAULT;
	}
}
