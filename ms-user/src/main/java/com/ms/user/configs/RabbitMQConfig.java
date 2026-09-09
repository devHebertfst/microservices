package com.ms.user.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {
    @Bean
    public JacksonJsonMessageConverter messageConverter(){
        JsonMapper jsonMapper = JsonMapper.builder().build();
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
