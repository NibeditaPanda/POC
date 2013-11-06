package com.tesco.services.resources;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.tesco.core.Configuration;
import com.tesco.services.DAO.PromotionDAO;
import com.tesco.core.DBFactory;
import com.tesco.services.resources.fixtures.TestPromotionDBObject;
import com.tesco.services.resources.fixtures.TestStoreDBObject;
import com.yammer.dropwizard.testing.ResourceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.tesco.core.PriceKeys.PROMOTION_COLLECTION;
import static com.tesco.core.PriceKeys.STORE_COLLECTION;
import static org.fest.assertions.api.Assertions.assertThat;

public class PromotionResourceTest extends ResourceTest {

    private static Configuration testConfiguration = new TestConfiguration();
    private static DBCollection promotionCollection;

    @Override
    protected void setUpResources() throws Exception {
        PromotionResource offerResource = new PromotionResource(new PromotionDAO(testConfiguration));
        addResource(offerResource);
    }

    @BeforeClass
    public static void setUp() throws IOException {
        DBFactory dbFactory = new DBFactory(testConfiguration);
        dbFactory.getCollection(PROMOTION_COLLECTION).drop();
        dbFactory.getCollection(STORE_COLLECTION).drop();

        DBCollection storeCollection = dbFactory.getCollection(STORE_COLLECTION);
        storeCollection.insert(new TestStoreDBObject("2000").withZoneId("5").build());

        promotionCollection = dbFactory.getCollection(PROMOTION_COLLECTION);

        promotionCollection.insert(new TestPromotionDBObject("123").withTPNB("1234").withPromotionZone("5").withStartDate("date1").withEndDate("date2").withName("name of promotion").withDescription1("blah").withDescription2("blah").withShelfTalker("OnSale.png").build());
        promotionCollection.insert(new TestPromotionDBObject("123").withTPNB("5678").withPromotionZone("4").withStartDate("date1").withEndDate("date2").withName("name of promotion").withDescription1("blah").withDescription2("blah").build());
        promotionCollection.insert(new TestPromotionDBObject("567").withTPNB("5678").withPromotionZone("4").withStartDate("date1").withEndDate("date2").withName("name of promotion").withDescription1("blah").withDescription2("blah").build());

        promotionCollection.insert(new TestPromotionDBObject("345").build());
    }

    @Test
    public void shouldReturnMultiplePromotions() throws Exception {
        String jsonRequest = "{\n" +
                "    \"promotions\":\n" +
                "        [\n" +
                "            { \"offerId\": \"123\", \"itemNumber\": \"1234\", \"zoneId\": \"5\"},\n" +
                "            { \"offerId\": \"567\", \"itemNumber\": \"5678\", \"zoneId\": \"4\"}\n" +
                "        ]\n" +
                "}";

        WebResource resource = client().resource("/promotion/find");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, jsonRequest);
        assertThat(response.getStatus()).isEqualTo(200);

        List<DBObject> promotions = (List<DBObject>) JSON.parse(resource.type("application/json").post(String.class, jsonRequest));

        assertThat(promotions.size()).isEqualTo(2);

        DBObject firstPromotion = promotions.get(0);
        assertThat(firstPromotion.get("offerId")).isEqualTo("567");
        assertThat(firstPromotion.get("itemNumber")).isEqualTo("5678");
        assertThat(firstPromotion.get("zoneId")).isEqualTo("4");
        assertThat(firstPromotion.get("offerName")).isEqualTo("name of promotion");
        assertThat(firstPromotion.get("startDate")).isEqualTo("date1");
        assertThat(firstPromotion.get("endDate")).isEqualTo("date2");
        assertThat(firstPromotion.get("CFDescription1")).isEqualTo("blah");
        assertThat(firstPromotion.get("CFDescription2")).isEqualTo("blah");
        assertThat(firstPromotion.get("offerText")).isEqualTo("default");

        DBObject secondPromotion = promotions.get(1);
        assertThat(secondPromotion.get("offerId")).isEqualTo("123");
        assertThat(secondPromotion.get("itemNumber")).isEqualTo("1234");
        assertThat(secondPromotion.get("zoneId")).isEqualTo("5");
        assertThat(secondPromotion.get("offerName")).isEqualTo("name of promotion");
        assertThat(secondPromotion.get("startDate")).isEqualTo("date1");
        assertThat(secondPromotion.get("endDate")).isEqualTo("date2");
        assertThat(secondPromotion.get("CFDescription1")).isEqualTo("blah");
        assertThat(secondPromotion.get("CFDescription2")).isEqualTo("blah");
        assertThat(secondPromotion.get("shelfTalkerImage")).isEqualTo("OnSale.png");
        assertThat(secondPromotion.get("offerText")).isEqualTo("default");

    }

    @Test
    public void shouldReturnEmptyList() throws Exception {
        String jsonRequest = "{\n" +
                "    \"promotions\":\n" +
                "        [\n" +
                "            { \"offerId\": \"something wrong\", \"itemNumber\": \"something wrong\", \"zoneId\": \"5\"}" +
                "        ]\n" +
                "}";

        WebResource resource = client().resource("/promotion/find");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, jsonRequest);
        assertThat(response.getStatus()).isEqualTo(200);

        List<DBObject> promotions = (List<DBObject>) JSON.parse(resource.type("application/json").post(String.class, jsonRequest));

        assertThat(promotions).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListGivenMissingAttribute() throws Exception {
        String jsonRequest = "{\n" +
                "    \"promotions\":\n" +
                "        [\n" +
                "            { \"offerId\": \"123\" }" +
                "        ]\n" +
                "}";

        WebResource resource = client().resource("/promotion/find");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, jsonRequest);
        assertThat(response.getStatus()).isEqualTo(200);

        List<DBObject> promotions = (List<DBObject>) JSON.parse(resource.type("application/json").post(String.class, jsonRequest));

        assertThat(promotions).isEmpty();
    }

    @Test
    public void shouldReturnValueForCorrectRequestItemOnly() throws Exception {
        String jsonRequest = "{\n" +
                "    \"promotions\":\n" +
                "        [\n" +
                "            { \"offerId\": \"123\", \"itemNumber\": \"1234\", \"zoneId\": \"5\"},\n" +
                "            { \"offerId\": \"567\"}\n" +
                "        ]\n" +
                "}";

        WebResource resource = client().resource("/promotion/find");
        ClientResponse response = resource.type("application/json").post(ClientResponse.class, jsonRequest);
        assertThat(response.getStatus()).isEqualTo(200);

        List<DBObject> promotions = (List<DBObject>) JSON.parse(resource.type("application/json").post(String.class, jsonRequest));

        assertThat(promotions.size()).isEqualTo(1);

        DBObject firstPromotion = promotions.get(0);
        assertThat(firstPromotion.get("offerId")).isEqualTo("123");
        assertThat(firstPromotion.get("itemNumber")).isEqualTo("1234");
        assertThat(firstPromotion.get("zoneId")).isEqualTo("5");
        assertThat(firstPromotion.get("offerName")).isEqualTo("name of promotion");
        assertThat(firstPromotion.get("startDate")).isEqualTo("date1");
        assertThat(firstPromotion.get("endDate")).isEqualTo("date2");
        assertThat(firstPromotion.get("CFDescription1")).isEqualTo("blah");
        assertThat(firstPromotion.get("CFDescription2")).isEqualTo("blah");
        assertThat(firstPromotion.get("shelfTalkerImage")).isEqualTo("OnSale.png");
        assertThat(firstPromotion.get("offerText")).isEqualTo("default");

    }
}
