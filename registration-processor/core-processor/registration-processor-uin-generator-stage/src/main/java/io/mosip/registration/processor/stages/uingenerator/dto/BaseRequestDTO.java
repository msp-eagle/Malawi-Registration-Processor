package io.mosip.registration.processor.stages.uingenerator.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Instantiates a new base request response DTO.
 * @author Monobikash Das
 */
@Data
public class BaseRequestDTO implements Serializable{
	
	private static final long serialVersionUID = 4373201325809902206L;

	/** The id. */
	private String id;
	
	/** The ver. */
	private String version;
	
	/** The timestamp. */
	private String requesttime;

}
