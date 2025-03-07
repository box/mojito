package com.box.l10n.mojito.rest.repository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchStatistic_;
import com.box.l10n.mojito.entity.Branch_;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.User_;
import jakarta.persistence.criteria.*;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BranchStatisticSpecificationTest {
  @Mock Root<BranchStatistic> root;
  @Mock CriteriaQuery<?> query;
  @Mock CriteriaBuilder builder;
  @Mock Join<BranchStatistic, Branch> branchJoin;
  @Mock Join<Branch, User> userJoin;
  @Mock Path<Long> totalCountPath;

  @BeforeEach
  void setUp() {}

  @Test
  void testBranchIdEquals() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);

    Long branchId = 123L;

    BranchStatisticSpecification.branchIdEquals(branchId).toPredicate(root, query, builder);

    verify(builder).equal(branchJoin.get(Branch_.id), branchId);
  }

  @Test
  void testBranchNameEquals() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);

    String branchName = "testBranch";

    BranchStatisticSpecification.branchNameEquals(branchName).toPredicate(root, query, builder);

    verify(builder).equal(branchJoin.get(Branch_.name), branchName);
  }

  @Test
  void testSearch() {
    @SuppressWarnings("unchecked")
    Root<BranchStatistic> root = mock(Root.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder builder = mock(CriteriaBuilder.class);

    @SuppressWarnings("unchecked")
    Join<BranchStatistic, Branch> branchJoin = mock(Join.class);
    @SuppressWarnings("unchecked")
    Join<Branch, User> userJoin = mock(Join.class);

    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);
    when(branchJoin.join(Branch_.createdByUser, JoinType.LEFT)).thenReturn(userJoin);

    @SuppressWarnings("unchecked")
    Path<String> branchNamePath = mock(Path.class);
    when(branchJoin.get(Branch_.name)).thenReturn(branchNamePath);

    @SuppressWarnings("unchecked")
    Path<String> usernamePath = mock(Path.class);
    when(userJoin.get(User_.username)).thenReturn(usernamePath);

    @SuppressWarnings("unchecked")
    Expression<String> branchNameLower = mock(Expression.class);
    when(builder.lower(branchNamePath)).thenReturn(branchNameLower);

    @SuppressWarnings("unchecked")
    Expression<String> usernameLower = mock(Expression.class);
    when(builder.lower(usernamePath)).thenReturn(usernameLower);

    Predicate branchNameLike = mock(Predicate.class);
    when(builder.like(branchNameLower, "search_term_text")).thenReturn(branchNameLike);

    Predicate usernameLike = mock(Predicate.class);
    when(builder.like(usernameLower, "search_term_text")).thenReturn(usernameLike);

    Predicate orPredicate = mock(Predicate.class);
    when(builder.or(branchNameLike, usernameLike)).thenReturn(orPredicate);

    String searchTerm = "search_term_text";

    Predicate result =
        BranchStatisticSpecification.search(searchTerm).toPredicate(root, query, builder);

    verify(builder).or(branchNameLike, usernameLike);
    Assertions.assertSame(orPredicate, result);
  }

  @Test
  void testCreatedByUserNameEquals() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);
    when(branchJoin.join(Branch_.createdByUser, JoinType.LEFT)).thenReturn(userJoin);

    String createdByUserName = "my_username";

    BranchStatisticSpecification.createdByUserNameEquals(createdByUserName)
        .toPredicate(root, query, builder);

    verify(builder).equal(userJoin.get(User_.username), createdByUserName);
  }

  @Test
  void testDeletedEquals() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);

    Boolean deleted = true;

    BranchStatisticSpecification.deletedEquals(deleted).toPredicate(root, query, builder);

    verify(builder).equal(branchJoin.get(Branch_.deleted), deleted);
  }

  @Test
  void testCreatedBefore() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);

    ZonedDateTime createdBefore = ZonedDateTime.now();

    BranchStatisticSpecification.createdBefore(createdBefore).toPredicate(root, query, builder);

    verify(builder).lessThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdBefore);
  }

  @Test
  void testCreatedAfter() {
    when(root.join(BranchStatistic_.branch, JoinType.LEFT)).thenReturn(branchJoin);

    ZonedDateTime createdAfter = ZonedDateTime.now();

    BranchStatisticSpecification.createdAfter(createdAfter).toPredicate(root, query, builder);

    verify(builder).greaterThanOrEqualTo(branchJoin.get(Branch_.createdDate), createdAfter);
  }

  @Test
  void testEmptyTrue() {
    Boolean empty = true;
    when(root.get(BranchStatistic_.totalCount)).thenReturn(totalCountPath);

    BranchStatisticSpecification.empty(empty).toPredicate(root, query, builder);

    verify(builder).equal(totalCountPath, 0L);
  }

  @Test
  void testEmptyFalse() {
    Boolean empty = false;
    when(root.get(BranchStatistic_.totalCount)).thenReturn(totalCountPath);

    BranchStatisticSpecification.empty(empty).toPredicate(root, query, builder);

    verify(builder).notEqual(totalCountPath, 0L);
  }

  @Test
  void testEmptyNullShouldBehaveAsFalse() {
    when(root.get(BranchStatistic_.totalCount)).thenReturn(totalCountPath);

    BranchStatisticSpecification.empty(null).toPredicate(root, query, builder);

    verify(builder).notEqual(totalCountPath, 0L);
  }
}
