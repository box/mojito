package com.box.l10n.mojito.rest.locale;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

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
      public Predicate toPredicate(
          Root<Locale> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return builder.equal(root.get(Locale_.bcp47Tag), bcp47Tag);
      }
    };
  }

  /**
   * A {@link Specification} the check for {@link Locale#bcp47Tag} is in a provided list
   *
   * @param bcp47Tags values to check
   * @return {@link Specification}
   */
  public static SingleParamSpecification<Locale> bcp47TagIn(final List<String> bcp47Tags) {
    return new SingleParamSpecification<Locale>(bcp47Tags) {
      public Predicate toPredicate(
          Root<Locale> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return root.get(Locale_.bcp47Tag).in(bcp47Tags);
      }
    };
  }
}
