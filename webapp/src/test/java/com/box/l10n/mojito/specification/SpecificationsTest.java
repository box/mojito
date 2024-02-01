package com.box.l10n.mojito.specification;

import static com.box.l10n.mojito.rest.repository.RepositorySpecification.deletedEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.springframework.data.jpa.domain.Specification.where;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Arrays;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class SpecificationsTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(Specifications.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryService repositoryService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testIfParamNotNull() throws RepositoryNameAlreadyUsedException {

    long numberOfRepositories = repositoryRepository.count();
    long numberOfDeletedRepositories = findAllDeleted().size();

    Repository repository1 =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository1"));
    Repository repository2 =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository2"));
    repository2.setDeleted(true);
    List<Repository> repositories = Arrays.asList(repository1, repository2);
    repositoryRepository.saveAll(repositories);

    long expectedNumberOfRepositories = numberOfRepositories + repositories.size();
    long expectedNumberOfNotDeletedRepositories =
        numberOfRepositories - numberOfDeletedRepositories + 1;

    List<Repository> findAll = findAll(null, null);
    Assert.assertEquals(expectedNumberOfRepositories, findAll.size());

    findAll = findAll(null, repository1.getName());
    Assert.assertEquals(1, findAll.size());
    Assert.assertEquals(repository1.getName(), findAll.get(0).getName());

    findAll = findAll(repository2.getName(), null);
    Assert.assertEquals(1, findAll.size());
    Assert.assertEquals(repository2.getName(), findAll.get(0).getName());

    findAll = findAll(repository1.getName(), repository2.getName());
    Assert.assertEquals(0, findAll.size());

    findAll = repositoryRepository.findByDeletedFalseOrderByNameAsc();
    Assert.assertEquals(expectedNumberOfNotDeletedRepositories, findAll.size());

    findAll = findIfNotDeleted(repository1.getName());
    Assert.assertEquals(1, findAll.size());
    Assert.assertEquals(repository1.getName(), findAll.get(0).getName());

    findAll = findIfNotDeleted(repository2.getName());
    Assert.assertEquals(0, findAll.size());

    findAll = findIfNotDeleted(null);
    Assert.assertEquals(expectedNumberOfNotDeletedRepositories, findAll.size());
  }

  private List<Repository> findAll(String name1, String name2) {

    logger.debug("{}, {}", name1, name2);
    List<Repository> findAll =
        repositoryRepository.findAll(
            where(ifParamNotNull(nameLike(name1))).and(ifParamNotNull(nameLike(name2))));

    return findAll;
  }

  private List<Repository> findAllDeleted() {

    logger.debug("all deleted");
    List<Repository> findAll = repositoryRepository.findAll(where(deletedEquals(true)));

    return findAll;
  }

  private List<Repository> findIfNotDeleted(String name) {

    logger.debug("{}, if not deleted", name);
    List<Repository> findAll =
        repositoryRepository.findAll(
            where(ifParamNotNull(nameLike(name))).and(deletedEquals(false)));

    return findAll;
  }

  public static SingleParamSpecification<Repository> nameLike(final String name) {

    return new SingleParamSpecification<Repository>(name) {

      public Predicate toPredicate(
          Root<Repository> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return builder.like(root.<String>get("name"), name);
      }
    };
  }
}
