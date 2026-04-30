package com.example.baseandroidmodulehybrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.baseandroidmodulehybrid.R
import com.example.baseandroidmodulehybrid.updater.UpdateState

@Composable
fun UpdateOverlay(state: UpdateState) {
    when (state) {
        is UpdateState.Checking, is UpdateState.Downloading, is UpdateState.Extracting, UpdateState.Applying -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val title = when (state) {
                            is UpdateState.Checking -> stringResource(R.string.overlay_checking)
                            is UpdateState.Downloading -> stringResource(R.string.overlay_downloading)
                            is UpdateState.Extracting -> stringResource(R.string.overlay_installing)
                            else -> stringResource(R.string.overlay_applying)
                        }
                        
                        val progressValue = when (state) {
                            is UpdateState.Downloading -> state.progress / 100f
                            is UpdateState.Extracting -> state.progress / 100f
                            else -> null
                        }

                        Text(text = title, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (progressValue != null) {
                            LinearProgressIndicator(
                                progress = { progressValue },
                                modifier = Modifier.width(120.dp).height(2.dp)
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
        else -> {}
    }
}
