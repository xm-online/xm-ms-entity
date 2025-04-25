package com.icthh.xm.ms.entity.service.storage;

import org.springframework.core.io.Resource;

import java.net.URI;

public record AvatarStorageResponse(Resource avatarResource, URI uri) {


    public AvatarStorageResponse {
        if (avatarResource == null && uri == null) {
            throw new RuntimeException("No fields specified");
        }
        if (uri == null) {
            throw new RuntimeException("Uri should be provided");
        }
    }

    public static AvatarStorageResponse withRedirectUrl(URI redirectUrl) {
        return new AvatarStorageResponse(null, redirectUrl);
    }

    public static AvatarStorageResponse withResource(Resource avatarResource, URI uri) {
        return new AvatarStorageResponse(avatarResource, uri);
    }

}
