package com.box.l10n.mojito.rest;

/**
 * This class is used with {@JsonView} to filter fields
 * depending on the context of serialization to avoid large payload.
 * 
 * @author jyi
 */
public class View {
    /**
     * This is used to get id and name of entity
     */
    public interface IdAndName {}
    /**
     * This is used to get created date in addition to id and name of entity
     */
    public interface IdAndNameAndCreated extends IdAndName {}
    /**
     * This is used to get locale entity with minimum fields for summary
     */
    public interface LocaleSummary {}
    /**
     * This is used to get pollable entity with minimum fields for summary
     */
    public interface PollableSummary {}
    /**
     * This is used to get drop entity with minimum fields for summary
     */
    public interface DropSummary extends IdAndNameAndCreated, LocaleSummary, PollableSummary, Pageable {}
    /**
     * This is used to get repository entity with minimum fields for summary
     */
    public interface RepositorySummary extends IdAndNameAndCreated, LocaleSummary {}
    /**
     * This is used to show {@link Pageable} information
     */
    public interface Pageable {}
}
