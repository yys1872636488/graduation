package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProcurementStatus;
import org.example.graduation.dto.InspectionReviewDTO;
import org.example.graduation.entity.InspectionImage;
import org.example.graduation.entity.InspectionRecord;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.Supplier;
import org.example.graduation.entity.SysUser;
import org.example.graduation.entity.VisionDetectionResult;
import org.example.graduation.mapper.InspectionImageMapper;
import org.example.graduation.mapper.InspectionRecordMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.mapper.SysUserMapper;
import org.example.graduation.mapper.VisionDetectionResultMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.InspectionService;
import org.example.graduation.service.MaterialBatchService;
import org.example.graduation.vo.InspectionDetailVO;
import org.example.graduation.vo.InspectionImageVO;
import org.example.graduation.vo.InspectionRecordQueryVO;
import org.example.graduation.vo.InspectionRecordVO;
import org.example.graduation.vo.MaterialBatchVO;
import org.example.graduation.vo.VisionDetectionResultVO;
import org.example.graduation.vision.VisionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/bmp");
    private static final Set<String> INSPECTABLE_STATUSES = Set.of(
            ProcurementStatus.PENDING_INSPECTION,
            ProcurementStatus.INSPECTING,
            ProcurementStatus.PENDING_REVIEW,
            ProcurementStatus.ON_HOLD);
    private static final Set<String> CONCLUSIONS = Set.of(
            ProcurementStatus.QUALIFIED,
            ProcurementStatus.UNQUALIFIED,
            ProcurementStatus.ON_HOLD);

    private final InspectionImageMapper imageMapper;
    private final VisionDetectionResultMapper detectionMapper;
    private final InspectionRecordMapper recordMapper;
    private final MaterialBatchMapper batchMapper;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final SysUserMapper sysUserMapper;
    private final MaterialBatchService materialBatchService;
    private final CurrentUserProvider currentUserProvider;
    private final VisionClient visionClient;
    private final ObjectMapper objectMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    @Transactional
    public InspectionImageVO uploadImage(Long batchId, MultipartFile file) {
        MaterialBatch batch = requireBatch(batchId);
        ensureInspectable(batch);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过20MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 JPG、PNG、BMP 格式的图片");
        }

        byte[] bytes;
        BufferedImage bufferedImage;
        try {
            bytes = file.getBytes();
            bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new BusinessException("图片读取失败");
        }
        if (bufferedImage == null) {
            throw new BusinessException("图片无法解析，请上传有效的图片文件");
        }

        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/bmp" -> "bmp";
            default -> "jpg";
        };
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativeDir = "inspection/" + batchId;
        String storedPath = relativeDir + "/" + filename;
        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = baseDir.resolve(storedPath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new BusinessException("图片保存失败");
        }

        String originalName = file.getOriginalFilename() == null
                ? "image." + extension : file.getOriginalFilename();
        if (originalName.length() > 255) {
            originalName = originalName.substring(originalName.length() - 255);
        }

        InspectionImage image = new InspectionImage();
        image.setBatchId(batchId);
        image.setOriginalName(originalName);
        image.setStoredPath(storedPath);
        image.setContentType(contentType);
        image.setFileSize(file.getSize());
        image.setImageWidth(bufferedImage.getWidth());
        image.setImageHeight(bufferedImage.getHeight());
        image.setCreatedBy(currentUserProvider.getUserId());
        image.setCreatedAt(LocalDateTime.now());
        imageMapper.insert(image);

        return toImageVO(image);
    }

    @Override
    @Transactional
    public void deleteImage(Long batchId, Long imageId) {
        MaterialBatch batch = requireBatch(batchId);
        if (!INSPECTABLE_STATUSES.contains(batch.getInspectionStatus())) {
            throw new BusinessException("该批次已完成质检，不能删除图片");
        }

        InspectionImage image = imageMapper.selectById(imageId);
        if (image == null || !batchId.equals(image.getBatchId())) {
            throw new BusinessException("图片不属于该原材料批次");
        }

        Long detectionCount = detectionMapper.selectCount(
                new LambdaQueryWrapper<VisionDetectionResult>()
                        .eq(VisionDetectionResult::getImageId, imageId));
        Long recordCount = recordMapper.selectCount(
                new LambdaQueryWrapper<InspectionRecord>()
                        .eq(InspectionRecord::getImageId, imageId));
        if ((detectionCount != null && detectionCount > 0)
                || (recordCount != null && recordCount > 0)) {
            throw new BusinessException("该图片已被检测或复核记录引用，不能删除");
        }

        imageMapper.deleteById(imageId);
        try {
            Path path = Paths.get(uploadPath).toAbsolutePath().normalize()
                    .resolve(image.getStoredPath()).normalize();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除图片文件失败: {}", image.getStoredPath(), e);
        }
    }

    @Override
    @Transactional
    public VisionDetectionResultVO detect(Long batchId, Long imageId) {
        MaterialBatch batch = requireBatch(batchId);
        ensureInspectable(batch);

        InspectionImage image = imageMapper.selectById(imageId);
        if (image == null || !batchId.equals(image.getBatchId())) {
            throw new BusinessException("图片不属于该原材料批次");
        }

        batch.setInspectionStatus(ProcurementStatus.INSPECTING);
        batchMapper.updateById(batch);

        VisionDetectionResult result = new VisionDetectionResult();
        result.setBatchId(batchId);
        result.setImageId(imageId);
        result.setCreatedBy(currentUserProvider.getUserId());
        result.setCreatedAt(LocalDateTime.now());

        try {
            Path imagePath = Paths.get(uploadPath).toAbsolutePath().normalize()
                    .resolve(image.getStoredPath());
            byte[] bytes = Files.readAllBytes(imagePath);
            JsonNode response = visionClient.detect(
                    bytes, image.getOriginalName(), image.getContentType());

            if (response == null) {
                result.setStatus(ProcurementStatus.DETECT_FAILED);
                result.setErrorMessage("视觉服务返回为空");
                result.setDetectionsJson("[]");
                result.setDetectionCount(0);
            } else {
                String status = normalizeDetectionStatus(response);
                result.setStatus(status);
                result.setModelName(text(response, "model_name"));
                result.setModelVersion(text(response, "model_version"));
                result.setImageWidth(intOrNull(response, "image_width", image.getImageWidth()));
                result.setImageHeight(intOrNull(response, "image_height", image.getImageHeight()));
                result.setAnnotatedImagePath(text(response, "annotated_image_path"));
                result.setInferenceTimeMs(intOrNull(response, "inference_time_ms", null));

                JsonNode detections = response.get("detections");
                if (detections != null && detections.isArray()) {
                    result.setDetectionCount(detections.size());
                    result.setDetectionsJson(objectMapper.writeValueAsString(detections));
                } else {
                    result.setDetectionCount(0);
                    result.setDetectionsJson("[]");
                }

                if (!ProcurementStatus.DETECT_SUCCESS.equals(status)
                        && !ProcurementStatus.DETECT_MODEL_NOT_CONFIGURED.equals(status)) {
                    result.setErrorMessage(text(response, "message"));
                }

                if (ProcurementStatus.DETECT_SUCCESS.equals(status)
                        || ProcurementStatus.DETECT_MODEL_NOT_CONFIGURED.equals(status)) {
                    batch.setInspectionStatus(ProcurementStatus.PENDING_REVIEW);
                    batchMapper.updateById(batch);
                }
            }
        } catch (Exception e) {
            result.setStatus(ProcurementStatus.DETECT_SERVICE_UNAVAILABLE);
            result.setErrorMessage("视觉服务不可用，请确认 Python 检测服务已启动：" + e.getMessage());
            result.setDetectionCount(0);
            result.setDetectionsJson("[]");
            result.setImageWidth(image.getImageWidth());
            result.setImageHeight(image.getImageHeight());
        }

        detectionMapper.insert(result);
        return toDetectionVO(result);
    }

    @Override
    public InspectionDetailVO detail(Long batchId) {
        MaterialBatchVO batch = materialBatchService.detail(batchId);

        List<InspectionImage> images = imageMapper.selectList(
                new LambdaQueryWrapper<InspectionImage>()
                        .eq(InspectionImage::getBatchId, batchId)
                        .orderByDesc(InspectionImage::getCreatedAt)
                        .orderByDesc(InspectionImage::getId));
        List<VisionDetectionResult> detections = detectionMapper.selectList(
                new LambdaQueryWrapper<VisionDetectionResult>()
                        .eq(VisionDetectionResult::getBatchId, batchId)
                        .orderByDesc(VisionDetectionResult::getCreatedAt)
                        .orderByDesc(VisionDetectionResult::getId));
        List<InspectionRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InspectionRecord>()
                        .eq(InspectionRecord::getBatchId, batchId)
                        .orderByDesc(InspectionRecord::getReviewTime)
                        .orderByDesc(InspectionRecord::getId));

        Map<Long, String> reviewerNames = loadReviewerNames(
                records.stream().map(InspectionRecord::getReviewerId).toList());

        return InspectionDetailVO.builder()
                .batch(batch)
                .images(images.stream().map(this::toImageVO).toList())
                .detections(detections.stream().map(this::toDetectionVO).toList())
                .records(records.stream()
                        .map(record -> toRecordVO(record, reviewerNames.get(record.getReviewerId())))
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public InspectionRecordVO review(Long batchId, InspectionReviewDTO dto) {
        MaterialBatch batch = requireBatch(batchId);
        if (!ProcurementStatus.PENDING_REVIEW.equals(batch.getInspectionStatus())
                && !ProcurementStatus.ON_HOLD.equals(batch.getInspectionStatus())) {
            throw new BusinessException("该批次当前状态不能进行质检复核");
        }

        String conclusion = dto.getConclusion() == null
                ? "" : dto.getConclusion().toUpperCase(java.util.Locale.ROOT);
        if (!CONCLUSIONS.contains(conclusion)) {
            throw new BusinessException("质检结论不合法");
        }
        if (ProcurementStatus.UNQUALIFIED.equals(conclusion)
                && !StringUtils.hasText(dto.getDisposition())) {
            throw new BusinessException("不合格批次必须选择处理方式");
        }
        if (dto.getImageId() != null) {
            InspectionImage image = imageMapper.selectById(dto.getImageId());
            if (image == null || !batchId.equals(image.getBatchId())) {
                throw new BusinessException("关联图片不属于该批次");
            }
        }
        if (dto.getDetectionId() != null) {
            VisionDetectionResult detection = detectionMapper.selectById(dto.getDetectionId());
            if (detection == null || !batchId.equals(detection.getBatchId())) {
                throw new BusinessException("关联检测结果不属于该批次");
            }
        }

        Long reviewerId = currentUserProvider.getUserId();
        if (reviewerId == null) {
            throw new BusinessException("无法识别当前复核人");
        }

        InspectionRecord record = new InspectionRecord();
        record.setBatchId(batchId);
        record.setImageId(dto.getImageId());
        record.setDetectionId(dto.getDetectionId());
        record.setReviewerId(reviewerId);
        record.setReviewTime(LocalDateTime.now());
        record.setConclusion(conclusion);
        record.setOpinion(dto.getOpinion());
        record.setDisposition(dto.getDisposition() == null ? "" : dto.getDisposition());
        record.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        batch.setInspectionStatus(conclusion);
        batchMapper.updateById(batch);

        return toRecordVO(record, loadReviewerNames(List.of(reviewerId)).get(reviewerId));
    }

    @Override
    public Page<InspectionRecordQueryVO> listRecords(Integer page, Integer size, String keyword,
                                                     String conclusion, LocalDate startDate,
                                                     LocalDate endDate) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            List<Long> batchIds = batchMapper.selectList(new LambdaQueryWrapper<MaterialBatch>()
                            .like(MaterialBatch::getBatchNo, keyword))
                    .stream().map(MaterialBatch::getId).toList();
            if (batchIds.isEmpty()) {
                return emptyRecordPage(page, size);
            }
            wrapper.in(InspectionRecord::getBatchId, batchIds);
        }
        wrapper.eq(StringUtils.hasText(conclusion), InspectionRecord::getConclusion, conclusion)
                .ge(startDate != null, InspectionRecord::getReviewTime,
                        startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, InspectionRecord::getReviewTime,
                        endDate == null ? null : endDate.atTime(LocalTime.MAX))
                .orderByDesc(InspectionRecord::getReviewTime)
                .orderByDesc(InspectionRecord::getId);

        Page<InspectionRecord> recordPage = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<InspectionRecord> records = recordPage.getRecords();
        Map<Long, MaterialBatch> batches = loadBatches(records.stream()
                .map(InspectionRecord::getBatchId).toList());
        Map<Long, Material> materials = loadMaterials(batches.values().stream()
                .map(MaterialBatch::getMaterialId).toList());
        Map<Long, Supplier> suppliers = loadSuppliers(batches.values().stream()
                .map(MaterialBatch::getSupplierId).toList());
        Map<Long, String> reviewers = loadReviewerNames(records.stream()
                .map(InspectionRecord::getReviewerId).toList());

        List<InspectionRecordQueryVO> vos = records.stream().map(record -> {
            MaterialBatch batch = batches.get(record.getBatchId());
            Material material = batch == null ? null : materials.get(batch.getMaterialId());
            Supplier supplier = batch == null ? null : suppliers.get(batch.getSupplierId());
            return InspectionRecordQueryVO.builder()
                    .id(record.getId())
                    .batchId(record.getBatchId())
                    .batchNo(batch == null ? null : batch.getBatchNo())
                    .materialCode(material == null ? null : material.getMaterialCode())
                    .materialName(material == null ? null : material.getName())
                    .supplierName(supplier == null ? null : supplier.getName())
                    .conclusion(record.getConclusion())
                    .opinion(record.getOpinion())
                    .disposition(record.getDisposition())
                    .remark(record.getRemark())
                    .reviewerId(record.getReviewerId())
                    .reviewerName(reviewers.get(record.getReviewerId()))
                    .reviewTime(record.getReviewTime())
                    .imageId(record.getImageId())
                    .detectionId(record.getDetectionId())
                    .build();
        }).toList();

        Page<InspectionRecordQueryVO> result = new Page<>(
                recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    private Page<InspectionRecordQueryVO> emptyRecordPage(Integer page, Integer size) {
        Page<InspectionRecordQueryVO> empty = new Page<>(page, size, 0);
        empty.setRecords(Collections.emptyList());
        return empty;
    }

    private Map<Long, MaterialBatch> loadBatches(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MaterialBatch::getId, batch -> batch));
    }

    private Map<Long, Material> loadMaterials(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private Map<Long, Supplier> loadSuppliers(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));
    }

    private MaterialBatch requireBatch(Long batchId) {
        MaterialBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("原材料批次不存在");
        }
        return batch;
    }

    private void ensureInspectable(MaterialBatch batch) {
        if (!INSPECTABLE_STATUSES.contains(batch.getInspectionStatus())) {
            throw new BusinessException("该批次已完成质检，不能继续上传或检测");
        }
    }

    private String normalizeDetectionStatus(JsonNode response) {
        String status = text(response, "status");
        if ("model_not_configured".equalsIgnoreCase(status)) {
            return ProcurementStatus.DETECT_MODEL_NOT_CONFIGURED;
        }
        if (response.path("success").asBoolean(false)) {
            return ProcurementStatus.DETECT_SUCCESS;
        }
        return ProcurementStatus.DETECT_FAILED;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private Integer intOrNull(JsonNode node, String field, Integer fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.isNumber() ? value.asInt() : fallback;
    }

    private InspectionImageVO toImageVO(InspectionImage image) {
        return InspectionImageVO.builder()
                .id(image.getId())
                .batchId(image.getBatchId())
                .originalName(image.getOriginalName())
                .url("/uploads/" + image.getStoredPath().replace('\\', '/'))
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .imageWidth(image.getImageWidth())
                .imageHeight(image.getImageHeight())
                .createdAt(image.getCreatedAt())
                .build();
    }

    private VisionDetectionResultVO toDetectionVO(VisionDetectionResult result) {
        List<Map<String, Object>> detections = Collections.emptyList();
        if (StringUtils.hasText(result.getDetectionsJson())) {
            try {
                detections = objectMapper.readValue(result.getDetectionsJson(),
                        new TypeReference<List<Map<String, Object>>>() { });
            } catch (Exception ignored) {
                detections = Collections.emptyList();
            }
        }
        return VisionDetectionResultVO.builder()
                .id(result.getId())
                .batchId(result.getBatchId())
                .imageId(result.getImageId())
                .modelName(result.getModelName())
                .modelVersion(result.getModelVersion())
                .status(result.getStatus())
                .imageWidth(result.getImageWidth())
                .imageHeight(result.getImageHeight())
                .detectionCount(result.getDetectionCount())
                .detections(detections)
                .annotatedImagePath(result.getAnnotatedImagePath())
                .inferenceTimeMs(result.getInferenceTimeMs())
                .errorMessage(result.getErrorMessage())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private InspectionRecordVO toRecordVO(InspectionRecord record, String reviewerName) {
        return InspectionRecordVO.builder()
                .id(record.getId())
                .batchId(record.getBatchId())
                .imageId(record.getImageId())
                .detectionId(record.getDetectionId())
                .reviewerId(record.getReviewerId())
                .reviewerName(reviewerName)
                .reviewTime(record.getReviewTime())
                .conclusion(record.getConclusion())
                .opinion(record.getOpinion())
                .disposition(record.getDisposition())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private Map<Long, String> loadReviewerNames(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        user -> StringUtils.hasText(user.getRealName())
                                ? user.getRealName() : user.getUsername()));
    }
}