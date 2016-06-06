package com.box.l10n.mojito.rest.locale;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * @author aloison
 */
public class LocaleSpecification {

    /**
     * A {@link Specification} the check if {@link Locale#bcp47Tag} is equal
     *
     * @param bcp47Tag value to check
     * @return {@link Specification}
     */
    public static SingleParamSpecification<Locale> bcp47TagEquals(final String bcp47Tag) {
        return new SingleParamSpecification<Locale>(bcp47Tag) {
            public Predicate toPredicate(Root<Locale> root, CriteriaQuery<?> query,
                                         CriteriaBuilder builder) {

                return builder.equal(root.get(Locale_.bcp47Tag), bcp47Tag);
            }
        };
    }
}
