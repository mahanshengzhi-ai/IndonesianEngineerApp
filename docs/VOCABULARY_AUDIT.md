# 第三阶段：印尼语词汇翻译质量审计

日期：2026-09-24  
仓库：mahanshengzhi-ai/IndonesianEngineerApp

## 最终全库统计

- 词汇条目：3281
- 中文唯一释义：3281
- ID 重复：0
- 印尼语重复：0
- 中文重复：0
- 印尼语字段混入中文：0
- 明显“X结果”伪模板：0
- 位置表达非规范项：0
- 模板化坏短语：0
- “计划”误用 `jadwal`：0
- 旧 `alat ukur nivo`：0
- 必填字段缺失：0
- 学习字段为空：0

最终质量门：

`VOCABULARY_QUALITY = PASS`

## 本次清理原则

### 1. 删除明显伪模板

原数据中大量出现类似“X结果”“图纸图纸”“工作工作”等机器拼接结构。

已删除不具备独立学习价值的 `X + 结果` 批量模板，并清理：

- `gambar gambar kerja`
- `gambar gambar detail`
- `gambar gambar penampang`
- `gambar gambar denah`
- `pekerjaan pekerjaan konstruksi`
- `pekerjaan pekerjaan tanah`
- `lokasi lokasi konstruksi`

### 2. 统一工程现场用语

“位置”相关表达统一采用 `lokasi` 作为主要现场地点表达，并删除重复的 `posisi` 派生条目。

同时保留测量语境下真正表达“位置/姿态”的术语时，按具体语义处理。

### 3. 统一计划表达

中文“计划”统一使用 `rencana`，避免把“计划”一律错误地翻成 `jadwal`。

`jadwal` 更适合具体时间表/进度安排，不能机械替代所有“计划”。

### 4. 测量术语处理

`水准仪` 统一使用现场常见的 `waterpass`。

官方印尼公共工程资料中实际使用 “Water Pass / waterpass”，同时将其与 Total Station 等测量设备并列使用。

### 5. 去除重复义项

相同中文释义如果只是 `lokasi/posisi`、词序变化或模板生成差异，不再作为两条学习数据保留。

### 6. 特殊测量短语重新规整

对明显不自然的：

- 测量尺寸
- 测量规格
- 测量图纸
- 测量记录
- 测量计划
- 测量质量

不再保留机器式直译。

其中保留并规范为更适合工程学习的表达，例如：

- 测量图 → `gambar hasil pengukuran`
- 测量记录 → `catatan pengukuran`
- 测量计划 → `rencana pengukuran`
- 测量质量 → `kualitas hasil pengukuran`

## 外部工程术语核验依据

本轮没有把普通机器翻译作为唯一依据，而是抽查并对照印度尼西亚公共工程体系中的实际用语。

印尼公共工程资料中可直接看到：

- `gambar kerja`
- `pengukuran dan pematokan`
- `pembesian`
- `pemasangan bekisting`
- `pengecoran beton`
- `perancah`
- `Water Pass / waterpass`
- `Total Station`

这些用语与本项目的工程学习定位一致。

参考来源：

1. Direktorat Jenderal Bina Marga，桥梁施工监督技术指南
   https://binamarga.pu.go.id/uploads/files/921/panduan-teknik-pengawasan-pelaksanaan-jembatan.pdf

2. Direktorat Jenderal Bina Marga，2025 公路和桥梁通用规范
   https://binamarga.pu.go.id/uploads/files/2104/Spesifikasi-Umum-2025-untuk-Pekerjaan-Konstruksi-Jalan-dan-Jembatan.pdf

3. Kementerian PUPR 水平测量 / Waterpass 相关技术资料
   https://sda.pu.go.id/balai/bbwspemalijuana/files/informasi/berkala/KAK/4%20KAK%20%20Desain%20Pengendalian%20Banjir%20Kota%20SMG%2004022022.pdf

4. Direktorat Jenderal Bina Marga，项目管理及测量资料
   https://binamarga.pu.go.id/uploads/files/1688/01mbm2022-manual-manajemen-proyek-project-management-untuk-pinjaman-luar-negeri-pada-infrastructure-reconstruction-sector-loan-irsl.pdf

## 当前结论

当前词库已经从“3600 条模板化数量”调整为 **3281 条通过全库结构质量门的数据**。

这里的“通过”代表：

- 中印尼字段一一对应结构完整
- 无重复 ID
- 无重复中文释义
- 无重复印尼语
- 无中文混入印尼语字段
- 无本轮定义的明显批量伪模板
- 计划/测量/水准仪等重点术语已按工程语境统一

这并不等于 3281 条已经逐条取得官方词典级语言学认证。后续如果某个具体专业模块继续扩充，还应优先引用真实印尼工程文件作为术语来源，而不是重新批量生成词组。

## 阶段产物

最终质量审计工作流：

`.github/workflows/vocabulary-quality.yml`

质量检查脚本：

`scripts/validate_vocabulary_quality.py`

最终质量门运行结果：

`VOCABULARY_QUALITY = PASS`

最终质量审计 PR：

PR #4，已合并到 `main`。
