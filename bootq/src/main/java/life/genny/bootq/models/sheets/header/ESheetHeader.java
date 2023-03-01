package life.genny.bootq.models.sheets.header;

import static life.genny.bootq.models.sheets.header.Header.*;

import java.util.HashSet;
import java.util.Set;

public enum ESheetHeader {
    CODE(HD_CODE),
    CODE_BA(HD_BASEENTITY_CODE, HD_ATTRIBUTE_CODE),
    CODE_TARGET_PARENT_LINK(HD_TARGET_CODE, HD_PARENT_CODE, HD_LINK_CODE, HD_CODE, HD_SOURCE_CODE),
    CODE_TARGET_PARENT(HD_TARGET_CODE, HD_PARENT_CODE, HD_SOURCE_CODE, HD_LINK_CODE),
    CODE_QUESTION_SOURCE_TARGET(HD_QUESTION_CODE, HD_SOURCE_CODE, HD_TARGET_CODE);

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
