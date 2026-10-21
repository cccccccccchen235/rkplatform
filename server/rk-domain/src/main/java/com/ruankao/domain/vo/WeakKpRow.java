package com.ruankao.domain.vo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 薄弱知识点查询投影（{@code stat_user_kp} JOIN {@code rk_knowledge_point}）。 */
@Getter
@Setter
@NoArgsConstructor
public class WeakKpRow {

    private Long kpId;

    private String kpName;

    private Integer answeredCount;

    private BigDecimal accuracy;

    private BigDecimal mastery;
}
