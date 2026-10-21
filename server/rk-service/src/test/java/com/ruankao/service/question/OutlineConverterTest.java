package com.ruankao.service.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.vo.OutlineNodeVO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 章节树组装（{@link OutlineConverter}）。
 *
 * <p>这几条断言都是<b>给前端看的约定</b>：根节点 {@code parentId} 恒为 {@code "0"}、
 * 叶子 {@code children} 是空列表而非 null。前端少写两个判空分支的代价，
 * 是后端必须把这两条钉死——一旦某天有人把 {@code "0"} 改成 {@code null}，
 * 表现是"章节树整棵树不渲染"，而报错指向的是前端。
 *
 * <p>输入顺序按约定是 {@code level, sort_no, id} 升序（SQL 里排好），
 * 本测试也按这个顺序构造数据；转换器依赖"父节点先出现"这一点。
 *
 * <p>注意：{@code 父节点缺失 → WARN} 这一半没有断言（本层没有日志采集依赖，
 * 引入一个只为断言一条日志不值得）。测试只钉住可观察的行为——提升为根。
 */
class OutlineConverterTest {

    private static OutlineNode node(long id, long parentId, int level, String name, int sortNo) {
        OutlineNode node = new OutlineNode();
        node.setId(id);
        node.setSubjectId(2L);
        node.setParentId(parentId);
        node.setLevel(level);
        node.setName(name);
        node.setSortNo(sortNo);
        node.setQuestionCount(0);
        node.setStatus("ENABLED");
        return node;
    }

    @Test
    @DisplayName("根节点的 parentId 是字符串 \"0\"，不是 null")
    void rootParentIdIsStringZero() {
        List<OutlineNodeVO> roots = OutlineConverter.toTree(List.of(node(1L, 0L, 1, "第一章", 1)));

        assertEquals(1, roots.size());
        assertEquals("0", roots.get(0).parentId());
        assertNotNull(roots.get(0).parentId(), "根节点 parentId 不能是 null——前端按 === \"0\" 找根");
    }

    @Test
    @DisplayName("叶子的 children 是空列表，不是 null，且可写")
    void leafChildrenIsEmptyListNotNull() {
        List<OutlineNodeVO> roots = OutlineConverter.toTree(List.of(node(1L, 0L, 1, "第一章", 1)));

        assertNotNull(roots.get(0).children());
        assertTrue(roots.get(0).children().isEmpty());
        // 必须是可变的：转换器自己就在往里 add（第 68 行），
        // 若哪天改成 List.of() 这类不可变空表，挂子节点会在运行时炸。
        List<OutlineNodeVO> children = roots.get(0).children();
        children.add(new OutlineNodeVO("9", "1", 2, "哨兵", 1, 0, new ArrayList<>()));
        assertEquals(1, children.size());
    }

    @Test
    @DisplayName("三层树按父子挂载，children 里就是子节点本身")
    void buildsThreeLevelTree() {
        List<OutlineNodeVO> roots = OutlineConverter.toTree(List.of(
                node(1L, 0L, 1, "第一章", 1),
                node(2L, 1L, 2, "1.1", 1),
                node(3L, 2L, 3, "1.1.1", 1)));

        assertEquals(1, roots.size());
        OutlineNodeVO chapter = roots.get(0);
        assertEquals(1, chapter.children().size());
        OutlineNodeVO section = chapter.children().get(0);
        assertEquals(1, section.children().size());
        assertEquals("1.1.1", section.children().get(0).name());
        assertEquals(3, section.children().get(0).level());
        // 子节点的 parentId 指向父节点的 id（字符串形态）
        assertEquals("1", section.parentId());
        assertEquals("2", section.children().get(0).parentId());
    }

    @Test
    @DisplayName("悬空的父节点被提升为根，而不是被丢掉")
    void danglingParentIsPromotedToRoot() {
        // 2 的父节点 999 不在输入里（可能已被删除）。它必须出现在根数组里——
        // 悄悄藏起来的表现是"我明明建过的章节不见了"，且没有任何线索。
        List<OutlineNodeVO> roots = OutlineConverter.toTree(List.of(
                node(1L, 0L, 1, "第一章", 1),
                node(2L, 999L, 2, "孤儿章节", 1)));

        assertEquals(2, roots.size(), "孤儿章节要留在根数组里");
        OutlineNodeVO orphan = roots.stream()
                .filter(vo -> "孤儿章节".equals(vo.name()))
                .findFirst()
                .orElseThrow();
        // parentId 原样透出（"999"），不被改写成 "0"：前端据此能看出它挂错了
        assertEquals("999", orphan.parentId());
    }

    @Test
    @DisplayName("同级按 sortNo 升序，与输入顺序无关")
    void sortsSiblingsBySortNo() {
        List<OutlineNodeVO> roots = OutlineConverter.toTree(new ArrayList<>(List.of(
                node(1L, 0L, 1, "甲", 3),
                node(2L, 0L, 1, "乙", 1),
                node(3L, 0L, 1, "丙", 2))));

        assertEquals(List.of("乙", "丙", "甲"),
                roots.stream().map(OutlineNodeVO::name).toList());
    }

    @Test
    @DisplayName("空输入返回空根数组，不抛异常")
    void emptyInputYieldsEmptyRoots() {
        assertTrue(OutlineConverter.toTree(List.of()).isEmpty());
    }

    @Test
    @DisplayName("VO 的 id/subjectId 以字符串形态透出，questionCount 原样带出")
    void carriesScalarFields() {
        OutlineNode raw = node(42L, 0L, 1, "带题量的章", 1);
        raw.setQuestionCount(7);

        OutlineNodeVO vo = OutlineConverter.toTree(List.of(raw)).get(0);
        assertEquals("42", vo.id());
        assertEquals(1, vo.level());
        assertEquals(7, vo.questionCount());
        assertEquals(1, vo.sortNo());
        assertEquals("带题量的章", vo.name(), "名字原样透出——trim 发生在写入口，不在读出口");
    }
}
