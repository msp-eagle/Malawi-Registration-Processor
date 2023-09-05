package io.mosip.registration.processor.stages.uingenerator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.mosip.kernel.core.exception.ServiceError;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Data
public class ResponseWrapper<T> {
    private String id;
    private String version;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime responsetime = LocalDateTime.now(ZoneId.of("UTC"));
    private Object metadata;
    @NotNull
    @Valid
    private T response;

    private List<ServiceError> errors = new ArrayList<>();
}