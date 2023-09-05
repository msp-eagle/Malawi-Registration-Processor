package io.mosip.registration.processor.biometric.authentication.dto;

import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;

import java.util.List;


@Data
public class DataShareResponseDto {

	private String policyId;
	private String signature;
	private String subscriberId;
	private Integer transactionsAllowed;
	private String url;
	private Integer validForInMinutes;
	private List<ErrorDTO> errors;
}
