package io.jenkins.plugins.analysis.warnings;

import java.io.IOException;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlNumberInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import edu.hm.hafner.analysis.Severity;

import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Result;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.steps.IssuesRecorder;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.core.util.HealthDescriptor;

import static org.assertj.core.api.Assertions.*;

public class RecorderITest extends IntegrationTestWithJenkinsPerSuite {

    private static final HealthDescriptor HEALTH_DESCRIPTOR = new HealthDescriptor(1, 9, Severity.WARNING_LOW);

    @Test
    public void shouldCreateFreestyle() {
        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, "javac.txt", "javac.txt");

        Java java = new Java();
        java.setPattern("javac.txt");

        IssuesRecorder recorder = enableWarnings(project, java);
        recorder.setHealthy(1);
        recorder.setUnhealthy(9);
        recorder.setMinimumSeverity("LOW");

        AnalysisResult result = scheduleBuildAndAssertStatus(project, Result.SUCCESS);

        HealthReport healthReport = project.getBuildHealth();
        assertThat(healthReport.getScore()).isEqualTo(80);

        System.out.println(healthReport.getScore());

    }

    @Test
    public void healthReportOneHealthyNineUnhealthyWithNoWarings() throws IOException {
        verifyHealthReportOfJavaFreeStyleJob("javac_no_warning.txt", 0, 100);
        verifyHealthReportOfJavaFreeStyleJobConfiguredByGui("javac_no_warning.txt", 0, 100);
    }

    @Test
    public void healthReportOneHealthyNineUnhealthyWithOneWarings() throws IOException {
        verifyHealthReportOfJavaFreeStyleJob("javac_one_warning.txt", 1, 90);
        verifyHealthReportOfJavaFreeStyleJobConfiguredByGui("javac_one_warning.txt", 1, 90);
    }

    @Test
    public void healthReportOneHealthyNineUnhealthyWithNineWarings() throws IOException {
        verifyHealthReportOfJavaFreeStyleJob("javac_9_warnings.txt", 9, 10);
        verifyHealthReportOfJavaFreeStyleJobConfiguredByGui("javac_9_warnings.txt", 9, 10);
    }

    @Test
    public void healthReportOneHealthyNineUnhealthyWithTenWarings() throws IOException {
        verifyHealthReportOfJavaFreeStyleJob("javac_10_warnings.txt", 10, 0);
        verifyHealthReportOfJavaFreeStyleJobConfiguredByGui("javac_10_warnings.txt", 10, 0);
    }

    private void verifyHealthReportOfJavaFreeStyleJob(final String javacFile, final int warnings, final int healthReportScore) {
        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, javacFile, "javac.txt");

        Java java = new Java();
        java.setPattern("javac.txt");

        IssuesRecorder recorder = enableWarnings(project, java);
        recorder.setHealthy(1);
        recorder.setUnhealthy(9);
        recorder.setMinimumSeverity("LOW");

        AnalysisResult result = scheduleBuildAndAssertStatus(project, Result.SUCCESS);
        HealthReport healthReport = project.getBuildHealth();

        assertThat(result.getTotalSize()).isEqualTo(warnings);
        assertThat(healthReport.getScore()).isEqualTo(healthReportScore);
    }

    private void verifyHealthReportOfJavaFreeStyleJobConfiguredByGui(final String javacFile, final int warnings, final int healthReportScore)
            throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, javacFile, "javac.txt");

        Java java = new Java();
        java.setPattern("javac.txt");

        IssuesRecorder recorder = enableWarnings(project, java);

        HtmlPage configPage = getWebPage(project, "configure");

        HtmlForm form = configPage.getFormByName("config");

        /*
        HtmlCheckBoxInput blameDisabledCheckBox = form.getInputByName("_.blameDisabled");
        System.out.println(blameDisabledCheckBox.isChecked());
        blameDisabledCheckBox.setChecked(false);
        */

        HtmlNumberInput healthyInput = form.getInputByName("_.healthy");
        healthyInput.setText(String.valueOf(1));

        HtmlNumberInput unhealthyInput = form.getInputByName("_.unhealthy");
        unhealthyInput.setText(String.valueOf(9));

        HtmlFormUtil.submit(form);

        AnalysisResult result = scheduleBuildAndAssertStatus(project, Result.SUCCESS);
        HealthReport healthReport = project.getBuildHealth();

        assertThat(result.getTotalSize()).isEqualTo(warnings);
        assertThat(healthReport.getScore()).isEqualTo(healthReportScore);
    }
}
