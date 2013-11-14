package com.tesco.adapters.core;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.tesco.adapters.core.exceptions.ColumnNotFoundException;
import com.tesco.core.DBFactory;
import com.tesco.core.DataGridResource;
import com.tesco.services.resources.TestConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static com.mongodb.QueryBuilder.start;
import static com.tesco.adapters.core.TestFiles.*;
import static com.tesco.core.PriceKeys.*;

public class ControllerIntegrationTest {
    protected DBCollection priceCollection;
    protected DBCollection storeCollection;
    protected DBCollection promotionCollection;
    protected DBCollection tempPriceCollection;
    protected DBCollection tempStoreCollection;
    protected DBCollection tempPromotionCollection;

    protected DataGridResource dataGridResource;

    @Before
    public void setUp() throws IOException, ParserConfigurationException, SAXException, ConfigurationException, JAXBException, ColumnNotFoundException {
        System.out.println("ControllerIntegrationTest setup");
        TestConfiguration configuration = new TestConfiguration();
        dataGridResource = new DataGridResource();

        DBFactory dbFactory = new DBFactory(configuration);

        dbFactory.getCollection(PRICE_COLLECTION).drop();
        priceCollection = dbFactory.getCollection(PRICE_COLLECTION);

        dbFactory.getCollection(STORE_COLLECTION).drop();
        storeCollection = dbFactory.getCollection(STORE_COLLECTION);

        dbFactory.getCollection(PROMOTION_COLLECTION).drop();
        promotionCollection = dbFactory.getCollection(PROMOTION_COLLECTION);

        dbFactory.getCollection(priceCollection + "_temp").drop();
        tempPriceCollection = dbFactory.getCollection(priceCollection + "_temp");
        dbFactory.getCollection(STORE_COLLECTION + "_temp").drop();
        tempStoreCollection = dbFactory.getCollection(STORE_COLLECTION + "_temp");
        dbFactory.getCollection(promotionCollection + "_temp").drop();
        tempPromotionCollection = dbFactory.getCollection(promotionCollection + "_temp");


        Controller controller = new Controller(
                RPM_PRICE_ZONE_CSV_FILE_PATH,
                RPM_STORE_ZONE_CSV_FILE_PATH,
                RPM_PROMOTION_CSV_FILE_PATH,
                SONETTO_PROMOTIONS_XML_FILE_PATH,
                RPM_PROMOTION_DESC_CSV_FILE_PATH, SONETTO_PROMOTIONS_XSD_FILE_PATH,
                "http://ui.tescoassets.com/Groceries/UIAssets/I/Sites/Retail/Superstore/Online/Product/pos/%s.png", dataGridResource.getPromotionCache());

        controller.processData(tempPriceCollection, tempStoreCollection, tempPromotionCollection, false);
    }

    @After
    public void tearDown() throws Exception {
        dataGridResource.stop();

    }

    protected DBObject findPricesFromZone(String itemNumber, String zoneId) {
        DBObject queryForItemNumber = start(ITEM_NUMBER).is(itemNumber).get();
        DBObject prices = priceCollection.findOne(queryForItemNumber);
        return (DBObject) ((DBObject) prices.get(ZONES)).get(zoneId);
    }
}