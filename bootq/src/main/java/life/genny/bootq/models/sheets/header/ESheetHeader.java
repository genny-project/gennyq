package life.genny.bootq.models.sheets.header;

import static life.genny.bootq.models.sheets.header.Header.*;

import java.util.HashSet;
import java.util.Set;

public enum ESheetHeader {
    CODE(HD_CODE),
    CODE_BA(HD_BASEENTITY_CODE, HD_ATTRIBUTE_CODE),
    CODE_TARGET_PARENT_LINK(HD_TARGET_CODE, HD_PARENT_CODE, HD_LINK_CODE, HD_CODE, HD_SOURCE_CODE),
    CODE_TARGET_PARENT(HD_TARGET_CODE, HD_PARENT_CODE, HD_SOURCE_CODE, HD_LINK_CODE),
    CODE_QUESTION_SOURCE_TARGET(HD_QUESTION_CODE, HD_SOURCE_CODE, HD_TARGET_CODE),
    
    PROJECTS(HD_NAME, HD_SHEET_ID, HD_INGRESS_LIST, 
             HD_URL_LIST, HD_IS_TLS, HD_CODE, HD_CLIENT_SECRET,
             HD_KEYCLOAK_URL, HD_DISABLE, HD_SKIP_GOOGLE_DOC, HD_ENV_SECURITY_KEY
             ,HD_ENV_SERVICE_PASSWORD, HD_PRJ_GIT_URL, HD_CLIENT_BACKEND_KEYCLOAK_SECRET),
             
    MODULES(HD_NAME, HD_MODULE, HD_SHEET_ID);

    private static final String FILTER = "^\"|\"$|_|-";

    private final Set<String> columns = new HashSet<>();

    private ESheetHeader(String... columns) {
        for(String column : columns) {
            this.columns.add(column.toLowerCase().replaceAll(FILTER, ""));
        }
    }

    public Set<String> getColumns() {
        return columns;
    }
}
