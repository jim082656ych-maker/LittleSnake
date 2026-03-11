package com.example.littlesnake
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 讀取你的遊戲畫面
        setContentView(R.layout.activity_main)
    }
}

