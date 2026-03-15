package app.juntos.alpha.config;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final app.juntos.alpha.auth.DidResolver didResolver;

    @Bean
    public AtprotoAuthFilter atprotoAuthFilter() {
        return new AtprotoAuthFilter(didResolver);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Explicitly include the current Vercel deployment origin
        config.addAllowedOrigin("https://project-falcon-91n9-git-juntos-project-falcon.vercel.app");
        
        // Use allowedOriginPatterns for general subdomains
        config.addAllowedOriginPattern("https://*.vercel.app");
        config.addAllowedOriginPattern("https://*.railway.app");
        config.addAllowedOriginPattern("http://localhost:*");
        
        // Allowed methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Explicitly list required headers for preflight compatibility
        config.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        config.addExposedHeader("Authorization");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // CRITICAL: Ensure CORS headers are added even during ERROR dispatches (e.g. 401/500 responses)
        bean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        return bean;
    }

    @Bean
    public FilterRegistrationBean<AtprotoAuthFilter> atprotoAuthFilterRegistration() {
        // We use the atprotoAuthFilter() bean method
        FilterRegistrationBean<AtprotoAuthFilter> bean = new FilterRegistrationBean<>(atprotoAuthFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        bean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
        return bean;
    }
}
