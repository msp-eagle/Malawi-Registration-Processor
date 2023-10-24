package io.mosip.registration.processor.message.sender.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationMappingFactory implements PropertySourceFactory {


    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        Map<?, ?> readValue = new ObjectMapper().readValue(resource.getInputStream(), Map.class);
        Map<String, Object> propertiesMap = new LinkedHashMap<>((Map<String, Object>) readValue);
        Map<String, Object> unmodifiableMap = Collections
                .unmodifiableMap(propertiesMap);
        return new MapPropertySource("json-property", unmodifiableMap);
    }
}
