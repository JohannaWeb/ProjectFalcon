package app.juntos.alpha.config;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final AtprotoAuthFilter atprotoAuthFilter;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allowed Origins
        config.setAllowedOriginPatterns(Arrays.asList(
            "https://*.vercel.app",
            "https://*.railway.app",
            "http://localhost:*",
            "https://*.github.dev",
            "https://project-falcon-91n9-git-juntos-project-falcon.vercel.app" // Specific origin from error
        ));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<AtprotoAuthFilter> atprotoAuthFilterRegistration() {
        FilterRegistrationBean<AtprotoAuthFilter> bean = new FilterRegistrationBean<>(atprotoAuthFilter);
        // Order must be AFTER CorsFilter
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }
}
