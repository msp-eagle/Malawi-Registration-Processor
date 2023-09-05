package io.mosip.registration.processor.stages.uingenerator.dto;

import lombok.Data;

@Data
public class NewTokenRequestDto extends BaseRequestDTO {

    private ClientIdSecretKeyRequestDto request;
}
