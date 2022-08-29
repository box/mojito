package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.rest.WSTestBase;

/**
 * Base class for testing services, compared to {@link WSTestBase} the in-memory http server
 * shouldn't be started.
 *
 * <p>TODO(P1) Review test issue
 *
 * <p>For now because of issue with different test context and session/entityManager Use same conf
 * as {@link WSTestBase}.
 *
 * <p>When replacing usage of entityManager by a repository the test seems to work, either the
 * entity manager are different (seemed to be the same though) or there are something additional in
 * the spring wrapper.
 *
 * @author jaurambault
 */
public class ServiceTestBase extends WSTestBase {}
