package life.genny.bootq.models.reporting;

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
        StringBuilder sb = new StringBuilder(LoadReport.REPORT_INDENTATION)
            .append("- ")
            .append(identifier)
            .append(details);
        
        if(showStackTraces && exception != null) {
            sb.append("\n")
                .append(LoadReport.EXCEPTION_INDENTATION);

            for(StackTraceElement element : exception.getStackTrace()) {
                sb.append("\n").append(LoadReport.EXCEPTION_INDENTATION).append(element.toString());
            }
        } else if(showStackTraces) {
            sb.append("\n")
            .append(LoadReport.EXCEPTION_INDENTATION)
            .append("- No Exception Provided");
        }

        return sb.toString();
    }
}