package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.LinkPermittedRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class LinkServiceUnitTest {

    private LinkService linkService;

    private LinkRepository linkRepository;

    private XmEntityRepository xmEntityRepository;

    private XmEntitySpecService xmEntitySpecService;

    private  LinkPermittedRepository permittedRepository;

    private  PermittedSearchRepository permittedSearchRepository;

    private  StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Before
    public void init() {

        xmEntitySpecService = mock(XmEntitySpecService.class);

        linkRepository = mock(LinkRepository.class);
        xmEntityRepository = mock(XmEntityRepository.class);
        permittedRepository = mock(LinkPermittedRepository.class);
        permittedSearchRepository = mock(PermittedSearchRepository.class);
        startUpdateDateGenerationStrategy = mock(StartUpdateDateGenerationStrategy.class);
        linkService = new LinkService(linkRepository, permittedRepository,
            permittedSearchRepository, startUpdateDateGenerationStrategy, xmEntityRepository, xmEntitySpecService);

    }

    @Test
    public void saveLinkWithId() {
        Link link = preparationForSavingLink(true,0,0);
        assertThat(link).isEqualTo(linkService.save(link));
        verify(linkRepository).save(link);
        verify(xmEntityRepository).getOne(entityId(link.getTarget()));
        verify(xmEntityRepository).getOne(entityId(link.getSource()));
    }

    @Test
    public void saveLinkWithOutId() {
        Link link = preparationForSavingLink(false,0,3);
        assertThat(link).isEqualTo(linkService.save(link));
        verify(linkRepository).save(link);
        verify(xmEntityRepository).findStateProjectionById(link.getSource().getId());
        verify(xmEntitySpecService).findLink(link.getSource().getTypeKey(), link.getTypeKey());
        verify(linkRepository).countBySourceIdAndTypeKey(link.getSource().getId(), link.getTypeKey());
        verify(xmEntityRepository).getOne(entityId(link.getTarget()));
        verify(xmEntityRepository).getOne(entityId(link.getSource()));
    }

    @Test(expected = BusinessException.class)
    public void saveLinkOutOfMaxValue() {
        Link link = preparationForSavingLink(false,3,3);
        linkService.save(link);
    }

    private Link preparationForSavingLink(boolean isWithId, int currentValueOfLink, int maxValueOfLink) {
        XmEntity target = new XmEntity().typeKey("TYPE1");
        target.setId(1L);
        XmEntity source = new XmEntity().typeKey("TYPE2");
        source.setId(2L);
        Link link = new Link().typeKey("LINK1");
        link.setSource(source);
        link.setTarget(target);
        if (isWithId) {
            link.setId(1L);
            when(linkRepository.save(link)).thenReturn(link);
            when(xmEntityRepository.getOne(entityId(link.getTarget()))).thenReturn(link.getTarget());
            when(xmEntityRepository.getOne(entityId(link.getSource()))).thenReturn(link.getSource());
            return link;
        }
        when(xmEntityRepository.findStateProjectionById(link.getSource().getId())).thenReturn(getXmEntityStateProjection(source.getTypeKey()));
        when(xmEntitySpecService.findLink(source.getTypeKey(), link.getTypeKey())).thenReturn(createLinkSpeckOptional(maxValueOfLink));
        when(linkRepository.countBySourceIdAndTypeKey(link.getSource().getId(), link.getTypeKey())).thenReturn(currentValueOfLink);
        when(linkRepository.save(link)).thenReturn(link);
        when(xmEntityRepository.getOne(entityId(link.getTarget()))).thenReturn(link.getTarget());
        when(xmEntityRepository.getOne(entityId(link.getSource()))).thenReturn(link.getSource());
        return link;
    }

    private Long entityId(XmEntity entity) {
        Long id = entity.getId();
        if (id == null) {
            id = xmEntityRepository.save(entity).getId();
        }
        return id;
    }

    private Optional<LinkSpec> createLinkSpeckOptional(int maxValue) {
        LinkSpec linkSpec = new LinkSpec();
        linkSpec.setMax(maxValue);
        return Optional.of(linkSpec);
    }

    private XmEntityStateProjection getXmEntityStateProjection(String typeKey) {
        return new XmEntityStateProjection() {
               @Override
               public String getStateKey() {
                   return null;
               }

               @Override
               public Long getId() {
                   return null;
               }

               @Override
               public String getKey() {
                   return null;
               }

               @Override
               public String getTypeKey() {
                   return typeKey;
               }
           };
    }
}

