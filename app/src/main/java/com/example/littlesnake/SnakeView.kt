package com.example.littlesnake // ⚠️ 貼上後，請務必確認這行的 package 名稱跟你的專案一致！

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

// 1. 定義遊戲狀態機
enum class GameState {
    MENU, PLAYING, PAUSED, GAMEOVER
}

class SnakeView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // --- 遊戲狀態與資料結構 ---
    private var currentState = GameState.MENU
    private val snakeBody = ArrayList<Point>()
    private val snakeSize = 100f

    // 🍎 蘋果與分數機制
    private var apple = Point(-100, -100)
    private var score = 0

    // 💾 最高紀錄機制
    private val sharedPreferences = context.getSharedPreferences("SnakeGamePrefs", Context.MODE_PRIVATE)
    private var highScore = sharedPreferences.getInt("HIGH_SCORE", 0)

    // --- 🚀 動力系統變數 ---
    private val gameHandler = Handler(Looper.getMainLooper())
    private var direction = "UP"

    // 🏎️ 漸進加速系統設定
    private val baseDelay = 400L  // 初始速度：400 毫秒走一格 (幼兒友善)
    private val minDelay = 100L   // 極限速度：最快 100 毫秒走一格 (防呆，避免太快)
    private val speedUpStep = 10L // 每吃一顆蘋果，減少 10 毫秒的延遲
    private var currentDelay = baseDelay // 目前的速度

    // --- 👆 滑動偵測變數 ---
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 50f

    // --- 畫筆設定 ---
    private val paintText = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val paintSnake = Paint().apply {
        isAntiAlias = true
    }

    // --- 節拍器邏輯 (遊戲循環) ---
    private val gameLoop = object : Runnable {
        override fun run() {
            if (currentState == GameState.PLAYING) {
                moveSnake()
                invalidate()

                if (currentState == GameState.PLAYING) {
                    // 🏎️ 依照目前的速度 (currentDelay) 來排程下一次移動
                    gameHandler.postDelayed(this, currentDelay)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (currentState) {
            GameState.MENU -> drawMenu(canvas)
            GameState.PLAYING -> {
                drawGame(canvas)
                drawPauseButton(canvas) // ⏸️ 畫出暫停鈕
            }
            GameState.PAUSED -> {
                drawGame(canvas) // 畫底圖
                drawPausedScreen(canvas) // ⏸️ 疊加半透明暫停遮罩
            }
            GameState.GAMEOVER -> drawGameOver(canvas)
        }
    }

    // --- 初始化遊戲 ---
    private fun initSnake() {
        snakeBody.clear()
        score = 0
        currentDelay = baseDelay // 🏎️ 每次重新開始，速度恢復成初始慢速

        val startX = (width / 2 / snakeSize.toInt()) * snakeSize.toInt()
        val startY = (height / 2 / snakeSize.toInt()) * snakeSize.toInt()

        snakeBody.add(Point(startX, startY))
        snakeBody.add(Point(startX, startY + snakeSize.toInt()))
        snakeBody.add(Point(startX, startY + 2 * snakeSize.toInt()))

        spawnApple()
    }

    // --- 隨機生成蘋果 ---
    private fun spawnApple() {
        val maxXCount = width / snakeSize.toInt()
        val maxYCount = height / snakeSize.toInt()

        var newAppleX: Int
        var newAppleY: Int
        var isOnSnake: Boolean

        do {
            newAppleX = (0 until maxXCount).random() * snakeSize.toInt()
            newAppleY = (0 until maxYCount).random() * snakeSize.toInt()
            isOnSnake = snakeBody.any { it.x == newAppleX && it.y == newAppleY }
        } while (isOnSnake)

        apple = Point(newAppleX, newAppleY)
    }

    // --- 🐍 移動核心邏輯 ---
    private fun moveSnake() {
        val head = snakeBody[0]
        val newHead = Point(head.x, head.y)

        when (direction) {
            "UP"    -> newHead.y -= snakeSize.toInt()
            "DOWN"  -> newHead.y += snakeSize.toInt()
            "LEFT"  -> newHead.x -= snakeSize.toInt()
            "RIGHT" -> newHead.x += snakeSize.toInt()
        }

        val maxX = (width / snakeSize.toInt()) * snakeSize.toInt()
        val maxY = (height / snakeSize.toInt()) * snakeSize.toInt()

        // 穿牆術
        if (newHead.x < 0) newHead.x = maxX - snakeSize.toInt()
        else if (newHead.x >= maxX) newHead.x = 0

        if (newHead.y < 0) newHead.y = maxY - snakeSize.toInt()
        else if (newHead.y >= maxY) newHead.y = 0

        // 自我碰撞偵測
        val isBitingSelf = snakeBody.any { it.x == newHead.x && it.y == newHead.y }

        if (isBitingSelf) {
            // 💾 更新最高紀錄
            if (score > highScore) {
                highScore = score
                sharedPreferences.edit().putInt("HIGH_SCORE", highScore).apply()
            }
            currentState = GameState.GAMEOVER
            return
        }

        snakeBody.add(0, newHead)

        // 🍎 吃蘋果判定
        if (newHead.x == apple.x && newHead.y == apple.y) {
            score++
            spawnApple()

            // 🏎️ 漸進加速：每吃一顆蘋果就變快一點，但不低於極限速度
            if (currentDelay > minDelay) {
                currentDelay -= speedUpStep
            }

        } else {
            // 沒吃到就移除尾巴
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    // --- 繪圖模組區 ---
    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#263238"))
        val centerX = width / 2f
        val centerY = height / 2f

        // ✨ 極簡版主選單
        paintText.textSize = 150f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("🐍 貪食蛇 🍎", centerX, centerY - 300f, paintText)

        paintText.textSize = 80f
        paintText.color = Color.parseColor("#A5D6A7")
        canvas.drawText("👑 最高紀錄: $highScore", centerX, centerY - 100f, paintText)

        drawButton(canvas, "開始遊戲", centerY + 250f, "#81C784")
    }

    private fun drawButton(canvas: Canvas, text: String, y: Float, colorHex: String) {
        paintSnake.color = Color.parseColor(colorHex)
        canvas.drawRoundRect(width / 2f - 300f, y - 60f, width / 2f + 300f, y + 60f, 30f, 30f, paintSnake)
        paintText.color = Color.WHITE
        paintText.textSize = 60f
        canvas.drawText(text, width / 2f, y + 20f, paintText)
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1B5E20"))

        // 畫蘋果
        paintSnake.color = Color.RED
        val radius = snakeSize / 2.5f
        val cx = apple.x + snakeSize / 2f
        val cy = apple.y + snakeSize / 2f
        canvas.drawCircle(cx, cy, radius, paintSnake)
        paintSnake.color = Color.GREEN
        canvas.drawCircle(cx + 15f, cy - 25f, 10f, paintSnake)

        // 畫蛇
        for (i in snakeBody.indices) {
            val part = snakeBody[i]
            if (i == 0) {
                paintSnake.color = Color.YELLOW
                canvas.drawRoundRect(part.x.toFloat(), part.y.toFloat(),
                    part.x + snakeSize, part.y + snakeSize, 20f, 20f, paintSnake)
                paintSnake.color = Color.BLACK
                canvas.drawCircle(part.x + 30f, part.y + 30f, 10f, paintSnake)
                canvas.drawCircle(part.x + 70f, part.y + 30f, 10f, paintSnake)
            } else {
                paintSnake.color = Color.parseColor("#A5D6A7")
                canvas.drawRoundRect(part.x.toFloat() + 5f, part.y.toFloat() + 5f,
                    part.x + snakeSize - 5f, part.y + snakeSize - 5f, 15f, 15f, paintSnake)
            }
        }

        // 畫記分板
        paintSnake.color = Color.parseColor("#80000000")
        canvas.drawRoundRect(width / 2f - 220f, 40f, width / 2f + 220f, 160f, 40f, 40f, paintSnake)
        paintText.textSize = 80f
        paintText.color = Color.WHITE
        canvas.drawText("🍎 : $score", width / 2f, 125f, paintText)
    }

    private fun drawPauseButton(canvas: Canvas) {
        val btnX = width - 100f
        val btnY = 100f
        paintSnake.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(btnX, btnY, 60f, paintSnake)

        paintSnake.color = Color.BLACK
        canvas.drawRect(btnX - 25f, btnY - 25f, btnX - 10f, btnY + 25f, paintSnake)
        canvas.drawRect(btnX + 10f, btnY - 25f, btnX + 25f, btnY + 25f, paintSnake)
    }

    private fun drawPausedScreen(canvas: Canvas) {
        paintSnake.color = Color.parseColor("#B3000000")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintSnake)

        paintText.textSize = 120f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("遊戲暫停 ⏸️", width / 2f, height / 2f - 100f, paintText)

        paintText.textSize = 60f
        paintText.color = Color.WHITE
        canvas.drawText("點擊螢幕任何地方繼續", width / 2f, height / 2f + 100f, paintText)
    }

    private fun drawGameOver(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#B71C1C"))

        paintText.color = Color.WHITE
        paintText.textSize = 150f
        canvas.drawText("Oops!", width / 2f, height / 2f - 300f, paintText)

        paintText.textSize = 80f
        canvas.drawText("本次得分: $score 🍎", width / 2f, height / 2f - 100f, paintText)

        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("👑 最高紀錄: $highScore 🍎", width / 2f, height / 2f + 50f, paintText)

        paintText.textSize = 60f
        paintText.color = Color.WHITE
        canvas.drawText("點擊螢幕回到主選單", width / 2f, height / 2f + 250f, paintText)
    }

    // --- 👆 互動邏輯區 ---
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y

                // Game Over 點擊回選單
                if (currentState == GameState.GAMEOVER) {
                    currentState = GameState.MENU
                    invalidate()
                    return true
                }

                // 暫停時點擊恢復
                if (currentState == GameState.PAUSED) {
                    currentState = GameState.PLAYING
                    gameHandler.removeCallbacks(gameLoop)
                    gameHandler.post(gameLoop)
                    invalidate()
                    return true
                }

                // 遊戲中點擊右上角暫停
                if (currentState == GameState.PLAYING) {
                    if (event.x > width - 200f && event.y < 200f) {
                        currentState = GameState.PAUSED
                        invalidate()
                        return true
                    }
                }

                // ✨ 極簡版 UX：主選單隨便點擊螢幕任何地方就能開始！
                if (currentState == GameState.MENU) {
                    initSnake()
                    direction = "UP"
                    currentState = GameState.PLAYING
                    gameHandler.removeCallbacks(gameLoop)
                    gameHandler.post(gameLoop)
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (currentState == GameState.PLAYING) {
                    val dx = event.x - touchStartX
                    val dy = event.y - touchStartY

                    if (abs(dx) > swipeThreshold || abs(dy) > swipeThreshold) {
                        if (abs(dx) > abs(dy)) {
                            if (dx > 0 && direction != "LEFT") direction = "RIGHT"
                            else if (dx < 0 && direction != "RIGHT") direction = "LEFT"
                        } else {
                            if (dy > 0 && direction != "UP") direction = "DOWN"
                            else if (dy < 0 && direction != "DOWN") direction = "UP"
                        }
                    }
                }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameHandler.removeCallbacks(gameLoop)
    }
}
