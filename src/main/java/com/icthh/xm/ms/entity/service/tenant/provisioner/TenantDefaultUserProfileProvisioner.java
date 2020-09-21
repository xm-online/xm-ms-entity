package com.icthh.xm.ms.entity.service.tenant.provisioner;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDefaultUserProfileProvisioner implements TenantProvisioner {

    private final ProfileService profileService;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Create profile for default user.
     *
     * @param tenant the tenant
     */
    @Override
    public void createTenant(final Tenant tenant) {

        String tenantKey = tenant.getTenantKey().toUpperCase();
        tenantContextHolder.getPrivilegedContext()
                           .execute(buildTenant(tenantKey),
                                    () -> profileService.save(buildProfileForDefaultUser(tenantKey)));
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with default user profile during manage tenant: {}, state = {}", tenantKey, state);
    }

    @Override
    public void deleteTenant(final String tenantKey) {
        log.info("Nothing to do with default user profile during manage tenant: {}", tenantKey);
    }

    private Profile buildProfileForDefaultUser(String tenantKey) {
        XmEntity entity = new XmEntity();
        entity.setTypeKey("ACCOUNT.USER");
        entity.setKey("ACCOUNT.USER-1");
        entity.setName("Administrator");
        entity.setStateKey(EntityState.NEW.name());
        entity.setStartDate(Instant.now());
        entity.setUpdateDate(Instant.now());
        entity.setCreatedBy(Constants.SYSTEM_ACCOUNT);

        Profile profile = new Profile();
        profile.setXmentity(entity);
        profile.setUserKey(tenantKey.toLowerCase());

        log.info("create profile: {}", profile);

        return profile;
    }

}
