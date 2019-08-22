package com.box.l10n.mojito.common;

import com.box.l10n.mojito.entity.BaseEntity;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.function.Function;

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

    /**
     * Like {@link org.mockito.Matchers.argThat} but with lambda support.
     *
     * Not needed with never Mockito (staying with spring version for now)
     *
     * @param fn
     * @param <R>
     * @return
     */
    public static <R> R argThatFn(Function<R, Boolean> fn) {
        ArgumentMatcher<R> argumentMatcher = new ArgumentMatcher<R>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("argThatFn");
            }

            @Override
            public boolean matches(Object item) {
                return fn.apply((R)item);
            }
        };

        return argThat(argumentMatcher);
    }
}
