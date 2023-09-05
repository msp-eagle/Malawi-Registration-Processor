package io.mosip.registration.processor.biometric.authentication.dto;

import lombok.Data;

import java.util.List;

@Data
public class Filter {
	public String language;
	public String type;
	public List<String> subType;
}
