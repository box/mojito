package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.Set;
import org.joda.time.DateTime;

/**
 *
 * @author jaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollableTask {

    private Long id;
    private String name;

    private DateTime finishedDate;

    private String output;

    private String message;

    private ErrorMessage errorMessage;

    private int expectedSubTaskNumber;

    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<PollableTask> subTasks;

    private boolean allFinished;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(DateTime finishedDate) {
        this.finishedDate = finishedDate;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getExpectedSubTaskNumber() {
        return expectedSubTaskNumber;
    }

    public void setExpectedSubTaskNumber(int expectedSubTaskNumber) {
        this.expectedSubTaskNumber = expectedSubTaskNumber;
    }

    public Set<PollableTask> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(Set<PollableTask> subTasks) {
        this.subTasks = subTasks;
    }

    public boolean isAllFinished() {
        return allFinished;
    }

    public void setAllFinished(boolean allFinished) {
        this.allFinished = allFinished;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
