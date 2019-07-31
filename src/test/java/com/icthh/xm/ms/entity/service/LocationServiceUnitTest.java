package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.swing.text.html.parser.Entity;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

public class LocationServiceUnitTest extends AbstractUnitTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private LocationService locationService;

    private PermittedRepository permittedRepository;
    private PermittedSearchRepository permittedSearchRepository;
    private XmEntityRepository xmEntityRepository;
    private LocationRepository locationRepository;
    private XmEntitySpecService xmEntitySpecService;

    @Before
    public void setUp() {
        permittedRepository = Mockito.mock(PermittedRepository.class);
        permittedSearchRepository = Mockito.mock(PermittedSearchRepository.class);
        locationRepository = Mockito.mock(LocationRepository.class);
        xmEntityRepository = Mockito.mock(XmEntityRepository.class);
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);

        locationService = new LocationService(permittedRepository, permittedSearchRepository,
            locationRepository, xmEntityRepository, xmEntitySpecService);
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
    public void shouldUpdateLocation() {
        Location l = new Location();
        l.setId(1L);
        l.setTypeKey("LT");
        XmEntity e = new XmEntity();
        e.setId(3L);
        e.setTypeKey("ET");
        l.setXmEntity(e);
        Location mock = new Location();
        mock.setId(222L);
        when(xmEntityRepository.findById(any())).thenReturn(Optional.of(e));

        LocationSpec spec = new LocationSpec();
        spec.setMax(0);

        when(xmEntitySpecService.findLocation("ET", "LT")).thenReturn(Optional.of(spec));
        when(locationRepository.save(any())).thenReturn(mock);
        assertThat(locationService.save(l).getId()).isEqualTo(222L);
    }

    @Test
    public void shouldInsertNewLocation() {
        Location l = new Location();
        l.setTypeKey("LT");
        XmEntity e = new XmEntity();
        e.setId(3L);
        e.setTypeKey("ET");
        l.setXmEntity(e);
        Location mock = new Location();
        mock.setId(222L);
        when(xmEntityRepository.findById(any())).thenReturn(Optional.of(e));

        LocationSpec spec = new LocationSpec();
        spec.setMax(2);
        spec.setKey("LT");

        when(xmEntitySpecService.findLocation("ET", "LT")).thenReturn(Optional.of(spec));
        when(locationRepository.countByXmEntityIdAndTypeKey(3L, "LT")).thenReturn(1);
        when(locationRepository.save(any())).thenReturn(mock);
        assertThat(locationService.save(l).getId()).isEqualTo(222L);
    }

    @Test
    public void shouldRaiseMaxLimitOnInsertNewLocation() {
        Location l = new Location();
        l.setTypeKey("LT");
        XmEntity e = new XmEntity();
        e.setId(3L);
        e.setTypeKey("ET");
        l.setXmEntity(e);
        Location mock = new Location();
        mock.setId(222L);
        when(xmEntityRepository.findById(any())).thenReturn(Optional.of(e));

        LocationSpec spec = new LocationSpec();
        spec.setMax(1);
        spec.setKey("LT");

        when(xmEntitySpecService.findLocation("ET", "LT")).thenReturn(Optional.of(spec));
        when(locationRepository.countByXmEntityIdAndTypeKey(3L, "LT")).thenReturn(1);
        when(locationRepository.save(any())).thenReturn(mock);

        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(LocationService.MAX_RESTRICTION)));

        locationService.save(l);
    }

    @Test
    public void shouldFailIfSpecNotFound() {
        XmEntity e = new XmEntity();
        e.setTypeKey("TYPE");
        Location a = new Location();
        a.setTypeKey("TYPE.A");
        exception.expect(EntityNotFoundException.class);
        exception.expectMessage(containsString("Spec.Location"));
        locationService.getSpec(e, a);
    }

    @Test
    public void shouldPassIfSpecProvided() {
        XmEntity e = new XmEntity();
        e.setTypeKey("TYPE");

        Location a = new Location();
        a.setTypeKey("TYPE.A");

        LocationSpec spec = new LocationSpec();
        spec.setKey("TYPE.A");

        when(xmEntitySpecService.findLocation("TYPE", "TYPE.A")).thenReturn(Optional.of(spec));

        assertThat(locationService.getSpec(e, a).getKey()).isEqualTo("TYPE.A");
    }

    @Test
    public void shouldFailIfMaxSizeIsZero() {
        LocationSpec spec = new LocationSpec();
        spec.setMax(0);
        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(LocationService.ZERO_RESTRICTION)));
        locationService.assertZeroRestriction(spec);
    }

    @Test
    public void shouldFailIfStoredSizeEqualsOrBiggerSpecMax() {
        LocationSpec spec = new LocationSpec();
        spec.setMax(0);
        spec.setKey("K");
        XmEntity entity = new XmEntity();
        entity.setId(1L);
        when(locationRepository.countByXmEntityIdAndTypeKey(1l, "K")).thenReturn(1);
        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(LocationService.MAX_RESTRICTION)));
        locationService.assertLimitRestriction(spec, entity);
    }

}
