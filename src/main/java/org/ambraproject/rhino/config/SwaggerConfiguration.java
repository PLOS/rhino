package org.ambraproject.rhino.config;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.wordnik.swagger.model.ApiInfo;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * For swagger configuration
 */
@Configuration
@EnableSwagger
public class SwaggerConfiguration {

  @Autowired
  private SpringSwaggerConfig springSwaggerConfig;

  @Autowired
  private ConfigurationReadService configurationReadService;

  /**
   * Every SwaggerSpringMvcPlugin bean is picked up by the swagger-mvc framework - allowing for multiple
   * swagger groups i.e. same code base multiple swagger resource listings.
   */
  @Bean
  public SwaggerSpringMvcPlugin customImplementation(){
    return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
        .apiInfo(apiInfo())
        .apiVersion("v1");  // whenever we upgrade our api version, we will need to update this
  }

  /**
   * API Info as it appears on the swagger-ui page
   */
  private ApiInfo apiInfo() {
    String buildProperties = null;
    try {
      buildProperties = configurationReadService.getBuildProperties().toString();
    } catch (IOException e) {

    }

    ApiInfo apiInfo = new ApiInfo(
        "REST API for Ambra services",  // api title
        buildProperties, // api description
        null, // api terms of service
        null, // api contact email
        null, // api licence type
        null  // api license url
    );
    return apiInfo;
  }

}
