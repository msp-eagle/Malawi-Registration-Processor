package io.mosip.registration.processor.stages.uingenerator.dto;

import lombok.Data;

@Data
public class NotifyDto {

    private String dateOfBirth;
    private String RegId;
    private String uin;
    private String vid;
    private String regType;
    private boolean biometricsCaptured;
    private String email;
    private String phone;
}
