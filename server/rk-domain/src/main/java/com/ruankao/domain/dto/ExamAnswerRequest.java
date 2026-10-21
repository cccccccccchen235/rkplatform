package com.ruankao.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 保存作答（05 §5.6 第 19 条）。
 *
 * <p>{@code items} 允许为空数组：此时退化为**心跳与计时校准**请求，服务端不写任何作答，
 * 只做超时检测并返回最新剩余时长（前端在长题干/写论文停顿期间每分钟调一次）。
 */
public class ExamAnswerRequest {

    @NotNull(message = "items 不能为空")
    @Size(max = 100, message = "单次最多保存 100 条作答")
    @Valid
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    /** 单条作答。不含对错：判分只发生在交卷时（05 §5.6 第 19 条）。 */
    public static class Item {

        @NotBlank(message = "itemId 不能为空")
        private String itemId;

        /** 客观题为选项 key；主观题/论文为文本。未作答传空串。 */
        private String userAnswer;

        private Integer durationMs;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        public Integer getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Integer durationMs) {
            this.durationMs = durationMs;
        }
    }
}
