package life.genny.bootq.models;

import java.util.HashSet;
import java.util.Set;

public class DataKeyColumn {
    protected static final Set<String> CODE = new HashSet<>();
    protected static final Set<String> CODE_BA = new HashSet<>();
    protected static final Set<String> CODE_TARGET_PARENT_LINK = new HashSet<>();
    protected static final Set<String> CODE_TARGET_PARENT = new HashSet<>();
    protected static final Set<String> CODE_QUESTION_SOURCE_TARGET = new HashSet<>();
    private static String regExString = "^\"|\"$|_|-";
    private static String targetCodeStr = "targetCode";

    private DataKeyColumn() {
    }

    static {
        CODE.add("code".replaceAll(regExString, ""));

        CODE_BA.add("baseEntityCode".toLowerCase().replaceAll(regExString, ""));
        CODE_BA.add("attributeCode".toLowerCase().replaceAll(regExString, ""));

        CODE_TARGET_PARENT_LINK.add(targetCodeStr.toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT_LINK.add("parentCode".toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT_LINK.add("linkCode".toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT_LINK.add("Code".toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT_LINK.add("SourceCode".toLowerCase().replaceAll(regExString, ""));

        CODE_TARGET_PARENT.add(targetCodeStr.toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT.add("parentCode".toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT.add("sourceCode".toLowerCase().replaceAll(regExString, ""));
        CODE_TARGET_PARENT.add("linkCode".toLowerCase().replaceAll(regExString, ""));

        CODE_QUESTION_SOURCE_TARGET.add("question_code".toLowerCase().replaceAll(regExString, ""));
        CODE_QUESTION_SOURCE_TARGET.add("sourceCode".toLowerCase().replaceAll(regExString, ""));
        CODE_QUESTION_SOURCE_TARGET.add(targetCodeStr.toLowerCase().replaceAll(regExString, ""));
    }
}
