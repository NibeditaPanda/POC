package com.tesco.adapters.rpm;

import au.com.bytecode.opencsv.CSVReader;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static com.tesco.adapters.core.PriceKeys.*;

public class RPMPromotionCSVFileReader implements RPMCSVFileReader {
    private CSVReader csvReader;
    private int itemNumberIndex;
    private int zoneIndex;
    private int offerNameIndex;
    private int startDateIndex;
    private int endDateIndex;
    private int cfDesc1Index;
    private int cfDesc2Index;
    private int offerIdIndex;

    public RPMPromotionCSVFileReader(String filePath) throws IOException {
        csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"), ',');

        List<String> headers = Arrays.asList(csvReader.readNext());
        itemNumberIndex = headers.indexOf("TPNB");
        zoneIndex = headers.indexOf("ZONE_ID");
        offerIdIndex = headers.indexOf("OFFER_ID");
        offerNameIndex = headers.indexOf("OFFER_NAME");
        startDateIndex = headers.indexOf("START_DATE");
        endDateIndex = headers.indexOf("END_DATE");
    }

    public DBObject getNext() throws IOException {
        String[] nextline = csvReader.readNext();

        if (nextline == null) {
            return null;
        }

        BasicDBObject promotion = new BasicDBObject();
        promotion.put(ITEM_NUMBER, nextline[itemNumberIndex]);
        promotion.put(ZONE_ID, nextline[zoneIndex]);
        promotion.put(PROMOTION_OFFER_ID, nextline[offerIdIndex]);
        promotion.put(PROMOTION_OFFER_NAME, nextline[offerNameIndex]);
        promotion.put(PROMOTION_START_DATE, nextline[startDateIndex]);
        promotion.put(PROMOTION_END_DATE, nextline[endDateIndex]);

        return promotion;
    }
}
