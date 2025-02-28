package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.apiclient.model.ImportRepositoryBody;
import com.box.l10n.mojito.apiclient.model.Repository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocale;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.command.param.Param;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"demo-create"},
    commandDescription = "Creates a demo repository and local resource bundle")
public class DemoCreateCommand extends RepoCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DemoCreateCommand.class);

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {"--output-directory", "-o"},
      arity = 1,
      required = false,
      description = "Provide the name of the directory that will contain the resource bundle")
  String outputDirectoryParam;

  @Autowired CommandHelper commandHelper;

  CommandDirectories commandDirectories;

  Path outputDirectoryPath;

  RepositoryRepository repository;

  @Override
  public void execute() throws CommandException {
    createDemoRepository();
    initOutputDirectory();
    addResourceBundleToDemoDirectory();
    importTM();
    push();

    consoleWriter
        .newLine()
        .a("Demo repository is ready!")
        .println()
        .println()
        .a("Go to directory: ")
        .fg(Ansi.Color.CYAN)
        .a(outputDirectoryPath.toString())
        .reset()
        .a(". Try to generate the localized files: ")
        .fg(Ansi.Color.CYAN)
        .a("mojito pull -r " + repository.getName())
        .reset()
        .a(" then modify ")
        .fg(Ansi.Color.CYAN)
        .a(DEMO_PROPERTIES)
        .reset()
        .a(" and re-synchronize: ")
        .fg(Ansi.Color.CYAN)
        .a("mojito push -r " + repository.getName())
        .println(2);
  }

  /** Create a directory that will contain the demo resource bundle */
  void initOutputDirectory() throws CommandException {

    if (outputDirectoryParam == null) {
      outputDirectoryPath = Paths.get(nameParam);
    } else {
      outputDirectoryPath = Paths.get(outputDirectoryParam);
    }

    try {
      java.nio.file.Files.createDirectories(outputDirectoryPath);
    } catch (IOException ioe) {
      throw new CommandException("Error creating output directory", ioe);
    }
  }

  void addResourceBundleToDemoDirectory() throws CommandException {
    try {
      String resourceBundleName = DEMO_PROPERTIES;

      URL inputResourceBundleUrl = getResourceURL(resourceBundleName);
      Path resourceBundlePath = outputDirectoryPath.resolve(resourceBundleName);

      String resourceBundleContent =
          Resources.toString(inputResourceBundleUrl, StandardCharsets.UTF_8);

      consoleWriter
          .newLine()
          .a("Add resource bundle: ")
          .fg(Ansi.Color.CYAN)
          .a(resourceBundlePath.toString())
          .println();
      Files.write(resourceBundleContent, resourceBundlePath.toFile(), StandardCharsets.UTF_8);

    } catch (IOException ioe) {
      throw new CommandException("Error copying resource bundle file into the demo directory", ioe);
    }
  }

  static final String DEMO_PROPERTIES = "demo.properties";

  void createDemoRepository() throws CommandException {

    consoleWriter
        .newLine()
        .a("Create demo repository: ")
        .fg(Ansi.Color.CYAN)
        .a(nameParam)
        .println();

    try {
      Repository repositoryBody = new Repository();
      repositoryBody.setName(nameParam);
      repositoryBody.setDescription("");
      repositoryBody.setSourceLocale(null);
      repositoryBody.setRepositoryLocales(getRepositoryLocales());
      repositoryBody.setAssetIntegrityCheckers(
          extractIntegrityCheckersFromInput("properties:MESSAGE_FORMAT", false));
      repositoryBody.setCheckSLA(null);
      repository = repositoryClient.createRepository(repositoryBody);

    } catch (ParameterException | ResourceNotCreatedException rnce) {
      throw new CommandException(rnce.getMessage(), rnce);
    }
  }

  List<RepositoryLocale> getRepositoryLocales() throws CommandException {

    List<String> encodedBcp47Tags = new ArrayList<>();

    encodedBcp47Tags.add("da-DK");
    encodedBcp47Tags.add("de-DE");
    encodedBcp47Tags.add("(en-GB)");
    encodedBcp47Tags.add("(en-CA)->en-GB");
    encodedBcp47Tags.add("(en-AU)->en-GB");
    encodedBcp47Tags.add("es-ES");
    encodedBcp47Tags.add("fi-FI");
    encodedBcp47Tags.add("fr-FR");
    encodedBcp47Tags.add("(fr-CA)->fr-FR");
    encodedBcp47Tags.add("it-IT");
    encodedBcp47Tags.add("ja-JP");
    encodedBcp47Tags.add("ko-KR");
    encodedBcp47Tags.add("nb-NO");
    encodedBcp47Tags.add("nl-NL");
    encodedBcp47Tags.add("pl-PL");
    encodedBcp47Tags.add("pt-BR");
    encodedBcp47Tags.add("ru-RU");
    encodedBcp47Tags.add("sv-SE");
    encodedBcp47Tags.add("tr-TR");
    encodedBcp47Tags.add("zh-CN");
    encodedBcp47Tags.add("zh-TW");

    return localeHelper.extractRepositoryLocalesFromInput(encodedBcp47Tags, false);
  }

  void importTM() throws CommandException {

    consoleWriter.newLine().a("Import translation memory: ").println();

    importTMFile("Demo-1.xliff");

    for (RepositoryLocale repositoryLocale : getRepositoryLocales()) {
      String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
      consoleWriter.a(" - locale: ").fg(Ansi.Color.CYAN).a(bcp47Tag).println();
      importTMFile("Demo-1_" + repositoryLocale.getLocale().getBcp47Tag() + ".xliff");
    }
  }

  void importTMFile(String tmFileName) throws CommandException {

    try {
      String tmFileContent = Resources.toString(getResourceURL(tmFileName), StandardCharsets.UTF_8);
      ImportRepositoryBody importRepositoryBody = new ImportRepositoryBody();
      importRepositoryBody.setXliffContent(tmFileContent);
      importRepositoryBody.setUpdateTM(false);
      repositoryClient.importRepository(importRepositoryBody, repository.getId());
    } catch (IOException | ResourceNotCreatedException e) {
      throw new CommandException("Error importing file [" + tmFileName + "]", e);
    }
  }

  void push() {
    new L10nJCommander()
        .run("push", "-r", repository.getName(), "-s", outputDirectoryPath.toString());
  }

  URL getResourceURL(String filename) {
    return getClass().getResource("DemoCreateCommand_IO/" + filename);
  }
}
