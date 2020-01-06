package com.box.l10n.mojito.service.eventlistener;

import javax.annotation.PostConstruct;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Registers hibernate event listeners.
 * 
 * @author jyi
 */
@Component
public class HibernateEventListenerConfig {

    @Autowired
    LocalContainerEntityManagerFactoryBean lcemfb;
    
    @Autowired
    EntityCrudEventListener entityCrudEventListener;

    @PostConstruct
    public void registerListeners() {
        EntityManagerFactoryImpl emf = (EntityManagerFactoryImpl) lcemfb.getNativeEntityManagerFactory();
        SessionFactoryImplementor sf = emf.getSessionFactory();
        EventListenerRegistry registry = sf.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_COMMIT_INSERT).appendListener(entityCrudEventListener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_UPDATE).appendListener(entityCrudEventListener);
        registry.getEventListenerGroup(EventType.POST_COMMIT_DELETE).appendListener(entityCrudEventListener);
    }
    
}
