package com.box.l10n.mojito.rest.drop;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Drop_;
import com.box.l10n.mojito.entity.PollableTask_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author jaurambault
 */
public class DropSpecification {

    /**
     * A {@link Specification} to filter Drops that have been imported or not.
     *
     * @param imported {@code true} to get Drops that have been imported,
     * {@code false} to get Drops that have not yet been imported
     *
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Drop> isImported(final Boolean imported) {
        return new SingleParamSpecification<Drop>(imported) {
            @Override
            public Predicate toPredicate(Root<Drop> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

                Predicate predicate;

                if (imported) {
                    predicate = builder.and(builder.isNotNull(root.get(Drop_.lastImportedDate)),
                            builder.isFalse(root.get(Drop_.importFailed)),
                            builder.isNotNull(root.get(Drop_.importPollableTask)),
                            builder.isNotNull(root.join(Drop_.importPollableTask, JoinType.LEFT).get(PollableTask_.finishedDate)));
                } else {
                    predicate = builder.or(builder.isNull(root.get(Drop_.lastImportedDate)),
                            builder.isTrue(root.get(Drop_.importFailed)),
                            builder.isNull(root.get(Drop_.importPollableTask)),
                            builder.isNull(root.join(Drop_.importPollableTask, JoinType.LEFT).get(PollableTask_.finishedDate)));
                }

                return predicate;
            }
        };
    }

    /**
     * A {@link Specification} to filter Drops that have been canceled or not.
     *
     * @param canceled {@code true} to get Drops that have been canceled,
     * {@code false} to get Drops that have not been canceled
     *
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Drop> isCanceled(final Boolean canceled) {
        return new SingleParamSpecification<Drop>(canceled) {
            @Override
            public Predicate toPredicate(Root<Drop> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                if (canceled) {
                    return builder.isTrue(root.get(Drop_.canceled));
                } else {
                    return builder.or(builder.isFalse(root.get(Drop_.canceled)),
                           builder.isNull(root.get(Drop_.canceled)));
                }
            }
        };
    }

    /**
     * A {@link Specification} the check if {@link Drop#repository} is equal.
     *
     * @param repositoryId value to check
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Drop> repositoryIdEquals(final Long repositoryId) {
        return new SingleParamSpecification<Drop>(repositoryId) {
            public Predicate toPredicate(Root<Drop> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return builder.equal(root.get(Drop_.repository), repositoryId);
            }
        };
    }
}
