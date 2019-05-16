package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SmartlingClientTest.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class SmartlingClientTest {

    @Value("${l10n.smartling.userIdentifier:#{null}}")
    String userIdentifier;

    @Value("${l10n.smartling.userSecret:#{null}}")
    String userSecret;

    @Value("${test.l10n.smartling.projectId:abc123}")
    String projectId;

    @Value("${test.l10n.smartling.file:demo.properties}")
    String file;

    @Value("${test.l10n.smartling.offset:0}")
    Integer offset;

    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    SmartlingClient smartlingClient = new SmartlingClient(userIdentifier, userSecret);

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(smartlingClient.restTemplate);

    SmartlingTestData smartlingTestData = new SmartlingTestData();

    @Test
    public void testClient() throws SmartlingClientException {
        Assume.assumeNotNull(userIdentifier, userSecret);

        try {
            mockServer
                    .expect(
                            MockRestRequestMatchers
                                    .requestTo("https://api.smartling.com/auth-api/v2/authenticate"))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            objectMapper.writeValueAsString(smartlingTestData.authenticationResponse),
                            MediaType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        try {
            mockServer
                    .expect(
                            MockRestRequestMatchers
                                    .requestTo("https://api.smartling.com/strings-api/v2/projects/abc123/source-strings?fileUri=demo.properties&offset=0"))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            objectMapper.writeValueAsString(smartlingTestData.sourceStringsResponse),
                            MediaType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        SourceStringsResponse testResponse = smartlingClient.getSourceStrings(projectId, file, offset);
        mockServer.verify();
        assertEquals(testResponse.getResponse().getData().getItems().get(0).getHashcode(),
                smartlingTestData.sourceStringsResponse.getResponse().getData().getItems().get(0).getHashcode());
    }
}