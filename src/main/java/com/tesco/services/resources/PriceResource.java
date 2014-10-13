package com.tesco.services.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.tesco.couchbase.AsyncCouchbaseWrapper;
import com.tesco.couchbase.CouchbaseWrapper;
import com.tesco.couchbase.exceptions.CouchbaseOperationException;
import com.tesco.services.core.Product;
import com.tesco.services.core.Store;
import com.tesco.services.repositories.CouchbaseConnectionManager;
import com.tesco.services.repositories.ProductRepository;
import com.tesco.services.repositories.StoreRepository;
import com.tesco.services.resources.model.ProductPriceBuilder;
import com.tesco.services.utility.Dockyard;
import com.wordnik.swagger.annotations.*;
import com.yammer.metrics.annotation.ExceptionMetered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.google.common.io.Files.readLines;
import static com.tesco.services.resources.HTTPResponses.*;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.lang.StringUtils.isBlank;

@Path("/{versionIdentifier}/price")
@Api(value = "/{versionIdentifier}/price", description = "Price API")
@Produces(ResourceResponse.RESPONSE_TYPE)
public class PriceResource {

    /*Added by Sushil - PS-83 added logger to log exceptions -Start*/
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceResource.class);

    public static final int NATIONAL_PRICE_ZONE_ID = 1;
    public static final String NATIONAL_ZONE_CURRENCY = "GBP";
    private static final int NATIONAL_PROMO_ZONE_ID = 5;

    public static final String STORE_NOT_FOUND = "Store not found";
    public static final String PRODUCT_NOT_FOUND = "Product not found";
    public static final String VERSION_MISMATCH = "Version Mismatch";
    /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - Start*/
    public static final String REQUEST_NOT_ALLOWED = "TPN Identifier and Value Mismatch - Invalid Request";
    /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - Start*/
    private static final String PRODUCT_OR_STORE_NOT_FOUND = PRODUCT_NOT_FOUND + " / " + STORE_NOT_FOUND;
    private String uriPath = "";

    private CouchbaseConnectionManager couchbaseConnectionManager;
    private CouchbaseWrapper couchbaseWrapper;
    private ObjectMapper mapper;
    private AsyncCouchbaseWrapper asyncCouchbaseWrapper;

    public PriceResource(CouchbaseConnectionManager couchbaseConnectionManager) {
        this.couchbaseConnectionManager = couchbaseConnectionManager;
    }
    public PriceResource(CouchbaseWrapper couchbaseWrapper,AsyncCouchbaseWrapper asyncCouchbaseWrapper,ObjectMapper mapper) {
        this.couchbaseWrapper = couchbaseWrapper;
        this.asyncCouchbaseWrapper = asyncCouchbaseWrapper;
        this.mapper = mapper;
    }

    @GET
    @Path("/{tpnIdentifier}/{tpn}")
    @ApiOperation(value = "Find price of product variants by product's base TPNB or variants' TPNC")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message =  PRODUCT_OR_STORE_NOT_FOUND),
            @ApiResponse(code = 400, message = HTTPResponses.INVALID_REQUEST),
            @ApiResponse(code = 500, message = HTTPResponses.INTERNAL_SERVER_ERROR)
    })
    public Response get(
            @ApiParam(value = "Type of Version", required = true) @PathParam("versionIdentifier") String versionIdentifier,
            @ApiParam(value = "Type of identifier(TPNB => TPNB, TPNC => TPNC)", required = true) @PathParam("tpnIdentifier") String tpnIdentifier,
            @ApiParam(value = "TPNB/TPNC of Product", required = true) @PathParam("tpn") String tpn,
            @ApiParam(value = "ID of Store if a store-specific price is desired", required = false) @QueryParam("store") String storeId,
            @Context UriInfo uriInfo) throws IOException {

        uriPath = uriInfo.getRequestUri().toString();
        String version = null;
       // readLines(new File("version"), defaultCharset()).get(0);
        BufferedReader br = new BufferedReader(new FileReader("version"));
        String line=null;
        while( (line=br.readLine()) != null) {
            version = line.trim();
        }

        if(!versionIdentifier.equals(version)){
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + VERSION_MISMATCH + "} -> (" + tpn + ")");
            }
            return notFound(VERSION_MISMATCH);
        }

        if (storeQueryParamWasSentWithoutAStoreID(storeId, uriInfo.getQueryParameters())) {
            if(LOGGER.isInfoEnabled()){
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_BAD_REQUEST + "- {" + HTTPResponses.INVALID_REQUEST + "}");
            }
            return badRequest();
          }

        ProductRepository productRepository = new ProductRepository(couchbaseWrapper,asyncCouchbaseWrapper,mapper);

        Optional<Product> productContainer ;
       /*Added By Nibedita - PS 37 - fetch info based on TPNC - Start*/
        /*Added By Nitisha - PS 234 - Changed the tpn identifer from "C"/"B"  to "TPNC"/"TPNB"- Start*/
        try {
            if(("TPNC").equalsIgnoreCase(tpnIdentifier)){
            /*Added By Nitisha - PS 234 - Changed the tpn identifer from "C"/"B"  to "TPNC"/"TPNB"- END*/
                try {
                int item = Integer.parseInt(tpn);
            }catch(NumberFormatException ne) {

                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + PRODUCT_NOT_FOUND + "} -> (" + tpn + ")");
                }
                return notFound(PRODUCT_NOT_FOUND);
            }
   /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - Start*/

            if(tpn.startsWith("0")){
                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_ACCEPTABLE + "- {" + REQUEST_NOT_ALLOWED + "} -> (" + tpn + ")");
                }

            return requestNotAllowed(REQUEST_NOT_ALLOWED);
            }
   /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - End*/

            String tpnb = productRepository.getMappedTPNCorTPNB(tpn);
            if(!Dockyard.isSpaceOrNull(tpnb)&&tpnb.contains("-")) {
                tpnb = tpnb.split("-")[0];
            }
            if(Dockyard.isSpaceOrNull(tpnb)) {

                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + PRODUCT_NOT_FOUND + "} -> (" + tpn + ")");
                }
                return notFound(PRODUCT_NOT_FOUND);
            }
            productContainer = productRepository.getByTPNB(tpnb, tpn);
 /*Added By Nitisha - PS 234 - Changed the tpn identifer from "C"/"B"  to "TPNC"/"TPNB"- Start*/
        } else if(("TPNB").equalsIgnoreCase(tpnIdentifier)) {

    /*Added By Nitisha - PS 234 - Changed the tpn identifer from "C"/"B"  to "TPNC"/"TPNB"- End*/
    /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - Start*/
            try {
                int item = Integer.parseInt(tpn);
            } catch(NumberFormatException ne) {

                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + PRODUCT_NOT_FOUND + "} -> (" + tpn + ")");
                }
                return notFound(PRODUCT_NOT_FOUND);
            }
            if(!tpn.startsWith("0")){
                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_ACCEPTABLE + "- {" + REQUEST_NOT_ALLOWED + "} -> (" + tpn + ")");
                }

                return requestNotAllowed(REQUEST_NOT_ALLOWED);
            }
   /*Added By Surya - PS 30 - Request handling for TPN identifier and value Mismatch  - End*/

            productContainer = productRepository.getByTPNB(tpn);

        }else {

            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_BAD_REQUEST + "- {" + HTTPResponses.INVALID_REQUEST + "}");
            }
            return badRequest();
        }
         /*Added By Nibedita - PS 37 - fetch info based on TPNC - End*/
        if (!productContainer.isPresent()) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + PRODUCT_NOT_FOUND + "} -> (" + tpn + ")");
            }
            return notFound(PRODUCT_NOT_FOUND);
        }

        if (storeId == null) {
            return getPriceResponse(productContainer, Optional.of(NATIONAL_PRICE_ZONE_ID), Optional.of(NATIONAL_PROMO_ZONE_ID), NATIONAL_ZONE_CURRENCY);
        }
        }catch(CouchbaseOperationException e){
            if(LOGGER.isErrorEnabled()) {
                LOGGER.error("error   : {" + uriPath + "} {}- {} -> {}", serverError().getStatus(), HTTPResponses.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            return serverError();
        }

        return getPriceResponse(storeId, productContainer);
    }

    private Response getPriceResponse(String storeIdValue, Optional<Product> productContainer) throws IOException {

        StoreRepository storeRepository = new StoreRepository(couchbaseWrapper,asyncCouchbaseWrapper,mapper);
        int storeId;

        try {
            storeId = Integer.parseInt(storeIdValue);
        } catch (NumberFormatException e) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + STORE_NOT_FOUND + "} -> (" + storeIdValue + ")");
            }
            return notFound(STORE_NOT_FOUND);
        }

        Optional<Store> storeContainer = storeRepository.getByStoreId(String.valueOf(storeId));

        if (!storeContainer.isPresent()) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_NOT_FOUND + "- {" + STORE_NOT_FOUND + "} -> (" + storeIdValue + ")");
            }
            return notFound(STORE_NOT_FOUND);
        }

        Store store = storeContainer.get();
        return getPriceResponse(productContainer, store.getPriceZoneId(), store.getPromoZoneId(), store.getCurrency());
    }

    private Response getPriceResponse(Optional<Product> productContainer, Optional<Integer> priceZoneId, Optional<Integer> promoZoneId, String currency) {
        ProductPriceBuilder productPriceVisitor = new ProductPriceBuilder(priceZoneId, promoZoneId, currency);
        productContainer.get().accept(productPriceVisitor);
        return ok(productPriceVisitor.getPriceInfo());
    }

    @GET
    @Path("/{tpnIdentifier}/{tpn}/{path: .*}")
    @ExceptionMetered(name = "getPriceItemNumber-Failures", group = "PriceServices")
    public Response getItem() {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("message : {" + uriPath + "} " + HttpServletResponse.SC_BAD_REQUEST + "- {" + HTTPResponses.INVALID_REQUEST + "}");
        }
        return badRequest();
    }

    @GET
    @Path("/")
    public Response getRoot(@Context UriInfo uriInfo) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("message : {" + uriInfo.getRequestUri().toString() + "} " + HttpServletResponse.SC_BAD_REQUEST + "- {" + HTTPResponses.INVALID_REQUEST + "}");
        }
        return badRequest();
    }
    /*Added by Sushil - PS-83 added logger to log exceptions -End*/
    private boolean storeQueryParamWasSentWithoutAStoreID(String storeId, MultivaluedMap<String, String> queryParameters) {
        return (queryParameters.size() > 0) && isBlank(storeId);
    }
}
