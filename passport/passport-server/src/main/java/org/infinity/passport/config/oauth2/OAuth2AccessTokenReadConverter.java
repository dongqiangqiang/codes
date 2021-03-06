package org.infinity.passport.config.oauth2;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.mongodb.DBObject;

/**
 * Converter to deserialize back into an OAuth2AccessToken Object made necessary because
 * Spring Mongo can't map oAuth2AccessToken to OAuth2AccessToken.
 */
public class OAuth2AccessTokenReadConverter implements Converter<DBObject, OAuth2AccessToken> {

    @SuppressWarnings({ "unchecked" })
    @Override
    public OAuth2AccessToken convert(DBObject source) {
        DefaultOAuth2AccessToken oAuth2AccessToken = new DefaultOAuth2AccessToken((String) source.get("value"));
        oAuth2AccessToken.setExpiration((Date) source.get("expiration"));
        oAuth2AccessToken.setTokenType((String) source.get("tokenType"));

        DBObject refreshToken = (DBObject) source.get("refreshToken");
        DefaultExpiringOAuth2RefreshToken oAuth2RefreshToken = new DefaultExpiringOAuth2RefreshToken(
                (String) refreshToken.get("value"), (Date) refreshToken.get("expiration"));
        oAuth2AccessToken.setRefreshToken(oAuth2RefreshToken);

        oAuth2AccessToken.setScope(new HashSet<>((List<String>) source.get("scope")));

        oAuth2AccessToken.setAdditionalInformation((Map<String, Object>) source.get("additionalInformation"));

        return oAuth2AccessToken;
    }

}
