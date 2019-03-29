package com.icthh.xm.ms.entity.service;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @Test(expected = BusinessException.class)
    public void testLinkOutOfMaxValue() {

        XmEntity target = new XmEntity().typeKey("TYPE1");
        XmEntity source = new XmEntity().typeKey("TYPE2");

        Link link = new Link().typeKey("LINK1").target(target).source(source);
        Link link2 = new Link().typeKey("LINK1").target(target).source(source);
        Link linkOutOfMaxValue = new Link().typeKey("LINK1").target(target).source(source);

        XmEntityStateProjection sd = new XmEntityStateProjection() {
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
               return source.getTypeKey();
           }
       };
        when(xmEntityRepository.findStateProjectionById(link.getSource().getId())).thenReturn(sd);
        when(xmEntityRepository.findStateProjectionById(link2.getSource().getId())).thenReturn(sd);
        when(xmEntityRepository.findStateProjectionById(linkOutOfMaxValue.getSource().getId())).thenReturn(sd);
        LinkSpec linkSpec = new LinkSpec();
        linkSpec.setMax(2);
        Optional sdc = Optional.of(linkSpec);
        when(xmEntitySpecService.findLink(source.getTypeKey(),link.getTypeKey())).thenReturn(sdc);
        when(xmEntitySpecService.findLink(source.getTypeKey(),link2.getTypeKey())).thenReturn(sdc);
        when(xmEntitySpecService.findLink(source.getTypeKey(),linkOutOfMaxValue.getTypeKey())).thenReturn(sdc);

        when(linkRepository.countBySourceIdAndTypeKey(link.getSource().getId(),link.getTypeKey())).thenReturn(2);
        when(linkRepository.countBySourceIdAndTypeKey(link2.getSource().getId(),link2.getTypeKey())).thenReturn(2);
        when(linkRepository.countBySourceIdAndTypeKey(linkOutOfMaxValue.getSource().getId(),linkOutOfMaxValue.getTypeKey())).thenReturn(2);

        linkService.save(link);
        linkService.save(link2);
        linkService.save(linkOutOfMaxValue);
      }
   }

