package com.beeregg2001.komorebi.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.beeregg2001.komorebi.data.model.EpgProgram

@Composable
fun MiniEpgOverlay(
    programs: List<EpgProgram>,
    modifier: Modifier = Modifier
) {
    if (programs.isEmpty()) return

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        programs.take(2).forEachIndexed { index, program ->
            Text(
                text = if (index == 0) "現在: ${program.title}" else "次: ${program.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
