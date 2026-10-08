# UI 审计（S0 基线）

学习提醒 1.7.8（`versionCode` 24）。目标设备是小米澎湃 OS / Android 17。本文件只记录合并版 v2 动手前的界面现状，供后面的阶段对照。不含同步地址、课表或身份信息。

记录日期：2026-10-08。环境是云端代理，没有用户的真机。

## 测试基线

命令：`./gradlew test`

结果：`BUILD SUCCESSFUL`（约 2 分 14 秒）。`testDebugUnitTest` 共 **37** 项，失败 0，错误 0，跳过 0。

| 套件 | 项数 | 失败 |
| --- | ---: | ---: |
| `AlarmWindowTest` | 3 | 0 |
| `FocusParamsTest` | 3 | 0 |
| `BackgroundScaleTest` | 1 | 0 |
| `PlanRepositoryTest` | 8 | 0 |
| `TodoRepositoryTest` | 8 | 0 |
| `IcsParserTest` | 2 | 0 |
| `PlanTimeTest` | 3 | 0 |
| `TextPlanParserTest` | 8 | 0 |
| `ScreenShotTest` | 1 | 0 |

`ScreenShotTest` 用 Robolectric（sdk 35）渲染主页和待办，只核对布局能否画出来，不测折射，也不测掉帧。

## 真机掉帧

真机掉帧待补。云端没有用户的小米真机，无法用 Android Studio Profiler 或 JankStats 记录主页滚动、换日、弹层的掉帧。S9 要和这份基线比，目前没有数字。

## 现状问题

### `liquidGlass` 把折射钳成同一档

`ui/glass/Liquid.kt` 的 `liquidGlass()` 默认 `clampLens = true`：

- 模糊被 `minOf(blurRadius, 2.dp)` 压到最多 2dp。
- 折射量被 `maxOf(refraction, 24.dp).coerceAtMost(24.dp)` 固定成 24dp。
- 透镜高度在钳制开启时固定为 12dp，不再跟传入的折射走。

因此默认情况下，调用处传入的 `refraction` / `blurRadius` 全部失效。全应用只有两处显式 `clampLens = false`（主页大标题拖动、全屏提醒的滑动拇指），这两处才会按传入值画，高度为折射的一半。

仍走默认钳制、但传入值并不是 24dp / 2dp 的例子：导入预览底部胶囊（模糊 4dp、折射 12dp）、主页折叠标题（模糊 6dp、折射 12dp）、设置里选中的外观行（模糊 4dp、折射 12dp）、权限状态药丸和主页小药丸（折射 6dp）、待办弹层把手（折射 8dp）、待办分组小节（折射 20dp）。这些现在看起来都和 24dp 透镜一样。

### 一屏透镜太多

每张计划卡、每条待办、每个 `RemindCircle`、每颗药丸都单独 `drawBackdrop` 再加 `lens`。一天有十几条计划时，一屏透镜远超过 8 个。列表滑出屏幕后，预取范围内的项仍会画。

### 其余（本阶段不改行为）

- `ui` 里几乎没有 `animateItem`、`AnimatedContent`、`Crossfade`。换日、完成、删除是直接换内容。
- 拖动多用 `scope.launch { offset.snapTo(...) }`，松手不带手指速度，越界没有阻尼。Kyant 的 `DampedDragAnimation` 和 `InteractiveHighlight` 已经用在开关、滑块和底部分组条上。
- 进行中、剩余分钟、进度不会自己走到下一分钟。
- `RemindCircle` 命中区 22dp；主页顶栏圆形按钮 40dp，都低于 44dp。
- 触感基本上只有完成待办用了 `CONFIRM`。
- 各 Activity 使用系统默认转场，窗口背景是纯色。
- 图标是 `Icons.Rounded`。
- 滑块上的数字直接压在彩色玻璃上，是局部可读性最差的地方。

这些留到 S2 及以后。闹钟、同步、权限判断、签名和 `versionCode` 不在本次界面审计里改。

保留、只改外观时也不能拆掉的行为：`LiquidPage` 的背景层和内容层；可拖动的 `LiquidBottomTabs` 和「…」选分组；待办右滑完成、左滑删除、4 秒撤销；分桶；标题折叠；左右滑换天；多个 Activity。

## `drawBackdrop` 怎么串起来

`LiquidPage` 用 `rememberLayerBackdrop()`，再把背景放进 `Modifier.layerBackdrop`。自选照片是一张 `Image` 加罩层；没有照片时，`Canvas` 画四段线性渐变和四颗光斑。前景玻璃都采样这一层，所以弹层必须留在同一个窗口里（`GlassOverlay` 的注释写了这一点）。

直接调用 `drawBackdrop` 的位置：

| 位置 | 采谁 | 作用 |
| --- | --- | --- |
| `liquidGlass` | 页面层 | 卡片、胶囊、弹层、图标按钮的公共入口 |
| `LiquidButton` | 页面层 | 实心操作按钮，透镜写死 12dp / 24dp |
| `LiquidSwitch` 拇指 | 页面层 + 轨道层（`rememberCombinedBackdrop`） | 拇指；轨道自己有一层 `layerBackdrop` |
| `LiquidSlider` 拇指 | 页面层 + 轨道层 | 同上 |
| `LiquidBottomTabs` 轨道 | 页面层 | 分组条底 |
| `LiquidBottomTabs` 标签层 | 页面层 | `alpha = 0` 的一层，仍挂了透镜 |
| `LiquidBottomTabs` 选中胶囊 | 页面层 + 分组条层 | 拖动的那一截 |
| `RemindCircle` | 页面层 | 待办每一行的圆圈 |
| 待办编辑弹层 | 页面层 | 整页底部弹层，透镜写死 16dp / 32dp |

`rememberBackdrop { drawBackdrop() }` 只是在组合采样，不再单独加一个透镜元件。

背景图：`Backgrounds.load()` 在组合阶段同步 `BitmapFactory.decodeFile`。每个 Activity 的 `remember` 各解一次，进程内没有共享位图。设置页缩略图又解一次。

`MainScreen.TopFade`：顶栏一条 `Canvas` 里调用 `drawStudyBackdrop`，把整屏渐变加四颗光斑（或整张照片加罩层）重画一遍，再用 `DstIn` 做竖向渐隐。默认背景也会走这条整屏重画。

## Backdrop 2.0.1（S1 动手前核对）

依赖是 `io.github.kyant0:backdrop:2.0.1`。对照过官方文档 <https://kyant.gitbook.io/backdrop/api/backdrop-effects>、底部弹层教程，以及标签 `2.0.1` 的目录示例和 `DrawBackdropModifier.kt` / `Lens.kt`。

`lens` 的参数名是：

```kotlin
lens(
    refractionHeight: Float,
    refractionAmount: Float = height,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
)
```

效果顺序必须是颜色滤镜，然后模糊，然后透镜。`shape` 必须是 `CornerBasedShape`（或 Kyant 的 `RoundedRectangularShape`），否则 `lens` 会抛异常。文档要求 `refractionHeight` 落在 `[0, shape.minCornerRadius]`（超出时角落可能断开，但仍允许），`refractionAmount` 落在 `[0, size.minDimension]`。第三个位置参数是 `depthEffect`，不是色散；色散必须写 `chromaticAberration`。

目录里的 `LiquidButton`（2.0.1）写法是 `vibrancy()`、`blur(2.dp)`、`lens(12.dp, 24.dp)`。滑块和开关的示例把透镜乘在按压进度上，静止时折射可以是 0，那是控件自己的动画，不是卡片的默认透镜。

轻量绘制：2.0.1 有公开的 `drawPlainBackdrop`。它走同一套效果，但不挂 highlight、投影和内阴影。没有另一套更便宜的透镜着色器；不调用 `lens()` 才会跳过折射 / 色散的 `RuntimeShader`。底部弹层教程用 `lens(24.dp, 48.dp, depthEffect = true)` 和 50% 白，那是示例，不是本应用的 Panel 档。

文档和示例都没有「降低玻璃效果」开关。Card 档不能靠加厚白罩或降低光斑透明度来换可读性。

## S9 真机核对（待真机复测）

云端没有真机，也没有 Android Studio Profiler。下面每条都标成「待真机复测」，不在这里填掉帧数字。

- 主页滚动：计划卡只有靠前的几条走透镜（`lensEnabled`，预算内），滑出组合的项不再画。待真机复测。
- 换日：左右滑换天，内容用约 280ms 的过渡，玻璃不闪成纯色。待真机复测。
- 弹层：计划编辑、待办编辑、更多菜单留在同一个 `LiquidPage` 里，折射能采到背后的背景。待真机复测。
- 全屏提醒：时间卡是 Card 档，滑动关闭还在；系统把动画时长或转场缩放关到 0 时，时间后的两圈不画。待真机复测。
- 折射对比：Card 白罩不超过 25%，和 Control / Float 不是同一套透镜。圆形按钮静止时仍接近目录的 12dp / 24dp。待真机复测。
- 待办列表：分组条（`LiquidBottomTabs` 和「…」）还在。磁贴按今天 / 已逾期 / 全部 / 已完成过滤。列表里超过 `GlassLensBudget`（8）的行不再调用 `lens()`。待真机复测。
- 提前提醒数字：浅色落在不透明白底上，深色落在不透明 `#0B1020` 上。待真机复测。
