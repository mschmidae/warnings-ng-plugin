package io.jenkins.plugins.analysis.warnings.recorder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import org.jenkinsci.test.acceptance.docker.DockerClassRule;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;

import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Maven;
import hudson.tasks.Shell;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.core.util.GccContainer;
import io.jenkins.plugins.analysis.warnings.Gcc4;
import io.jenkins.plugins.analysis.warnings.MavenConsole;

import static org.assertj.core.api.Assertions.*;

public class BuildOnWorkerITest extends IntegrationTestWithJenkinsPerSuite {
    @ClassRule
    public static final DockerClassRule<JavaContainer> JAVA_DOCKER = new DockerClassRule<>(JavaContainer.class);

    @ClassRule
    public static final DockerClassRule<GccContainer> GCC_DOCKER = new DockerClassRule<>(GccContainer.class);

    @Test
    public void buildMavenOnDumpSlave() {
        DumbSlave worker = setupDumpSlave();
        buildMavenProjectOnWorker(worker);
    }

    @Test
    public void buildMavenOnDocker() {
        DumbSlave worker = setupDockerContainer(JAVA_DOCKER);
        buildMavenProjectOnWorker(worker);
    }

    @Test
    public void buildMakeOnDumpSlave() {
        DumbSlave worker = setupDumpSlave();
        buildMakeProjectOnWorker(worker);
    }

    @Test
    public void buildMakeOnDocker() {
        DumbSlave worker = setupDockerContainer(GCC_DOCKER);
        buildMakeProjectOnWorker(worker);
    }

    private void buildMavenProjectOnWorker(final Slave worker) {
        FreeStyleProject project = createFreeStyleProject();
        try {
            project.setAssignedNode(worker);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        buildSuccessfully(project);


        project.getBuildersList().add(new Maven("verify", null));
        copySingleFileToAgentWorkspace(worker, project, "affected-files/Main.java", "src/main/java/hm/edu/hafner/analysis/Main.java");
        copySingleFileToAgentWorkspace(worker, project, "pom.xml", "pom.xml");
        enableWarnings(project, createTool(new MavenConsole(), ""));

        buildSuccessfully(project);

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());

        assertThat(project.getLastBuild().getBuiltOn().getLabelString()).isEqualTo(worker.getLabelString());
    }

    private void buildMakeProjectOnWorker(final Slave worker) {
        FreeStyleProject project = createFreeStyleProject();
        try {
            project.setAssignedNode(worker);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        buildSuccessfully(project);


        project.getBuildersList().add(new Shell("make"));
        copySingleFileToAgentWorkspace(worker, project, "make-gcc/Makefile", "Makefile");
        copySingleFileToAgentWorkspace(worker, project, "make-gcc/main.c", "main.c");
        enableWarnings(project, createTool(new Gcc4(), ""));

        buildSuccessfully(project);

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        //assertThat(result.get(0).getTotalSize()).isEqualTo(2);
        assertThat(project.getLastBuild().getBuiltOn().getLabelString()).isEqualTo(worker.getLabelString());
    }

    private DumbSlave setupDumpSlave() {
        try {
            return JENKINS_PER_SUITE.createSlave();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private <T extends SshdContainer> DumbSlave setupDockerContainer(final DockerClassRule<T> docker) {
        DumbSlave worker;
        try {
            T container = docker.create();
            worker = new DumbSlave("docker", "/home/test", new SSHLauncher(container.ipBound(22), container.port(22), "test", "test", "", "-Dfile.encoding=ISO-8859-1"));
            worker.setNodeProperties(Arrays.asList(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64/jre"))));
            getJenkins().jenkins.addNode(worker);
            getJenkins().waitOnline(worker);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return worker;
    }
}