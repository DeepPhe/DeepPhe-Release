package org.healthnlp.deepphe.neo4j.node;

public class Report {
    String reportType, reportId, reportDate, reportEpisode;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getReportEpisode() {
        return reportEpisode;
    }

    public void setReportEpisode(String reportEpisode) {
        this.reportEpisode = reportEpisode;
    }
}
