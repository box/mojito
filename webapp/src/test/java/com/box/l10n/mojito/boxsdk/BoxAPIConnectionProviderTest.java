package com.box.l10n.mojito.boxsdk;

import com.box.sdk.BoxAPIConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
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

    public BoxSDKServiceConfig getTestConfig() {
        BoxSDKServiceConfig config = Mockito.spy(BoxSDKServiceConfigFromProperties.class);
        Mockito.when(config.getClientId()).thenReturn("1");
        Mockito.when(config.getClientSecret()).thenReturn("2");
        Mockito.when(config.getPublicKeyId()).thenReturn("3");
        Mockito.when(config.getPrivateKey()).thenReturn("4");
        Mockito.when(config.getPrivateKeyPassword()).thenReturn("4");
        Mockito.when(config.getEnterpriseId()).thenReturn("5");
        Mockito.when(config.getAppUserId()).thenReturn("6");
        Mockito.when(config.getRootFolderId()).thenReturn("7");
        Mockito.when(config.getDropsFolderId()).thenReturn("8");
        Mockito.when(config.getProxyHost()).thenReturn(null);
        Mockito.when(config.getProxyPort()).thenReturn(null);
        Mockito.when(config.getProxyUser()).thenReturn(null);
        Mockito.when(config.getProxyPassword()).thenReturn(null);

        return config;
    }

    public BoxSDKServiceConfig getTestConfig2() {
        BoxSDKServiceConfig config = Mockito.spy(BoxSDKServiceConfigFromProperties.class);
        Mockito.when(config.getClientId()).thenReturn("a");
        Mockito.when(config.getClientSecret()).thenReturn("b");
        Mockito.when(config.getPublicKeyId()).thenReturn("c");
        Mockito.when(config.getPrivateKey()).thenReturn("d");
        Mockito.when(config.getPrivateKeyPassword()).thenReturn("e");
        Mockito.when(config.getEnterpriseId()).thenReturn("f");
        Mockito.when(config.getAppUserId()).thenReturn("g");
        Mockito.when(config.getRootFolderId()).thenReturn("h");
        Mockito.when(config.getDropsFolderId()).thenReturn("i");
        Mockito.when(config.getProxyHost()).thenReturn(null);
        Mockito.when(config.getProxyPort()).thenReturn(null);
        Mockito.when(config.getProxyUser()).thenReturn(null);
        Mockito.when(config.getProxyPassword()).thenReturn(null);

        return config;
    }



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
        BoxSDKServiceConfig config = getTestConfig();
        Mockito.when(boxSDKServiceConfigProvider.getConfig()).thenReturn(config);

        BoxAPIConnectionProvider providerSpy = getBoxAPIConnectionProviderMock();
        BoxAPIConnection connection = providerSpy.getConnection();

        Assert.assertNotNull("A connection is returned", connection);
        Mockito.verify(providerSpy, Mockito.times(1)).createBoxAPIConnection();
    }

    @Test
    public void testGetConnectionWillReuseConnection() throws BoxSDKServiceException {
        BoxSDKServiceConfig config = getTestConfig();
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
        BoxSDKServiceConfig config = getTestConfig();
        Mockito.when(boxSDKServiceConfigProvider.getConfig())
                .thenReturn(config)
                .thenReturn(config)
                .thenReturn(getTestConfig2());

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

    // TODO testGetConnectionWillCreateProxiedConnection()
    // TODO testGetConnectionWillCreateAuthenticatedProxiedConnection()
}
