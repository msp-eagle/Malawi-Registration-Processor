package io.mosip.registration.processor.stages.uingenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordHistoryReqDTO {
    private String individualId;
    private String individualIdType;
    private String transactionID;
    private LocalDateTime requestDatetime;
    private LocalDateTime responseDatetime;
    private String statusComment;
    private String statusCode;
    private String langCode;
    private String authTypeCode;
    private String refId;
    private String requestedEntityType;
    private String responseSignature;
    private String requestedEntityName;
    private String requestSignature;
    //    private String authTknId;
    private String requestedEntityId;
    private String attributes;
    private String CreatedBy;
}
