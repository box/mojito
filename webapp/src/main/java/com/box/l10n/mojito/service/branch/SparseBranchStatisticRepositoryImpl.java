package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Component;

/**
 * @author garion
 */
@Component
public class SparseBranchStatisticRepositoryImpl implements SparseBranchStatisticRepository {
  @Autowired EntityManager entityManager;

  @Override
  public Page<Long> findAllWithIdOnly(
      Specification<BranchStatistic> specification, Pageable pageable) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> idQuery = criteriaBuilder.createQuery(Long.class);
    Root<BranchStatistic> root = idQuery.from(BranchStatistic.class);
    if (specification != null) {
      Predicate predicate = specification.toPredicate(root, idQuery, criteriaBuilder);
      if (predicate != null) {
        idQuery.where(predicate);
      }
    }

    Long count = getTotalCount(specification, criteriaBuilder, idQuery, root);
    if (count == 0 || pageable.getOffset() >= count) {
      return Page.empty();
    }

    List<Long> ids = getBranchStatisticIds(pageable, criteriaBuilder, idQuery, root);
    return new PageImpl<>(ids, pageable, count);
  }

  private Long getTotalCount(
      Specification<BranchStatistic> specification,
      CriteriaBuilder criteriaBuilder,
      CriteriaQuery<Long> idQuery,
      Root<BranchStatistic> root) {
    idQuery.select(criteriaBuilder.count(root));
    return entityManager.createQuery(idQuery).getSingleResult();
  }

  private List<Long> getBranchStatisticIds(
      Pageable pageable,
      CriteriaBuilder criteriaBuilder,
      CriteriaQuery<Long> idQuery,
      Root<BranchStatistic> root) {
    idQuery.select(root.get(BranchStatistic_.id));

    Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
    if (sort.isSorted()) {
      idQuery.orderBy(QueryUtils.toOrders(sort, root, criteriaBuilder));
    }

    TypedQuery<Long> query = entityManager.createQuery(idQuery);

    if (pageable.isPaged()) {
      query.setFirstResult((int) pageable.getOffset());
      query.setMaxResults(pageable.getPageSize());
    }

    List<Long> ids = query.getResultList();
    return ids;
  }
}
