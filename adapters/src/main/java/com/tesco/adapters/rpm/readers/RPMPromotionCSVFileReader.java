package com.tesco.adapters.rpm.readers;

import au.com.bytecode.opencsv.CSVReader;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tesco.adapters.core.exceptions.ColumnNotFoundException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.tesco.adapters.core.PriceKeys.*;
import static com.tesco.adapters.core.utils.ExtractionUtils.getHeader;
import static java.util.Arrays.asList;

public class RPMPromotionCSVFileReader implements RPMCSVFileReader {
    private CSVReader csvReader;
    private int itemNumberIndex;
    private int zoneIndex;
    private int offerNameIndex;
    private int startDateIndex;
    private int endDateIndex;
    private int offerIdIndex;

    public RPMPromotionCSVFileReader(String filePath) throws IOException, ColumnNotFoundException {
        csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"), ',');

        List<String> headers = asList(csvReader.readNext());

        itemNumberIndex = getHeader(headers, "TPNB");
        zoneIndex = getHeader(headers, "ZONE_ID");
        offerIdIndex = getHeader(headers, "OFFER_ID");
        offerNameIndex = getHeader(headers, "OFFER_NAME");
        startDateIndex = getHeader(headers, "START_DATE");
        endDateIndex = getHeader(headers, "END_DATE");
    }

    public DBObject getNext() throws IOException {
        String[] nextLine = csvReader.readNext();

        if (nextLine == null) {
            return null;
        }

        BasicDBObject promotion = new BasicDBObject();
        promotion.put(ITEM_NUMBER, nextLine[itemNumberIndex]);
        promotion.put(ZONE_ID, nextLine[zoneIndex]);
        promotion.put(PROMOTION_OFFER_ID, nextLine[offerIdIndex]);
        promotion.put(PROMOTION_OFFER_NAME, nextLine[offerNameIndex]);
        promotion.put(PROMOTION_START_DATE, nextLine[startDateIndex]);
        promotion.put(PROMOTION_END_DATE, nextLine[endDateIndex]);

        return promotion;
    }
}
