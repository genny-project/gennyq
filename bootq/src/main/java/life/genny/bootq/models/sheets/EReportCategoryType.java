package life.genny.bootq.models.sheets;

public enum EReportCategoryType {
    
    VALIDATION(" Validations "),
    DATA_TYPE(" DataTypes "),
    ATTRIBUTE(" Attributes "),
    BASE_ENTITY(" BaseEntities "),
    DEF_BASEENTITY_ATTRIBUTE(" DEF_BaseEntityAttributes "),
    BASEENTITY_ATTRIBUTE(" BaseEntityAttributes "),
    LINKING_ENTITIES(" Linking Entities "),
    QUESTION(" Questions "),
    QUESTION_QUESTION(" QuestionQuestions ");

    private final String logLine;

    private EReportCategoryType(String logLine) {
        this.logLine = logLine;
    }

    public String getLogLine() {
        return logLine;
    }
}
