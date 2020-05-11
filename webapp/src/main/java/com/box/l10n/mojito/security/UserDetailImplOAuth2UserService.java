package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Mostly a copy of {@link DefaultOAuth2UserService} that returns a {@link UserDetailsImpl} as principal.
 * <p>
 * Also it adds an option to unwrap the user attributes if {@link SecurityConfig.OAuth2#unwrapUserAttributes} is
 * provided. This allows to process payloads similar to
 * <p>
 * * <pre>
 *  *  {'user': {
 *  *         "username": "xyz",
 *  *         "first_name": "X",
 *  *         "last_name": "Y",
 *  *         "email": "xyz@test.com"
 *  *  }}
 *  * </pre>
 */
public class UserDetailImplOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    static final Logger logger = LoggerFactory.getLogger(UserDetailImplOAuth2UserService.class);

    private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";
    private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";
    private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";
    private static final ParameterizedTypeReference<Map<String, Object>> PARAMETERIZED_RESPONSE_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {
            };
    private Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter = new OAuth2UserRequestEntityConverter();

    private RestOperations restOperations;

    private SecurityConfig securityConfig;

    private UserService userService;

    public UserDetailImplOAuth2UserService(SecurityConfig securityConfig, UserService userService) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.restOperations = restTemplate;

        // This is the part of the implementation that diverge from {@link DefaultOAuth2UserService}
        this.securityConfig = securityConfig;
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");

        if (!StringUtils.hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
            OAuth2Error oauth2Error = new OAuth2Error(
                    MISSING_USER_INFO_URI_ERROR_CODE,
                    "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: " +
                            userRequest.getClientRegistration().getRegistrationId(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (!StringUtils.hasText(userNameAttributeName)) {
            OAuth2Error oauth2Error = new OAuth2Error(
                    MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
                    "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: " +
                            userRequest.getClientRegistration().getRegistrationId(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        RequestEntity<?> request = this.requestEntityConverter.convert(userRequest);

        ResponseEntity<Map<String, Object>> response;
        try {
            response = this.restOperations.exchange(request, PARAMETERIZED_RESPONSE_TYPE);
        } catch (OAuth2AuthorizationException ex) {
            OAuth2Error oauth2Error = ex.getError();
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append("Error details: [");
            errorDetails.append("UserInfo Uri: ").append(
                    userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
            errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
            if (oauth2Error.getDescription() != null) {
                errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
            }
            errorDetails.append("]");
            oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + errorDetails.toString(), null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        } catch (RestClientException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        }

        Map<String, Object> userAttributes = response.getBody();

        // This is the part of the implementation that diverge from {@link DefaultOAuth2UserService}
        logger.debug("user attributes: {}", userAttributes);
        return getOAuth2UserDetailsImpl(userRequest, userNameAttributeName, userAttributes);
    }


    OAuth2User getOAuth2UserDetailsImpl(OAuth2UserRequest userRequest, String userNameAttributeName, Map<String, Object> userAttributes) {

        logger.debug("Get OAuth2UserDetailsImpl user for client registration: {}", userRequest.getClientRegistration().getRegistrationId());

        SecurityConfig.OAuth2 oAuth2ConfigForRegistration = securityConfig.getoAuth2().getOrDefault(userRequest.getClientRegistration().getRegistrationId(), new SecurityConfig.OAuth2());

        userAttributes = optionallyUnwrapUserAttributes(userAttributes, oAuth2ConfigForRegistration);

        String username = getUsername(userRequest, userNameAttributeName, userAttributes);
        String givenName = (String) userAttributes.get(oAuth2ConfigForRegistration.getGivenNameAttribute());
        String surname = (String) userAttributes.get(oAuth2ConfigForRegistration.getSurnameAttribute());
        String commonName = (String) userAttributes.get(oAuth2ConfigForRegistration.getCommonNameAttribute());

        User user = userService.getOrCreateOrUpdateBasicUser(username, givenName, surname, commonName);
        return new OAuth2UserDetailsImpl(user, userAttributes);
    }

    String getUsername(OAuth2UserRequest userRequest, String userNameAttributeName, Map<String, Object> userAttributes) {
        Object usernameObject = userAttributes.get(userNameAttributeName);

        if (usernameObject == null) {
            throw new IllegalArgumentException("Missing required user name (attribute: " + userNameAttributeName + ") for Client Registration: " +
                    userRequest.getClientRegistration().getRegistrationId());
        }

        return usernameObject.toString();
    }

    Map<String, Object> optionallyUnwrapUserAttributes(Map<String, Object> userAttributes, SecurityConfig.OAuth2 oAuth2ConfigForRegistration) {
        if (oAuth2ConfigForRegistration.getUnwrapUserAttributes() != null) {
            logger.debug("Unwrap user attributes using key: {}", oAuth2ConfigForRegistration.getUnwrapUserAttributes());

            String unwrapUserAttributes = oAuth2ConfigForRegistration.getUnwrapUserAttributes();

            if (!userAttributes.containsKey(unwrapUserAttributes)) {
                throw new IllegalArgumentException("Missing attribute '" + unwrapUserAttributes + "' in attributes for unwrapping user attributes");
            }

            userAttributes = (Map<String, Object>) userAttributes.get(unwrapUserAttributes);
        }

        return userAttributes;
    }
}
