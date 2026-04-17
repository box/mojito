package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.LeveragingClient;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.CopyTmConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.Map;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to create copy TM from a source repository into a target repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"leveraging-copy-tm"},
    commandDescription = "Copy TM from a source repository into a target repository")
public class LeveragingCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LeveragingCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.SOURCE_REPOSITORY_LONG, Param.SOURCE_REPOSITORY_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_REPOSITORY_DESCRIPTION)
  String sourceRepositoryParam;

  @Parameter(
      names = {Param.TARGET_REPOSITORY_LONG, Param.TARGET_REPOSITORY_SHORT},
      arity = 1,
      required = false,
      description = Param.TARGET_REPOSITORY_DESCRIPTION)
  String targetRepositoryParam;

  @Parameter(
      names = {"--name-regex", "-nr"},
      arity = 1,
      required = false,
      description =
          "Leveraging will be performed only for target text units whose name matches provided regex")
  String nameRegexParam;

  @Parameter(
      names = {"--target-asset-path", "-ta"},
      arity = 1,
      required = false,
      description = "Leveraging will be performed only for the target asset path")
  String targetAssetPathParam;

  @Parameter(
      names = {"--source-asset-path", "-sa"},
      arity = 1,
      required = false,
      description = "Use only translations from specified source asset")
  String sourceAssetPathParam;

  @Parameter(
      names = {"--target-branch-name", "-tbn"},
      arity = 1,
      required = false,
      description = "Leveraging will be performed only for the target branch name")
  String targetBranchNameParam;

  @Parameter(
      names = {"--mode", "-m"},
      arity = 1,
      required = false,
      description =
          """
          Matching mode.
          - MD5: match on all of the resource name, source content and comment.
          - EXACT: match on the source (!) content.
          - NAME: match on the resource name.
          - TUIDS: only leverage between tmTextUnitIds specified in explicit mapping.""",
      converter = CopyTmConfigModeConverter.class)
  CopyTmConfig.Mode mode = CopyTmConfig.Mode.MD5;

  @Parameter(
      names = {"--preserve-status", "-ps"},
      arity = 1,
      required = false,
      description =
          """
          Controls whether to keep the leveraged translation's original status or downgrade \
          it to TRANSLATION_NEEDED.
          A match is 'unique' when exactly one candidate text unit matched (no ambiguity). \
          A match is 'high-precision' when matched on both name and content (or full MD5).
          - PRECISION (default): preserve status only when the match is both unique and \
              high-precision. Matches on name-only or content-only are always downgraded, \
              even if unique. Low risk.
          - UNIQUE: preserve status for any unique match, regardless of its precision. \
              Useful when e.g. migrating between naming schemes. Medium risk.
          - ALL: always preserve the original status, even for ambiguous matches. HIGH RISK - \
              this will arbitrarily pick one of several candidates.""",
      converter = PreserveStatusModeConverter.class)
  CopyTmConfig.PreserveStatusMode preserveStatusMode = CopyTmConfig.PreserveStatusMode.PRECISION;

  @Parameter(
      names = {"--overwrite-mode", "-om"},
      arity = 1,
      required = false,
      description =
          """
          Controls when existing translations may be overwritten based on status comparison.
          - ALL (default): overwrite regardless of status.
          - HIGHER_STATUS: overwrite only when the candidate status is strictly higher \
              (e.g. TRANSLATION_NEEDED -> REVIEW_NEEDED, REVIEW_NEEDED -> APPROVED).
          - HIGHER_OR_EQUAL_STATUS: same as HIGHER_STATUS but also overwrite when statuses are equal.
          - UNTRANSLATED_ONLY: only leverage into locales that have no translation.""",
      converter = OverwriteModeConverter.class)
  CopyTmConfig.OverwriteMode overwriteMode = CopyTmConfig.OverwriteMode.ALL;

  @Parameter(
      names = {"--tuids-mapping"},
      required = false,
      description =
          """
          Text unit mapping (by tmTextUnitId) for TUIDS mode.
          Format: "1001:2001;1002:2002" ("source_tm_text_unit_id:target_tm_text_unit_id;...").
          Note: source_tm_text_unit_id should be unique. Use multiple calls to copy the same source to multiple targets.""",
      converter = TmTextUnitMappingConverter.class)
  Map<Long, Long> sourceToTargetTmTextUnitMapping;

  @Autowired CommandHelper commandHelper;

  @Autowired LeveragingClient leveragingClient;

  @Autowired AssetClient assetClient;

  @Override
  public void execute() throws CommandException {

    if (CopyTmConfig.Mode.TUIDS.equals(mode)) {
      copyTranslationBetweenTextUnits();
    } else {
      copyTmBetweenRepositories();
    }
  }

  void copyTmBetweenRepositories() throws CommandException {

    if (sourceRepositoryParam == null || targetRepositoryParam == null) {
      throw new CommandException(
          "Both --source-repository and --target-repository options must be provided in mode: "
              + mode.toString());
    }

    consoleWriter
        .newLine()
        .a("Copy TM from repository: ")
        .fg(Color.CYAN)
        .a(sourceRepositoryParam)
        .reset()
        .a(" into repository: ")
        .fg(Color.CYAN)
        .a(targetRepositoryParam)
        .println(2);

    Repository sourceRepository = commandHelper.findRepositoryByName(sourceRepositoryParam);
    Repository targetRepository = commandHelper.findRepositoryByName(targetRepositoryParam);

    try {
      CopyTmConfig copyTmConfig = new CopyTmConfig();
      copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
      copyTmConfig.setTargetRepositoryId(targetRepository.getId());
      copyTmConfig.setNameRegex(nameRegexParam);
      copyTmConfig.setTargetBranchName(targetBranchNameParam);
      copyTmConfig.setPreserveStatusMode(preserveStatusMode);
      copyTmConfig.setOverwriteMode(overwriteMode);

      if (mode != null) {
        copyTmConfig.setMode(mode);
      }

      if (targetAssetPathParam != null) {
        Asset asset =
            assetClient.getAssetByPathAndRepositoryId(
                targetAssetPathParam, targetRepository.getId());
        copyTmConfig.setTargetAssetId(asset.getId());
      }

      if (sourceAssetPathParam != null) {
        Asset asset =
            assetClient.getAssetByPathAndRepositoryId(
                sourceAssetPathParam, sourceRepository.getId());
        copyTmConfig.setSourceAssetId(asset.getId());
      }

      copyTmConfig = leveragingClient.copyTM(copyTmConfig);

      PollableTask pollableTask = copyTmConfig.getPollableTask();
      commandHelper.waitForPollableTask(pollableTask.getId());

    } catch (AssetNotFoundException assetNotFoundException) {
      throw new CommandException(assetNotFoundException);
    }
  }

  void copyTranslationBetweenTextUnits() throws CommandException {
    consoleWriter.newLine().a("Copy TM with mapping: ").println();

    for (Map.Entry<Long, Long> entry : sourceToTargetTmTextUnitMapping.entrySet()) {
      consoleWriter
          .newLine()
          .fg(Color.MAGENTA)
          .a(entry.getKey())
          .reset()
          .a(" --> ")
          .fg(Color.MAGENTA)
          .a(entry.getValue());
    }

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setNameRegex(nameRegexParam);
    copyTmConfig.setMode(CopyTmConfig.Mode.TUIDS);
    copyTmConfig.setSourceToTargetTmTextUnitIds(sourceToTargetTmTextUnitMapping);

    copyTmConfig = leveragingClient.copyTM(copyTmConfig);

    PollableTask pollableTask = copyTmConfig.getPollableTask();
    commandHelper.waitForPollableTask(pollableTask.getId());
  }
}
