package ro.incremental.anaf.declaratii;

/**
 * Created by Alex Proca <alex.proca@gmail.com> on 18/03/16.
 */

import org.json.JSONObject;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.glassfish.jersey.media.multipart.*;
import java.io.*;
import com.google.common.io.Files;

@Path("/")
public class ValidateWebService {

    private static final String indexHtml;
    private static final String javascript;

    static {
        indexHtml = cacheResource("index.html");
        javascript = cacheResource("javascript.js");
    }

    // @GET
    // @Path("/")
    // @Produces(MediaType.TEXT_HTML)
    // public Response index() {
    //     return Response.ok(indexHtml).build();
    // }

    // @GET
    // @Path("/javascript.js")
    // @Produces(MediaType.TEXT_HTML)
    // public Response javascript() {
    //     return Response.ok(javascript).build();
    // }

    @GET
    @Path("/robots.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public String robots() {
        return "User-agent: *\nDisallow: /\n";
    }

    @GET
    @Path("/available")
    @Produces(MediaType.TEXT_PLAIN)
    public String available() {
        return "yes";
    }

    @GET
    @Path("/download/{id}")
    @Produces("application/pdf")
    public Response download(@PathParam("id") String id) {

        Result result = Result.getFromCache(id);

        if (result == null) {
            return Response.noContent().build();
        }

        Response.ResponseBuilder response = Response.ok(result.pdfFile);
        response.header("Content-Disposition", "attachment; filename=\"" + result.decName + ".pdf\"");

        return response.build();
    }

    @POST
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response available(JSONObject input) {

        Result result = Result.generateFromXMLString(json2Xml(input), getDeclName(input));

        if (result.getHashCode() != null) {
            Result.cacheResult(result);
        }

        return Response.ok(result.toJSON(), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadXmlFile(
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
        @FormDataParam("decName") String decName,
        @QueryParam("sync") @DefaultValue("false") boolean sync) {

        if (uploadedInputStream == null || fileDetail == null || decName == null || decName.isEmpty()) {
            // Return JSON error response with 400 status
            String errorMessage = "No file uploaded or missing declaration name";
            Result result = new Result(errorMessage, -9);
            return Response.ok(result.toJSON(), MediaType.APPLICATION_JSON).build();
        }

        try {
            String lowerCaseDecName = decName.toLowerCase();
            Result result = Result.generateFromXMLStream(uploadedInputStream, lowerCaseDecName);

            if (sync) {
                if (result.getHashCode() != null) {
                    // Send the resulting file as response body
                    Response.ResponseBuilder response = Response.ok(result.pdfFile);
                    response.header("Content-Disposition", "attachment; filename=\"" + result.decName + ".pdf\"");
                    response.header("X-Validation-Message", result.message);
                    return response.build();
                } else {
                    // Send error response
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                   .entity(result.toJSON())
                                   .type(MediaType.APPLICATION_JSON)
                                   .build();
                }
            } else {
                if (result.getHashCode() != null) {
                    Result.cacheResult(result);
                }
                // Return JSON response with result data
                return Response.ok(result.toJSON(), MediaType.APPLICATION_JSON)
                               .header("X-Validation-Message", result.message)
                               .build();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Result result = new Result(e.getMessage(), -9);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(result.toJSON())
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }
    }

    private static String json2Xml(JSONObject input) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + org.json.JSONML.toString(input);
    }

    private static String getDeclName(JSONObject input) {
        return input.getString("tagName").replace("declaratie", "d");
    }

    private static String cacheResource(String resource) {
        String line;
        InputStream indexHtmlStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        BufferedReader htmlReader = new BufferedReader(new InputStreamReader(indexHtmlStream));

        StringBuilder result = new StringBuilder();

        try {
            while ((line = htmlReader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

}
