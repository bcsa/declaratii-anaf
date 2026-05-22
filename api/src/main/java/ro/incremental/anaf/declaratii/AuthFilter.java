package ro.incremental.anaf.declaratii;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class AuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get the requested path to bypass public endpoints
        String path = requestContext.getUriInfo().getPath();
        
        // Allow public access to robots.txt, available (health checks), and CORS preflight OPTIONS requests
        if (path.equals("robots.txt") || path.equals("available") || path.equals("") || requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            return;
        }

        // Read the secret auth token from environment variables
        String expectedToken = System.getenv("API_AUTH_TOKEN");
        if (expectedToken == null || expectedToken.isEmpty()) {
            // If no token is configured in environment variables, permit requests (backward compatibility / easy local dev)
            return;
        }

        // Get the token from request headers
        String clientToken = requestContext.getHeaderString("X-API-Token");

        if (clientToken == null || !clientToken.equals(expectedToken)) {
            // Abort the request with a 401 Unauthorized response
            requestContext.abortWith(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"Unauthorized API access. Valid X-API-Token header is required.\"}")
                .type("application/json")
                .build());
        }
    }
}
