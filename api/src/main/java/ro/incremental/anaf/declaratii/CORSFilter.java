package ro.incremental.anaf.declaratii;

/**
 * Created by Alex Proca <alex.proca@gmail.com> on 14/04/16.
 */
import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext cres) throws IOException {
        
        // Dynamic CORS Configuration
        String origin = requestContext.getHeaderString("Origin");
        String allowedOrigin = System.getenv("ALLOWED_ORIGIN");
        if (allowedOrigin == null || allowedOrigin.isEmpty()) {
            allowedOrigin = "http://localhost:3000"; // Fallback to local frontend development server
        }

        if (origin != null) {
            if (origin.equalsIgnoreCase(allowedOrigin)) {
                cres.getHeaders().add("Access-Control-Allow-Origin", origin);
            } else if (origin.equalsIgnoreCase("http://localhost:3000") || origin.equalsIgnoreCase("http://localhost:5001")) {
                cres.getHeaders().add("Access-Control-Allow-Origin", origin);
            } else {
                cres.getHeaders().add("Access-Control-Allow-Origin", "false");
            }
        } else {
            cres.getHeaders().add("Access-Control-Allow-Origin", allowedOrigin);
        }

        cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
        cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        cres.getHeaders().add("Access-Control-Max-Age", "1209600");

        // SEO/Crawler Index Protection (X-Robots-Tag)
        cres.getHeaders().add("X-Robots-Tag", "noindex, nofollow, noarchive, nosnippet");
    }

}
