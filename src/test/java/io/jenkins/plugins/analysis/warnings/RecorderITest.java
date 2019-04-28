package io.jenkins.plugins.analysis.warnings;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.steps.IssuesRecorder;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.core.util.QualityGate.QualityGateResult;
import io.jenkins.plugins.analysis.core.util.QualityGate.QualityGateType;
import io.jenkins.plugins.analysis.core.util.QualityGateStatus;

import static io.jenkins.plugins.analysis.core.assertions.Assertions.*;

public class RecorderITest extends IntegrationTestWithJenkinsPerSuite {
    @Test
    public void shouldCreateFreestyle() {
        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, "javac.txt", "javac.txt");
        Java java = new Java();
        java.setPattern("**/*.txt");
        IssuesRecorder recorder = enableWarnings(project, java);
        recorder.addQualityGate(1, QualityGateType.TOTAL, QualityGateResult.FAILURE);

        //Run<?, ?> run = buildWithStatus(project, Result.SUCCESS);
        AnalysisResult result = scheduleBuildAndAssertStatus(project, Result.FAILURE);

        //assertThat(run.getNumber()).isEqualTo(1);
        assertThat(result).hasTotalSize(2);
        assertThat(result).hasQualityGateStatus(QualityGateStatus.FAILED);
    }
}
