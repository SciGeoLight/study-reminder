package com.partner.studyreminder.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.Todo
import com.partner.studyreminder.data.TodoGroup
import com.partner.studyreminder.data.TodoImages
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassOverlay
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.LiquidSwitch
import com.partner.studyreminder.ui.glass.StudyColors
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.liquidPressFeedback
import com.partner.studyreminder.ui.glass.rememberLiquidPress
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay

class TodosActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)
    private var imageSink: ((List<Uri>) -> Unit)? = null
    private var cameraSink: ((File?) -> Unit)? = null
    private var cameraFile: File? = null

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(TodoImages.MAX_COUNT),
    ) { uris ->
        val sink = imageSink
        imageSink = null
        sink?.invoke(uris)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraFile
        val sink = cameraSink
        cameraSink = null
        if (ok && file != null && file.length() > 0L) {
            sink?.invoke(file)
        } else {
            file?.delete()
            sink?.invoke(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installPush()
        edgeToEdge()
        setContent {
            StudyTheme {
                TodosScreen(
                    refreshKey = tick,
                    onBack = { finishPush() },
                    onToggle = { todo ->
                        Todos.of(this).setDone(todo.id, !todo.done)
                        AlarmScheduler.rescheduleAll(this)
                        tick++
                    },
                    onSave = { existing, title, note, start, end, remindDate, remindMinutes, groupId, photos ->
                        saveTodo(existing, title, note, start, end, remindDate, remindMinutes, groupId, photos)
                    },
                    onDelete = { todo ->
                        Todos.of(this).delete(todo.id)
                        AlarmScheduler.rescheduleAll(this)
                        tick++
                    },
                    onUndo = { todo ->
                        Todos.of(this).insert(todo)
                        AlarmScheduler.rescheduleAll(this)
                        tick++
                    },
                    onForget = { todo -> Todos.of(this).purgeImages(todo.id) },
                    onAddGroup = { name ->
                        val created = Todos.of(this).addGroup(name)
                        if (created == null) toast("已经有这个分组，或名字不合适。")
                        tick++
                        created != null
                    },
                    onRenameGroup = { id, name ->
                        val ok = Todos.of(this).renameGroup(id, name)
                        if (!ok) toast("已经有这个分组，或名字不合适。")
                        tick++
                        ok
                    },
                    onRecolorGroup = { id ->
                        val current = Todos.of(this).groups().firstOrNull { it.id == id } ?: return@TodosScreen
                        val index = TodoGroup.COLORS.indexOf(current.color).let { if (it < 0) 0 else it }
                        Todos.of(this).recolorGroup(id, TodoGroup.COLORS[(index + 1) % TodoGroup.COLORS.size])
                        tick++
                    },
                    onMoveGroup = { id, direction ->
                        Todos.of(this).moveGroup(id, direction)
                        tick++
                    },
                    onDeleteGroup = { id, deleteTodos ->
                        Todos.of(this).deleteGroup(id, deleteTodos)
                        AlarmScheduler.rescheduleAll(this)
                        tick++
                    },
                    onRemoveStoredImage = { todo, name ->
                        TodoImages.delete(filesDir, todo.id, name)
                        Todos.of(this).update(
                            id = todo.id,
                            title = todo.title,
                            note = todo.note,
                            startDate = todo.startDate,
                            endDate = todo.endDate,
                            remindDate = todo.remindDate,
                            remindMinutes = todo.remindMinutes,
                            done = todo.done,
                            groupId = todo.groupId,
                            images = todo.images.filterNot { it == name },
                            groupChanged = false,
                        )
                        tick++
                    },
                    onPickImages = { sink ->
                        imageSink = sink
                        try {
                            pickImages.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        } catch (_: ActivityNotFoundException) {
                            imageSink = null
                            toast("没有找到相册")
                        }
                    },
                    onTakePhoto = { sink ->
                        val file = File(cacheDir, "camera/${UUID.randomUUID()}.jpg")
                        file.parentFile?.mkdirs()
                        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
                        cameraFile = file
                        cameraSink = sink
                        try {
                            takePhoto.launch(uri)
                        } catch (_: ActivityNotFoundException) {
                            cameraFile = null
                            cameraSink = null
                            file.delete()
                            toast("没有找到相机")
                        }
                    },
                )
            }
        }
    }

    private fun saveTodo(
        existing: Todo?,
        title: String,
        note: String,
        start: String,
        end: String,
        remindDate: String?,
        remindMinutes: Int?,
        groupId: String?,
        photos: List<DraftPhoto>,
    ) {
        val repo = Todos.of(this)
        val id = existing?.id ?: UUID.randomUUID().toString()
        val names = mutableListOf<String>()
        var failed = false
        for (photo in photos) {
            when {
                photo.storedName != null -> names += photo.storedName
                photo.uri != null -> {
                    val saved = TodoImages.importUri(this, filesDir, id, photo.uri)
                    if (saved == null) failed = true else names += saved
                }
                photo.file != null -> {
                    val saved = TodoImages.importFile(filesDir, id, photo.file)
                    if (saved == null) failed = true else names += saved
                }
            }
        }
        existing?.images.orEmpty().filter { it !in names }.forEach { TodoImages.delete(filesDir, id, it) }
        if (existing == null) {
            repo.add(title, note, start, end, remindDate, remindMinutes, groupId, names, id)
            toast(if (failed) "已添加，有图片没能保存" else "已添加")
        } else {
            repo.update(
                id = existing.id,
                title = title,
                note = note,
                startDate = start,
                endDate = end,
                remindDate = remindDate,
                remindMinutes = remindMinutes,
                done = existing.done,
                groupId = groupId,
                images = names,
                groupChanged = existing.groupId != groupId,
            )
            toast(if (failed) "已修改，有图片没能保存" else "已修改")
        }
        AlarmScheduler.rescheduleAll(this)
        tick++
    }

    override fun onResume() {
        super.onResume()
        edgeToEdge()
        tick++
    }
}
