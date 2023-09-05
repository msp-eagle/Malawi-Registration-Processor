package io.mosip.registration.processor.stages.uingenerator.idrepo.dto;

import java.util.List;

import io.mosip.registration.processor.core.idrepo.dto.Documents;
import lombok.Data;

/**
 * The Class ResponseDTO.
 *
 * @author M1049387
 */
@Data
public class ResponseDTO {
	
	/** The entity. */
	private String entity;
	
	/** The identity. */
	private Object identity;
	
	private List<Documents> documents;
	
	/** The status. */
	private String status;
}
