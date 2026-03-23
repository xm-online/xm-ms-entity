package com.icthh.xm.ms.entity.service.mapper;

/**
 * Base class for mappers that need Hibernate lazy-loading awareness.
 * Shared mapping methods (shallowXmEntityToDto/Entity) are in {@link XmEntityRefMapper}
 * to avoid MapStruct ambiguity when mappers use other mappers via {@code uses}.
 */
public class LazyLoadingAwareMapper {

}
