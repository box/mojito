package com.box.l10n.mojito.db;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class HibernateInfoHolder {

  private static Metadata metadata;

  private static SessionFactoryImplementor sessionFactory;

  private static SessionFactoryServiceRegistry serviceRegistry;

  public static Metadata getMetadata() {
    return metadata;
  }

  public static void setMetadata(Metadata metadata) {
    HibernateInfoHolder.metadata = metadata;
  }

  public static SessionFactoryImplementor getSessionFactory() {
    return sessionFactory;
  }

  public static void setSessionFactory(SessionFactoryImplementor sessionFactory) {
    HibernateInfoHolder.sessionFactory = sessionFactory;
  }

  public static SessionFactoryServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public static void setServiceRegistry(SessionFactoryServiceRegistry serviceRegistry) {
    HibernateInfoHolder.serviceRegistry = serviceRegistry;
  }
}
