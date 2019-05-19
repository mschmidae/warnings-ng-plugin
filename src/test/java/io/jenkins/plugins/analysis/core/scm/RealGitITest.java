package io.jenkins.plugins.analysis.core.scm;

import org.junit.Rule;
import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitSCMBuilder;
import jenkins.plugins.git.GitSCMBuilderTest;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.scm.DefaultSCMCheckoutStrategyImpl;
import jenkins.scm.SCMCheckoutStrategy;
import jenkins.scm.SCMCheckoutStrategyDescriptor;
import jenkins.scm.api.SCMHead;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.steps.IssuesRecorder;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.warnings.Doxygen;
import io.jenkins.plugins.analysis.warnings.Java;

public class RealGitITest extends IntegrationTestWithJenkinsPerSuite {

    @Rule
    public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

    @Test
    public void firstTest() throws Exception {
        sampleRepo.init();
        sampleRepo.write("init.txt", "init");
        sampleRepo.git("add", "init.txt");
        sampleRepo.git("commit", "--all", "--message=init");

        GitSCMBuilder builder = new GitSCMBuilder(new SCMHead("master"), null, sampleRepo.fileUrl(), null);
        RelativeTargetDirectory extension = new RelativeTargetDirectory("src"); // JENKINS-57260
        builder.withExtension(extension);
        GitSCM git = builder.build(); //new GitSCM(sampleRepo.fileUrl());

        FreeStyleProject project = createFreeStyleProject();
        project.setScm(git);

        IssuesRecorder recorder = enableGenericWarnings(project, new Java());
        Run<?, ?> run = buildWithResult(project, Result.SUCCESS);

        System.out.println("End");
    }

    @Test
    public void gitBlameDifferentDirectory() throws Exception {
        sampleRepo.init();
        sampleRepo.write("sub/init.txt", "init");
        sampleRepo.git("add", "sub/init.txt");
        sampleRepo.git("commit", "--all", "--message=init");

        GitSCMBuilder builder = new GitSCMBuilder(new SCMHead("master"), null, sampleRepo.fileUrl(), null);
        RelativeTargetDirectory extension = new RelativeTargetDirectory("src"); // JENKINS-57260
        builder.withExtension(extension);
        GitSCM git = builder.build();

        FreeStyleProject project = createFreeStyleProject();
        copySingleFileToWorkspace(project, "doxygen.log", "build/doxygen/doxygen/doxygen.log");
        project.setScm(git);


        IssuesRecorder recorder = enableWarnings(project, createTool(new Doxygen(), "build/doxygen/doxygen/doxygen.log"));
        recorder.setAggregatingResults(true);
        recorder.setEnabledForFailure(true);

        AnalysisResult result = scheduleSuccessfulBuild(project);
        result.getBlames().getRequests().forEach(r -> r.getCommit(1));

        System.out.println("End");
    }
}
