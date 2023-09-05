package io.mosip.registration.processor.biometric.authentication.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareableAttributes {
	public boolean encrypted;

	public String format;

	public String attributeName;

	public List<Source> source;

	public String group;
}
