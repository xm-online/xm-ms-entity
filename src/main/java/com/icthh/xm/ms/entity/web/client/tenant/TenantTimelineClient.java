package com.icthh.xm.ms.entity.web.client.tenant;

import com.icthh.xm.commons.client.feign.AuthorizedFeignClient;
import com.icthh.xm.commons.gen.model.Tenant;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

@AuthorizedFeignClient(
    name = "timeline"
)
public interface TenantTimelineClient extends TenantClient {

    @Override
    default String getName() {
        return "timeline";
    }

    @Override
    @PostMapping({"tenants"})
    void addTenant(@Valid @RequestBody Tenant body);

    @Override
    @GetMapping({"tenants"})
    List<Tenant> getAllTenantInfo();

    @Override
    @GetMapping({"tenants/{tenantKey}"})
    Tenant getTenant(@PathVariable("tenantKey") String tenantKey);

    @Override
    @PutMapping(value = {"tenants/{tenantKey}"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    void manageTenant(@PathVariable("tenantKey") String tenantKey, @RequestPart(value = "status") String status);

    @Override
    @DeleteMapping({"tenants/{tenantKey}"})
    void deleteTenant(@PathVariable("tenantKey") String tenantKey);
}
