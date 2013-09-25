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

public class RPMPriceZoneCSVFileReader implements RPMCSVFileReader {

    private static final String COLUMN_FORMAT = "%s.%s.%s";

    private final CSVReader csvReader;
    private final int itemNumberIndex;
    private final int zoneIdIndex;
    private final int priceIndex;
    private final int promotionalPriceIndex;

    public RPMPriceZoneCSVFileReader(String filePath) throws IOException, ColumnNotFoundException {
        csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"), ',');

        List<String> headers = asList(csvReader.readNext());

        itemNumberIndex = getHeader(headers, "ITEM");
        zoneIdIndex = getHeader(headers, "ZONE_ID");
        priceIndex = getHeader(headers, "SELLING_RETAIL");
        promotionalPriceIndex = getHeader(headers, "SIMPLE_PROMO_RETAIL");
    }

    public DBObject getNext() throws IOException {
        String[] nextLine = csvReader.readNext();

        if(nextLine == null){
            return null;
        }

        String itemNumber = nextLine[itemNumberIndex];
        String zoneId = nextLine[zoneIdIndex];
        String price = nextLine[priceIndex];
        String promotionalPrice = nextLine[promotionalPriceIndex];

        DBObject priceObject = new BasicDBObject();
        priceObject.put(ITEM_NUMBER, itemNumber);
        priceObject.put(String.format(COLUMN_FORMAT, ZONES, zoneId, PRICE), price);
        priceObject.put(String.format(COLUMN_FORMAT, ZONES, zoneId, PROMO_PRICE), promotionalPrice);

        return priceObject;
    }
}
