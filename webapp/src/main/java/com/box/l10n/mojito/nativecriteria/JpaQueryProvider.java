package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.NativeQueryProvider;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 *
 * @author jeanaurambault
 */
@Configurable
public class JpaQueryProvider implements NativeQueryProvider {

    @Autowired
    EntityManager em;

    @Override
    public NativeQuery getNativeQuery(String sql) {
        return new JpaNativeQuery(sql, em.createNativeQuery(sql));
    }

}
