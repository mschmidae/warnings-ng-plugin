package io.jenkins.plugins.analysis.warnings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlNumberInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Project;
import hudson.model.Result;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class RecorderITest extends IntegrationTestWithJenkinsPerSuite {

    private static final int HEALTHY_THRESHOLD = 1;
    private static final int UNHEALTHY_THRESHOLD = 9;

    @Test
    public void noWarningsWithHealthReport100() throws IOException {
        verifyInfoPageWithHealthReport("javac_no_warning.txt", 0, 100);
    }

    @Test
    public void noWarningsWithoutHealthReport() throws IOException {
        verifyInfoPageWithoutHealthReport("javac_no_warning.txt", 0);
    }

    @Test
    public void oneWarningWithHealthReport90() throws IOException {
        verifyInfoPageWithHealthReport("javac_one_warning.txt", 1, 90);
    }

    @Test
    public void oneWarningWithoutHealthReport() throws IOException {
        verifyInfoPageWithoutHealthReport("javac_one_warning.txt", 1);
    }

    @Test
    public void nineWarningsWithHealthReport10() throws IOException {
        verifyInfoPageWithHealthReport("javac_9_warnings.txt", 9, 10);
    }

    @Test
    public void nineWarningsWithoutHealthReport() throws IOException {
        verifyInfoPageWithoutHealthReport("javac_9_warnings.txt", 9);
    }

    @Test
    public void tenWarningsWithHealthReport0() throws IOException {
        verifyInfoPageWithHealthReport("javac_10_warnings.txt", 10, 0);
    }

    @Test
    public void tenWarningsWithoutHealthReport() throws IOException {
        verifyInfoPageWithoutHealthReport("javac_10_warnings.txt", 10);
    }

    private void verifyInfoPageWithoutHealthReport(final String javacFile, final int warnings)
            throws IOException {
        verifyInfoPage(javacFile, warnings, 0, false);
    }

    private void verifyInfoPageWithHealthReport(final String javacFile, final int warnings,
            final int healthReportScore)
            throws IOException {
        verifyInfoPage(javacFile, warnings, healthReportScore, true);
    }

    private void verifyInfoPage(final String javacFile, final int warnings,
            final int healthReportScore, final boolean withHealthReport)
            throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, javacFile, "javac.txt");

        Java java = new Java();
        java.setPattern("javac.txt");
        enableWarnings(project, java);

        if (withHealthReport) {
            WarningsRecorderConfigurationPage config = new WarningsRecorderConfigurationPage(project);
            config.setHealthyThreshold(HEALTHY_THRESHOLD);
            config.setUnhealthyThreshold(UNHEALTHY_THRESHOLD);
            config.submit();
        }

        AnalysisResult result = scheduleBuildAndAssertStatus(project, Result.SUCCESS);
        JavaInfoPage infoPage = new JavaInfoPage(project, 1);

        assertThat(result.getTotalSize()).isEqualTo(warnings);
        assertThat(infoPage.getInformationMessages()).isEqualTo(result.getInfoMessages());
        assertThat(infoPage.getErrorMessages()).isEqualTo(result.getErrorMessages());

        if (withHealthReport) {
            assertThat(infoPage.getInformationMessages()).contains(
                    "Enabling health report (Healthy=1, Unhealthy=9, Minimum Severity=LOW)");
            HealthReport healthReport = project.getBuildHealth();
            assertThat(healthReport.getScore()).isEqualTo(healthReportScore);
        }
    }

    private class JavaInfoPage {
        private final HtmlPage infoPage;

        JavaInfoPage(final Project project, final int buildNumber) {
            infoPage = getWebPage(project, buildNumber + "/java/info");
        }

        public List<String> getErrorMessages() {
            return getMessagesById("errors");
        }

        public List<String> getInformationMessages() {
            return getMessagesById("info");
        }

        private List<String> getMessagesById(final String id) {
            DomElement info = getInfoPage().getElementById(id);

            return info == null ? new ArrayList<>()
                    : StreamSupport.stream(info.getChildElements().spliterator(), false)
                    .map(DomElement::asText)
                    .collect(Collectors.toList());
        }

        public HtmlPage getInfoPage() {
            return infoPage;
        }
    }

    private class WarningsRecorderConfigurationPage {
        private static final String ID_HEALTHY_THRESHOLD = "_.healthy";
        private static final String ID_UNHEALTHY_THRESHOLD = "_.unhealthy";
        private final HtmlForm form;

        WarningsRecorderConfigurationPage(final Project project) {
            form = getWebPage(project, "configure").getFormByName("config");
        }

        public int getHealthyThreshold() {
            return getNumber(ID_HEALTHY_THRESHOLD);
        }

        public void setHealthyThreshold(final int healthy) {
            setInput(ID_HEALTHY_THRESHOLD, healthy);
        }

        public int getUnhealthyThreshold() {
            return getNumber(ID_UNHEALTHY_THRESHOLD);
        }

        public void setUnhealthyThreshold(final int unhealthy) {
            setInput(ID_UNHEALTHY_THRESHOLD, unhealthy);
        }

        public void submit() throws IOException {
            HtmlFormUtil.submit(getForm());
        }

        private void setInput(final String id, final int value) {
            HtmlNumberInput input = getForm().getInputByName(id);
            input.setText(String.valueOf(value));
        }

        private int getNumber(final String id) {
            HtmlNumberInput input = getForm().getInputByName(id);
            return Integer.parseInt(input.getText());
        }

        private HtmlForm getForm() {
            return form;
        }
    }
}
