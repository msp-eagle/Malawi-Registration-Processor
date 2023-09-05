package io.mosip.registration.processor.biometric.authentication.response.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class CandidateList {

	@NotNull
	private Integer count;
	/**
	 * Analytics will be dumped in the manual verification table hence its expected as
	 * key value pair of String and Object.
	 */
	private Map<String, Object> analytics;
	private List<Candidate> candidates;
	
	
}
