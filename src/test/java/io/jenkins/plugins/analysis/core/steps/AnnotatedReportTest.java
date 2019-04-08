package io.jenkins.plugins.analysis.core.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Report;

import io.jenkins.plugins.analysis.core.scm.Blames;

import static io.jenkins.plugins.analysis.core.assertions.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link AnnotatedReport}.
 *
 * @author Ullrich Hafner
 */
class AnnotatedReportTest {
    private static final String ID = "id";

    @Test
    void shouldCreateEmptyReport() {
        AnnotatedReport report = new AnnotatedReport(ID);

        assertThat(report.getId()).isEqualTo(ID);
        assertThat(report.size()).isZero();
    }

    // constructor one report
    @Test
    void constructAnnotatedReportWithOneReport() {
        Report report = new Report();
        report.add(new IssueBuilder().build());
        AnnotatedReport sut = new AnnotatedReport(ID, report);

        assertThat(sut.getId()).isEqualTo(ID);
        assertThat(sut.size()).isEqualTo(1);
        assertThat(sut.getReport()).isEqualTo(report);
        assertThat(sut.getSizeOfOrigin()).containsExactly(entry(ID, 1));
    }


    // constructor list of reports

    @Test
    void constructAnnotatedReportFromEmptyListOfAnnotatedReports() {
        List<AnnotatedReport> reports = new ArrayList<>();
        AnnotatedReport sut = new AnnotatedReport(ID, reports);

        assertThat(sut.size()).isZero();
        assertThat(sut.getSizeOfOrigin()).isEmpty();
    }

    @Test
    void addTwoReportsToAnEmptyAnnotatedReport() {

        Report report1 = new Report();
        report1.add(new IssueBuilder().setMessage("issue-1").build());
        Report report2 = new Report();
        report2.add(new IssueBuilder().setMessage("issue-2").build());
        report2.add(new IssueBuilder().setMessage("issue-3").build());

        AnnotatedReport sut = new AnnotatedReport(ID);
        sut.add(new AnnotatedReport("report-1", report1));
        sut.add(new AnnotatedReport("report-2", report2));
        //sut.add(new AnnotatedReport(null, report2), "2");


        assertThat(sut.size()).isEqualTo(3);
        //assertThat(sut.getReport()).isEqualTo(report);
        assertThat(sut.getSizeOfOrigin()).containsExactly(entry("report-1", 1), entry("report-2", 2));
    }

    // add one

    // add list

    //blames
}