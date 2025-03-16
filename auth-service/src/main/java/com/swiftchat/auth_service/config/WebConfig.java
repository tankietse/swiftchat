package com.swiftchat.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Web configuration for the application.
 * Configures controllers, converters, and formatters.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Set up HTTP request interceptors.
     *
     * @param registry The interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Configures interceptor for changing locale based on a request parameter.
     *
     * @return LocaleChangeInterceptor with configured parameter name
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
}
