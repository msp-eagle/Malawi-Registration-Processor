package io.mosip.registration.processor.stages.uingenerator.dto;

import io.mosip.registration.processor.stages.uingenerator.idrepo.dto.IdResponseDTO;
import lombok.Data;


@Data
public class UinUpdateResponseHandler {
    public IdResponseDTO idResponseDTO;
    public String uin;
    public String email;
    public String phone;
    public String dateOfBirth;
    public boolean isTransactionSuccessful;
}
