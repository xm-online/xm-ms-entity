package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

public class LocationServiceUnitTest extends AbstractUnitTest {

    private LocationService locationService;

    private PermittedRepository permittedRepository;
    private PermittedSearchRepository permittedSearchRepository;
    private XmEntityRepository xmEntityRepository;
    private LocationRepository locationRepository;
    private XmEntitySpecService xmEntitySpecService;

    @Before
    public void setUp() throws Exception {
        permittedRepository = Mockito.mock(PermittedRepository.class);
        permittedSearchRepository = Mockito.mock(PermittedSearchRepository.class);
        locationRepository = Mockito.mock(LocationRepository.class);
        xmEntityRepository = Mockito.mock(XmEntityRepository.class);
        locationService = new LocationService(permittedRepository, permittedSearchRepository,
            locationRepository, xmEntityRepository);
    }

    @Test
    public void findAll() {
        Location l = new Location();
        when(permittedRepository.findAll(Location.class, "P"))
            .thenReturn(Lists.newArrayList(l ,l));
        assertThat(locationService.findAll("P").size()).isEqualTo(2);
    }

    @Test
    public void search() {
        Location l = new Location();
        when(permittedSearchRepository.search("Q", Location.class, "P"))
            .thenReturn(Lists.newArrayList(l, l));
        assertThat(locationService.search("Q", "P").size()).isEqualTo(2);
    }

    @Test
    public void save() {
        Location l = new Location();
        XmEntity e = new XmEntity();
        e.setId(3L);
        l.setXmEntity(e);
        Location mock = new Location();
        mock.setId(222L);
        when(xmEntityRepository.getOne(any())).thenReturn(e);
        when(locationRepository.save(any())).thenReturn(mock);
        assertThat(locationService.save(l).getId()).isEqualTo(222L);
    }
}
