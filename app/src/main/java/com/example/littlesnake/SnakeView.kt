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

// 1. ⏸️ 暫停機制：定義遊戲狀態機 (新增 PAUSED 狀態)
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
    private val delayMillis = 400L // 🐢 慢速友善

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
                    gameHandler.postDelayed(this, delayMillis)
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
                drawPauseButton(canvas) // ⏸️ 畫出右上角的暫停鈕
            }
            GameState.PAUSED -> {
                drawGame(canvas) // 先畫出原本的遊戲畫面當底圖
                drawPausedScreen(canvas) // ⏸️ 疊加暫停選單
            }
            GameState.GAMEOVER -> drawGameOver(canvas)
        }
    }

    // --- 初始化遊戲 ---
    private fun initSnake() {
        snakeBody.clear()
        score = 0

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

        if (newHead.x < 0) newHead.x = maxX - snakeSize.toInt()
        else if (newHead.x >= maxX) newHead.x = 0

        if (newHead.y < 0) newHead.y = maxY - snakeSize.toInt()
        else if (newHead.y >= maxY) newHead.y = 0

        val isBitingSelf = snakeBody.any { it.x == newHead.x && it.y == newHead.y }

        if (isBitingSelf) {
            if (score > highScore) {
                highScore = score
                sharedPreferences.edit().putInt("HIGH_SCORE", highScore).apply()
            }
            currentState = GameState.GAMEOVER
            return
        }

        snakeBody.add(0, newHead)

        if (newHead.x == apple.x && newHead.y == apple.y) {
            score++
            spawnApple()
        } else {
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    // --- 繪圖模組區 ---
    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#263238"))
        val centerX = width / 2f
        val centerY = height / 2f

        paintText.textSize = 120f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("🐍 選擇遊玩時間", centerX, centerY - 550f, paintText)

        paintText.textSize = 70f
        paintText.color = Color.parseColor("#A5D6A7")
        canvas.drawText("👑 最高紀錄: $highScore 🍎", centerX, centerY - 400f, paintText)

        drawButton(canvas, "15 分鐘", centerY - 200f, "#81C784")
        drawButton(canvas, "30 分鐘", centerY, "#64B5F6")
        drawButton(canvas, "45 分鐘", centerY + 200f, "#E57373")
        drawButton(canvas, "不限制", centerY + 400f, "#BCAAA4")
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

        paintSnake.color = Color.RED
        val radius = snakeSize / 2.5f
        val cx = apple.x + snakeSize / 2f
        val cy = apple.y + snakeSize / 2f
        canvas.drawCircle(cx, cy, radius, paintSnake)

        paintSnake.color = Color.GREEN
        canvas.drawCircle(cx + 15f, cy - 25f, 10f, paintSnake)

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

        // 畫出記分板
        paintSnake.color = Color.parseColor("#80000000")
        canvas.drawRoundRect(width / 2f - 220f, 40f, width / 2f + 220f, 160f, 40f, 40f, paintSnake)
        paintText.textSize = 80f
        paintText.color = Color.WHITE
        canvas.drawText("🍎 : $score", width / 2f, 125f, paintText)
    }

    // ⏸️ 畫出右上角的暫停按鈕
    private fun drawPauseButton(canvas: Canvas) {
        val btnX = width - 100f
        val btnY = 100f
        // 畫一個白色的半透明圓底
        paintSnake.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(btnX, btnY, 60f, paintSnake)

        // 畫出兩條線 (⏸ 符號)
        paintSnake.color = Color.BLACK
        canvas.drawRect(btnX - 25f, btnY - 25f, btnX - 10f, btnY + 25f, paintSnake)
        canvas.drawRect(btnX + 10f, btnY - 25f, btnX + 25f, btnY + 25f, paintSnake)
    }

    // ⏸️ 畫出暫停時的半透明覆蓋選單
    private fun drawPausedScreen(canvas: Canvas) {
        // 鋪上一層半透明的黑色遮罩，讓後面的遊戲畫面變暗
        paintSnake.color = Color.parseColor("#B3000000") // 70% 透明度的黑色
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

                // 💀 Game Over 畫面點擊回選單
                if (currentState == GameState.GAMEOVER) {
                    currentState = GameState.MENU
                    invalidate()
                    return true
                }

                // ⏸️ 暫停畫面點擊恢復遊戲
                if (currentState == GameState.PAUSED) {
                    currentState = GameState.PLAYING
                    // 重新啟動節拍器！先 remove 確保不會有兩個節拍器同時跑
                    gameHandler.removeCallbacks(gameLoop)
                    gameHandler.post(gameLoop)
                    invalidate()
                    return true
                }

                // ⏸️ 遊戲進行中：檢查是否點擊右上角暫停按鈕
                if (currentState == GameState.PLAYING) {
                    // 判斷點擊位置是否在右上角 (寬 200px, 高 200px 範圍內)
                    if (event.x > width - 200f && event.y < 200f) {
                        currentState = GameState.PAUSED
                        invalidate() // 重新畫圖，就會畫出暫停遮罩
                        return true  // 攔截點擊，不要去算滑動了
                    }
                }

                // 主選單點擊開始遊戲
                if (currentState == GameState.MENU) {
                    val centerY = height / 2f
                    if (event.y in (centerY - 260f)..(centerY + 460f)) {
                        initSnake()
                        direction = "UP"
                        currentState = GameState.PLAYING
                        gameHandler.removeCallbacks(gameLoop)
                        gameHandler.post(gameLoop)
                        invalidate()
                    }
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
