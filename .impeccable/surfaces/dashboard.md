# 车机驾驶仪表

## Direction contract

- Mode：Operate，Android 原生横屏。
- THESIS：一眼看速度与当前时间，定位诊断按需查看。
- OWN-WORLD：深蓝灰夜间底色、白色速度、青绿色时钟与琥珀色日期；日间以浅灰绿底、深色数字保持相同角色。
- STORY：先读速度，再读时分秒，日期次一级；记录行程，停车时打开定位详情。
- FIRST VIEWPORT：实际可用窗口内，顶部一行状态，中央左侧速度、右侧日期时间；下方四列行程统计和固定操作区。
- FORM：根据用户确认的信息优先级与双色时间要求直接实现。设计探索参考 seed:51c8a55e72be0a63；未向用户承诺任何图库概念。
- FINISH：unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## 验证范围

1024×528 内容区、API 27/28 离线原生渲染、1.0/1.3 倍字号、日夜主题、定位详情、授权与结束确认。真实车机系统栏与 GNSS 驱动仍待硬件验收。截图使用测试样例数据，不是实测驾驶记录。
