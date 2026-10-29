/** Presents expandable beginner lessons without adding accounts, tracking or financial advice. */
package com.pawan.kmpnativeuitrading.features.learning.ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pawan.kmpnativeuitrading.features.learning.domain.model.LearningCatalog

@Composable
internal fun LearningBasics() {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    Column {
        Text("Learn before you trade")
        LearningCatalog.lessons.forEach { lesson ->
            TextButton(onClick = { expanded = if (expanded == lesson.id) null else lesson.id }) { Text(lesson.title) }
            if (expanded == lesson.id) Text(lesson.explanation, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}
