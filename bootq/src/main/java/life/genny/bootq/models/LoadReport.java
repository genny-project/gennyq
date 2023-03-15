package life.genny.bootq.models;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.jsonwebtoken.io.IOException;
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


    List<Object> linesToOutput = new ArrayList<>();
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

        String path = CommonUtils.getSystemEnv("GENNY_BOOTQ_OUTPUT_FILE_PATH", "output/");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-LLLL-yyyy");
        LocalDate localDate = LocalDate.now();
        String fmtedString = localDate.format(formatter);
        String output = CommonUtils.getSystemEnv("GENNY_BOOTQ_OUTPUT_FILE", "output-" + fmtedString + ".log");

        setOutputFile(path + "/" + output);
    }

    public void setOutputFile(String path) {
        outputFile = new File(path);
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

    public void printCategory(ReportType reportType, Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry, boolean showStackTraces) {
        EReportCategoryType category = reportEntry.getKey();

        List<ReportLog> logs = reportEntry.getValue();
        logAndDump(ANSIColour.GREEN + CATEGORY_INDENTATION + "-" + category.getLogLine() + ANSIColour.RESET);
        // Print errors if exists
        if(hasErrors(category, reportType)) {
            CommonUtils.printCollection(logs, this::logAndDump, reportLog -> ANSIColour.RED + reportLog.getReportLine(showStackTraces) + ANSIColour.RESET);
        } else {
            logAndDump(REPORT_INDENTATION + ANSIColour.doColour("No " + CommonUtils.normalizeString(reportType.name()) + " Errors for type" + category.getLogLine() + "!", ANSIColour.GREEN));
        }
    }

    private void logAndDump(Object msg) {
        linesToOutput.add(ANSIColour.strip(msg + "\n"));
        log.info(msg);
    }

    public void printLoadReport(boolean showStackTraces) throws java.io.IOException {
        logAndDump(ANSIColour.doColour("/************ Load Summary Start ************/", ANSIColour.YELLOW));

        // Print all build errors (if exists)
        if(hasErrors(ReportType.BUILD_ERRORS)) {
            logAndDump(ANSIColour.doColour(TYPE_INDENTATION + "- Build Errors", ANSIColour.RED));
            Map<EReportCategoryType, List<ReportLog>> buildErrorMap = loadReportMap.get(ReportType.BUILD_ERRORS);
            for(Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry : buildErrorMap.entrySet()) {
                printCategory(ReportType.BUILD_ERRORS, reportEntry, showStackTraces);
            }
        } else {
            logAndDump(ANSIColour.doColour(TYPE_INDENTATION + "- NO Build Errors :)", ANSIColour.GREEN));
        }

        // Print all persist errors (if exists)
        if(hasErrors(ReportType.PERSIST_ERRORS)) {
            logAndDump(ANSIColour.doColour(TYPE_INDENTATION + "- Persist Errors", ANSIColour.CYAN));
            Map<EReportCategoryType, List<ReportLog>> buildErrorMap = loadReportMap.get(ReportType.BUILD_ERRORS);
            for(Map.Entry<EReportCategoryType, List<ReportLog>> reportEntry : buildErrorMap.entrySet()) {
                printCategory(ReportType.PERSIST_ERRORS, reportEntry, showStackTraces);
            }
        } else {
            logAndDump(ANSIColour.doColour(TYPE_INDENTATION + "- NO Persist Errors :)", ANSIColour.GREEN));
        }

        logAndDump(ANSIColour.doColour("/************ Load Summary END ************/", ANSIColour.YELLOW));


        FileWriter fw = null;
        try {
            fw = new FileWriter(outputFile);
        } catch (java.io.IOException e) {
            log.error("Error opening file: " + outputFile);
            e.printStackTrace();
        }
        try(BufferedWriter writer = new BufferedWriter(fw)) {
            for(Object msg : linesToOutput) {
                writer.append(msg.toString());
            }
        }
        if(fw != null)
            fw.close();
    }
    
    public class ReportLog {

        private final String identifier;
        private Exception exception;
        private final String details;

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
