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
    MENU, PLAYING, GAMEOVER
}

class SnakeView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // --- 遊戲狀態與資料結構 ---
    private var currentState = GameState.MENU
    private val snakeBody = ArrayList<Point>()
    private val snakeSize = 100f

    // 🍎 蘋果與分數機制
    private var apple = Point(-100, -100)
    private var score = 0

    // --- 🚀 動力系統變數 ---
    private val gameHandler = Handler(Looper.getMainLooper())
    private var direction = "UP"
    private val delayMillis = 400L // 🐢 慢速友善：每 0.4 秒移動一格

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

    // --- 節拍器邏輯 (遊戲循環 Game Loop) ---
    private val gameLoop = object : Runnable {
        override fun run() {
            if (currentState == GameState.PLAYING) {
                moveSnake()
                invalidate()

                // 只有在還活著 (PLAYING) 的時候，才預約下一次移動
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
            GameState.PLAYING -> drawGame(canvas)
            GameState.GAMEOVER -> drawGameOver(canvas)
        }
    }

    // --- 初始化遊戲 ---
    private fun initSnake() {
        snakeBody.clear()
        score = 0 // 分數歸零

        val startX = (width / 2 / snakeSize.toInt()) * snakeSize.toInt()
        val startY = (height / 2 / snakeSize.toInt()) * snakeSize.toInt()

        // 預設給蛇三節身體
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

        // 穿牆術邏輯 (無邊界宇宙)
        val maxX = (width / snakeSize.toInt()) * snakeSize.toInt()
        val maxY = (height / snakeSize.toInt()) * snakeSize.toInt()

        if (newHead.x < 0) newHead.x = maxX - snakeSize.toInt()
        else if (newHead.x >= maxX) newHead.x = 0

        if (newHead.y < 0) newHead.y = maxY - snakeSize.toInt()
        else if (newHead.y >= maxY) newHead.y = 0

        // 💀 自我碰撞偵測 (Game Over 邏輯)
        // 檢查新算出來的頭部座標，有沒有跟目前身體的任何一個點重疊？
        val isBitingSelf = snakeBody.any { it.x == newHead.x && it.y == newHead.y }

        if (isBitingSelf) {
            currentState = GameState.GAMEOVER // 切換到死亡狀態
            return // 直接中斷這個 function，不要再往下執行了
        }

        // 如果沒咬到自己，就正常把新頭加進去
        snakeBody.add(0, newHead)

        if (newHead.x == apple.x && newHead.y == apple.y) {
            // 吃到了！分數加一，產生新蘋果
            score++
            spawnApple()
        } else {
            // 沒吃到！移除尾巴
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    // --- 繪圖模組區 ---
    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#263238")) // 深色質感背景
        val centerX = width / 2f
        val centerY = height / 2f

        paintText.textSize = 120f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("🐍 選擇遊玩時間", centerX, centerY - 550f, paintText)

        drawButton(canvas, "15 分鐘", centerY - 350f, "#81C784")
        drawButton(canvas, "30 分鐘", centerY - 150f, "#64B5F6")
        drawButton(canvas, "45 分鐘", centerY + 50f, "#E57373")
        drawButton(canvas, "不限制", centerY + 250f, "#BCAAA4")
    }

    private fun drawButton(canvas: Canvas, text: String, y: Float, colorHex: String) {
        paintSnake.color = Color.parseColor(colorHex)
        canvas.drawRoundRect(width / 2f - 300f, y - 60f, width / 2f + 300f, y + 60f, 30f, 30f, paintSnake)
        paintText.color = Color.WHITE
        paintText.textSize = 60f
        canvas.drawText(text, width / 2f, y + 20f, paintText)
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1B5E20")) // 草地綠背景

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
                // 畫蛇頭
                paintSnake.color = Color.YELLOW
                canvas.drawRoundRect(part.x.toFloat(), part.y.toFloat(),
                    part.x + snakeSize, part.y + snakeSize, 20f, 20f, paintSnake)
                // 畫眼睛
                paintSnake.color = Color.BLACK
                canvas.drawCircle(part.x + 30f, part.y + 30f, 10f, paintSnake)
                canvas.drawCircle(part.x + 70f, part.y + 30f, 10f, paintSnake)
            } else {
                // 畫蛇身
                paintSnake.color = Color.parseColor("#A5D6A7")
                canvas.drawRoundRect(part.x.toFloat() + 5f, part.y.toFloat() + 5f,
                    part.x + snakeSize - 5f, part.y + snakeSize - 5f, 15f, 15f, paintSnake)
            }
        }

        // 🏆 畫出精美的視覺化記分板
        paintSnake.color = Color.parseColor("#80000000")
        canvas.drawRoundRect(width / 2f - 220f, 40f, width / 2f + 220f, 160f, 40f, 40f, paintSnake)
        paintText.textSize = 80f
        paintText.color = Color.WHITE
        canvas.drawText("🍎 : $score", width / 2f, 125f, paintText)
    }

    private fun drawGameOver(canvas: Canvas) {
        // 💀 進階的遊戲結束畫面
        canvas.drawColor(Color.parseColor("#B71C1C")) // 暗紅色警告背景

        paintText.color = Color.WHITE
        paintText.textSize = 150f
        canvas.drawText("Oops!", width / 2f, height / 2f - 200f, paintText)

        // 顯示最終分數
        paintText.textSize = 80f
        canvas.drawText("總共吃了 $score 顆蘋果 🍎", width / 2f, height / 2f, paintText)

        // 提示玩家如何重來
        paintText.textSize = 60f
        paintText.color = Color.parseColor("#FFF176") // 黃色提示字
        canvas.drawText("點擊螢幕回到主選單", width / 2f, height / 2f + 200f, paintText)
    }

    // --- 👆 互動邏輯區 (點擊與滑動偵測) ---
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y

                // 💀 如果在 Game Over 畫面點擊螢幕，就回到選單
                if (currentState == GameState.GAMEOVER) {
                    currentState = GameState.MENU
                    invalidate() // 重新畫出選單
                    return true  // 攔截這次點擊，不往下執行
                }

                if (currentState == GameState.MENU) {
                    val centerY = height / 2f
                    if (event.y in (centerY - 410f)..(centerY + 310f)) {
                        initSnake()
                        direction = "UP" // 預設往上游
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
                            // 防呆：不能瞬間 180 度回頭
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

    // --- 系統生命週期管理 ---
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameHandler.removeCallbacks(gameLoop)
    }
}
