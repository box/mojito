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
     * This is used to get repository entity
     */
    public interface Repository extends RepositorySummary, DropSummary {}
    /**
     * This is used to show {@link Pageable} information
     */
    public interface Pageable {}
    /**
     * This is used to get asset entity with minimum fields for summary
     */
    public interface AssetSummary extends IdAndName {}
    /**
     * This is used to get Branch entity with minimum fields for summary
     */
    public interface BranchSummary extends IdAndName {}
    /**
     * This is used to get TmTextUnit entity with minimum fields for summary
     */
    public interface TmTextUnitSummary extends IdAndName {}
    /**
     * This is used to show the branch statistics
     */
    public interface BranchStatistic extends BranchSummary, TmTextUnitSummary, Pageable {}
    /**
     * This is used to show git blame information
     */
    public interface GitBlame extends IdAndName {};
    /**
     * This is used to show git bame information with usages
     */
    public interface GitBlameWithUsage extends GitBlame, BranchSummary {};
    /**
     * This is used to restrict screenshot information returned
     */
    public interface Screenshots extends IdAndName, TmTextUnitSummary {};
}
