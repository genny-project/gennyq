package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class EditService extends KogitoService  {

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

	public String getEditPcmCodes(String targetCode) {
		String editCodes = qwandaUtils.getEditPcmCodes(targetCode);
		log.debug("Got edit codes for target: " + targetCode);

        if("[]".equals(editCodes)) {
            log.warn("No PCM Codes present for target: " + targetCode + ". Defaulting to PCM_EDIT and QUE_BASEENITY_GRP");
            return "[PCM_EDIT]";
        }

        return editCodes;
	}
}
