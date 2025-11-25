package com.hospital.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Map;

/**
 * 提交体质测试请求DTO
 *
 * @author TCM Health Team
 * @since 2025-11-03
 */
@Data
public class SubmitTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户答案（问题ID -> 选项ID）
     */
    @NotNull(message = "答案不能为空")
    @Size(min = 66, max = 66, message = "必须完成所有66题")
    private Map<Long, Long> answers;

    /**
     * 关联的预约ID（可选，如果是通过预约提醒进行的测试）
     */
    private Long appointmentId;
}

