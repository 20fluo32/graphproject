package com.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads 映射到本地文件路径 D:/github/graduation_project/demo/uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/github/graduation_project/demo/uploads/");
    }
}
