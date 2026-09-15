package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.Map;

/**
 * Published once per tenant by {@code XmEntitySpecService.refreshFinished}, after the entity spec of that
 * tenant has been rebuilt. Listeners run synchronously in the publishing thread and inside the tenant context.
 *
 * <p>Listen to this instead of implementing {@code XmEntitySpecService.EntitySpecUpdateListener} when the
 * listener also needs the spec service itself: the service collects its listeners, so the two beans would
 * depend on each other while being constructed.
 *
 * @param tenantKey tenant whose spec was refreshed
 * @param specs     type key to type spec of that tenant, as just rebuilt
 */
public record XmEntitySpecUpdatedEvent(String tenantKey, Map<String, TypeSpec> specs) {
}
