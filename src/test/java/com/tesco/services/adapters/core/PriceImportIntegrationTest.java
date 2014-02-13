package com.tesco.services.adapters.core;

import com.mongodb.DBObject;
import com.tesco.services.core.Product;
import com.tesco.services.core.ProductVariant;
import com.tesco.services.core.SaleInfo;
import com.tesco.services.repositories.ProductRepository;
import com.tesco.services.resources.TestConfiguration;
import org.junit.Test;

import java.io.IOException;

import static com.tesco.services.adapters.core.TestFiles.*;
import static com.tesco.services.core.PriceKeys.PRICE;
import static com.tesco.services.core.PriceKeys.PROMO_PRICE;
import static org.fest.assertions.api.Assertions.assertThat;

public class PriceImportIntegrationTest extends ImportJobIntegrationTestBase {
    private String oldTpnb;

    @Override
    protected void preImportCallBack() {
        oldTpnb = "01212323";
        dataGridResource.getProductPriceCache().put(oldTpnb, new Product(oldTpnb));
    }

    @Test
    public void shouldFindPriceFromOneZone() throws IOException {
        DBObject prices = findPricesFromZone("050925811", "5");

        assertThat(prices.get(PRICE)).isEqualTo("1.33");
        assertThat(prices.get(PROMO_PRICE)).isEqualTo("2.33");
    }

    @Test
    public void shouldFindPricesGivenMultipleZones() throws IOException {

        DBObject pricesFromZoneFive = findPricesFromZone("050940579", "5");
        assertThat(pricesFromZoneFive.get(PRICE)).isEqualTo("5.33");
        assertThat(pricesFromZoneFive.get(PROMO_PRICE)).isEqualTo("5.33");

        DBObject pricesFromZoneThree = findPricesFromZone("050940579", "3");
        assertThat(pricesFromZoneThree.get(PRICE)).isEqualTo("2.33");
        assertThat(pricesFromZoneThree.get(PROMO_PRICE)).isEqualTo("2.33");
    }

    @Test
    public void shouldUpdatePrices() throws Exception {
        TestConfiguration testConfiguration = new TestConfiguration();
        ImportJob importJob = new ImportJob(
                RPM_PRICE_ZONE_TO_UPDATE_CSV_FILE_PATH,
                RPM_STORE_ZONE_CSV_FILE_PATH,
                RPM_PROMOTION_CSV_FILE_PATH,
                SONETTO_PROMOTIONS_XML_FILE_PATH,
                RPM_PROMOTION_DESC_CSV_FILE_PATH,
                SONETTO_PROMOTIONS_XSD_FILE_PATH,
                testConfiguration.getSonettoShelfImageUrl(),
                RPM_PRICE_ZONE_PRICE_CSV_FILE_PATH,
                RPM_PROMO_ZONE_PRICE_CSV_FILE_PATH,
                RPM_PROMO_EXTRACT_CSV_FILE_PATH,
                dbFactory, dataGridResource);
        importJob.run();

        DBObject prices = findPricesFromZone("050925811", "5");

        assertThat(prices.get(PRICE)).isEqualTo("20.33");
        assertThat(prices.get(PROMO_PRICE)).isEqualTo("12.33");
    }

    // =========
    // DataGrid
    // =========
    @Test
    public void shouldUpdatePriceZonePricesToReplacedCache() {
        String tpnb,tpnc1,tpnc2;
        tpnb = tpnc1 = "050925811";
        tpnc2 = "050925811-001";

        ProductVariant productVariant1 = new ProductVariant(tpnc1);
        productVariant1.addSaleInfo(new SaleInfo(1, "1.40"));
        productVariant1.addSaleInfo(new SaleInfo(5, "0.30"));

        ProductVariant productVariant2 = new ProductVariant(tpnc2);
        productVariant2.addSaleInfo(new SaleInfo(1, "1.39"));
        productVariant2.addSaleInfo(new SaleInfo(2, "1.38"));
        productVariant2.addSaleInfo(new SaleInfo(5, "0.34"));
        productVariant2.addSaleInfo(new SaleInfo(14, "0.35"));

        Product product = new Product(tpnb);
        product.addProductVariant(productVariant1);
        product.addProductVariant(productVariant2);

        ProductRepository productRepository = new ProductRepository(dataGridResource.getProductPriceCache());
        assertThat(productRepository.getByTPNB(tpnb).get()).isEqualTo(product);
        assertThat(productRepository.getByTPNB(oldTpnb).isPresent()).isFalse();
    }
}
