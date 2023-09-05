package io.mosip.registration.processor.biometric.authentication.dto;

import lombok.Data;

import java.util.List;

@Data
public class Source {
	public String attribute;

	public List<Filter> filter;
}
