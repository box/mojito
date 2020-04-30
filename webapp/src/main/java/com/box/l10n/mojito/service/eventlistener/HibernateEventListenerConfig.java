package com.box.l10n.mojito.service.eventlistener;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 * Registers hibernate event listeners.
 *
 * @author jyi
 */
@Component
public class HibernateEventListenerConfig {

    @PersistenceUnit
    EntityManagerFactory emf;

    @Autowired
    EntityCrudEventListener entityCrudEventListener;

    @PostConstruct
    public void registerListeners() {
        // TODO(spring2) must test that

        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = (EventListenerRegistry)sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_COMMIT_INSERT).appendListener(entityCrudEventListener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_UPDATE).appendListener(entityCrudEventListener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_DELETE).appendListener(entityCrudEventListener);
    }
    
}
