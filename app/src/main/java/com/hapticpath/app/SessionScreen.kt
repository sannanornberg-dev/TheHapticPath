package com.hapticpath.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val rmsVolume by viewModel.rmsVolume.collectAsState()
    val annotatedWords by viewModel.annotatedWords.collectAsState()
    val errorCount by viewModel.errorCount.collectAsState()

    val isRecording = sessionState is SessionState.Recording

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRÄNINGSSESSION",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Mönsteravbrott: $errorCount",
                    color = if (errorCount > 0) Color(0xFFFF5252) else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // --- MITTEN: Volymindikator ---
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Rättat: isRecording har tagits bort från anropet
            RmsVolumeIndicator(
                rmsVolume = rmsVolume
            )
        }

        // --- NEDRE DELEN: Text & Knappar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LiveTextDisplay
            LiveTextDisplay(
                words = annotatedWords,
                isRecording = isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            )

            // 2. Testknappar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Testa regelmotor manuellt:",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testInjectSentence("jag inte vet om detta") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Testa Fel", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.testInjectSentence("jag vet inte om detta") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Testa Rätt", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // 3. Huvudknapp Start/Stopp
            Button(
                onClick = {
                    if (isRecording) {
                        viewModel.stopSession()
                    } else {
                        viewModel.startSession()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFFF3333) else Color(0xFF00E676)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isRecording) "STOPPA SESSION" else "STARTA SESSION",
                    color = if (isRecording) Color.White else Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}