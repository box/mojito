package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.google.common.collect.Sets;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author jaurambault
 */
@Component
public class PushService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PushService.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    CommandHelper commandHelper;

    public void push(Repository repository, Stream<SourceAsset> sourceAssetStream, String branchName) throws CommandException {

        List<PollableTask> pollableTasks = new ArrayList<>();
        Set<Long> usedAssetIds = new HashSet<>();

        sourceAssetStream.forEach(sourceAsset -> {
            consoleWriter.a(" - Uploading: ").fg(Ansi.Color.CYAN).a(sourceAsset.getPath()).println();

            SourceAsset assetAfterSend = assetClient.sendSourceAsset(sourceAsset);
            pollableTasks.add(assetAfterSend.getPollableTask());

            consoleWriter.a(" --> asset id: ").fg(Ansi.Color.MAGENTA).a(assetAfterSend.getAddedAssetId()).reset().
                    a(", task: ").fg(Ansi.Color.MAGENTA).a(assetAfterSend.getPollableTask().getId()).println();
            usedAssetIds.add(assetAfterSend.getAddedAssetId());
        });

        try {
            logger.debug("Wait for all \"push\" tasks to be finished");
            for (PollableTask pollableTask : pollableTasks) {
                commandHelper.waitForPollableTask(pollableTask.getId());
            }
        } catch (PollableTaskException e) {
            throw new CommandException(e.getMessage(), e.getCause());
        }

        Branch branch = repositoryClient.getBranch(repository.getId(), branchName);

        if (branch == null) {
            logger.debug("No branch in the repository, no asset must have been pushed yet, no need to delete");
        } else {
            logger.debug("process deleted assets here");
            Set<Long> assetIds = Sets.newHashSet(assetClient.getAssetIds(repository.getId(), false, false, branch.getId()));

            assetIds.removeAll(usedAssetIds);
            if (!assetIds.isEmpty()) {
                consoleWriter.newLine().a("Delete assets from repository, ids: ").fg(Ansi.Color.CYAN).a(assetIds.toString()).println();
                PollableTask pollableTask = assetClient.deleteAssetsInBranch(assetIds, branch.getId());
                consoleWriter.a(" --> task id: ").fg(Ansi.Color.MAGENTA).a(pollableTask.getId()).println();
                commandHelper.waitForPollableTask(pollableTask.getId());
            }
        }
    }
}
