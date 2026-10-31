package dev.niklas.fitnessrpg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TestScreen() }
    }
}

@Composable
fun TestScreen() {
    var xp by remember { mutableIntStateOf(0) }
    val accent = Color(0xFF4FC3F7)
    Box(
        Modifier.fillMaxSize().background(Color(0xFF0A0E14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .border(1.dp, accent, RoundedCornerShape(8.dp))
                .background(Color(0xFF111A24), RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LIVE UPDATE WORKS", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("XP: $xp", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { xp += 10 },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) { Text("+10 XP") }
        }
    }
}
