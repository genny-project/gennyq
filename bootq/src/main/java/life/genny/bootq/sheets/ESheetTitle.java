package life.genny.bootq.sheets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import life.genny.bootq.models.DataKeyColumn;

/**
 * An enumerator containing essential static data for parsing data from the
 * sheets
 */
public enum ESheetTitle {
    // If no DataKeyColumn is set here, this defaults to DataKeyColumn.CODE
    VALIDATION("Validation"),
    DATATYPE("DataType"),
    ATTRIBUTE("Attribute"),
    BASE_ENTITY("BaseEntity"),
    QUESTION_QUESTION("QuestionQuestion", DataKeyColumn.CODE_TARGET_PARENT),
    QUESTION("Question"),
    ENTITY_ATTRIBUTE("EntityAttribute", DataKeyColumn.CODE_BA),
    DEF_BASE_ENTITY("DEF_BaseEntity"),
    DEF_ENTITY_ATTRIBUTE("DEF_EntityAttribute", DataKeyColumn.CODE_BA);

    private ESheetTitle(String title) {
        this(title, DataKeyColumn.CODE);
    }

    private ESheetTitle(String title, String... dataKeyColumns) {
        this(title, Set.of(dataKeyColumns));
    }

    private ESheetTitle(String title, Set<String> dataKeyColumns) {
        this.title = title;
        this.dataKeyColumns = dataKeyColumns;
    }

    private final String title;
    private Set<String> dataKeyColumns;


    public String getTitle() {
        return title;
    }

    public Set<String> getDataKeyColumns() {
        return dataKeyColumns;
    }

    // Initialise an internal hashmap to allow instant fetching of Enums on title key 
    private static Map<String, ESheetTitle> titleCacheMap = new HashMap<>();

    static {
        for(ESheetTitle title : values()) {
            titleCacheMap.put(title.title, title);
        }
    }

    /**
     * Get a Valid {@link ESheetTitle SheetTitle} from a predefined 
     * set of sheet titles within {@link ESheetTitle}
     * @param title
     * @return
     */
    public static ESheetTitle getByTitle(String title) {
        return titleCacheMap.get(title);
    }

    public static boolean isValidTitle(String title) {
        return titleCacheMap.get(title) != null;
    }
}