package io.jenkins.plugins.analysis.warnings.recorder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.ClassRule;
import org.junit.Test;

import org.jenkinsci.test.acceptance.docker.DockerClassRule;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.security.SecurityRealm;
import hudson.slaves.DumbSlave;
import hudson.tasks.Maven;
import hudson.tasks.Shell;

import jenkins.AgentProtocol;
import jenkins.security.s2m.AdminWhitelistRule;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.steps.IssuesRecorder;
import io.jenkins.plugins.analysis.core.testutil.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.analysis.warnings.MavenConsole;

public class DistributedExecutionITest extends IntegrationTestWithJenkinsPerSuite {

    @ClassRule
    public static DockerClassRule<JavaContainer> dockerUbuntu = new DockerClassRule<>(JavaContainer.class);


    @Test
    public void distributionWithDumpSlave() throws Exception {
        DumbSlave slave = JENKINS_PER_SUITE.createSlave();
        /*
        FreeStyleProject project = createFreeStyleProject();

        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        copySingleFileToAgentWorkspace(slave, project, "eclipse_4_Warnings.txt", "eclipse_4_Warnings-issues.txt");
        enableEclipseWarnings(project);


        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        System.out.println(result.size());

*/
        //executeOnWorker(slave);
        executeMavenOnWorker(slave);

        System.out.println("End");

    }


    @Test
    public void distributionWithDockerSlave() throws Exception {
        JavaContainer container = dockerUbuntu.create();
        //JavaContainer container = new JavaContainer();
        DumbSlave slave = new DumbSlave("docker", "/home/test", new SSHLauncher(container.ipBound(22), container.port(22), "test", "test", "", "-Dfile.encoding=ISO-8859-1"));
        getJenkins().jenkins.addNode(slave);
        getJenkins().waitOnline(slave);

        //FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles("eclipse_4_Warnings.txt");

        //enableEclipseWarnings(project);
        //getJenkins().jenkins.getQueue().schedule(project);

        //executeOnWorker(slave);
        executeMavenOnWorker(slave);

        System.out.println("End");

    }

    private void executeOnWorker(final Slave worker) throws ExecutionException, InterruptedException {
        FreeStyleProject project = createFreeStyleProject();

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

    private void executeMavenOnWorker(final Slave worker) throws ExecutionException, InterruptedException {
        FreeStyleProject project = createFreeStyleProject();

        System.out.println(getJenkins().jenkins.getSecurity());
        System.out.println(getJenkins().jenkins.getSecurityRealm());
        //ToDo Security

        Set<String> agentProtocols = new HashSet<>();
        agentProtocols.add("JNLP4-connect");
        agentProtocols.add("Ping");
        getJenkins().jenkins.setAgentProtocols(agentProtocols);
        getJenkins().jenkins.getInjector().getInstance(AdminWhitelistRule.class)
                .setMasterKillSwitch(false);


        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        project.getBuildersList().add(new Maven("verify", null));
        copySingleFileToAgentWorkspace(worker, project, "eclipse_4_Warnings.txt", "eclipse_4_Warnings-issues.txt");
        copySingleFileToAgentWorkspace(worker, project, "pom.xml", "pom.xml");
        enableEclipseWarnings(project);
        IssuesRecorder recorder = enableWarnings(project, createTool(new MavenConsole(), ""));
        /*
        IssuesRecorder publisher = new IssuesRecorder();
        publisher.setTool(new MavenConsole());
        project.getPublishersList().add(publisher);
*/


        getJenkins().jenkins.getQueue().schedule(project).getFuture().get();

        List<AnalysisResult> result = getAnalysisResults(project.getLastBuild());
        System.out.println(result.size());
    }
}
