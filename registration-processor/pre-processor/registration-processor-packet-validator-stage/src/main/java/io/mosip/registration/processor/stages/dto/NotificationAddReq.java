package io.mosip.registration.processor.stages.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationAddReq {

    private String notificationType;
    private String notificationId;
    private String requestServiceName;
    private String status;

}
