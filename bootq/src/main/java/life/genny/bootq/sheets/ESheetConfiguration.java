package life.genny.bootq.sheets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import life.genny.bootq.models.sheets.header.ESheetHeader;

/**
 * An enumerator containing essential static data for parsing data from the
 * sheets
 */
public enum ESheetConfiguration {
    // If no DataKeyColumn is set here, this defaults to DataKeyColumn.CODE
    VALIDATION("Validation"),
    DATATYPE("DataType"),
    ATTRIBUTE("Attribute"),
    BASE_ENTITY("BaseEntity"),
    QUESTION_QUESTION("QuestionQuestion", ESheetHeader.CODE_TARGET_PARENT),
    QUESTION("Question"),
    ENTITY_ATTRIBUTE("EntityAttribute", ESheetHeader.CODE_BA),
    DEF_BASE_ENTITY("DEF_BaseEntity"),
    DEF_ENTITY_ATTRIBUTE("DEF_EntityAttribute", ESheetHeader.CODE_BA);

    private ESheetConfiguration(String title) {
        this(title, ESheetHeader.CODE);
    }

    private ESheetConfiguration(String title, ESheetHeader headerRow) {
        this.title = title;
        this.headerRow = headerRow;
    }

    private final String title;
    private ESheetHeader headerRow;

    public String getTitle() {
        return title;
    }

    /**
     * Get the header (1st row defining each field) in a sheet within a doc
     * @return
     */
    public Set<String> getHeaderRow() {
        return headerRow.getColumns();
    }

    // Initialise an internal hashmap to allow instant fetching of Enums on title key 
    private static Map<String, ESheetConfiguration> titleCacheMap = new HashMap<>();

    static {
        for(ESheetConfiguration title : values()) {
            titleCacheMap.put(title.title, title);
        }
    }

    /**
     * Get a Valid {@link ESheetConfiguration SheetTitle} from a predefined 
     * set of sheet titles within {@link ESheetConfiguration}
     * @param title
     * @return
     */
    public static ESheetConfiguration getByTitle(String title) {
        return titleCacheMap.get(title);
    }

    public static boolean isValidTitle(String title) {
        return titleCacheMap.get(title) != null;
    }
}