package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.CommitClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class PushService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PushService.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired AssetClient assetClient;

  @Autowired CommitClient commitClient;

  @Autowired RepositoryClient repositoryClient;

  @Autowired CommandHelper commandHelper;

  public void push(
      Repository repository,
      Stream<SourceAsset> sourceAssetStream,
      String branchName,
      PushType pushType)
      throws CommandException {

    List<PollableTask> pollableTasks = new ArrayList<>();
    Set<Long> usedAssetIds = new HashSet<>();

    sourceAssetStream.forEach(
        sourceAsset -> {
          consoleWriter.a(" - Uploading: ").fg(Ansi.Color.CYAN).a(sourceAsset.getPath()).println();

          SourceAsset assetAfterSend = assetClient.sendSourceAsset(sourceAsset);
          pollableTasks.add(assetAfterSend.getPollableTask());

          consoleWriter
              .a(" --> asset id: ")
              .fg(Ansi.Color.MAGENTA)
              .a(assetAfterSend.getAddedAssetId())
              .reset()
              .a(", task: ")
              .fg(Ansi.Color.MAGENTA)
              .a(assetAfterSend.getPollableTask().getId())
              .println();
          usedAssetIds.add(assetAfterSend.getAddedAssetId());
        });

    if (PushType.SEND_ASSET_NO_WAIT_NO_DELETE.equals(pushType)) {
      consoleWriter
          .fg(Ansi.Color.YELLOW)
          .a(
              "Warning you are using push type: SEND_ASSET_NO_WAIT_NO_DELETE. The"
                  + "command won't wait for the asset processing to finish (ie. if any error "
                  + "happens it will silently fail) and it will skip the asset delete.");
    } else {
      waitForPollableTasks(pollableTasks);
      optionalDeleteUnusedAssets(repository, branchName, pushType, usedAssetIds);
    }
  }

  void optionalDeleteUnusedAssets(
      Repository repository, String branchName, PushType pushType, Set<Long> usedAssetIds)
      throws CommandException {
    Branch branch = repositoryClient.getBranch(repository.getId(), branchName);

    if (PushType.NO_DELETE.equals(pushType)) {
      logger.debug("Don't delete unused asset");
    } else {
      if (branch == null) {
        logger.debug(
            "No branch in the repository, no asset must have been pushed yet, no need to delete");
      } else {
        logger.debug("process deleted assets here");
        Set<Long> assetIds =
            Sets.newHashSet(
                assetClient.getAssetIds(repository.getId(), false, false, branch.getId()));

        assetIds.removeAll(usedAssetIds);
        if (!assetIds.isEmpty()) {
          consoleWriter
              .newLine()
              .a("Delete assets from repository, ids: ")
              .fg(Ansi.Color.CYAN)
              .a(assetIds.toString())
              .println();
          PollableTask pollableTask = assetClient.deleteAssetsInBranch(assetIds, branch.getId());
          consoleWriter
              .a(" --> task id: ")
              .fg(Ansi.Color.MAGENTA)
              .a(pollableTask.getId())
              .println();
          commandHelper.waitForPollableTask(pollableTask.getId());
        }
      }
    }
  }

  void waitForPollableTasks(List<PollableTask> pollableTasks) throws CommandException {
    try {
      logger.debug("Wait for all \"push\" tasks to be finished");
      for (PollableTask pollableTask : pollableTasks) {
        commandHelper.waitForPollableTask(pollableTask.getId());
      }
    } catch (PollableTaskException e) {
      throw new CommandException(e.getMessage(), e.getCause());
    }
  }

  public void associatePushRun(Repository repository, String pushRunName, String commitHash) {
    if (!StringUtils.isBlank(pushRunName) && !StringUtils.isBlank(commitHash)) {
      logger.debug(
          "Associating commit with hash: {} to push run with name: {}.", commitHash, pushRunName);
      commitClient.associateCommitToPushRun(commitHash, repository.getId(), pushRunName);
    }
  }

  enum PushType {
    /** Normal processing: send asset, wait for them to be process and remove unused assets. */
    NORMAL,
    /**
     * Just send the assets to the server. Don't wait for them to be processed. Don't delete the
     * assets.
     *
     * <p>This is can be used to speed up the asset submission. The compromise is that there is no
     * visibility on the success or failure during processing. It also won't run the logic to remove
     * assets that are not used anymore.
     *
     * <p>Usage example is to speed up CI jobs but it is a stop gap until Mojito backend performance
     * are improved and/or more evolved async system is implemented.
     *
     * <p>Don't use unless you know what you're doing.
     */
    SEND_ASSET_NO_WAIT_NO_DELETE,
    /**
     * Send asset but don't remove unused assets.
     *
     * <p>While it could be used to keep adding assets to a repository, that use case has never
     * really showed up as it.
     *
     * <p>The actual use case we have now is to be used as a workaround for the fact that we can't
     * provide multiple source directories (this should eventually be supported in some ways but is
     * not simple). With NO_DELETE option you can chain push command with different source directory
     * without marking the asset as deleted.
     *
     * <p>The first call of the chain can do a normal push which will end removing used asset in the
     * end
     */
    NO_DELETE
  }
}
