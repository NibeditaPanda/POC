package com.tesco.services.adapters.core;

import com.mongodb.DBCollection;
import com.tesco.services.repositories.DataGridResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ImportJobTest {

    @Mock
    private DBCollection tempPriceDbCollection;

    @Mock
    private DBCollection tempStoreDbCollection;

    @Mock
    private DBCollection tempPromotionDbCollection;

    @Mock
    private DataGridResource dataGridResource;

    @Test
    public void shouldNotRenameCollectionGivenFileIsCorrupted() throws Exception {
        ControllerWithTempFilesBuilder controllerBuilder = new ControllerWithTempFilesBuilder().withFakeRpmPriceZoneCsvFile(",,,");
        ImportJob importJob = controllerBuilder.build();
        importJob.processData(tempPriceDbCollection, tempStoreDbCollection, tempPromotionDbCollection, true);

        verify(tempPriceDbCollection, never()).rename(anyString());
        verify(tempStoreDbCollection, never()).rename(anyString());
        verify(tempPromotionDbCollection, never()).rename(anyString());
    }

    @Test
    public void shouldNotRenameCollectionGivenSonettoIsCorrupted() throws Exception {
        ControllerWithTempFilesBuilder controllerBuilder = new ControllerWithTempFilesBuilder()
                .withFakeSonettoPromotionsXMLFile(",,,");
        ImportJob importJob = controllerBuilder.build();
        importJob.processData(tempPriceDbCollection, tempStoreDbCollection, tempPromotionDbCollection, true);

        importJob.processData(tempPriceDbCollection, tempStoreDbCollection, tempPromotionDbCollection, false);

        verify(tempPriceDbCollection, never()).rename(anyString());
        verify(tempStoreDbCollection, never()).rename(anyString());
        verify(tempPromotionDbCollection, never()).rename(anyString());

        assertThat(controllerBuilder.getRpmPriceZoneCsvFile().exists(), is(false));
        assertThat(controllerBuilder.getRpmStoreZoneCsvFile().exists(), is(false));
        assertThat(controllerBuilder.getRpmPromotionCsvFile().exists(), is(false));
        assertThat(controllerBuilder.getSonettoPromotionsXMLFile().exists(), is(false));
        assertThat(controllerBuilder.getRpmPromotionDescCSV().exists(), is(false));
        controllerBuilder.deleteTempFiles();
    }

   @Test
    public void shouldNotReplaceCachesInEventOfFailure() throws IOException {
       ControllerWithTempFilesBuilder controllerBuilder = new ControllerWithTempFilesBuilder().withInvalidStoreZoneFile(",,,");
       ImportJob importJob = controllerBuilder.build();
       importJob.run();

       verify(dataGridResource, never()).replaceCurrentWithRefresh();
   }
}
