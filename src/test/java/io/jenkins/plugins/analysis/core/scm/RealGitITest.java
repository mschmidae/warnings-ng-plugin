package io.jenkins.plugins.analysis.core.scm;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CreateFileBuilder;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
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

import static org.assertj.core.api.Assertions.*;

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
        //CentralDifferenceSolver.cpp
        //LCPcalc.cpp

        sampleRepo.init();
        sampleRepo.write("CentralDifferenceSolver.cpp", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12");
        sampleRepo.write("LCPcalc.cpp", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13");
        sampleRepo.git("add", "CentralDifferenceSolver.cpp", "LCPcalc.cpp");
        sampleRepo.git("commit", "--all", "--message=init");

        GitSCMBuilder builder = new GitSCMBuilder(new SCMHead("master"), null, sampleRepo.fileUrl(), null);
        RelativeTargetDirectory extension = new RelativeTargetDirectory("src"); // JENKINS-57260
        builder.withExtension(extension);
        GitSCM git = builder.build();

        FreeStyleProject project = createFreeStyleProject();

        copySingleFileToWorkspace(project, "doxygen.log", "build/doxygen/doxygen/doxygen.log");
        project.setScm(git);
        project.getBuildersList().add(new CreateFileBuilder("build/doxygen/doxygen/doxygen.log",
                "Notice: Output directory `build/doxygen/doxygen' does not exist. I have created it for you.\n"
                        + "src/CentralDifferenceSolver.cpp:11: Warning: reached end of file while inside a dot block!\n"
                        + "The command that should end the block seems to be missing!\n"
                        + " \n"
                        + "src/LCPcalc.cpp:12: Warning: the name `lcp_lexicolemke.c' supplied as the second argument in the \\file statement is not an input file"));


        IssuesRecorder recorder = enableWarnings(project, createTool(new Doxygen(), "build/doxygen/doxygen/doxygen.log"));
        recorder.setAggregatingResults(true);
        recorder.setEnabledForFailure(true);


        scheduleSuccessfulBuild(project);
        AnalysisResult result = scheduleSuccessfulBuild(project);
        assertThat(result.getErrorMessages()).doesNotContain("Can't determine head commit using 'git rev-parse'. Skipping blame.");
    }

    @Test
    public void gitBlameDifferentDirectoryPipeline() throws Exception {

        sampleRepo.init();
        sampleRepo.write("CentralDifferenceSolver.cpp", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12");
        sampleRepo.write("LCPcalc.cpp", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13");
        sampleRepo.git("add", "CentralDifferenceSolver.cpp", "LCPcalc.cpp");
        sampleRepo.git("commit", "--all", "--message=init");

        WorkflowJob project = createPipeline();

        project.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "agent any\n"
                + "options{\n"
                + "skipDefaultCheckout()\n"
                + "}\n"
                + "stages{\n"
                + "stage('Prepare') {\n"
                + "  steps {\n"
                + "    dir('src') {\n"
                + "      checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: '" + sampleRepo.fileUrl() + "']]])\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "stage('Doxygen') {\n"
                + "  steps {\n"
                + "    dir('build/doxygen') {\n"
                + "      writeFile file: 'doxygen/doxygen.log', text: '\"Notice: Output directory doc/doxygen/framework does not exist. I have created it for you.\\nsrc/CentralDifferenceSolver.cpp:11: Warning: reached end of file while inside a dot block!\\nThe command that should end the block seems to be missing!\\nsrc/LCPcalc.cpp:12: Warning: the name lcp_lexicolemke.c supplied as the second argument in the file statement is not an input file\"'\n"
                + "    }\n"
                + "    recordIssues(aggregatingResults: true, enabledForFailure: true, tools: [ doxygen(name: 'Doxygen', pattern: 'build/doxygen/doxygen/doxygen.log') ] )\n"
                + "  }\n"
                + "}\n"
                + "}}", false));


        scheduleSuccessfulBuild(project);
        AnalysisResult result = scheduleSuccessfulBuild(project);
        assertThat(result.getErrorMessages()).doesNotContain("Can't determine head commit using 'git rev-parse'. Skipping blame.");
    }
}
