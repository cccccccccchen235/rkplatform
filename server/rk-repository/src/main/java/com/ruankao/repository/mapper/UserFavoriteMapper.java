package com.ruankao.repository.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目收藏读写（{@code user_favorite}，04 §4.3；接口 05 §5.7 第 29~31 条）。
 *
 * <p>表上有 {@code uk_user_question(user_id, question_id)}，因此收藏是<b>幂等</b>操作
 * （05 §5.1「幂等」约定：重复调用不得报错、不得产生副作用）：{@link #insertIgnore}
 * 用 {@code INSERT IGNORE} 让"重复收藏"退化成一条 {@code affected rows = 0} 的空操作，
 * 而不是抛 {@code DuplicateKeyException} 让接口层把它翻译成错误码。
 *
 * <p>收藏练习（{@code PracticeMode.FAVORITE}）的取题入口是 {@link #selectQuestionIds}，
 * 它按 {@code created_at DESC, id DESC} 取最新收藏的题；{@link #selectFavoritedIds}
 * 则服务于"在一张练习卷上标出哪些题已收藏"。
 *
 * <p>SQL 写在 {@code resources/mapper/UserFavoriteMapper.xml}。
 */
@Mapper
public interface UserFavoriteMapper {

    /**
     * 收藏（05 §5.7 第 29 条）。重复收藏幂等：命中唯一键时忽略，返回 0 行。
     *
     * @return 真正插入返回 1；已收藏返回 0（两者都算成功）
     */
    int insertIgnore(@Param("userId") Long userId,
                     @Param("questionId") Long questionId,
                     @Param("subjectId") Long subjectId);

    /** 取消收藏（05 §5.7 第 30 条）。重复取消同样无害，返回 0 行。 */
    int delete(@Param("userId") Long userId,
               @Param("questionId") Long questionId);

    /** 是否已收藏；{@code > 0} 表示已收藏（用于收藏/取消前的幂等判定）。 */
    int exists(@Param("userId") Long userId,
               @Param("questionId") Long questionId);

    /**
     * 某用户在某科目下收藏的题目 ID，按收藏时间倒序（收藏练习取题）。
     *
     * @param offset 起始偏移
     * @param limit  单页条数，由调用方收敛后传入（AC-4 上界）
     */
    List<Long> selectQuestionIds(@Param("userId") Long userId,
                                 @Param("subjectId") Long subjectId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /** 某用户在某科目下的收藏总数，供分页元信息使用。 */
    long countBySubject(@Param("userId") Long userId,
                        @Param("subjectId") Long subjectId);

    /**
     * 在给定题目集合里，筛出当前用户已收藏的那些 ID（用于给练习卷标记 {@code isFavorited}）。
     *
     * <p>调用方必须传非空集合：空集合会退化成 {@code IN ()} 语法错误，XML 侧对空集合
     * 显式返回空结果（见 {@code AND 1 = 0} 分支）以兜底。入参是"一张练习卷的题"，
     * 规模天然有界；此处再叠一条防御性 LIMIT，避免将来被误用于全库扫描。
     *
     * @param questionIds 非空题目 ID 集合
     */
    List<Long> selectFavoritedIds(@Param("userId") Long userId,
                                  @Param("questionIds") List<Long> questionIds);
}