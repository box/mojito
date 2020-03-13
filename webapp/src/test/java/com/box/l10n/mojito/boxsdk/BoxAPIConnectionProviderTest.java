package com.box.l10n.mojito.boxsdk;

import com.box.sdk.BoxAPIConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
@RunWith(MockitoJUnitRunner.class)
public class BoxAPIConnectionProviderTest {

    /**
     * logger
     */
    static Logger logger = getLogger(BoxAPIConnectionProviderTest.class);

    @InjectMocks
    BoxAPIConnectionProvider boxAPIConnectionProvider;

    @Mock
    BoxSDKServiceConfigProvider boxSDKServiceConfigProvider;

    @Mock
    BoxSDKJWTProvider boxSDKJWTProvider;

    public BoxAPIConnectionProvider getBoxAPIConnectionProviderMock() throws BoxSDKServiceException {
        BoxAPIConnection boxAPIConnection = Mockito.mock(BoxAPIConnection.class);
        BoxAPIConnection boxAPIConnection2 = Mockito.mock(BoxAPIConnection.class);
        BoxAPIConnectionProvider providerSpy = Mockito.spy(boxAPIConnectionProvider);
        Mockito.doReturn(boxAPIConnection)
                .doReturn(boxAPIConnection2)
                .when(providerSpy).createBoxAPIConnection();
        return providerSpy;
    }

    @Test
    public void testGetConnectionWillCreateBoxAPIConnection() throws Exception {
        BoxSDKServiceConfig config = Mockito.mock(BoxSDKServiceConfigFromProperties.class);
        Mockito.when(boxSDKServiceConfigProvider.getConfig()).thenReturn(config);

        BoxAPIConnectionProvider providerSpy = getBoxAPIConnectionProviderMock();
        BoxAPIConnection connection = providerSpy.getConnection();

        Assert.assertNotNull("A connection is returned", connection);
        Mockito.verify(providerSpy, Mockito.times(1)).createBoxAPIConnection();
    }

    @Test
    public void testGetConnectionWillReuseConnection() throws BoxSDKServiceException {
        BoxSDKServiceConfig config = Mockito.mock(BoxSDKServiceConfigFromProperties.class);
        Mockito.when(boxSDKServiceConfigProvider.getConfig()).thenReturn(config);

        BoxAPIConnectionProvider providerSpy = getBoxAPIConnectionProviderMock();

        logger.debug("Get connection first time will create a new connection");
        BoxAPIConnection connection = providerSpy.getConnection();

        logger.debug("Get connection 2nd time will resuse the last one");
        BoxAPIConnection connection2 = providerSpy.getConnection();

        Assert.assertNotNull("A connection is returned", connection);
        Assert.assertNotNull("A connection is returned", connection2);
        Assert.assertEquals("Connections should be the same", connection, connection2);

        logger.debug("Box API connection should only be created once even though getConnection was called multiple times");
        Mockito.verify(providerSpy, Mockito.times(1)).createBoxAPIConnection();
    }

    @Test
    public void testGetConnectionWillCreateNewConnectionIfConfigChanges() throws BoxSDKServiceException {
        BoxSDKServiceConfig config = Mockito.mock(BoxSDKServiceConfigFromProperties.class);
        Mockito.when(boxSDKServiceConfigProvider.getConfig())
                .thenReturn(config)
                .thenReturn(config)
                .thenReturn(Mockito.mock(BoxSDKServiceConfigFromProperties.class));

        BoxAPIConnectionProvider providerSpy = getBoxAPIConnectionProviderMock();

        logger.debug("Get connection first time will create a new connection");
        BoxAPIConnection connection = providerSpy.getConnection();

        logger.debug("Get connection 2nd time will resuse the last one");
        BoxAPIConnection connection2 = providerSpy.getConnection();

        logger.debug("Get connection 3nd time will have a different config, so should create a new connection");
        BoxAPIConnection connection3 = providerSpy.getConnection();

        Assert.assertNotNull("A connection is returned", connection);
        Assert.assertNotNull("A connection is returned", connection2);
        Assert.assertNotNull("A connection is returned", connection3);

        Assert.assertNotEquals(connection2, connection3);

        logger.debug("Box API connection should only be created twice.  The first time getConnection was called and when config was changed.");
        Mockito.verify(providerSpy, Mockito.times(2)).createBoxAPIConnection();

    }
}
