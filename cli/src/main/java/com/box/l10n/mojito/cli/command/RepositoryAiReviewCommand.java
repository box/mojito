package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient.RetrieveBatchResponse;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryAiReviewClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.entity.PollableTask;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to machine review strings in a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repository-ai-review"},
    commandDescription = "Ai review translated strings in a repository")
public class RepositoryAiReviewCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryAiReviewCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      description =
          "List of locales (bcp47 tags) to review, if not provided review all locales in the repository")
  List<String> locales;

  @Parameter(
      names = {"--source-text-max-count"},
      arity = 1,
      description =
          "Text unit variant max count per locale sent to review (this param is used to avoid "
              + "sending too many strings to MT)")
  int sourceTextMaxCount = 100;

  @Parameter(
      names = {"--text-unit-ids"},
      arity = 1,
      description = "The list of TmTextUnitIds to review")
  List<Long> textUnitIds;

  @Parameter(
      names = {"--use-batch"},
      arity = 1,
      description = "To use the batch API or not")
  boolean useBatch = false;

  @Parameter(
      names = {"--use-model"},
      arity = 1,
      description = "Use a specific model for the review")
  String useModel;

  @Parameter(
      names = {"--run-name"},
      arity = 1,
      description =
          "Name of the review run. Acts as an identifier to distinguish multiple reviews of the same translations.")
  String runName;

  @Parameter(
      names = "--attach-job-id",
      arity = 1,
      description =
          "ID of an existing job to re-attach to; the CLI will only poll its status and will not start any new work.")
  Long attachJobId;

  @Parameter(
      names = "--retry-import-job-id",
      arity = 1,
      description =
          "ID of an existing job to try to re-import; If a job stopped because a transient error, try to import remaining data")
  Long retryImportJobId;

  @Autowired CommandHelper commandHelper;

  @Autowired RepositoryAiReviewClient repositoryAiReviewClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired ObjectMapper objectMapper;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {

    if (retryImportJobId != null) {
      consoleWriter.a("Retry importing task id: ").fg(Color.MAGENTA).a(retryImportJobId).println();

      Optional<PollableTask> lastForReimport =
          pollableTaskClient.getPollableTask(retryImportJobId).getSubTasks().stream()
              .filter(t -> t.getCreatedDate() != null)
              .sorted(Comparator.comparing(PollableTask::getCreatedDate).reversed())
              .filter(PollableTask::isAllFinished)
              .filter(pt -> pt.getErrorMessage() != null)
              .findFirst();

      if (lastForReimport.isPresent()) {
        long pollableTaskId = repositoryAiReviewClient.retryImport(lastForReimport.get().getId());
        waitForPollable(pollableTaskId);
      } else {
        consoleWriter
            .fg(Color.YELLOW)
            .a("Last task did not finish with an error, don't retry")
            .println();
      }

    } else if (attachJobId != null) {
      consoleWriter.a("Attaching, task id: ").fg(Color.MAGENTA).a(attachJobId).println();
      waitForPollable(attachJobId);
    } else {
      consoleWriter
          .newLine()
          .a("Ai review repository: ")
          .fg(Color.CYAN)
          .a(repositoryParam)
          .reset()
          .a(", model: ")
          .fg(Color.CYAN)
          .a(useModel)
          .reset()
          .a(", run name: ")
          .fg(Color.CYAN)
          .a(runName)
          .reset()
          .a(" for locales: ")
          .fg(Color.CYAN)
          .a(
              locales == null
                  ? "<all>"
                  : locales.stream().collect(Collectors.joining(", ", "[", "]")))
          .println(2);

      RepositoryAiReviewClient.ProtoAiReviewResponse protoAiTranslateResponse =
          repositoryAiReviewClient.reviewRepository(
              new RepositoryAiReviewClient.ProtoAiReviewRequest(
                  repositoryParam,
                  locales,
                  sourceTextMaxCount,
                  textUnitIds,
                  useBatch,
                  useModel,
                  runName));

      PollableTask pollableTask = protoAiTranslateResponse.pollableTask();
      consoleWriter.a("Running, task id: ").fg(Color.MAGENTA).a(pollableTask.getId()).println();
      waitForPollable(pollableTask.getId());
    }

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }

  void waitForPollable(Long pollableTaskId) {
    try {
      final AtomicBoolean firstRender = new AtomicBoolean(true);

      pollableTaskClient.waitForPollableTask(
          pollableTaskId,
          PollableTaskClient.NO_TIMEOUT,
          pollableTask -> {
            Optional<PollableTask> lastFinishedForOutput =
                pollableTask.getSubTasks().stream()
                    .filter(t -> t.getCreatedDate() != null)
                    .sorted(Comparator.comparing(PollableTask::getCreatedDate).reversed())
                    .filter(PollableTask::isAllFinished)
                    .findFirst();

            if (lastFinishedForOutput.isPresent()) {
              if (!firstRender.get()) {
                consoleWriter.erasePreviouslyPrintedLines();
              } else {
                firstRender.set(false);
              }

              Long lastFinishedTaskId = lastFinishedForOutput.get().getId();
              consoleWriter
                  .a("Running, task id: ")
                  .fg(Color.MAGENTA)
                  .a(pollableTaskId)
                  .reset()
                  .a(", child task id: ")
                  .fg(Color.MAGENTA)
                  .a(lastFinishedTaskId)
                  .reset()
                  .a(", run name: ")
                  .fg(Color.MAGENTA)
                  .a(runName)
                  .newLine();
              String pollableTaskOutput =
                  pollableTaskClient.getPollableTaskOutput(lastFinishedTaskId);
              try {
                renderAiReviewBatchesImportOutput(
                    objectMapper.readValueUnchecked(
                        pollableTaskOutput, AiReviewBatchesImportOutput.class));
              } catch (Exception e) {
                logger.error("Can't render", e);
                consoleWriter
                    .reset()
                    .a("Can't render:" + e.getMessage())
                    .newLine()
                    .a(pollableTaskOutput)
                    .newLine();
              }
            }
          });

    } catch (PollableTaskException e) {
      throw new CommandException(e.getMessage(), e.getCause());
    }
  }

  void renderAiReviewBatchesImportOutput(AiReviewBatchesImportOutput aiReviewBatchesImportOutput) {

    if (!aiReviewBatchesImportOutput.batchCreationErrors().isEmpty()) {
      consoleWriter
          .fg(Color.RED)
          .a(
              "Some batches failed to be created. The following locales will not be processed and will need to be retried:")
          .newLine();
      for (String batchCreationError : aiReviewBatchesImportOutput.batchCreationErrors()) {
        consoleWriter.a("- " + batchCreationError).newLine();
      }
      consoleWriter.newLine();
    }

    if (!aiReviewBatchesImportOutput.skippedLocales().isEmpty()) {
      consoleWriter
          .reset()
          .a("No content to review for the following locales; skipping: ")
          .fg(Color.MAGENTA)
          .a(String.join(",", aiReviewBatchesImportOutput.skippedLocales()))
          .reset()
          .newLine();
      consoleWriter.newLine();
    }

    aiReviewBatchesImportOutput
        .retrieveBatchResponses()
        .forEach(
            r ->
                renderBatch(
                    r,
                    aiReviewBatchesImportOutput.failedToImport.get(r.id()),
                    aiReviewBatchesImportOutput.processed.contains(r.id())));
    consoleWriter.println();
  }

  void renderBatch(
      RetrieveBatchResponse retrieveBatchResponse, String importError, boolean processed) {
    consoleWriter.a("- ").fg(Color.CYAN).a(retrieveBatchResponse.id()).a(" ");

    consoleWriter.reset().a("[import: ");
    if (importError != null) {
      consoleWriter.fg(Color.RED).a("failed");
    } else {
      if (processed) {
        if ("completed".equals(retrieveBatchResponse.status())) {
          consoleWriter.fg(Color.GREEN).a("success");
        } else {
          consoleWriter.fg(Color.YELLOW).a(" - ");
        }
      } else {
        consoleWriter.fg(Color.YELLOW).a("waiting");
      }
    }
    consoleWriter.reset().a("]");

    Color batchStatusColor =
        switch (retrieveBatchResponse.status()) {
          case "completed" -> Color.GREEN;
          case "failed" -> Color.RED;
          case "running", "queued", "in_progress" -> Color.YELLOW;
          default -> Color.DEFAULT;
        };

    RetrieveBatchResponse.RequestCounts c = retrieveBatchResponse.requestCounts();

    consoleWriter
        .reset()
        .a(" [batch: ")
        .fg(batchStatusColor)
        .a(retrieveBatchResponse.status())
        .reset()
        .a(" ; total=")
        .a(c.total())
        .a(", completed=")
        .a(c.completed())
        .a(", ");

    if (c.failed() > 0) {
      consoleWriter.fg(Color.RED);
    }

    consoleWriter.a("failed=").a(c.failed()).reset();
    consoleWriter.a("]").newLine();

    if (importError != null) {
      consoleWriter.newLine().a("Import error: ").newLine().fg(Color.RED).a(importError).newLine();
    }

    if (retrieveBatchResponse.errors() != null
        && retrieveBatchResponse.errors().data() != null
        && !retrieveBatchResponse.errors().data().isEmpty()) {
      consoleWriter.fg(Color.RED).a("   Errors:").reset().newLine();
      retrieveBatchResponse
          .errors()
          .data()
          .forEach(
              e ->
                  consoleWriter
                      .a("    - ")
                      .a(
                          "[%s] %s (param=%s, line=%s)"
                              .formatted(e.code(), e.message(), e.param(), e.line()))
                      .newLine());
    }
  }

  public record AiReviewBatchesImportOutput(
      String runName,
      List<RetrieveBatchResponse> retrieveBatchResponses,
      List<String> skippedLocales,
      List<String> batchCreationErrors,
      List<String> processed,
      Map<String, String> failedToImport,
      Long nextJob) {}
}
