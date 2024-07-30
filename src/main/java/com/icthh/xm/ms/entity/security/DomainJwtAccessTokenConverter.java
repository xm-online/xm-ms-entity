//package com.icthh.xm.ms.entity.security;
//
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_ADDITIONAL_DETAILS;
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_LOGINS_KEY;
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_TENANT_KEY;
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_USER_KEY;
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_ROLE_KEY;
//import static com.icthh.xm.ms.entity.config.Constants.AUTH_AUTHORITIES;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.SneakyThrows;
//import org.springframework.security.oauth2.provider.OAuth2Authentication;
//import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Overrides to get token tenant.
// */
//@Deprecated
//public class DomainJwtAccessTokenConverter extends JwtAccessTokenConverter {
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    @Override
//    @SneakyThrows
//    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
//
//        final OAuth2Authentication authentication = super.extractAuthentication(map);
//
//        final Map<String, Object> details = new HashMap<>();
//        details.put(AUTH_TENANT_KEY, map.get(AUTH_TENANT_KEY));
//        details.put(AUTH_USER_KEY, map.get(AUTH_USER_KEY));
//
//        details.put(AUTH_ROLE_KEY, map.get(AUTH_ROLE_KEY));
//        details.put(AUTH_AUTHORITIES, mapper.writeValueAsString(map.get(AUTH_AUTHORITIES)));
//
//        details.put(AUTH_LOGINS_KEY, mapper.writeValueAsString(map.get(AUTH_LOGINS_KEY)));
//        details.put(AUTH_ADDITIONAL_DETAILS, map.get(AUTH_ADDITIONAL_DETAILS));
//        authentication.setDetails(details);
//
//        return authentication;
//    }
//}
