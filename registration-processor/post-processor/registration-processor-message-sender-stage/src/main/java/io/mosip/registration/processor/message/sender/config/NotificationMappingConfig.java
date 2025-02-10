package io.mosip.registration.processor.message.sender.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.Map;

@Configuration
@PropertySource(value = "${regproc.notification.mapping.property.source}", factory = NotificationMappingFactory.class)
@ConfigurationProperties
@Data
public class NotificationMappingConfig {


    public Map<String,List<String>> notification;

}
