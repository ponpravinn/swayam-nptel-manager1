package com.nptelmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * MVC configuration - root redirect and static resource handling.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to dashboard (Spring Security will redirect to login if unauthenticated)
        registry.addRedirectViewController("/", "/dashboard");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/");
    }
}
