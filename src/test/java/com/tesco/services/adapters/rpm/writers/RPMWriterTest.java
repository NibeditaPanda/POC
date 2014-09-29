package com.tesco.services.adapters.rpm.writers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.tesco.couchbase.AsyncCouchbaseWrapper;
import com.tesco.couchbase.CouchbaseWrapper;
import com.tesco.couchbase.listeners.Listener;
import com.tesco.services.adapters.core.exceptions.ColumnNotFoundException;
import com.tesco.services.adapters.rpm.comparators.RPMComparator;
import com.tesco.services.adapters.rpm.readers.PriceServiceCSVReader;
import com.tesco.services.adapters.sonetto.SonettoPromotionXMLReader;
import com.tesco.services.builder.PromotionBuilder;
import com.tesco.services.core.*;
import com.tesco.services.repositories.ProductRepository;
import com.tesco.services.repositories.PromotionRepository;
import com.tesco.services.repositories.StoreRepository;
import com.tesco.services.repositories.UUIDGenerator;
import com.tesco.services.utility.Dockyard;
import net.spy.memcached.internal.OperationFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.util.Lists.newArrayList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RPMWriterTest {
    private RPMWriter rpmWriter;

    @Mock
    private SonettoPromotionXMLReader sonettoPromotionXMLReader;

    @Mock
    private PriceServiceCSVReader rpmPriceReader;

    @Mock
    private PriceServiceCSVReader rpmPromoPriceReader;

    @Mock
    private PriceServiceCSVReader storeZoneReader;

    @Mock
    private PriceServiceCSVReader rpmPromotionReader;

    @Mock
    private PriceServiceCSVReader rpmPromotionDescReader;

    @Mock
    private UUIDGenerator uuidGenerator;

    @Mock
    public PromotionRepository promotionRepository;

    @Mock
    public ProductRepository productRepository;

    @Mock
    public StoreRepository storeRepository;

    @Mock
    public AsyncCouchbaseWrapper asyncCouchbaseWrapper;

    @Mock
    public CouchbaseWrapper couchbaseWrapper;

    @Mock
    public OperationFuture<?> operationFuture;
    @Mock
    private ObjectMapper mapper;
    private int zoneId = 1;
    /*Added by Nitisha and Surya for Junit Corrections for PS-112 - Start*/
    private String sellingUOM = "KG";
    /*Added by Nitisha and Surya for Junit Corrections for PS-112 - End*/
    @Before
    public void setUp() throws IOException,ColumnNotFoundException {
        rpmWriter = new RPMWriter("./src/test/java/resources/com/tesco/adapters/sonetto/PromotionsDataExport.xml",
                sonettoPromotionXMLReader,
                promotionRepository,
                productRepository,
                storeRepository,
                rpmPriceReader,
                rpmPromoPriceReader,
                storeZoneReader,
                rpmPromotionReader,
                rpmPromotionDescReader);
        when(rpmPriceReader.getNext()).thenReturn(null);
        when(rpmPromoPriceReader.getNext()).thenReturn(null);
        when(storeZoneReader.getNext()).thenReturn(null);
        when(rpmPromotionReader.getNext()).thenReturn(null);
        when(rpmPromotionDescReader.getNext()).thenReturn(null);

        when(uuidGenerator.getUUID()).thenReturn("uuid");

        when(this.promotionRepository.getPromotionsByOfferIdZoneIdAndItemNumber("promotionOfferId", "itemNumber", zoneId)).thenReturn(newArrayList(aPromotionWithDescriptions()));
    }

    @Test
    public void shouldInsertPriceZonePrice() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        String tpnb = "059428124";
        String tpnc = "284347092";
        ProductVariant productVariant = new ProductVariant(tpnc);
        String price = "2.4";
        productVariant.addSaleInfo(new SaleInfo(zoneId, price));
        /*Added by Nitisha and Surya for PS-112 Junits corrections- Start */
        productVariant.setSellingUOM(sellingUOM);
        /*Added by Nitisha and Surya for PS-112 Junits corrections- Start */
       final Product product = createProduct(tpnb, productVariant);
        mockAsyncProductInsert();
        Map<String, String> productInfoMap = productInfoMap(tpnb,tpnc, zoneId, price);
        when(rpmPriceReader.getNext()).thenReturn(productInfoMap).thenReturn(null);
        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.<Product>absent());
        this.rpmWriter.write();
        verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,product);
            }
        }),any(Listener.class));
    }

    @Test
    public void shouldInsertMultiplePriceZonePricesForAVariant() throws  IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        String itemNumber = "0123";
        String tpnc = "284347092";

        when(rpmPriceReader.getNext()).thenReturn(productInfoMap(itemNumber, tpnc, 2, "2.4"))
                .thenReturn(productInfoMap(itemNumber,tpnc, 4, "4.4"))
                .thenReturn(null);

        Product product = createProductWithVariant(itemNumber, tpnc);
        mockAsyncProductInsert();
//Changes made By Surya for PS-120 . The JUnit should pass for the Code which will Create a new Product for the first time and then amend Multiple Price zones- Start
        when(productRepository.getByTPNB(itemNumber)).thenReturn(Optional.of(product));
 //Changes made By Surya for PS-120 . The JUnit should pass for the Code which will Create a new Product for the first time and then amend Multiple Price zones - End

        mockAsyncProductInsert();
        this.rpmWriter.write();

        InOrder inOrder = inOrder(productRepository);

        ProductVariant expectedProductVariant = createProductVariant(tpnc, 2, "2.4", null);
       final Product expectedProduct = createProduct(itemNumber, expectedProductVariant);
        /*PS-238 Modified By Nibedita - to resolve errors after code modification - Start*/
        expectedProductVariant.addSaleInfo(new SaleInfo(4, "4.4"));
        /*PS-238 Modified By Nibedita - to resolve errors after code modification - End*/
        inOrder.verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,expectedProduct);
            }
        }),any(Listener.class));
    }

    @Test
    public void shouldInsertPriceZonePricesForMultipleVariants() throws  IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        String tpnb = "1123";
        String tpnc = "284347092";
        String tpnc2 = "304347092";
        String itemNumber = String.format("%s-001", tpnb);
        String itemNumber2 = String.format("%s-002", tpnb);
        mockAsyncProductInsert();

        when(rpmPriceReader.getNext()).thenReturn(productInfoMap(tpnb, tpnc, 2, "2.4"))
                .thenReturn(productInfoMap(itemNumber2,tpnc2, 3, "3.0"))
                .thenReturn(null);

        Product product = createProductWithVariant(tpnb, tpnc);

        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.<Product>absent()).thenReturn(Optional.of(product));

        this.rpmWriter.write();

        InOrder inOrder = inOrder(productRepository);
      final  Product expectedProduct = createProductWithVariant(tpnb, tpnc);
        /*PS-238 Modified By Nibedita - to resolve errors after code modification - Start*/
      ProductVariant expectedProductVariant2 = createProductVariant(tpnc2, 3, "3.0", null);
      expectedProduct.addProductVariant(expectedProductVariant2);
        /*PS-238 Modified By Nibedita - to resolve errors after code modification - End*/
        inOrder.verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,expectedProduct);
            }
        }),any(Listener.class));
    }

    private Map<String, String> productInfoMap(String itemNumber, String tpnc, int zoneId, String price) {
        Map<String, String> productInfoMap = new HashMap<>();
        productInfoMap.put(CSVHeaders.Price.ITEM, itemNumber);
        productInfoMap.put(CSVHeaders.Price.TPNC, tpnc);
        productInfoMap.put(CSVHeaders.Price.PRICE_ZONE_ID, String.valueOf(zoneId));
        productInfoMap.put(CSVHeaders.Price.PRICE_ZONE_PRICE, price);
      /*Added by Nitisha and Surya for PS-112 Junits corrections- Start */
        productInfoMap.put(CSVHeaders.Price.SELLING_UOM, sellingUOM);
        /*Added by Nitisha and Surya for PS-112 Junits corrections- End */


        return productInfoMap;
    }

    private Map<String, String> productPromoInfoMap(String itemNumber,String tpnc, int zoneId, String price) {
        Map<String, String> productInfoMap = new HashMap<>();
        productInfoMap.put(CSVHeaders.Price.ITEM, itemNumber);
        productInfoMap.put(CSVHeaders.Price.TPNC, tpnc);
        productInfoMap.put(CSVHeaders.Price.PROMO_ZONE_ID, String.valueOf(zoneId));
        productInfoMap.put(CSVHeaders.Price.PROMO_ZONE_PRICE, price);

        return productInfoMap;
    }

    @Test
    public void shouldInsertPromoZonePrice() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        // This will change when TPNC story is played
        final String tpnc = "284347092";
        int priceZoneId = 2;
        String price = "2.3";
        ProductVariant productVariant = createProductVariant(tpnc, priceZoneId, price, null);

        final String tpnb = "059428124";
        Product product = createProduct(tpnb, productVariant);

        int promoZoneId = 5;
        String promoPrice = "2.0";
        Map<String, String> productInfoMap = productPromoInfoMap(tpnb,tpnc, promoZoneId, promoPrice);
        mockAsyncProductInsert();

        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.of(product));
        when(rpmPromoPriceReader.getNext()).thenReturn(productInfoMap).thenReturn(null);

        this.rpmWriter.write();

        ProductVariant expectedProductVariant = createProductVariant(tpnc, priceZoneId, price, null);
        expectedProductVariant.addSaleInfo(new SaleInfo(promoZoneId, promoPrice));

       final Product expectedProduct = createProduct(tpnb, expectedProductVariant);
        verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,expectedProduct);
            }
        }),any(Listener.class));
    }

    @Test
    public void shouldInsertPromotionIntoProductPriceRepository() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
       // This will change when TPNC story is played
        final String tpnc = "284347092";
        int zoneIds = 5;
        String price = "2.3";

        String tpnb = "070461113";
        String offerId = "A01";
        String offerName = "Test Offer Name";
        String startDate = "20130729";
        String endDate = "20130819";
        String description1 = "description1";
        String description2 = "description2";

        when(rpmPromotionReader.getNext()).thenReturn(promotionInfoMap(tpnb,tpnc, zoneIds, offerId, offerName, startDate, endDate)).thenReturn(null);
        when(rpmPromotionDescReader.getNext()).thenReturn(promotionDescInfoMap(tpnb, zoneIds, offerId, description1, description2)).thenReturn(null);
        mockAsyncProductInsert();

        ProductVariant productVariant = createProductVariant(tpnc, zoneIds, price, null);
        ProductVariant productVariant2 = createProductVariant(tpnc, zoneIds, price, createPromotion(offerId,zoneIds, offerName, startDate, endDate));
        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.of(createProduct(tpnb, productVariant)), Optional.of(createProduct(tpnb, productVariant2)));
        when(productRepository.getProductTPNC(tpnb)).thenReturn(getTPNCForTPNB(tpnc));

        this.rpmWriter.write();

         ArgumentCaptor<Product> arguments = ArgumentCaptor.forClass(Product.class);
         verify(productRepository, atLeastOnce()).insertProduct(arguments.capture(),any(Listener.class));

        Promotion expectedPromotion = createPromotion(offerId,zoneIds, offerName, startDate, endDate);
        ProductVariant expectedProductVariant = createProductVariant(tpnc, zoneIds, price, expectedPromotion);

        Product expectedProduct = createProduct(tpnb, expectedProductVariant);

        List<Product> actualProducts = arguments.getAllValues();
        assertThat(actualProducts.get(0)).isEqualTo(expectedProduct);

        expectedPromotion.setCFDescription1(description1);
        expectedPromotion.setCFDescription2(description2);
        assertThat(actualProducts.get(1)).isEqualTo(expectedProduct);
    }

    private Promotion createPromotion(String offerId,int zoneId, String offerName, String startDate, String endDate) {
        return new PromotionBuilder()
                .offerId(offerId)
                .zoneId(zoneId)
                .offerName(offerName)
                .startDate(startDate)
                .endDate(endDate)
                .description1(null)
                .description2(null)
                .createPromotion();
    }

    private Map<String,String> promotionDescInfoMap(String tpnb, int zoneId, String offerId, String description1, String description2) {
        Map<String, String> promotionInfoMap = new HashMap<>();
        promotionInfoMap.put(CSVHeaders.PromoDescExtract.ITEM, tpnb);
        promotionInfoMap.put(CSVHeaders.PromoDescExtract.ZONE_ID, String.valueOf(zoneId));
        promotionInfoMap.put(CSVHeaders.PromoDescExtract.OFFER_ID, offerId);
        promotionInfoMap.put(CSVHeaders.PromoDescExtract.DESC1, description1);
        promotionInfoMap.put(CSVHeaders.PromoDescExtract.DESC2, description2);
        return promotionInfoMap;

    }

    private Map<String, String> promotionInfoMap(String tpnb,String tpnc, int zoneId, String offerId, String offerName, String startDate, String endDate) {
        Map<String, String> promotionInfoMap = new HashMap<>();
        promotionInfoMap.put(CSVHeaders.PromoExtract.ITEM, tpnb);
        promotionInfoMap.put(CSVHeaders.PromoExtract.TPNC, tpnc);
        promotionInfoMap.put(CSVHeaders.PromoExtract.ZONE_ID, String.valueOf(zoneId));
        promotionInfoMap.put(CSVHeaders.PromoExtract.OFFER_ID, offerId);
        promotionInfoMap.put(CSVHeaders.PromoExtract.OFFER_NAME, offerName);
        promotionInfoMap.put(CSVHeaders.PromoExtract.START_DATE, startDate);
        promotionInfoMap.put(CSVHeaders.PromoExtract.END_DATE, endDate);
        return promotionInfoMap;
    }

    @Test
    public void shouldInsertStorePriceZones() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        String firstStoreId = "2002";
        String secondStoreId = "2003";
        mockAsyncStoreInsert();
        when(storeZoneReader.getNext()).thenReturn(getStoreInfoMap(firstStoreId, 1, 1, "GBP")).thenReturn(getStoreInfoMap(secondStoreId, 2, 1, "EUR")).thenReturn(null);
        when(storeRepository.getByStoreId(String.valueOf(firstStoreId))).thenReturn(Optional.<Store>absent());
        when(storeRepository.getByStoreId(String.valueOf(secondStoreId))).thenReturn(Optional.<Store>absent());
        this.rpmWriter.write();

      final  Store firstStore = new Store(firstStoreId, Optional.of(1), Optional.<Integer>absent(), "GBP");
      final  Store secondStore = new Store(secondStoreId, Optional.of(2), Optional.<Integer>absent(), "EUR");

        InOrder inOrder = inOrder(storeRepository);
        inOrder.verify(storeRepository).insertStore(argThat(new CapturingMatcher<Store>() {
            @Override
            public boolean matches(Object o) {
                Store store = (Store) o;
                return new RPMComparator().compare(store,firstStore);
            }
        }),any(Listener.class));
        inOrder.verify(storeRepository).insertStore(argThat(new CapturingMatcher<Store>() {
            @Override
            public boolean matches(Object o) {
                Store store = (Store) o;
                return new RPMComparator().compare(store,secondStore);
            }
        }),any(Listener.class));
    }

    @Test
    public void shouldInsertStorePriceAndPromoZones() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        String storeId = "2002";

        when(storeZoneReader.getNext()).thenReturn(getStoreInfoMap(storeId, 1, 1, "GBP")).thenReturn(getStoreInfoMap(storeId, 5, 2, "GBP")).thenReturn(null);
        Store store = new Store(storeId, Optional.of(1), Optional.<Integer>absent(), "GBP");
        mockAsyncStoreInsert();
        when(storeRepository.getByStoreId(String.valueOf(storeId))).thenReturn(Optional.<Store>absent()).thenReturn(Optional.of(store));
        final  Store firstStore = new Store(storeId, Optional.of(1), Optional.<Integer>absent(), "GBP");
        final  Store secondStore = new Store(storeId, Optional.of(1), Optional.of(5), "GBP");
        this.rpmWriter.write();

        InOrder inOrder = inOrder(storeRepository);
        inOrder.verify(storeRepository).insertStore(argThat(new CapturingMatcher<Store>() {
            @Override
            public boolean matches(Object o) {
                Store store = (Store) o;
                return new RPMComparator().compare(store,firstStore);
            }
        }),any(Listener.class));
        inOrder.verify(storeRepository).insertStore(argThat(new CapturingMatcher<Store>() {
            @Override
            public boolean matches(Object o) {
                Store store = (Store) o;
                return new RPMComparator().compare(store,secondStore);
            }
        }),any(Listener.class));
    }

    private Map<String, String> getStoreInfoMap(String firstStoreId, int zoneId, int zoneType, String currency) {
        Map<String, String> storeInfoMap = new HashMap<>();
        storeInfoMap.put(CSVHeaders.StoreZone.STORE_ID, firstStoreId);
        storeInfoMap.put(CSVHeaders.StoreZone.ZONE_ID, String.valueOf(zoneId));
        storeInfoMap.put(CSVHeaders.StoreZone.ZONE_TYPE, String.valueOf(zoneType));
        storeInfoMap.put(CSVHeaders.StoreZone.CURRENCY_CODE, currency);

        return storeInfoMap;
    }

    private ProductVariant createProductVariant(String tpnc, int zoneId, String price, Promotion promotion) {
        ProductVariant productVariant = new ProductVariant(tpnc);
        SaleInfo saleInfo = new SaleInfo(zoneId, price);
        if (promotion != null){
            saleInfo.addPromotion(promotion);
        }
        productVariant.addSaleInfo(saleInfo);
        /*Added by Nitisha and Surya for PS-112 Junits corrections- Start */
        productVariant.setSellingUOM(sellingUOM);
        /*Added by Nitisha and Surya for PS-112 Junits corrections- End */

        return productVariant;
    }

    private Product createProductWithVariant(String tpnb, String tpnc) {
        ProductVariant productVariant = createProductVariant(tpnc, 2, "2.4", null);
        Product product = createProduct(tpnb, productVariant);

        return product;
    }

    private Product createProduct(String tpnb, ProductVariant productVariant) {
        Product product = new Product(tpnb);
        product.addProductVariant(productVariant);
        product.setLast_updated_date(Dockyard.getSysDate("yyyyMMdd"));
        return product;
    }

    private String getTPNCForTPNB(String tpnc) {

        return tpnc;
    }

    private Promotion aPromotionWithDescriptions() {
        Promotion promotion = new Promotion();
        promotion.setUniqueKey("uuid");
        promotion.setItemNumber("itemNumber");
        promotion.setZoneId(zoneId);
        promotion.setOfferId("promotionOfferId");
        promotion.setOfferName("promotionOfferName");
        promotion.setEffectiveDate("promotionStartDate");
        promotion.setEndDate("promotionEndDate");
        promotion.setCFDescription1("promotionCfDesc1");
        promotion.setCFDescription2("promotionCfDesc2");
        return promotion;
    }
    private void mockAsyncProductInsert() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Listener<Void,Exception>) invocation.getArguments()[1]).onComplete(null);
                return null;
            }
        }).when(productRepository).insertProduct(any(Product.class), any(Listener.class));
    }
    private void mockAsyncStoreInsert() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Listener<Void,Exception>) invocation.getArguments()[1]).onComplete(null);
                return null;
            }
        }).when(storeRepository).insertStore(any(Store.class), any(Listener.class));
    }

    /**
     * Added By Nibedita - PS-116 - Positive Scenario
     * Given the Prom_extract(RPM - promotion) and prom_desc_extract(CMHOPOS) extract file ,
     * When the Prom_extract file doesn't have data for the item and CMHOPOS file has data and import rest call is triggered
     * then the constructed product shouldn't contain CF descriptions in the promotion information block */
    @Test
    public void shouldNotInsertCFDescWhenPromoExtractNotPresent() throws IOException, ParserConfigurationException, ColumnNotFoundException, SAXException, JAXBException {
        String tpnb = "09098000";
        Product product = new Product(tpnb);
        ProductVariant productVariant = createProductVariant("23232323", 1, "1", null);
        productVariant.addSaleInfo(new SaleInfo(5, "2"));
        product.addProductVariant(productVariant);

        String offerId = "A01";
        String description1 = "description1";
        String description2 = "description2";

        when(rpmPromotionDescReader.getNext()).thenReturn(promotionDescInfoMap(tpnb, zoneId, offerId, description1, description2)).thenReturn(null);
        mockAsyncProductInsert();

        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.of(product));
        when(productRepository.getProductTPNC(tpnb)).thenReturn(getTPNCForTPNB("23232323"));
        this.rpmWriter.write();
        final Product expectedProduct = product;
        verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod, expectedProduct);
            }
        }), any(Listener.class));
    }
    /**
     * Added By Nibedita - PS-116 - Negetive Scenario
     * Given the Prom_zone, Prom_extract(RPM - promotion) and prom_desc_extract(CMHOPOS) extract ,
     * When the prom_zone extract doesn't have data for a item and other 2 extracts contains data and import rest call is triggered
     * then the constructed product should contain promotion information with no price information */

    @Test
    public void shouldInsertCFDescWhenPromoZoneNotPresent() throws IOException,ParserConfigurationException,JAXBException,SAXException,ColumnNotFoundException {
        final String tpnc = "284347092";
        int zoneIds = 5;
        String price = null;
        String tpnb = "070461113";
        String offerId = "A01";
        String offerId2 = "A02";
        String offerName = "Test Offer Name";
        String startDate = "20130729";
        String endDate = "20130819";
        String description1 = "description1";
        String description2 = "description2";

        when(rpmPromotionReader.getNext()).thenReturn(promotionInfoMap(tpnb, tpnc, zoneIds, offerId, offerName, startDate, endDate)).thenReturn(null);
        when(rpmPromotionDescReader.getNext()).thenReturn(promotionDescInfoMap(tpnb, zoneIds, offerId, description1, description2)).thenReturn(null);
        mockAsyncProductInsert();

        ProductVariant productVariant = createProductVariant(tpnc, zoneIds, price, null);
        ProductVariant productVariant2 = createProductVariant(tpnc, zoneIds, price, createPromotion(offerId, zoneIds, offerName, startDate, endDate));
        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.of(createProduct(tpnb, productVariant)), Optional.of(createProduct(tpnb, productVariant2)));
        when(productRepository.getProductTPNC(tpnb)).thenReturn(getTPNCForTPNB(tpnc));

        this.rpmWriter.write();

        ArgumentCaptor<Product> arguments = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).insertProduct(arguments.capture(), any(Listener.class));

        Promotion expectedPromotion = createPromotion(offerId, zoneIds, offerName, startDate, endDate);
        ProductVariant expectedProductVariant = createProductVariant(tpnc, zoneIds, price, expectedPromotion);

        Product expectedProduct = createProduct(tpnb, expectedProductVariant);

        List<Product> actualProducts = arguments.getAllValues();
        assertThat(actualProducts.get(0)).isEqualTo(expectedProduct);

        expectedPromotion.setCFDescription1(description1);
        expectedPromotion.setCFDescription2(description2);
        assertThat(actualProducts.get(1)).isEqualTo(expectedProduct);
    }
    /**
     * Added By Nitisha - PS-112 - Apending SellingUOM info to Product variant
     * Given the tpnb , tpnc ,Price_zone, selling reatil and selling UOM details ,
     * The product variant should carry the newly added SellingUOM details while building a PRODUCT */

    @Test
    public void shouldAppendSellingUOMForPriceZoneExtarct() throws IOException, ParserConfigurationException, ColumnNotFoundException, SAXException, JAXBException {
        String tpnb = "123456789";
        String tpnc = "987654321";
        ProductVariant productVariant = new ProductVariant(tpnc);
        String price = "2.4";
        productVariant.addSaleInfo(new SaleInfo(zoneId, price));
        productVariant.setSellingUOM(sellingUOM);
        final Product product = createProduct(tpnb, productVariant);
        mockAsyncProductInsert();
        Map<String, String> productInfoMap = productInfoMap(tpnb,tpnc, zoneId, price);
        when(rpmPriceReader.getNext()).thenReturn(productInfoMap).thenReturn(null);
        when(productRepository.getByTPNB(tpnb)).thenReturn(Optional.<Product>absent());
        this.rpmWriter.write();
        verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,product);
            }
        }),any(Listener.class));

    }
    /**
     * Added By Abrar - PS-112 - Insert Null SellingUOM when Selling UOM data is not present in the Extract
     * When the selling UOM is NULL(Data not available in Extract), a Null should be inserted. This test case will pass only if Mocked(Null SellingUOM) and Expected(Null sellingUOM) Products are same */
@Test
    public void shouldInsertNullwhenSellingUOMdataisMissinginExtarct() throws IOException, ParserConfigurationException, ColumnNotFoundException, SAXException, JAXBException {
        String TPNB = "123456789";
        String TPNC = "987654321";
        String selling_retail = "1.0";
        ProductVariant productVariant = new ProductVariant(TPNC);
        SaleInfo saleInfo = new SaleInfo(zoneId, selling_retail);
        productVariant.addSaleInfo(saleInfo);
        final Product expectedProduct = createProduct(TPNB,productVariant);
        Map<String, String> productInfoMap = new HashMap<>();
        productInfoMap.put(CSVHeaders.Price.ITEM, TPNB);
        productInfoMap.put(CSVHeaders.Price.TPNC, TPNC);
        productInfoMap.put(CSVHeaders.Price.PRICE_ZONE_ID, String.valueOf(zoneId));
        productInfoMap.put(CSVHeaders.Price.PRICE_ZONE_PRICE, selling_retail);
        when(rpmPriceReader.getNext()).thenReturn(productInfoMap).thenReturn(null);
        when(productRepository.getByTPNB(TPNB)).thenReturn(Optional.<Product>absent());

        this.rpmWriter.write();
        verify(productRepository).insertProduct(argThat(new CapturingMatcher<Product>() {
            @Override
            public boolean matches(Object o) {
                Product prod = (Product) o;
                return new RPMComparator().compare(prod,expectedProduct);
            }
        }),any(Listener.class));



    }

}
