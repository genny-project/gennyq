package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;

import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class EditService extends KogitoService  {
    
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
		log.info("Got edit codes for target: " + targetCode);
		log.info("	- Edit Codes: " + editCodes);
		return editCodes;
	}
}
