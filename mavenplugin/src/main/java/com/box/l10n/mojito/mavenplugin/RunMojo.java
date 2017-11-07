package com.box.l10n.mojito.mavenplugin;

import com.box.l10n.mojito.cli.App;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Run Mojo is a wrapper around Mojito CLI that can be used to run any CLI command
 * via Maven. 
 * 
 * The command is passed through the "cmd" parameters.
 *
 * Usage: mvn com.box.l10n.mojito:mojito-maven-plugin:run -Dcmd="pull -r Demo
 * -localeMapping=\"fr:fr-FR,es:fr-FR\""
 *
 * @author jaurambault
 */
@Mojo(name = "run", requiresProject = false)
public class RunMojo extends AbstractMojo {

    /**
     * the CLI arguments
     */
    @Parameter(property = "cmd", required = true)
    private String commandParameter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Iterable<String> args = Splitter.on(" ").split(commandParameter);
        
        ArrayList<String> argsWithOutputType = Lists.newArrayList(args);
        argsWithOutputType.add("--l10n.consoleWritter.outputType=ANSI_LOGGER");

        App.main(Iterables.toArray(argsWithOutputType, String.class));
    }

}
