package io.mosip.registration.processor.biometric.authentication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TypeKey;
import com.google.common.collect.Lists;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.biometric.authentication.constants.BiometricAuthenticationConstants;
import io.mosip.registration.processor.biometric.authentication.dto.*;
import io.mosip.registration.processor.biometric.authentication.exception.DataShareException;
import io.mosip.registration.processor.biometric.authentication.exception.InvalidFileNameException;
import io.mosip.registration.processor.biometric.authentication.exception.InvalidRidException;
import io.mosip.registration.processor.biometric.authentication.exception.NoRecordAssignedException;
import io.mosip.registration.processor.biometric.authentication.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.biometric.authentication.stage.BiometricAuthenticationStage;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.*;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.constant.*;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.DemoUpdateManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.DemoUpdateManualVerificationPKEntity;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationPKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.BIRConverter;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.DataInput;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DemoUpdateService {
    private static Logger regProcLogger = RegProcessorLogger.getLogger(DemoUpdateService.class);
    @Autowired
    private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;
    private LinkedHashMap<String, Object> policies = null;
    @Value("${mosip.kernel.vid.length:-1}")
    private int vidLength;
    /** The registration status service. */
    @Autowired
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Autowired
    private PriorityBasedPacketManagerService packetManagerService;
    private static final String MANUAL_VERIFICATION = "manualverification";
    @Autowired
    private RegistrationProcessorRestClientService registrationProcessorRestClientService;

    @Autowired
    private Environment env;
    /** The address. */
    @Value("${registration.processor.queue.demo.update.manualverification.request:mosip-to-demoupdatemv}")
    private String demoupdateRequestAddress;

    /**Manual verification queue message expiry in seconds, if given 0 then message will never expire*/
    @Value("${registration.processor.queue.manualverification.request.messageTTL}")
    private int mvRequestMessageTTL;

    @Value("${registration.processor.manual.adjudication.policy.id:mpolicy-default-adjudication}")
    private String policyId;

    @Value("${registration.processor.manual.adjudication.subscriber.id:mpartner-default-adjudication}")
    private String subscriberId;

    @Value("${activemq.message.format}")
    private String messageFormat;

    /** Comman seperated stage names that should be excluded while pushing to queue. */
    @Value("#{T(java.util.Arrays).asList('${mosip.registration.processor.demo.manual.verification.queue.exclude-field-names:email}')}")
    private List<String> queueExcludeFiledNames;

    @Autowired
    private BasePacketRepository<DemoUpdateManualVerificationEntity, String> basePacketRepository;

    @Autowired
    private BiometricAuthenticationStage biometricAuthenticationStage;

    @Autowired
    RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

    /** The Constant USER. */
    private static final String USER = "MOSIP_SYSTEM";
    private static final String TEXT_MESSAGE = "text";
    private static final String DATASHARE = "dataShare";
    private static final String ERRORS = "errors";
    private static final String URL = "url";
    private static final String META_INFO = "meta_info";
    private static final String AUDITS = "audits";

    public void pushRequestToQueue(String RegId, MosipQueue queue) throws Exception {

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                RegId, "DemoUpdateServiceImpl::pushRequestToQueue()::entry");

        List<DemoUpdateManualVerificationEntity> mve = basePacketRepository.getDemoVerificationEntity(RegId);
        if(mve.size() == 0) {


            ManualAdjudicationRequestDTO mar = prepareManualAdjudicationRequest(RegId);
            String requestId = UUID.randomUUID().toString();
            mar.setRequestId(requestId);
            regProcLogger.info("Request : " + JsonUtils.javaObjectToJsonString(mar));
            if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
                mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar), demoupdateRequestAddress, mvRequestMessageTTL);
            else
                mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar).getBytes(), demoupdateRequestAddress, mvRequestMessageTTL);
            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    RegId, "DemoUpdateServiceImpl::pushRequestToQueue()::success");

            saveDemoManualEntity(mar);

        }else{
            mve.forEach(dto ->{
                try {
                    if (dto.getStatusCode().equals(DemoManualVerificationStatus.PENDING.name())) {
                        ManualAdjudicationRequestDTO mar = prepareManualAdjudicationRequest(RegId);
                        String requestId = UUID.randomUUID().toString();
                        mar.setRequestId(requestId);
                        regProcLogger.info("Request : " + JsonUtils.javaObjectToJsonString(mar));
                        if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
                            mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar), demoupdateRequestAddress, mvRequestMessageTTL);
                        else
                            mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar).getBytes(), demoupdateRequestAddress, mvRequestMessageTTL);
                        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                                RegId, "DemoUpdateServiceImpl::pushRequestToQueue()::success");
                        updateDemoManualVerificationEntityRID(dto, requestId);

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });


        }
//        updateManualVerificationEntityRID(mves, requestId);
    }

    private void saveDemoManualEntity(ManualAdjudicationRequestDTO mar){
        DemoUpdateManualVerificationEntity manualVerificationEntity = new DemoUpdateManualVerificationEntity();
        DemoUpdateManualVerificationPKEntity manualVerificationPKEntity = new DemoUpdateManualVerificationPKEntity();
        manualVerificationPKEntity.setMatchedRefId(mar.getGallery().getReferenceIds().get(0).getReferenceId());
        manualVerificationPKEntity.setMatchedRefType("rid");
        manualVerificationPKEntity.setRegId(mar.getReferenceId());

        manualVerificationEntity.setId(manualVerificationPKEntity);
        manualVerificationEntity.setLangCode("eng");
        manualVerificationEntity.setRequestId(mar.getRequestId());
        //manualVerificationEntity.setMatchedScore(null);
        manualVerificationEntity.setReponseText(null);
        manualVerificationEntity.setTransactionId(null);
        manualVerificationEntity.setMvUsrId(null);
        manualVerificationEntity.setReasonCode("Demographic update verification");
        manualVerificationEntity.setStatusCode(DemoManualVerificationStatus.INQUEUE.name());
        manualVerificationEntity.setStatusComment("Assigned to manual Adjudication");
        manualVerificationEntity.setIsActive(true);
        manualVerificationEntity.setIsDeleted(false);
        manualVerificationEntity.setCrBy("SYSTEM");
        manualVerificationEntity.setCrDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
        manualVerificationEntity.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
        manualVerificationEntity.setTrnTypCode("DEMO");
        basePacketRepository.save(manualVerificationEntity);
    }

    private void updateDemoManualVerificationEntityRID(DemoUpdateManualVerificationEntity mve, String requestId) {
            regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    mve.getId().getRegId(), "ManualVerificationServiceImpl::updateManualVerificationEntityRID()::entry");
            mve.setStatusCode(DemoManualVerificationStatus.INQUEUE.name());
            mve.setStatusComment("Sent to manual adjudication queue");
            mve.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
            mve.setRequestId(requestId);
            basePacketRepository.update(mve);
            regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    mve.getId().getRegId(), "ManualVerificationServiceImpl::updateManualVerificationEntityRID()::exit");
    }

    private JSONObject validateIdrepoResponse(org.json.JSONObject obj ) throws Exception {
        if (obj.get("response") != JSONObject.NULL) {
            return obj.getJSONObject("response").getJSONObject("identity");
        } else if (obj.get("errors") != JSONObject.NULL) {
            JSONArray arr = obj.getJSONArray("errors");
            for (int i = 0; i < arr.length(); i++) {
//                log.error("idrepo identity service " + arr.getJSONObject(i).getString("message"));
                throw new Exception(arr.getJSONObject(i).getString("message"));
//                    return arr.getJSONObject(i).getString("message");

            }

        }
        return null;
    }

    public boolean isupdatevalid(String rid, String process) throws Exception{
        LinkedHashMap<String, Object> policy = getPolicy();

        Map<String, String> policyMap = getPolicyMap(policy);

        // set rid demographic
        Map<String, String> demographicMap = policyMap.entrySet().stream().filter(e -> e.getValue() != null &&
                (!META_INFO.equalsIgnoreCase(e.getValue()) && !AUDITS.equalsIgnoreCase(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        Map<String,String> packetDetails = packetManagerService.getFields(rid, demographicMap.values().stream().collect(Collectors.toList()), process, ProviderStageName.MANUAL_VERIFICATION);

        List<String> res = new ArrayList<>();
        List<String> check = new LinkedList<String>(queueExcludeFiledNames);

        packetDetails.forEach((key,value) ->{
            if (packetDetails.get(key) != null){
                res.add(key);
            }

        });
        boolean valid = false;
        res.removeAll(check);
        return res.size()>0;
    }

    private String getDataShareUrl(String id, String process) throws Exception {
        DataShareRequestDto requestDto = new DataShareRequestDto();

        LinkedHashMap<String, Object> policy = getPolicy();

        Map<String, String> policyMap = getPolicyMap(policy);

        List<String> pathsegmentsPcn = new ArrayList<>();
        if(id.length()== vidLength) {
            try {
                pathsegmentsPcn.add(id);
                String response = (String) registrationProcessorRestClientService.getApi(ApiName.IDREPOGETIDBYUIN,
                        pathsegmentsPcn, "", "", String.class);
                org.json.JSONObject json = new org.json.JSONObject(response);


                JSONObject identity = validateIdrepoResponse(json);



                System.out.println("idrepo identity json for pcn : "+identity);

                HashMap<String, String> remaining = new HashMap<>();

                identity.keys().forEachRemaining(k ->
                {
                    remaining.put(String.valueOf(k),identity.optString(String.valueOf(k)));

                });
                try {
                    remaining.forEach((key, value) -> System.out.println(key + " " + value));
                }catch (Exception e){
                    System.out.println("map print failed");
                }
//                HashMap<String, String> result =
//                        new ObjectMapper().readValue(String.valueOf(identity), new TypeReference<Map<String, String>>(){});

                requestDto.setIdentity(remaining);
            }catch (Exception e){

                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString() + id, "error Response from IdRepo API",
                        "is : " + e.getMessage());

                e.printStackTrace();
            }
        }else{
            // set rid demographic
            Map<String, String> demographicMap = policyMap.entrySet().stream().filter(e -> e.getValue() != null &&
                    (!META_INFO.equalsIgnoreCase(e.getValue()) && !AUDITS.equalsIgnoreCase(e.getValue())))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            requestDto.setIdentity(packetManagerService.getFields(id, demographicMap.values().stream().collect(Collectors.toList()), process, ProviderStageName.MANUAL_VERIFICATION));

            System.out.println("printing packet manager fields of datashare req");
            try {
                requestDto.getIdentity().forEach((key, value) -> System.out.println(key + " " + value));
            }catch (Exception e){
                System.out.println("map print failed");
            }
        }


        String req = JsonUtils.javaObjectToJsonString(requestDto);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("name", MANUAL_VERIFICATION);
        map.add("filename", MANUAL_VERIFICATION);

        ByteArrayResource contentsAsResource = new ByteArrayResource(req.getBytes()) {
            @Override
            public String getFilename() {
                return MANUAL_VERIFICATION;
            }
        };
        map.add("file", contentsAsResource);

        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(policyId);
        pathSegments.add(subscriberId);
        io.mosip.kernel.core.http.ResponseWrapper<DataShareResponseDto> resp = new io.mosip.kernel.core.http.ResponseWrapper<>();

        LinkedHashMap response = (LinkedHashMap) registrationProcessorRestClientService.postApi(ApiName.DATASHARECREATEURL, MediaType.MULTIPART_FORM_DATA, pathSegments, null, null, map, LinkedHashMap.class);
        if (response == null || (response.get(ERRORS) != null))
            throw new DataShareException(response == null ? "Datashare response is null" : response.get(ERRORS).toString());

        LinkedHashMap datashare = (LinkedHashMap) response.get(DATASHARE);
        return datashare.get(URL) != null ? datashare.get(URL).toString() : null;
    }

    private ManualAdjudicationRequestDTO prepareManualAdjudicationRequest(String RegId) throws Exception {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                "ManualVerificationServiceImpl::formAdjudicationRequest()::entry");

        String requestId = UUID.randomUUID().toString();
        ManualAdjudicationRequestDTO req = new ManualAdjudicationRequestDTO();
        req.setId(BiometricAuthenticationConstants.MANUAL_ADJUDICATION_ID);
        req.setVersion(BiometricAuthenticationConstants.VERSION);
        req.setRequestId(requestId);
        req.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(BiometricAuthenticationConstants.DATETIME_PATTERN)));
        req.setReferenceId(RegId);
        InternalRegistrationStatusDto registrationStatusDtoupdate = null;
        registrationStatusDtoupdate = registrationStatusService.getRegistrationStatus(RegId);
        try {
            req.setReferenceURL(
                    getDataShareUrl(RegId, registrationStatusDtoupdate.getRegistrationType()));

        } catch (PacketManagerException | ApisResourceAccessException ex) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    ex.getErrorCode(), ex.getErrorText());
            throw ex;
        }
//        List.of();
        Map<String,String> field = packetManagerService.getFields(RegId,
                List.of("UIN"), "UPDATE", ProviderStageName.UIN_GENERATOR);

        String vid = field.get("UIN");
        List<ReferenceIds> referenceIds = new ArrayList<>();

            ReferenceIds r = new ReferenceIds();
//            InternalRegistrationStatusDto registrationStatusDto1 = null;
//            registrationStatusDto1 = registrationStatusService.getRegistrationStatus(e.getId().getMatchedRefId());

            try {
                r.setReferenceId(vid);
                r.setReferenceURL(getDataShareUrl(vid,"NEW"));
                referenceIds.add(r);
            } catch (PacketManagerException | ApisResourceAccessException ex) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), ex.getErrorCode(), ex.getErrorText());
                r.setReferenceURL(null);
                referenceIds.add(r);
            } catch (Exception exp) {
                regProcLogger.error(ExceptionUtils.getStackTrace(exp));
            }


        Gallery g = new Gallery();
        g.setReferenceIds(referenceIds);
        req.setGallery(g);
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                "ManualVerificationServiceImpl::formAdjudicationRequest()::entry");

        return req;
    }
    private Map<String, String> getPolicyMap(LinkedHashMap<String, Object> policies) throws DataShareException, IOException, ApisResourceAccessException {
        Map<String, String> policyMap = new HashMap<>();
        List<LinkedHashMap> attributes = (List<LinkedHashMap>) policies.get(BiometricAuthenticationConstants.SHAREABLE_ATTRIBUTES);
        ObjectMapper mapper = new ObjectMapper();
        for (LinkedHashMap map : attributes) {
            ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
                    ShareableAttributes.class);
            policyMap.put(shareableAttributes.getAttributeName(), shareableAttributes.getSource().iterator().next().getAttribute());
        }
        return policyMap;

    }


    private LinkedHashMap<String, Object> getPolicy() throws DataShareException, ApisResourceAccessException {
        if (policies != null && policies.size() > 0)
            return policies;

        ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
                ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "", ResponseWrapper.class);
        if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() >0)) {
            throw new DataShareException(policyResponse == null ? "Policy Response response is null" : policyResponse.getErrors().get(0).getMessage());

        } else {
            LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
            policies = (LinkedHashMap<String, Object>) responseMap.get(BiometricAuthenticationConstants.POLICIES);
        }
        return policies;

    }





    public boolean updatePacketStatus(ManualAdjudicationResponseDTO manualVerificationDTO, String stageName, MosipQueue queue) {
        TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
        LogDescription description = new LogDescription();
        boolean isTransactionSuccessful = false;

        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
                manualVerificationDTO.getRequestId(), "ManualVerificationServiceImpl::updatePacketStatus()::entry");

        String regId = validateRequestIdAndReturnRid(manualVerificationDTO.getRequestId());

        InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
                .getRegistrationStatus(regId);
        registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.BIOMETRIC_AUTHENTICATION.name());
        registrationStatusDto.setRegistrationStageName(stageName);

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setInternalError(false);
        messageDTO.setIsValid(false);
        messageDTO.setRid(regId);
        messageDTO.setReg_type(RegistrationType.valueOf(registrationStatusDto.getRegistrationType()));

        try {

            List<DemoUpdateManualVerificationEntity> entities = retrieveInqueuedRecordsByRid(regId);

            // check if response is marked for resend
            if (isResendFlow(regId, manualVerificationDTO, entities)) {
                registrationStatusDto.setStatusComment(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
                registrationStatusDto.setSubStatusCode(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getCode());
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                registrationStatusDto
                        .setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                description.setMessage(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
                description.setCode(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode());
                messageDTO.setInternalError(true);
                messageDTO.setIsValid(isTransactionSuccessful);
                biometricAuthenticationStage.sendMessage(messageDTO);
            } else {
                // call success flow and process the response received from manual verification system
                isTransactionSuccessful = successFlow(
                        regId, manualVerificationDTO, entities, registrationStatusDto, messageDTO, description);

                registrationStatusDto.setUpdatedBy(USER);
                regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        regId, description.getMessage());
            }

        } catch (TablenotAccessibleException e) {
            registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
            registrationStatusDto.setStatusComment(trimExceptionMessage
                    .trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());

            description.setMessage(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getMessage());
            description.setCode(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
            registrationStatusDto.setStatusComment(trimExceptionMessage
                    .trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());

            description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
            registrationStatusDto.setStatusComment(trimExceptionMessage
                    .trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());

            description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
        } finally {
            /** Module-Id can be Both Success/Error code */
            String moduleId = isTransactionSuccessful
                    ? PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode()
                    : description.getCode();
            String moduleName = ModuleName.BIOMETRIC_AUTHENTICATION.toString();
            registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

            String eventId = isTransactionSuccessful ? io.mosip.registration.processor.core.code.EventId.RPR_402.toString() : io.mosip.registration.processor.core.code.EventId.RPR_405.toString();
            String eventName = eventId.equalsIgnoreCase(io.mosip.registration.processor.core.code.EventId.RPR_402.toString()) ? io.mosip.registration.processor.core.code.EventName.UPDATE.toString()
                    : EventName.EXCEPTION.toString();
            String eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? io.mosip.registration.processor.core.code.EventType.BUSINESS.toString()
                    : EventType.SYSTEM.toString();

//            auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
//                    moduleId, moduleName, regId);

        }
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "ManualVerificationServiceImpl::updatePacketStatus()::exit");
        return isTransactionSuccessful;

    }

    private String validateRequestIdAndReturnRid(String reqId) {
        List<String> regIds = basePacketRepository.getDemoRegistrationIdbyRequestId(reqId);

        if (CollectionUtils.isEmpty(regIds) || new HashSet<>(regIds).size() != 1) {
            regProcLogger.error("Multiple rids found against request id : " + reqId +
                    "regids : " + regIds.toString());
            throw new InvalidRidException(
                    PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode(), PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode());
        }

        String rid = regIds.iterator().next();

        if (StringUtils.isEmpty(rid)) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    rid, "ManualVerificationServiceImpl::updatePacketStatus()::InvalidFileNameException"
                            + PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
            throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
                    PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
        }
        return rid;
    }

    private List<DemoUpdateManualVerificationEntity> retrieveInqueuedRecordsByRid(String regId) {

        List<DemoUpdateManualVerificationEntity> entities = basePacketRepository.getAllDemoAssignedRecord(
                regId, DemoManualVerificationStatus.INQUEUE.name());

        if (CollectionUtils.isEmpty(entities)) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, "ManualVerificationServiceImpl::updatePacketStatus()"
                            + PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
            throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
                    PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
        }

        return entities;
    }

    public boolean isResendFlow(String regId, ManualAdjudicationResponseDTO manualVerificationDTO, List<DemoUpdateManualVerificationEntity> entities) throws JsonProcessingException {
        boolean isResendFlow = false;
        if(manualVerificationDTO.getReturnValue() == 2 || !isResponseValidationSuccess(regId, manualVerificationDTO, entities)) {
            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, "Received resend request from manual verification application. This will be marked for reprocessing.");

            // updating status code to pending so that it can be marked for manual verification again
            entities.forEach(e -> {
                e.setStatusCode(DemoManualVerificationStatus.PENDING.name());
                basePacketRepository.update(e);
            });
            isResendFlow = true;
        }
        return isResendFlow;
    }
    private boolean isResponseValidationSuccess(String regId, ManualAdjudicationResponseDTO manualVerificationDTO, List<DemoUpdateManualVerificationEntity> entities) throws JsonProcessingException {
        boolean isValidationSuccess = true;
        // if candidate count is a positive number
        if (manualVerificationDTO.getReturnValue() == 1
                && manualVerificationDTO.getCandidateList() != null
                && manualVerificationDTO.getCandidateList().getCount() > 0) {

            // get the reference ids from response candidates.
            List<String> refIdsFromResponse = !CollectionUtils.isEmpty(manualVerificationDTO.getCandidateList().getCandidates()) ?
                    manualVerificationDTO.getCandidateList().getCandidates().stream().map(c -> c.getReferenceId()).collect(Collectors.toList())
                    : Collections.emptyList();

            // get the reference ids from manual verification table entities.
            List<String> refIdsFromEntities = entities.stream().map(e -> e.getId().getMatchedRefId()).collect(Collectors.toList());

            if (!manualVerificationDTO.getCandidateList().getCount().equals(refIdsFromResponse.size())) {
                String errorMessage = "Validation error - Candidate count does not match reference ids count.";
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        regId, errorMessage);
//                auditLogRequestBuilder.createAuditRequestBuilder(
//                        errorMessage + " Response received : "
//                                +JsonUtils.javaObjectToJsonString(manualVerificationDTO), EventId.RPR_405.toString(),
//                        EventName.EXCEPTION.name(), EventType.BUSINESS.name(),
//                        PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode(), ModuleName.MANUAL_VERIFICATION.toString(), regId);
                isValidationSuccess = false;

            } else if (!refIdsFromEntities.containsAll(refIdsFromResponse)) {
                String errorMessage = "Validation error - Received ReferenceIds does not match reference ids in manual verification table.";
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        regId, errorMessage);
//                auditLogRequestBuilder.createAuditRequestBuilder(
//                        errorMessage + " Response received : "
//                                +JsonUtils.javaObjectToJsonString(manualVerificationDTO), EventId.RPR_405.toString(),
//                        EventName.EXCEPTION.name(), EventType.BUSINESS.name(),
//                        PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode(), ModuleName.MANUAL_VERIFICATION.toString(), regId);
                isValidationSuccess = false;
            }
        }
        return isValidationSuccess;
    }

    private boolean successFlow(String regId, ManualAdjudicationResponseDTO manualVerificationDTO,
                                List<DemoUpdateManualVerificationEntity> entities,
                                InternalRegistrationStatusDto registrationStatusDto, MessageDTO messageDTO,
                                LogDescription description) throws com.fasterxml.jackson.core.JsonProcessingException {

        boolean isTransactionSuccessful = false;
        String statusCode = manualVerificationDTO.getReturnValue() == 1 &&
                CollectionUtils.isEmpty(manualVerificationDTO.getCandidateList().getCandidates()) ?
                DemoManualVerificationStatus.APPROVED.name() : DemoManualVerificationStatus.REJECTED.name();

        for (int i = 0; i < entities.size(); i++) {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] responsetext = objectMapper.writeValueAsBytes(manualVerificationDTO);

            DemoUpdateManualVerificationEntity manualVerificationEntity=entities.get(i);
            manualVerificationEntity.setStatusCode(statusCode);
            manualVerificationEntity.setReponseText(responsetext);
            manualVerificationEntity.setStatusComment(statusCode.equalsIgnoreCase(DemoManualVerificationStatus.APPROVED.name()) ?
                    StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getMessage() :
                    StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getMessage());
            entities.set(i, manualVerificationEntity);
        }
        isTransactionSuccessful = true;
        registrationStatusDto
                .setLatestTransactionTypeCode(RegistrationTransactionTypeCode.BIOMETRIC_AUTHENTICATION.toString());
        registrationStatusDto.setRegistrationStageName(registrationStatusDto.getRegistrationStageName());

        if (statusCode != null && statusCode.equalsIgnoreCase(DemoManualVerificationStatus.APPROVED.name())) {
//            if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(RegistrationType.LOST.toString())) {
//                for(ManualVerificationEntity detail: entities) {
//                    packetInfoManager.saveRegLostUinDet(regId, detail.getId().getMatchedRefId(),
//                            PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode(),
//                            ModuleName.MANUAL_VERIFICATION.toString());
//                }
//            }
            messageDTO.setIsValid(isTransactionSuccessful);
            biometricAuthenticationStage.sendMessage(messageDTO);
            registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getMessage());
            registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getCode());
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
            registrationStatusDto
                    .setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());

            description.setMessage(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getMessage());
            description.setCode(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode());

        } else if (statusCode != null && statusCode.equalsIgnoreCase(DemoManualVerificationStatus.REJECTED.name())) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
            registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getMessage());
            registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getCode());
            registrationStatusDto
                    .setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

            description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getCode());
            messageDTO.setIsValid(Boolean.FALSE);
            biometricAuthenticationStage.sendMessage(messageDTO);
        } else {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
            registrationStatusDto.setStatusComment(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
            registrationStatusDto.setSubStatusCode(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getCode());
            registrationStatusDto
                    .setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());

            description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
            description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode());
            messageDTO.setIsValid(Boolean.FALSE);
            biometricAuthenticationStage.sendMessage(messageDTO);
        }
        List<DemoUpdateManualVerificationEntity> maVerificationEntity = new ArrayList<>();
        for(DemoUpdateManualVerificationEntity manualVerificationEntity: entities) {
            maVerificationEntity.add(basePacketRepository.update(manualVerificationEntity));
        }

        return isTransactionSuccessful;
    }

}
