package io.mosip.registration.processor.stages.helper;

import java.util.function.Supplier;

import io.mosip.registration.processor.stages.dto.ActivateUinDto;
import io.mosip.registration.processor.stages.dto.AsyncRequestDTO;

/*
 * The class RestHelper
 */
public interface RestHelper {

	/**
	 * Request to send/receive HTTP requests and return the response asynchronously.
	 *
	 * @param request
	 *            the request
	 * @return the supplier
	 */

	Supplier<Object> requestAsync(AsyncRequestDTO request);

	public boolean reactivateApplicant(String url, ActivateUinDto uin);
	public boolean isApplicantUinDeactivated(String url,String id);
}
