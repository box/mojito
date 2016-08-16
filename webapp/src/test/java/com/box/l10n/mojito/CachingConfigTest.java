package com.box.l10n.mojito;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author jaurambault
 */
public class CachingConfigTest extends ServiceTestBase {

    int i = 0;

    /**
     * Because the "default" cache is shared, we need to make sure it is empty
     * before testing it. Otherwise, if a value of another type is stored in it,
     * an exception will be thrown.
     */
    @After
    @Before
    @CacheEvict("default")
    public void post() {

    }

    @Test
    public void testCacheable() {
        assertEquals(1, getInt());
        assertEquals(1, getInt());
    }

    @Cacheable("default")
    public int getInt() {
        return ++i;
    }

}
