package com.partner.studyreminder.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.ui.Motion

@Composable
fun BoxScope.GlassUndoBar(
    visible: Boolean,
    backdrop: Backdrop,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    bottom: Dp = 76.dp,
) {
    val colors = studyColors()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.align(Alignment.BottomCenter),
        enter = fadeIn(Motion.snappy()) + slideInVertically(Motion.snappy()) { it },
        exit = fadeOut(Motion.snappy()) + slideOutVertically { it },
    ) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, bottom = bottom)
                .glass(backdrop, GlassTier.Float, Capsule(), surface = colors.glass)
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("已删除", color = colors.label, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "撤销",
                color = colors.blue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null, onClick = onUndo)
                    .padding(8.dp),
            )
        }
    }
}
