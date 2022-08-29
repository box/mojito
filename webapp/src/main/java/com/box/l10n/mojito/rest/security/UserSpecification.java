package com.box.l10n.mojito.rest.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.User_;
import com.box.l10n.mojito.specification.SingleParamSpecification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/** @author jyi */
public class UserSpecification {
  /**
   * A {@link Specification} that checks if {@link User#username} is equal
   *
   * @param username value to check
   * @return {@link Specification}
   */
  public static SingleParamSpecification<User> usernameEquals(final String username) {
    return new SingleParamSpecification<User>(username) {
      @Override
      public Predicate toPredicate(
          Root<User> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return builder.equal(root.get(User_.username), username);
      }
    };
  }

  /**
   * A {@link Specification} that checks if {@link User} is enabled
   *
   * @param enabled
   * @return {@link Specification}
   */
  public static SingleParamSpecification<User> enabledEquals(final Boolean enabled) {
    return new SingleParamSpecification<User>(enabled) {
      @Override
      public Predicate toPredicate(
          Root<User> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.equal(root.get(User_.enabled), enabled);
      }
    };
  }
}
