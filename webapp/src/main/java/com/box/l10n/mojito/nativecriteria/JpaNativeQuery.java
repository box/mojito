package com.box.l10n.mojito.nativecriteria;

import com.box.l10n.mojito.JSR310Migration;
import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.QueryInfo;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 * @author jeanaurambault
 */
public class JpaNativeQuery implements NativeQuery {

  Query query;
  QueryInfo queryInfo;

  public JpaNativeQuery(String sql, Query sqlQuery) {
    this.queryInfo = new QueryInfo(sql);
    this.query = sqlQuery;
  }

  @Override
  public List list() {
    return query.getResultList();
  }

  @Override
  public Object uniqueResult() {
    return query.getSingleResult();
  }

  @Override
  public NativeQuery setMaxResults(int maxResults) {
    query.setMaxResults(maxResults);
    return this;
  }

  @Override
  public NativeQuery setFirstResult(int firstResult) {
    query.setFirstResult(firstResult);
    return this;
  }

  @Override
  public NativeQuery setParameter(String name, Object val) {
    queryInfo.getParameters().put(name, val);
    query.setParameter(name, val);
    return this;
  }

  @Override
  public NativeQuery setString(String name, String value) {
    queryInfo.getParameters().put(name, value);
    query.setParameter(name, value);
    return this;
  }

  @Override
  public NativeQuery setParameterList(String name, Collection values) {
    queryInfo.getParameters().put(name, values);
    query.setParameter(name, values);
    return this;
  }

  @Override
  public NativeQuery setParameterList(String name, Object[] values) {
    queryInfo.getParameters().put(name, values);
    query.setParameter(name, values);
    return this;
  }

  @Override
  public QueryInfo getQueryInfo() {
    return queryInfo;
  }

  public NativeQuery setParameter(String name, ZonedDateTime dateTime) {
    queryInfo.getParameters().put(name, dateTime);
    query.setParameter(name, JSR310Migration.dateTimeToDate(dateTime), TemporalType.TIMESTAMP);
    return this;
  }
}
