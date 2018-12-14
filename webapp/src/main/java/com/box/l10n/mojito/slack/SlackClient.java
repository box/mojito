package com.box.l10n.mojito.slack;

import com.google.common.base.Preconditions;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Slack client based on the Web API: https://api.slack.com/web
 */
public class SlackClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SlackClient.class);

    static final String BASE_API_URL = "https://slack.com/api/";
    static final String API_USER_LOOKUP_BY_EMAIL = "users.lookupByEmail";
    static final String API_IM_OPEN = "im.open";
    static final String API_CHAT_POST_MESSAGE = "chat.postMessage";

    RestTemplate restTemplate = new RestTemplate();
    String authToken = null;

    public SlackClient(String authToken) {
        this.authToken = authToken;
    }

    /**
     * Sends a instant message to a user identified by his email.
     *
     * @param email user's email
     * @param message message to send
     * @throws SlackClientException
     */
    public void sendInstantMessage(String email, String message) throws SlackClientException {
        User user = lookupUserByEmail(email);
        Channel channel = openIm(user);
        BaseResponse baseResponse = chatPostMessage(channel, message);
    }

    User lookupUserByEmail(String email) throws SlackClientException {

        MultiValueMap<String, Object> payload = getBasePayloadMapWithAuthToken();
        payload.add("email", email);

        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityForPayload(payload);
        UserResponse userResponse = restTemplate.postForObject(getUrl(API_USER_LOOKUP_BY_EMAIL), httpEntity, UserResponse.class);

        if (!userResponse.getOk()) {
            String msg = MessageFormat.format("Cannot lookup user by email: {0} ({1})", email, userResponse.getError());
            logger.error(msg);
            throw new SlackClientException(msg);
        }

        return userResponse.getUser();
    }

    Channel openIm(User user) throws SlackClientException {
        Preconditions.checkNotNull(user.getId());

        MultiValueMap<String, Object> payload = getBasePayloadMapWithAuthToken();
        payload.add("user", user.getId());
        payload.add("return_im", "true");

        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityForPayload(payload);

        ImOpenResponse imOpenResponse = restTemplate.postForObject(getUrl(API_IM_OPEN), httpEntity, ImOpenResponse.class);

        if (!imOpenResponse.getOk()) {
            String msg = MessageFormat.format("Cannot open instant message: {0}", imOpenResponse.getError());
            logger.error(msg);
            throw new SlackClientException(msg);
        }

        return imOpenResponse.getChannel();
    }

    BaseResponse chatPostMessage(Channel channel, String text) throws SlackClientException {
        Preconditions.checkNotNull(channel.getId());

        MultiValueMap<String, Object> payload = getBasePayloadMapWithAuthToken();
        payload.add("channel", channel.getId());
        payload.add("text", text);

        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityForPayload(payload);

        BaseResponse postForObject = restTemplate.postForObject(getUrl(API_CHAT_POST_MESSAGE), httpEntity, BaseResponse.class);

        if (!postForObject.getOk()) {
            String msg = MessageFormat.format("Cannot post message in chat: {0}", postForObject.getError());
            logger.error(msg);
            throw new SlackClientException(msg);
        }

        return postForObject;
    }

    MultiValueMap<String, Object> getBasePayloadMapWithAuthToken() {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("token", authToken);
        return map;
    }

    HttpHeaders getBaseHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    HttpEntity<MultiValueMap<String, Object>> getHttpEntityForPayload(MultiValueMap<String, Object> payload) {
        return new HttpEntity<>(payload, getBaseHttpHeaders());
    }

    String getUrl(String subpath) {
        return BASE_API_URL + subpath;
    }
}