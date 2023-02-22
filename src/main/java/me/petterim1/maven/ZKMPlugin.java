package me.petterim1.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.*;

@Mojo(name = "zkm-obfuscate", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ZKMPlugin extends AbstractMojo {

    @Parameter(required = true)
    private boolean obfuscate;

    @Parameter(required = true)
    private File script;

    @Parameter(required = true)
    private File zkm;

    @Parameter(required = true)
    private File java;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        if (!obfuscate) {
            log.info("Obfuscation is not enabled in pom.xml");
            return;
        }

        String basedir = project.getBasedir().getAbsolutePath();
        log.info("Running ZKM plugin in " + basedir);

        if (script == null) {
            throw new MojoFailureException("ZKM script file is null");
        }

        if (java == null) {
            throw new MojoFailureException("Java path is null");
        }

        String libPath = java.getAbsolutePath();
        log.debug("Java: " + libPath);

        StringBuilder libPaths = new StringBuilder();
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("resources.jar\"\n");
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("rt.jar\"\n");
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("jsse.jar\"\n");
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("jce.jar\"\n");
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("charsets.jar\"\n");
        libPaths.append('\"').append(libPath).append(File.separatorChar).append("lib").append(File.separatorChar).append("jfr.jar\"\n");

        for (Object o : project.getArtifacts()) {
            Artifact a = (Artifact) o;
            if (a.getArtifactHandler().isAddedToClasspath()) {
                libPaths.append('\"').append(a.getFile().getPath()).append("\"\n");
            }
        }

        log.debug("Libraries:\n" + libPaths);

        File parsedScript = new File(basedir + File.separatorChar + "target" + File.separatorChar + script.getName());
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(script));
            String line = reader.readLine();
            while (line != null) {
                builder.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
            writer = new FileWriter(parsedScript);
            writer.write(builder.toString().replace("${basedir}", basedir).replace("${libs}", libPaths.toString()).replace("${pom.version}", project.getVersion()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String run = "java -jar " + zkm.getAbsolutePath() + ' ' + parsedScript.getAbsolutePath();
        getLog().info("Running script " + run);
        try {
            Process p = Runtime.getRuntime().exec(run);
            p.waitFor();
            log.info("ZKM exit");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
