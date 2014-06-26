package com.tesco.services.adapters.core;

import com.couchbase.client.CouchbaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesco.couchbase.AsyncCouchbaseWrapper;
import com.tesco.couchbase.CouchbaseWrapper;
import com.tesco.services.adapters.core.exceptions.ColumnNotFoundException;
import com.tesco.services.adapters.rpm.readers.PriceServiceCSVReader;
import com.tesco.services.adapters.rpm.readers.PriceServiceCSVReaderImpl;
import com.tesco.services.adapters.rpm.writers.CSVHeaders;
import com.tesco.services.adapters.rpm.writers.RPMWriter;
import com.tesco.services.adapters.sonetto.SonettoPromotionXMLReader;
import com.tesco.services.repositories.*;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

public class ImportJob implements Runnable {

    private static final Logger logger = getLogger("Price_ImportJob");

    private final String rpmStoreZoneCsvFilePath;
    private String sonettoPromotionXSDDataPath;
    private String rpmPriceZoneDataPath;
    private String rpmPromoZoneDataPath;
    private String rpmPromoExtractDataPath;
    private String rpmPromoDescExtractDataPath;
    private CouchbaseConnectionManager couchbaseConnectionManager;
    private CouchbaseWrapper couchbaseWrapper;
    private AsyncCouchbaseWrapper asyncCouchbaseWrapper;

    private String sonettoPromotionsXMLFilePath;
    private String sonettoShelfImageUrl;

    public  ImportJob(String rpmStoreZoneCsvFilePath,
                      String sonettoPromotionsXMLFilePath,
                      String sonettoPromotionXSDDataPath,
                      String sonettoShelfImageUrl,
                      String rpmPriceZoneDataPath,
                      String rpmPromZoneDataPath,
                      String rpmPromoExtractDataPath,
                      String rpmPromoDescExtractDataPath,
                      CouchbaseConnectionManager couchbaseConnectionManager) {
        this.rpmStoreZoneCsvFilePath = rpmStoreZoneCsvFilePath;
        this.sonettoPromotionsXMLFilePath = sonettoPromotionsXMLFilePath;
        this.sonettoPromotionXSDDataPath = sonettoPromotionXSDDataPath;
        this.sonettoShelfImageUrl = sonettoShelfImageUrl;
        this.rpmPriceZoneDataPath = rpmPriceZoneDataPath;
        this.rpmPromoZoneDataPath = rpmPromZoneDataPath;
        this.rpmPromoExtractDataPath = rpmPromoExtractDataPath;
        this.rpmPromoDescExtractDataPath = rpmPromoDescExtractDataPath;
        this.couchbaseConnectionManager = couchbaseConnectionManager;
    }
    public  ImportJob(String rpmStoreZoneCsvFilePath,
                      String sonettoPromotionsXMLFilePath,
                      String sonettoPromotionXSDDataPath,
                      String sonettoShelfImageUrl,
                      String rpmPriceZoneDataPath,
                      String rpmPromZoneDataPath,
                      String rpmPromoExtractDataPath,
                      String rpmPromoDescExtractDataPath,
                      CouchbaseWrapper couchbaseWrapper,
                      AsyncCouchbaseWrapper asyncCouchbaseWrapper) {
        this.rpmStoreZoneCsvFilePath = rpmStoreZoneCsvFilePath;
        this.sonettoPromotionsXMLFilePath = sonettoPromotionsXMLFilePath;
        this.sonettoPromotionXSDDataPath = sonettoPromotionXSDDataPath;
        this.sonettoShelfImageUrl = sonettoShelfImageUrl;
        this.rpmPriceZoneDataPath = rpmPriceZoneDataPath;
        this.rpmPromoZoneDataPath = rpmPromZoneDataPath;
        this.rpmPromoExtractDataPath = rpmPromoExtractDataPath;
        this.rpmPromoDescExtractDataPath = rpmPromoDescExtractDataPath;
        this.couchbaseWrapper = couchbaseWrapper;
        this.asyncCouchbaseWrapper = asyncCouchbaseWrapper;
    }

    @Override
    public void run() {
        try {
            logger.info("Firing up imports...");

            fetchAndSavePriceDetails();

            logger.info("Successfully imported data for " + new Date());

        } catch (Exception exception) {
            logger.error("Error importing data", exception);
            // TODO: Do error handling / recovery. As per previous implementation using Mongo, the files were deleted.
        }
    }

    private void fetchAndSavePriceDetails() throws IOException, ParserConfigurationException, ConfigurationException, JAXBException, ColumnNotFoundException, SAXException, URISyntaxException, InterruptedException {
        logger.info("Importing data from RPM....");
        SonettoPromotionXMLReader sonettoPromotionXMLReader = new SonettoPromotionXMLReader(sonettoShelfImageUrl, sonettoPromotionXSDDataPath);

        UUIDGenerator uuidGenerator = new UUIDGenerator();
        ObjectMapper mapper = new ObjectMapper();

       /* final CouchbaseClient couchbaseClient = couchbaseConnectionManager.getCouchbaseClient();
        PromotionRepository promotionRepository = new PromotionRepository(uuidGenerator, couchbaseClient);
        ProductRepository productRepository = new ProductRepository(couchbaseClient);
         StoreRepository storeRepository = new StoreRepository(couchbaseClient);*/
        //AsyncReadWriteProductRepository asyncReadWriteProductRepository = new AsyncReadWriteProductRepository(couchbaseClient);
        //final CouchbaseClient couchbaseClient = couchbaseConnectionManager.getCouchbaseClient();

        PromotionRepository promotionRepository = new PromotionRepository(uuidGenerator, couchbaseWrapper);
        ProductRepository productRepository = new ProductRepository(couchbaseWrapper,asyncCouchbaseWrapper,mapper);
       // AsyncReadWriteProductRepository asyncReadWriteProductRepository = new AsyncReadWriteProductRepository(asyncCouchbaseWrapper,mapper);
        StoreRepository storeRepository = new StoreRepository(couchbaseWrapper,asyncCouchbaseWrapper,mapper);

        PriceServiceCSVReader rpmPriceReader = new PriceServiceCSVReaderImpl(rpmPriceZoneDataPath, CSVHeaders.Price.PRICE_ZONE_HEADERS);
        PriceServiceCSVReader rpmPromoPriceReader = new PriceServiceCSVReaderImpl(rpmPromoZoneDataPath, CSVHeaders.Price.PROMO_ZONE_HEADERS);
        PriceServiceCSVReader storeZoneReader = new PriceServiceCSVReaderImpl(rpmStoreZoneCsvFilePath, CSVHeaders.StoreZone.HEADERS);
        PriceServiceCSVReader rpmPromotionReader = new PriceServiceCSVReaderImpl(rpmPromoExtractDataPath, CSVHeaders.PromoExtract.HEADERS);
        PriceServiceCSVReader rpmPromotionDescReader = new PriceServiceCSVReaderImpl(rpmPromoDescExtractDataPath, CSVHeaders.PromoDescExtract.HEADERS);

        new RPMWriter(sonettoPromotionsXMLFilePath,
                sonettoPromotionXMLReader,
                promotionRepository,
                productRepository,
                storeRepository,
                rpmPriceReader,
                rpmPromoPriceReader,
                storeZoneReader,
                rpmPromotionReader,
                rpmPromotionDescReader)
                .write();
    }
}
