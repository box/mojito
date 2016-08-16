package com.box.l10n.mojito.common;

import com.box.l10n.mojito.entity.BaseEntity;
import static org.mockito.Mockito.when;
import org.springframework.data.jpa.repository.JpaRepository;
import static org.mockito.Mockito.mock;

/**
 * Utility class to create mocks.
 * 
 * @author jaurambault
 */
public class Mocks {

    /**
     * Creates a simple mock for a {@link JpaRepository} that mocks the 
     * {@link JpaRepository#getOne(java.io.Serializable)} to return a base
     * entity that only has its id set.
     * 
     * @param <U> type of repository
     * @param <T> type of repository entity
     * @param repositoryClass class of repository to be created
     * @param entityClass class of the entity to be created and returned by that mock
     * @param id the id that will be set on the created entity
     * @return 
     */
    static public <U extends JpaRepository<T, Long>, T extends BaseEntity> U getJpaRepositoryMockForGetOne(Class<U> repositoryClass, Class<T> entityClass, Long id) {

        try {
            T baseEntity = entityClass.newInstance();
            baseEntity.setId(id);

            U mock = mock(repositoryClass);
            when(mock.getOne(id)).thenReturn(baseEntity);

            return mock;

        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Can't create mock for repository", e);
        }
    }
}
