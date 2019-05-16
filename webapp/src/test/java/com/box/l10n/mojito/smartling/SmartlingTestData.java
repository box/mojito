package com.box.l10n.mojito.smartling;

        import com.box.l10n.mojito.smartling.request.*;
        import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
        import com.box.l10n.mojito.smartling.response.BaseResponse;
        import com.box.l10n.mojito.smartling.response.SourceStringsResponse;

        import java.util.Collections;

public class SmartlingTestData {
    public AuthenticationData authenticationData = new AuthenticationData();
    public AuthenticationObject authenticationObject = new AuthenticationObject();
    public AuthenticationResponse authenticationResponse = new AuthenticationResponse();

    public StringInfo stringInfo = new StringInfo();
    public StringData stringData = new StringData();
    public SourceStringsObject sourceStringsObject = new SourceStringsObject();
    public SourceStringsResponse sourceStringsResponse = new SourceStringsResponse();

    public SmartlingTestData() {
        authenticationData.setTokenType("bearer");
        authenticationData.setRefreshToken("refresh");
        authenticationData.setAccessToken("access");
        authenticationData.setRefreshExpiresIn(1);
        authenticationData.setExpiresIn(1);

        authenticationObject.setCode(BaseResponse.API_SUCCESS_CODE);
        authenticationObject.setErrors(new Errors());
        authenticationObject.setData(authenticationData);

        authenticationResponse.setResponse(authenticationObject);

        stringInfo.setStringVariant("fake_for_test#@#TEST1");
        stringInfo.setStringText("Content1");
        stringInfo.setParsedStringText("Content1");
        stringInfo.setHashcode("hashcode");
        stringInfo.setKeys(Collections.singletonList(new Key()));

        stringData.setItems(Collections.singletonList(stringInfo));
        stringData.setTotalCount(1);

        sourceStringsObject.setCode(BaseResponse.API_SUCCESS_CODE);
        sourceStringsObject.setErrors(new Errors());
        sourceStringsObject.setData(stringData);

        sourceStringsResponse.setResponse(sourceStringsObject);
    }
}