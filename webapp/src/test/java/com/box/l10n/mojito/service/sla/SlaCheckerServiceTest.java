package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.SlaIncident;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.sla.email.SlaCheckerEmailService;
import com.box.l10n.mojito.utils.DateTimeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author jeanaurambault
 */
@RunWith(MockitoJUnitRunner.class)
public class SlaCheckerServiceTest {

    @Spy
    @InjectMocks
    SlaCheckerService slaCheckerService;

    @Mock
    SlaIncidentRepository slaIncidentRepository;

    @Mock
    SlaCheckerEmailService slaCheckerEmailService;

    @Mock
    RepositoryRepository repositoryRepository;

    @Spy
    DateTimeUtils dateTimeUtils;

    @Test
    public void testCheckForIncidents() {
        SlaIncident openIncident = mock(SlaIncident.class);

        doReturn(openIncident).when(slaIncidentRepository).findFirstByClosedDateIsNull();
        doNothing().when(slaCheckerService).checkWithOpenIncident(openIncident);

        slaCheckerService.checkForIncidents();

        verify(slaCheckerService).checkWithOpenIncident(openIncident);
    }

    @Test
    public void testCheckForIncidentsNoOpen() {
        doReturn(null).when(slaIncidentRepository).findFirstByClosedDateIsNull();
        doNothing().when(slaCheckerService).checkWithNoOpenIncident();

        slaCheckerService.checkForIncidents();

        verify(slaCheckerService).checkWithNoOpenIncident();
    }

    @Test
    public void testCheckWithOpenIncidentClose() {
        SlaIncident openIncident = getSlaIncidentForTest();

        doReturn(new ArrayList<>()).when(repositoryRepository).findByDeletedFalseAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(0L);

        slaCheckerService.checkWithOpenIncident(openIncident);

        verify(slaCheckerService).closeIncidents();
        verify(slaCheckerEmailService).sendCloseIncidentEmail(openIncident.getId());
    }

    @Test
    public void testCheckWithOpenIncidentResend() {
        SlaIncident openIncident = getSlaIncidentForTest();
        List<Repository> repositories = getRepositoriesForTest();

        doReturn(repositories).when(repositoryRepository).findByDeletedFalseAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(0L);
        doReturn(true).when(slaCheckerEmailService).shouldResendEmail(openIncident.getCreatedDate());

        slaCheckerService.checkWithOpenIncident(openIncident);

        verify(slaCheckerEmailService).sendOpenIncidentEmail(openIncident.getId(), repositories);
    }

    @Test
    public void testCheckWithOpenIncidentDoNothing() {
        SlaIncident openIncident = getSlaIncidentForTest();
        List<Repository> repositories = getRepositoriesForTest();

        doReturn(repositories).when(repositoryRepository).findByDeletedFalseAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(0L);
        doReturn(false).when(slaCheckerEmailService).shouldResendEmail(openIncident.getCreatedDate());

        slaCheckerService.checkWithOpenIncident(openIncident);

        verify(slaCheckerEmailService, never()).sendOpenIncidentEmail(openIncident.getId(), repositories);
    }

    @Test
    public void testCheckWithNoOpenIncidentCreate() {
        List<Repository> repositories = getRepositoriesForTest();
        SlaIncident openIncident = getSlaIncidentForTest();

        doReturn(repositories).when(repositoryRepository).findByDeletedFalseAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(0L);
        doReturn(openIncident).when(slaCheckerService).createIncident(repositories);
        doNothing().when(slaCheckerEmailService).sendOpenIncidentEmail(openIncident.getId(), repositories);

        slaCheckerService.checkWithNoOpenIncident();

        verify(slaCheckerEmailService).sendOpenIncidentEmail(openIncident.getId(), repositories);
    }

    @Test
    public void testCheckWithNoOpenIncidentNothing() {
        doReturn(new ArrayList<>()).when(repositoryRepository).findByDeletedFalseAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(0L);

        slaCheckerService.checkWithNoOpenIncident();

        verify(slaCheckerEmailService, never()).sendOpenIncidentEmail(anyLong(), Matchers.anyList());
    }

    @Test
    public void testCreateIncident() {

        SlaIncident slaIncidentForTest = getSlaIncidentForTest();

        doReturn(slaIncidentForTest).when(slaIncidentRepository).save(any(SlaIncident.class));

        SlaIncident createIncident = slaCheckerService.createIncident(getRepositoriesForTest());
        assertEquals(slaIncidentForTest, createIncident);
    }

    @Test
    public void testCloseIncidents() {
        List<SlaIncident> incidentIn = Arrays.asList(mock(SlaIncident.class), mock(SlaIncident.class));
        List<SlaIncident> incidentOut = Arrays.asList(mock(SlaIncident.class), mock(SlaIncident.class));

        DateTime now = new DateTime();
        doReturn(now).when(dateTimeUtils).now();

        doReturn(incidentIn).when(slaIncidentRepository).findByClosedDateIsNull();
        doReturn(incidentOut).when(slaIncidentRepository).save(incidentIn);

        slaCheckerService.closeIncidents();

        verify(slaIncidentRepository).save(incidentIn);

        for (SlaIncident slaIncident : incidentIn) {
            verify(slaIncident).setClosedDate(now);
        }
    }

    List<Repository> getRepositoriesForTest() {
        Repository repository1 = new Repository();
        repository1.setName("test1");
        repository1.setRepositoryStatistic(new RepositoryStatistic());
        repository1.getRepositoryStatistic().setOoslaTextUnitWordCount(10L);

        Repository repository2 = new Repository();
        repository2.setName("test2");
        repository2.setRepositoryStatistic(new RepositoryStatistic());
        repository2.getRepositoryStatistic().setOoslaTextUnitWordCount(12L);

        return Arrays.asList(repository1, repository2);
    }

    SlaIncident getSlaIncidentForTest() {
        DateTime createDate = new DateTime();
        SlaIncident openIncident = new SlaIncident();
        openIncident.setId(1212L);
        openIncident.setCreatedDate(createDate);
        return openIncident;
    }
}
