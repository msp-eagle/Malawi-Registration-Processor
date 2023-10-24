package io.mosip.registration.processor.stages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.Map;

@Configuration
@PropertySource(value = "${packet.validator.mapping.property.source}", factory = NotificationMappingFactory.class)
@ConfigurationProperties
@Data
public class NotificationMappingConfig {


    public Map<String,List<String>> notification;

//    private List<String> RPR_RPV_SUC_SMS;
//    private List<String> RPR_RPV_SUC_EMAIL;
//    private List<String> RPR_LPV_SUC_SMS;
//    private List<String> RPR_LPV_SUC_EMAIL;
//    private List<String> RPR_UPV_SUC_SMS;
//    private List<String> RPR_UPV_SUC_EMAIL;
//    private List<String> RPR_PPV_SUC_SMS;
//    private List<String> RPR_PPV_SUC_EMAIL;
//    private List<String> RPR_APV_SUC_SMS;
//    private List<String> RPR_APV_SUC_EMAIL;
//    private List<String> RPR_DPV_SUC_SMS;
//    private List<String> RPR_DPV_SUC_EMAIL;
//    private List<String> RPR_RUPV_SUC_SMS;
//    private List<String> RPR_RUPV_SUC_EMAIL;
//    private List<String> RPR_TEC_ISSUE_SMS;
//    private List<String> RPR_TEC_ISSUE_EMAIL;

}
