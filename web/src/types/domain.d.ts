// 前端内部使用的类型（不是接口契约）。
//
// 与 api.d.ts 的分工：api.d.ts 的每个字都来自 05 文档；这里放的是前端自己的概念，
// 05 与 02 都不关心。把两者混在一个文件里，会让"这一处改动要不要同步文档"变得无法判断。
//
// 本文件**只放类型**：`.d.ts` 不能包含带实现的声明，所以展示用的中文映射表
// 放在同目录的 labels.ts 里（07 §7.11 只写了"枚举联合类型"，没有规定映射表放哪）。

/** 练习模式（07 §7.3.1）。仅 SAMPLE 允许游客访问。 */
export type PracticeMode = 'CHAPTER' | 'KP' | 'SEQUENCE' | 'FAVORITE' | 'WRONG_REDO' | 'SAMPLE'

/** 科目列表页的三级分组顺序（01 §1.3）。页面按它渲染，不依赖接口返回的键序。 */
export type SubjectLevelOrder = ['JUNIOR', 'MIDDLE', 'SENIOR']
