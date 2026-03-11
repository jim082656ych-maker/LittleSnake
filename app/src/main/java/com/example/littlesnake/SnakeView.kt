package com.example.littlesnake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class SnakeView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    // 🎨 畫筆設定
    private val paintBody = Paint().apply { color = Color.parseColor("#4CAF50"); isAntiAlias = true }
    private val paintHead = Paint().apply { color = Color.parseColor("#66BB6A"); isAntiAlias = true }
    private val paintApple = Paint().apply { color = Color.parseColor("#E53935"); isAntiAlias = true }
    private val paintDetails = Paint().apply { isAntiAlias = true }
    private val paintText = Paint().apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    // 🕹️ 遊戲核心數據
    private var snake = mutableListOf(Pair(10, 10), Pair(10, 11), Pair(10, 12))
    private var direction = Pair(0, -1)
    private val gridSize = 20
    private var timer: Timer? = null
    private var apple = Pair(Random.nextInt(gridSize), Random.nextInt(gridSize))

    private var startX = 0f
    private var startY = 0f
    private var score = 0
    private var isPaused = false

    // ⏳ 狀態機與時間管理
    private enum class GameState { MENU, PLAYING, TIME_UP }
    private var currentState = GameState.MENU
    private var endTimeMs: Long = 0

    init {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (currentState == GameState.PLAYING) {
                    // 【修改】如果 endTimeMs 不是 -1 (代表有時間限制)，才檢查有沒有超時
                    if (endTimeMs != -1L && System.currentTimeMillis() >= endTimeMs) {
                        currentState = GameState.TIME_UP
                    } else if (!isPaused) {
                        moveSnake()
                    }
                }
                postInvalidate()
            }
        }, 0, 300)
    }

    // 啟動遊戲 (傳入 -1 代表不限制時間)
    private fun startGame(minutes: Int) {
        if (minutes == -1) {
            endTimeMs = -1L // 設定為 -1，啟動無限模式
        } else {
            endTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000L)
        }

        snake.clear()
        snake.addAll(listOf(Pair(10, 10), Pair(10, 11), Pair(10, 12)))
        direction = Pair(0, -1)
        score = 0
        isPaused = false
        apple = Pair(Random.nextInt(gridSize), Random.nextInt(gridSize))
        currentState = GameState.PLAYING
    }

    private fun moveSnake() {
        val head = snake[0]
        val newX = (head.first + direction.first + gridSize) % gridSize
        val newY = (head.second + direction.second + gridSize) % gridSize
        val newHead = Pair(newX, newY)

        if (snake.contains(newHead)) {
            snake.clear()
            snake.addAll(listOf(Pair(10, 10), Pair(10, 11), Pair(10, 12)))
            direction = Pair(0, -1)
            apple = Pair(Random.nextInt(gridSize), Random.nextInt(gridSize))
            score = 0
            return
        }

        snake.add(0, newHead)
        if (newHead == apple) {
            apple = Pair(Random.nextInt(gridSize), Random.nextInt(gridSize))
            score += 10
        } else {
            snake.removeAt(snake.size - 1)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (currentState) {
            GameState.MENU -> drawMenu(canvas)
            GameState.PLAYING -> drawGame(canvas)
            GameState.TIME_UP -> {
                drawGame(canvas)
                drawTimeUpScreen(canvas)
            }
        }
    }

    // 🎨 主選單 (加入四個按鈕)
    private fun drawMenu(canvas: Canvas) {
        paintText.textSize = 120f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("🐍 選擇遊玩時間", width / 2f, height / 2f - 550f, paintText)

        // 重新排版，畫出四個按鈕
        drawButton(canvas, "15 分鐘", height / 2f - 350f, Color.parseColor("#81C784"))
        drawButton(canvas, "30 分鐘", height / 2f - 150f, Color.parseColor("#64B5F6"))
        drawButton(canvas, "45 分鐘", height / 2f + 50f, Color.parseColor("#E57373"))
        drawButton(canvas, "不限制", height / 2f + 250f, Color.parseColor("#BCAAA4")) // 溫暖的棕色
    }

    private fun drawButton(canvas: Canvas, text: String, y: Float, color: Int) {
        val cx = width / 2f
        val bw = 350f
        val bh = 80f
        paintDetails.color = color
        canvas.drawRoundRect(cx - bw, y - bh, cx + bw, y + bh, 50f, 50f, paintDetails)
        paintText.textSize = 80f
        paintText.color = Color.WHITE
        canvas.drawText(text, cx, y + 25f, paintText)
    }

    // 🎨 休息畫面
    private fun drawTimeUpScreen(canvas: Canvas) {
        paintDetails.color = Color.parseColor("#EE000000") // 稍微加深一點黑色遮罩
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintDetails)

        paintText.textSize = 130f
        paintText.color = Color.parseColor("#FFF176")
        canvas.drawText("🌙 時間到囉", width / 2f, height / 2f - 100f, paintText)

        paintText.textSize = 80f
        paintText.color = Color.WHITE
        canvas.drawText("蛇寶寶要去睡覺了", width / 2f, height / 2f + 50f, paintText)
        canvas.drawText("請讓眼睛休息一下喔！", width / 2f, height / 2f + 180f, paintText)
    }

    // 🎨 遊戲畫面
    private fun drawGame(canvas: Canvas) {
        val cellW = width.toFloat() / gridSize
        val cellH = height.toFloat() / gridSize
        val borderRadius = cellW / 4f

        paintText.color = Color.WHITE
        paintText.textSize = 100f
        canvas.drawText("🍎 分數: $score", width / 2f, 220f, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        paintText.textSize = 120f
        if (isPaused) canvas.drawText("▶️", width - 50f, 220f, paintText)
        else canvas.drawText("⏸️", width - 50f, 220f, paintText)
        paintText.textAlign = Paint.Align.CENTER

        if (isPaused && currentState == GameState.PLAYING) {
            paintText.color = Color.parseColor("#FFF176")
            paintText.textSize = 150f
            canvas.drawText("遊戲暫停", width / 2f, height / 2f, paintText)
        }

        val ax = apple.first * cellW + 4
        val ay = apple.second * cellH + 4
        val aw = cellW - 8
        val ah = cellH - 8
        canvas.drawRoundRect(ax, ay, ax + aw, ay + ah, borderRadius, borderRadius, paintApple)
        paintDetails.color = Color.parseColor("#8D6E63")
        canvas.drawRect(ax + aw/2f - 2, ay - 10, ax + aw/2f + 2, ay + 5, paintDetails)
        paintDetails.color = Color.parseColor("#66BB6A")
        canvas.drawCircle(ax + aw/2f + 10, ay - 10, 10f, paintDetails)

        for (i in snake.indices) {
            val pos = snake[i]
            val sx = pos.first * cellW + 2
            val sy = pos.second * cellH + 2
            val sw = cellW - 4
            val sh = cellH - 4

            canvas.drawRoundRect(sx, sy, sx + sw, sy + sh, borderRadius, borderRadius, paintBody)

            if (i == 0) {
                canvas.drawRoundRect(sx, sy, sx + sw, sy + sh, borderRadius, borderRadius, paintHead)
                paintDetails.color = Color.WHITE
                val eyeRadius = sw / 6f
                canvas.drawCircle(sx + sw/3f, sy + sh/3f, eyeRadius, paintDetails)
                canvas.drawCircle(sx + 2*sw/3f, sy + sh/3f, eyeRadius, paintDetails)
                paintDetails.color = Color.BLACK
                canvas.drawCircle(sx + sw/3f, sy + sh/3f, eyeRadius/2f, paintDetails)
                canvas.drawCircle(sx + 2*sw/3f, sy + sh/3f, eyeRadius/2f, paintDetails)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (currentState) {
            // 1️⃣ 選單觸控判定 (更新為四個按鈕)
            GameState.MENU -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val cx = width / 2f
                    val y1 = height / 2f - 350f
                    val y2 = height / 2f - 150f
                    val y3 = height / 2f + 50f
                    val y4 = height / 2f + 250f
                    val bw = 350f
                    val bh = 80f

                    if (event.x in (cx - bw)..(cx + bw)) {
                        if (event.y in (y1 - bh)..(y1 + bh)) startGame(15)
                        else if (event.y in (y2 - bh)..(y2 + bh)) startGame(30)
                        else if (event.y in (y3 - bh)..(y3 + bh)) startGame(45)
                        else if (event.y in (y4 - bh)..(y4 + bh)) startGame(-1) // 啟動不限制模式！
                    }
                }
            }
            // 2️⃣ 遊玩觸控判定
            GameState.PLAYING -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (event.x in (width - 200f)..(width - 10f) && event.y in 100f..300f) {
                        isPaused = !isPaused
                        return true
                    }
                    startX = event.x
                    startY = event.y
                } else if (event.action == MotionEvent.ACTION_UP) {
                    if (isPaused) return true
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (abs(dx) > 50 || abs(dy) > 50) {
                        if (abs(dx) > abs(dy)) {
                            if (dx > 0 && direction != Pair(-1, 0)) direction = Pair(1, 0)
                            else if (dx < 0 && direction != Pair(1, 0)) direction = Pair(-1, 0)
                        } else {
                            if (dy > 0 && direction != Pair(0, -1)) direction = Pair(0, 1)
                            else if (dy < 0 && direction != Pair(0, 1)) direction = Pair(0, -1)
                        }
                    }
                }
            }
            // 3️⃣ 時間到鎖死
            GameState.TIME_UP -> { }
        }
        return true
    }
}
