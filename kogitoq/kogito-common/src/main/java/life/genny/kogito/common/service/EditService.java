package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;

import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class EditService extends KogitoService  {
    
    public String getCurrentPcm(String pcmCodes, int currentIndex) {
        String[] pcmCodesArray = CommonUtils.getArrayFromString(pcmCodes, String.class, (str) -> str);

        // TODO: Replace when kogito workflow errors get revised
        if(currentIndex > pcmCodesArray.length - 1)
            return pcmCodesArray[pcmCodesArray.length - 1];
        
    }
}
