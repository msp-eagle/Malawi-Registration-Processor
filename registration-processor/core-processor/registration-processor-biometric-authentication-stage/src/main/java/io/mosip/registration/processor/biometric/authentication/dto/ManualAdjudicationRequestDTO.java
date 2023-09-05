package io.mosip.registration.processor.biometric.authentication.dto;

import lombok.Data;

import java.util.List;

@Data
public class ManualAdjudicationRequestDTO {
	
	private String id;
	
	private String version;

	private String requestId;

	private String referenceId;

	private String requesttime;
	
	private String referenceURL;
	
	private List<Addtional> addtional;
	
	private Gallery gallery;
	

}
