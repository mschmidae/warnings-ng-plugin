package io.jenkins.plugins.analysis.warnings.recorder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.ClassRule;
import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.test.acceptance.docker.DockerClassRule;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import hudson.model.Descriptor.FormException;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Maven;
import hudson.tasks.Shell;

import jenkins.security.s2m.AdminWhitelistRule;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.warnings.Java;
import io.jenkins.plugins.analysis.warnings.MavenConsole;
import io.jenkins.plugins.analysis.warnings.checkstyle.CheckStyle;

import static org.assertj.core.api.Assertions.*;

public class DistributedExecutionITest extends IntegrationTestWithJenkinsPerSuite {

    @ClassRule
    public static DockerClassRule<JavaContainer> DOCKER = new DockerClassRule<>(JavaContainer.class);


    @Test
    public void buildMavenOnDumpSlave() throws Exception {
        DumbSlave worker = setupDumpSlave();
        buildMavenProjectOnWorker(worker);
    }

    @Test
    public void buildMavenOnDocker() throws ExecutionException, InterruptedException {
        DumbSlave worker = setupDockerContainer();
        buildMavenProjectOnWorker(worker);
    }

    @Test
    public void buildMakeOnDumpSlave() {
        DumbSlave worker = setupDumpSlave();
        buildMakeProjectOnWorker(worker);
    }

    @Test
    public void buildMakeOnDocker() {
        DumbSlave worker = setupDockerContainer();
        buildMakeProjectOnWorker(worker);
    }

    private DumbSlave setupDumpSlave() {
        try {
            return JENKINS_PER_SUITE.createSlave();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private DumbSlave setupDockerContainer() {
        DumbSlave worker = null;
        try {
            JavaContainer container = DOCKER.create();
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


    private void executeOnWorker(final Slave worker) throws ExecutionException, InterruptedException, IOException {
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(worker);

        System.out.println(getJenkins().jenkins.getSecurity());
        System.out.println(getJenkins().jenkins.getSecurityRealm());
        //ToDo Security
        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        copySingleFileToAgentWorkspace(worker, project, "eclipse_4_Warnings.txt", "eclipse_4_Warnings-issues.txt");
        enableEclipseWarnings(project);



        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        System.out.println(result.size());
    }

    private void buildMavenProjectOnWorker(final Slave worker) throws ExecutionException, InterruptedException {
        FreeStyleProject project = createFreeStyleProject();
        try {
            project.setAssignedNode(worker);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        System.out.println(getJenkins().jenkins.getSecurity());
        System.out.println(getJenkins().jenkins.getSecurityRealm());
        System.out.println(getJenkins().jenkins.isUseSecurity());
        getJenkins().jenkins.disableSecurity();
        System.out.println(getJenkins().jenkins.isUseSecurity());
        //ToDo Security

        Set<String> agentProtocols = new HashSet<>();
        agentProtocols.add("JNLP4-connect");
        agentProtocols.add("Ping");
        getJenkins().jenkins.setAgentProtocols(agentProtocols);

        getJenkins().jenkins.getInjector().getInstance(AdminWhitelistRule.class)
                .setMasterKillSwitch(true);

        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        project.getBuildersList().add(new Maven("verify -Dmaven.compiler.showWarnings=true", null));
        copySingleFileToAgentWorkspace(worker, project, "real-sourcecode/ClassWithWarnings.java", "src/main/java/ClassWithWarnings.java");
        //copySingleFileToAgentWorkspace(worker, project, "eclipse_4_Warnings.txt", "eclipse_4_Warnings-issues.txt");
        copySingleFileToAgentWorkspace(worker, project, "pom.xml", "pom.xml");
        //enableEclipseWarnings(project);
        enableWarnings(project, createTool(new CheckStyle(),  ""));
        enableWarnings(project, createTool(new Java(),  ""));
        enableWarnings(project, createTool(new MavenConsole(), ""));


        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        System.out.println(result.size());
    }

    private void buildMakeProjectOnWorker(final Slave worker) {
        FreeStyleProject project = createFreeStyleProject();
        try {
            project.setAssignedNode(worker);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        scheduleBuildAndAssertStatus(project, Result.SUCCESS);


        //project.getBuildersList().add(new Shell("make"));
        project.getBuildersList().add(new Maven("verify -Dmaven.compiler.showWarnings=true", null));

        copySingleFileToAgentWorkspace(worker, project, "real-sourcecode/ClassWithWarnings.java", "src/main/java/ClassWithWarnings.java");
        copySingleFileToAgentWorkspace(worker, project, "pom.xml", "pom.xml");
        enableWarnings(project, createTool(new CheckStyle(),  ""));
        enableWarnings(project, createTool(new Java(),  ""));
        enableWarnings(project, createTool(new MavenConsole(), ""));

        scheduleBuildAndAssertStatus(project, Result.SUCCESS);

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        assertThat(project.getLastBuild().getBuiltOn().getLabelString()).isEqualTo(worker.getLabelString());
        System.out.println(result.size());
    }

    private void executePipelineWorker(final Slave worker) throws ExecutionException, InterruptedException {
        WorkflowJob project = createPipeline();
        project.setDefinition(new CpsFlowDefinition("node('" + worker.getNodeName() + "') {\n"
                + "\n"
                + "    echo '[javac] Test.java:39: warning: Test Warning'\n"
                + "\n"
                + "    echo 'MediaPortal.cs(3001,5): warning CS0162: Hier kommt der Warnings Text'\n"
                + "\n"
                + "    recordIssues tools: [msBuild(), java()]\n"
                + "\n"
                + "}", false));

        System.out.println(getJenkins().jenkins.getSecurity());
        System.out.println(getJenkins().jenkins.getSecurityRealm());
        getJenkins().jenkins.disableSecurity();
        //ToDo Security

        Set<String> agentProtocols = new HashSet<>();
        agentProtocols.add("JNLP4-connect");
        agentProtocols.add("Ping");
        getJenkins().jenkins.setAgentProtocols(agentProtocols);
        getJenkins().jenkins.getInjector().getInstance(AdminWhitelistRule.class)
                .setMasterKillSwitch(false);


        buildWithResult(project, Result.SUCCESS);


        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        System.out.println(result.size());
    }
}
