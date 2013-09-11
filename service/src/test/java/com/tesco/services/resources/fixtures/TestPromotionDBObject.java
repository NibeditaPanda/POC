package com.tesco.services.resources.fixtures;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class TestPromotionDBObject {

    private String offerId;
    private String offerName = "default";
    private String startDate = "default";
    private String endDate = "default";
    private String cfDescription1 = "default";
    private String cfDescription2 = "default";
    private String itemNumber = "default";
    private String promotionZoneId = "default";

    public TestPromotionDBObject(String offerId){
        this.offerId = offerId;
    }

    public TestPromotionDBObject withName(String offerName){
        this.offerName = offerName;
        return this;
    }

    public TestPromotionDBObject withStartDate(String startDate){
        this.startDate = startDate;
        return this;
    }

    public TestPromotionDBObject withEndDate(String endDate){
        this.endDate = endDate;
        return this;
    }

    public TestPromotionDBObject withDescription1(String description1){
        this.cfDescription1 = description1;
        return this;
    }

    public TestPromotionDBObject withDescription2(String description2){
        this.cfDescription2 = description2;
        return this;
    }

    public TestPromotionDBObject withTPNB(String tpnb){
        this.itemNumber = tpnb;
        return this;
    }

    public TestPromotionDBObject withPromotionZone(String promotionZoneId){
        this.promotionZoneId = promotionZoneId;
        return this;
    }

    public DBObject build() {
        BasicDBObject promotion = new BasicDBObject();
        promotion.put("itemNumber", itemNumber);
        promotion.put("offerId", offerId);
        promotion.put("offerName", offerName);
        promotion.put("startDate", startDate);
        promotion.put("endDate", endDate);
        promotion.put("cfDescription1", cfDescription1);
        promotion.put("cfDescription2", cfDescription2);
        promotion.put("promotionZoneId", promotionZoneId);
        return promotion;
    }
}
