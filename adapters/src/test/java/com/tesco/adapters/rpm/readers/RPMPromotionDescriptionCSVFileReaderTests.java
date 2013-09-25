package com.tesco.adapters.rpm.readers;

import com.mongodb.DBObject;
import com.tesco.adapters.core.exceptions.ColumnNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static com.tesco.adapters.core.PriceKeys.*;
import static org.fest.assertions.api.Assertions.assertThat;

public class RPMPromotionDescriptionCSVFileReaderTests {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private RPMPromotionDescriptionCSVFileReader rpmPromotionDescriptionCSVFileReader;

    @Test
    public void shouldReadDescriptionsFromCSV() throws IOException, ColumnNotFoundException {
        rpmPromotionDescriptionCSVFileReader = new RPMPromotionDescriptionCSVFileReader("src/test/resources/com/tesco/adapters/rpm/fixtures/PROM_DESC_EXTRACT.csv");

        DBObject promotionInfo = rpmPromotionDescriptionCSVFileReader.getNext();

        assertThat(promotionInfo.get(PROMOTION_OFFER_ID)).isEqualTo("A29721647");
        assertThat(promotionInfo.get(ZONE_ID)).isEqualTo("5");
        assertThat(promotionInfo.get(PROMOTION_CF_DESCRIPTION_1)).isEqualTo("SPECIAL PURCHASE 50p");
        assertThat(promotionInfo.get(PROMOTION_CF_DESCRIPTION_2)).isEqualTo("3 LIONS FLAG");
    }

    @Test
    public void shouldThrowExceptionGivenOfferIdIsNotFound() throws Exception {
        expectedEx.expect(ColumnNotFoundException.class);
        expectedEx.expectMessage("OFFER_ID is not found");

        this.rpmPromotionDescriptionCSVFileReader = new RPMPromotionDescriptionCSVFileReader("src/test/resources/com/tesco/adapters/rpm/readers/promotion_desc/PROM_DESC_EXTRACT_OFFER_ID_NOT_FOUND.csv");
    }

    @Test
    public void shouldThrowExceptionGivenZoneIdIsNotFound() throws Exception {
        expectedEx.expect(ColumnNotFoundException.class);
        expectedEx.expectMessage("ZONE_ID is not found");

        this.rpmPromotionDescriptionCSVFileReader = new RPMPromotionDescriptionCSVFileReader("src/test/resources/com/tesco/adapters/rpm/readers/promotion_desc/PROM_DESC_EXTRACT_ZONE_ID_NOT_FOUND.csv");
    }

    @Test
    public void shouldThrowExceptionGivenDesc1IsNotFound() throws Exception {
        expectedEx.expect(ColumnNotFoundException.class);
        expectedEx.expectMessage("CF_DESC1 is not found");

        this.rpmPromotionDescriptionCSVFileReader = new RPMPromotionDescriptionCSVFileReader("src/test/resources/com/tesco/adapters/rpm/readers/promotion_desc/PROM_DESC_EXTRACT_CF_DESC1_NOT_FOUND.csv");
    }

    @Test
    public void shouldThrowExceptionGivenDesc2IsNotFound() throws Exception {
        expectedEx.expect(ColumnNotFoundException.class);
        expectedEx.expectMessage("CF_DESC2 is not found");

        this.rpmPromotionDescriptionCSVFileReader = new RPMPromotionDescriptionCSVFileReader("src/test/resources/com/tesco/adapters/rpm/readers/promotion_desc/PROM_DESC_EXTRACT_CF_DESC2_NOT_FOUND.csv");
    }
}
