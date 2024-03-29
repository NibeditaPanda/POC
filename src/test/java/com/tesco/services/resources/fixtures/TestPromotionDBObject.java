package com.tesco.services.resources.fixtures;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tesco.services.core.Promotion;

public class TestPromotionDBObject {

    private String offerId;
    private String offerName = "default";
    private String effectiveDate = "default";
    private String endDate = "default";
    private String cfDescription1 = "default";
    private String cfDescription2 = "default";
    private String itemNumber = "default";
    private int promotionZoneId = -1;
    private String shelfTalkerImage = "default";
    private String offerText = "default";

    public TestPromotionDBObject(String offerId){
        this.offerId = offerId;
    }

    public TestPromotionDBObject withName(String offerName){
        this.offerName = offerName;
        return this;
    }

    public TestPromotionDBObject withStartDate(String startDate){
        this.effectiveDate = startDate;
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

    public TestPromotionDBObject withPromotionZone(int promotionZoneId){
        this.promotionZoneId = promotionZoneId;
        return this;
    }

    public TestPromotionDBObject withShelfTalker(String shelfTalkerImage) {
        this.shelfTalkerImage = shelfTalkerImage;
        return this;
    }

    public TestPromotionDBObject withOfferText(String offerText) {
        this.offerText = offerText;
        return this;
    }

    public DBObject build() {
        BasicDBObject promotion = new BasicDBObject();
        promotion.put("itemNumber", itemNumber);
        promotion.put("offerId", offerId);
        promotion.put("offerName", offerName);
        promotion.put("startDate", effectiveDate);
        promotion.put("endDate", endDate);
        promotion.put("CFDescription1", cfDescription1);
        promotion.put("CFDescription2", cfDescription2);
        promotion.put("zoneId", promotionZoneId);
        promotion.put("shelfTalkerImage", shelfTalkerImage);
        promotion.put("offerText", offerText);
        return promotion;
    }

    public Promotion buildJDG() {
        Promotion promotion = new Promotion();
        promotion.setItemNumber(itemNumber);
        promotion.setOfferId(offerId);
        promotion.setOfferName(offerName);
        promotion.setEffectiveDate(effectiveDate);
        promotion.setEndDate(endDate);
        promotion.setCFDescription1(cfDescription1);
        promotion.setCFDescription2(cfDescription2);
        promotion.setZoneId(promotionZoneId);
        promotion.setShelfTalkerImage(shelfTalkerImage);
        return promotion;
    }
}
