package com.icthh.xm.ms.entity.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The {@link XmEntityUtils} class.
 */
@UtilityClass
public class XmEntityUtils {

    public static XmEntity getRequiredLinkedTarget(XmEntity xmEntity, String linkTypeKey, String linkTargetTypeKey) {
        Optional<Link> firstLink = findFirstLink(xmEntity, linkTypeKey);
        if (firstLink.isEmpty()) {
            throw new IllegalArgumentException("Link with type key '" + linkTypeKey + "' are required");
        }
        Link link = firstLink.get();
        XmEntity target = Objects.requireNonNull(link.getTarget(), "Link with type key '" + linkTypeKey + "' has null target");
        if (Objects.equals(target.getTypeKey(), linkTargetTypeKey)) {
            return target;
        }

        throw new IllegalArgumentException("Link with type key '" + linkTypeKey + "' expected target with type key '" +
                                               linkTargetTypeKey + "' but actual '" + target.getTypeKey() + "'");
    }

    public static Optional<XmEntity> getLinkedTarget(XmEntity xmEntity, String linkTypeKey, String linkTargetTypeKey) {
        Optional<Link> firstLink = findFirstLink(xmEntity, linkTypeKey);
        if (firstLink.isPresent()) {
            Link link = firstLink.get();
            XmEntity target = Objects.requireNonNull(link.getTarget(), "Link with type key '" + linkTypeKey + "' has null target");
            if (Objects.equals(target.getTypeKey(), linkTargetTypeKey)) {
                return Optional.of(target);
            }
        }

        return Optional.empty();
    }

    public static XmEntity getRequiredTargetByLink(XmEntity xmEntity, String linkTypeKey) {
        Optional<Link> firstLink = findFirstLink(xmEntity, linkTypeKey);
        if (firstLink.isEmpty()) {
            throw new IllegalArgumentException("Link with type key '" + linkTypeKey + "' are required");
        }
        Link link = firstLink.get();
        return Objects.requireNonNull(link.getTarget(), "Link with type key '" + linkTypeKey + "' has null target");
    }

    public static Link findFirstRequiredLink(XmEntity xmEntity, String linkTypeKey) {
        Optional<Link> firstLink = findFirstLink(xmEntity, linkTypeKey);
        if (firstLink.isEmpty()) {
            throw new IllegalArgumentException("Link with type key '" + linkTypeKey + "' are required");
        }
        return firstLink.get();
    }

    private static Optional<Link> findFirstLink(XmEntity xmEntity, String linkTypeKey) {
        return getLinks(xmEntity, linkTypeKey).findFirst();
    }

    private static Stream<Link> getLinks(XmEntity xmEntity, String linkTypeKey) {
        Objects.requireNonNull(linkTypeKey, "linkTypeKey can't be null");

        return xmEntity.getTargets().stream().filter(link -> Objects.equals(linkTypeKey, link.getTypeKey()));
    }

    public static Optional<XmEntity> getLinkedTarget(XmEntity xmEntity, String targetTypeKey) {
        Optional<Link> linkOpt = getLink(xmEntity, targetTypeKey);
        return linkOpt.map(Link::getTarget);
    }

    // XmEntity -> link's -> (filter by: target.typeKey) -> target
    private static Optional<Link> getLink(XmEntity xmEntity, String targetTypeKey) {
        Objects.requireNonNull(targetTypeKey, "targetTypeKey can't be null");

        return xmEntity.getTargets().stream().filter(link -> {
            XmEntity target = link.getTarget();
            return (target != null) && Objects.equals(targetTypeKey, target.getTypeKey());
        }).findFirst();
    }

    public static XmEntity getRequiredLinkedTarget(XmEntity xmEntity, String targetTypeKey) {
        Optional<Link> linkOpt = getLink(xmEntity, targetTypeKey);
        XmEntity target = linkOpt.orElseThrow(() -> new BusinessException("Can't find linkOpt with target type key: "
                                                                                    + targetTypeKey)).getTarget();
        return Objects.requireNonNull(target, "Target in link with typeKey: " + targetTypeKey + " is null");
    }

    public static <T> Optional<T> getDataParam(XmEntity xmEntity, String name, Class<T> valueType) {
        Objects.requireNonNull(xmEntity, "xmEntity can't be null");
        Map<String, Object> data = xmEntity.getData();
        return (data != null) ? Optional.ofNullable(valueType.cast(data.get(name))) : Optional.empty();
    }

    private static <T> T getDataParam(XmEntity xmEntity,
                                      String name,
                                      Class<T> valueType,
                                      boolean required,
                                      String msgTemplate) {
        Optional<T> dataParam = getDataParam(xmEntity, name, valueType);
        if (required) {
            return dataParam.orElseThrow(() -> {
                String message = (msgTemplate != null) ? String.format(msgTemplate, name)
                    : "Entity has no data parameter: " + name;
                return new IllegalArgumentException(message);
            });
        }
        return dataParam.orElse(null);
    }

    public static <T> T getRequiredDataParam(XmEntity xmEntity, String name, Class<T> valueType) {
        return getDataParam(xmEntity, name, valueType, true, null);
    }

    public static <T> T getRequiredDataParam(XmEntity xmEntity, String name, Class<T> valueType, String msgTemplate) {
        return getDataParam(xmEntity, name, valueType, true, msgTemplate);
    }

}
