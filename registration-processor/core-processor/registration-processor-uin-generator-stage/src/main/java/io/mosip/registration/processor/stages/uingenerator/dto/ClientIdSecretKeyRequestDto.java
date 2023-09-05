package io.mosip.registration.processor.stages.uingenerator.dto;

import lombok.Data;

@Data
public class ClientIdSecretKeyRequestDto {
	public String clientId;
	public String secretKey;
	public String appId;
}
