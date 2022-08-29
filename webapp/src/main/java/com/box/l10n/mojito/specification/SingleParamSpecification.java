package com.box.l10n.mojito.specification;

import org.springframework.data.jpa.domain.Specification;

/**
 * A {@link Specification} that has a single parameter.
 *
 * <p>Can be used to implement a filter pattern with {@link
 * BaseSpecifications#ifParamNotNull(com.box.l10n.mojito.specification.SingleParamSpecification) }
 *
 * @author jaurambault
 */
public abstract class SingleParamSpecification<T> implements Specification<T> {

  /** The specification parameter */
  Object param;

  private SingleParamSpecification() {}

  public SingleParamSpecification(Object param) {
    this.param = param;
  }

  public boolean isParamNull() {
    return param == null;
  }
}
