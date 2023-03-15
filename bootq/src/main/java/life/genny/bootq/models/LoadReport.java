package life.genny.bootq.models;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.vavr.Tuple2;
import life.genny.bootq.models.sheets.EReportCategoryType;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class LoadReport {
    public static final String MSG_LOADED_SUCCESSFULLY = " loaded successfully.";
    public static final String MSG_ERROR_WHILE_SAVING = "Error while saving %s - [%s] : %s";
    public static final String MSG_ERROR_WHILE_BUILDING = "Error while building %s - [%s] : %s";

    public static final String TYPE_INDENTATION = "\t";
    public static final String CATEGORY_INDENTATION = TYPE_INDENTATION + "\t";
    public static final String REPORT_INDENTATION = CATEGORY_INDENTATION + "\t";
    public static final String EXCEPTION_INDENTATION = REPORT_INDENTATION + "\t";


    // CDI has broken here for some reason
    @Inject
    Logger log;


    File outputFile;

    private List<Tuple2<EReportCategoryType, Integer>> successes = new LinkedList<>();

    private Map<ReportType, Map<EReportCategoryType, List<ReportLog>>> loadReportMap = new EnumMap<>(ReportType.class);
    
    public enum ReportType {
        BUILD_ERRORS,
        PERSIST_ERRORS;
    }

    
    public LoadReport() {
        for(ReportType reportType : ReportType.values()) {
            Map<EReportCategoryType, List<ReportLog>> map = new EnumMap<>(EReportCategoryType.class);

            for(EReportCategoryType type : EReportCategoryType.values()) {
                map.put(type, new LinkedList<>());
            }

            loadReportMap.put(reportType, map);
        }
    }

    public void setOutputFile(String path) {

    }

    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Add a build error to the report
     * @param model - {@link EReportCategoryType model} that had the error
     * @param identifier - Identifier include model's product and any relevant primary keys
     * @param error - related exception
     */
    public void addBuildError(EReportCategoryType model, String identifier, Exception error) {
        ReportLog reportLog = new ReportLog(model.getLogLine() + "\t" + identifier, error);
        log.error(ANSIColour.RED + String.format(MSG_ERROR_WHILE_BUILDING, identifier, model.getLogLine(), error.getMessage()) + "! Skipping." + ANSIColour.RESET);
        loadReportMap.get(ReportType.BUILD_ERRORS).get(model).add(reportLog);
    }

    /**
     * Add a persist error to the report
     * @param model - {@link EReportCategoryType model} that had the error
     * @param identifier - Identifier include model's product and any relevant primary keys
     * @param error - related exception
     */
    public void addPersistError(EReportCategoryType model, String identifier, Exception error) {
        ReportLog reportLog = new ReportLog(model.getLogLine() + "\t" + identifier, error);
        log.warnf(MSG_ERROR_WHILE_SAVING, identifier, model.getLogLine(), error.getMessage());
        loadReportMap.get(ReportType.PERSIST_ERRORS).get(model).add(reportLog);
    }

    public boolean hasErrors(ReportType reportType) {
        for(Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry : loadReportMap.get(reportType).entrySet()) {
            if(!reportEntry.getValue().isEmpty())
                return true;
        }

        return false;
    }

    public boolean hasErrors(EReportCategoryType model, ReportType reportType) {
        return !loadReportMap.get(reportType).get(model).isEmpty();
    }

    public boolean hasErrors(EReportCategoryType model) {
        return !hasErrors(model, ReportType.BUILD_ERRORS) || !hasErrors(model, ReportType.PERSIST_ERRORS);
    }

    public void addSuccess(EReportCategoryType type, Integer successCount) {
        // ReportLog reportLog = new ReportLog(type.getLogLine(), Integer.toString(successCount) + type.getLogLine() + " " + MSG_LOADED_SUCCESSFULLY);
        successes.add(new Tuple2<>(type, successCount));
    }
    
    public List<ReportLog> popReports(ReportType reportType, EReportCategoryType model) {
        List<ReportLog> reports = loadReportMap.get(reportType).get(model);
        loadReportMap.get(reportType).get(model).clear();
        return reports;
    }

    public int getReportSize(ReportType reportType, EReportCategoryType model) {
        if(model != null) {
            return loadReportMap.get(reportType).get(model).size();
        }

        int total = 0;
        for(Map.Entry<EReportCategoryType, List<ReportLog>> entry : loadReportMap.get(reportType).entrySet()) {
            total += entry.getValue().size();
        }

        return total;
    }

    public void clear() {
        for(ReportType reportType : ReportType.values()) {
            for(EReportCategoryType type : EReportCategoryType.values()) {
                loadReportMap.get(reportType).get(type).clear();
            }
        }
    }

    private void infoAndDump(Object msg) {

        log.info(msg);
    }

    public void printCategory(ReportType reportType, Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry, boolean showStackTraces) {
        EReportCategoryType category = reportEntry.getKey();

        List<ReportLog> logs = reportEntry.getValue();
        log.info(ANSIColour.GREEN + CATEGORY_INDENTATION + "-" + category.getLogLine() + ANSIColour.RESET);
        // Print errors if exists
        if(hasErrors(category, reportType)) {
            CommonUtils.printCollection(logs, this::infoAndDump, reportLog -> ANSIColour.RED + reportLog.getReportLine(showStackTraces) + ANSIColour.RESET);
        } else {
            log.info(REPORT_INDENTATION + ANSIColour.doColour("No " + CommonUtils.normalizeString(reportType.name()) + " Errors for type" + category.getLogLine() + "!", ANSIColour.GREEN));
        }
    }

    public void printLoadReport(boolean showStackTraces) {
        log.info(ANSIColour.YELLOW + "/************ Load Summary Start ************/" + ANSIColour.RESET);

        // Print all build errors (if exists)
        if(hasErrors(ReportType.BUILD_ERRORS)) {
            log.info(ANSIColour.RED + TYPE_INDENTATION + "- Build Errors" + ANSIColour.RESET);
            Map<EReportCategoryType, List<ReportLog>> buildErrorMap = loadReportMap.get(ReportType.BUILD_ERRORS);
            for(Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry : buildErrorMap.entrySet()) {
                printCategory(ReportType.BUILD_ERRORS, reportEntry, showStackTraces);
            }
        } else {
            log.info(ANSIColour.GREEN + TYPE_INDENTATION + "- NO Build Errors :)" + ANSIColour.RESET);
        }

        // Print all persist errors (if exists)
        if(hasErrors(ReportType.PERSIST_ERRORS)) {
            log.info(ANSIColour.CYAN + TYPE_INDENTATION + "- Persist Errors" + ANSIColour.RESET);
            Map<EReportCategoryType, List<ReportLog>> buildErrorMap = loadReportMap.get(ReportType.BUILD_ERRORS);
            for(Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry : buildErrorMap.entrySet()) {
                printCategory(ReportType.PERSIST_ERRORS, reportEntry, showStackTraces);
            }
        } else {
            log.info(ANSIColour.GREEN + TYPE_INDENTATION + "- NO Persist Errors :)" + ANSIColour.RESET);
        }

        log.info(ANSIColour.doColour("/************ Load Summary END ************/", ANSIColour.YELLOW));
    }
    
    public class ReportLog {

        public final String identifier;
        public Exception exception;
        public final String details;

        public ReportLog(String identifier, String details) {
            this(identifier, null, details);
        }

        public ReportLog(String identifier, Exception exception) {
            this(identifier, exception, exception.getMessage());
        }

        public ReportLog(String identifier, Exception exception, String details) {
            this.identifier = identifier;
            this.exception = exception;
            this.details = details;
        }

        public String getReportLine(boolean showStackTraces) {
            StringBuilder sb = new StringBuilder(REPORT_INDENTATION)
                .append("- ")
                .append(identifier)
                .append(details);
            
            if(showStackTraces && exception != null) {
                sb.append("\n")
                    .append(EXCEPTION_INDENTATION);

                for(StackTraceElement element : exception.getStackTrace()) {
                    sb.append("\n").append(EXCEPTION_INDENTATION).append(element.toString());
                }
            } else if(showStackTraces) {
                sb.append("\n")
                .append(EXCEPTION_INDENTATION)
                .append("- No Exception Provided");
            }

            return sb.toString();
        }
    }

}
