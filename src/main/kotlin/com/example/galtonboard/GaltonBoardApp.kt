package com.example.galtonboard

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.scene.paint.Color
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val PEG_RADIUS  = 3.0
const val BALL_RADIUS = 5.0
const val PEGS_IN_FIRST_ROW = 5
const val SCREEN_X = 600.0
const val SCREEN_Y = 600.0
const val NUM_ROWS = 10
const val NUM_BUCKETS = NUM_ROWS + PEGS_IN_FIRST_ROW + 1 // Probably... don't change this
const val NUM_BALLS = 100
const val GRAVITY = 0.005
const val WIGGLE_DEG = 5.0
const val ELASTICITY = 0.5

class GaltonBoardApp : Application() {
    val pegs: MutableList<Peg> = mutableListOf<Peg>()
    val balls: MutableList<Ball> = mutableListOf<Ball>()
    val buckets: MutableList<Pair<Double, Double>> = mutableListOf()
    // These two are initialized in drawGaltonBoard
    var firstBucketRightX = 0.0
    var lastBucketLeftX = 0.0
    var lastRowY = 0.0

    override fun start(primaryStage: Stage) {
        val boardCanvas = Canvas(SCREEN_X, SCREEN_Y)
        val boardGC: GraphicsContext = boardCanvas.graphicsContext2D
        drawGaltonBoard(boardGC)
        val ballCanvas = Canvas(SCREEN_X, SCREEN_Y)
        val ballGC: GraphicsContext = ballCanvas.graphicsContext2D
        addBalls(balls)
        boardGC.stroke = Color.RED
        boardGC.strokeLine(firstBucketRightX, 0.0, firstBucketRightX, SCREEN_Y)
        boardGC.strokeLine(lastBucketLeftX, 0.0, lastBucketLeftX, SCREEN_Y)


        val root = StackPane()
        root.children.add(makeGrid(50.0))
        root.children.add(boardCanvas)
        root.children.add(ballCanvas)

        val timer = object : AnimationTimer() {
            var prevTime = System.nanoTime() / 1_000_000_000.0

            override fun handle(now: Long) {
                val curTime = now / 1_000_000_000.0
                val deltaTime = curTime - prevTime
                updateBalls(balls, deltaTime)
                clearBalls(ballGC)
                drawBalls(balls, ballGC)
            }
        }

        val scene = Scene(root, SCREEN_X, SCREEN_Y)
        primaryStage.title = "Galton Board"
        primaryStage.scene = scene
        primaryStage.show()

        timer.start()
    }

    fun drawBalls(balls: MutableList<Ball>, gc: GraphicsContext) {
        for (ball in balls) {
            ball.draw(gc)
        }
    }

    fun updateBalls(balls: MutableList<Ball>, dt: Double) {
        var i = 0
        while (i < balls.size) {
            val ball = balls[i]
            applyForces(ball, dt)
            ball.move(dt)
            if (ball.outOfBounds()) {
                balls[i] = balls[balls.lastIndex]
                balls.removeLast()
            } else {
                ++i
            }
        }
    }

    fun applyForces(ball: Ball, dt: Double) {
        // Gravity
        ball.vy += GRAVITY * dt

        // Far walls
        if (ball.x <= firstBucketRightX) {
            ball.x = firstBucketRightX + 0.015
            ball.vx = -ball.vx * ELASTICITY
        } else if (ball.x >= lastBucketLeftX) {
            ball.x = lastBucketLeftX - 0.015
            ball.vx = -ball.vx * ELASTICITY
        }

        // Peg Collisions and bucket walls
        if (ball.y <= lastRowY) {
            for (peg in pegs) {
                if (ball.isCollidingWith(peg)) {
                    ball.pegBounce(peg)
                    break
                }
            }
        } else {
            for (bucket in buckets) {
                if (ball.isCollidingWith(bucket)) {
                    ball.bucketBounce(bucket)
                    break
                }
            }
        }
    }

    fun clearBalls(gc: GraphicsContext) {
        gc.clearRect(0.0, 0.0, SCREEN_X, SCREEN_Y)
    }

    fun addBalls(balls: MutableList<Ball>) {
        for (ball in 0..<NUM_BALLS) {
            balls.add(Ball((SCREEN_X / 2) + Random.nextDouble(-1.0, 1.0), 0.0))
        }
    }

    fun makeGrid(spacing: Double): Canvas {
        val canvas = Canvas(SCREEN_X, SCREEN_Y)
        val gc = canvas.graphicsContext2D
        gc.stroke = Color.LIGHTGREY

        for (x in 0..(SCREEN_X/spacing).toInt()) {
            val lineX = x * spacing
            gc.strokeLine(lineX, 0.0, lineX, SCREEN_Y)
        }

        for (y in 0..(SCREEN_X/spacing).toInt()) {
            val lineY = y * spacing
            gc.strokeLine(0.0, lineY, SCREEN_X, lineY)
        }

        return canvas
    }

    fun drawGaltonBoard(gc: GraphicsContext) {
        val dx = 3 * (2 * BALL_RADIUS)
        val dy = 3 * (2 * BALL_RADIUS)

        // Draw pegs
        for (row in 0..NUM_ROWS) {
            val pegX = (SCREEN_X / 2) - (2 + row / 2.0) * dx
            val pegY = 100 + row * dy
            for (peg in 0..< PEGS_IN_FIRST_ROW + row) {
                pegs.add(Peg(pegX + peg * dx, pegY))
            }
            lastRowY = pegY // Overwrites, only the last one gets out.
        }

        // Draw buckets
        var bucketX = (SCREEN_X / 2) - (2 + NUM_ROWS / 2.0) * dx // This is centered, not like the bounding box.
        val bucketY: Double = 100 + NUM_ROWS * dy
        firstBucketRightX = bucketX - dx + PEG_RADIUS
        gc.fillRect(bucketX - dx - PEG_RADIUS, 0.0, 2 * PEG_RADIUS, SCREEN_Y)
        for (bucket in 1..<NUM_BUCKETS) {
            gc.fillRect(bucketX - PEG_RADIUS, bucketY, 2 * PEG_RADIUS, SCREEN_Y - bucketY)
            buckets.add(Pair<Double, Double>(bucketX - PEG_RADIUS, bucketX + PEG_RADIUS))
            bucketX += dx
        }
        lastBucketLeftX = bucketX - PEG_RADIUS
        gc.fillRect(bucketX - PEG_RADIUS, 0.0, 2 * PEG_RADIUS, SCREEN_Y)


        for (peg in pegs) {
            peg.draw(gc)
        }
    }
}

abstract class Collider {
    abstract var x: Double
    abstract var y: Double
    abstract val r: Double

    abstract fun draw(gc: GraphicsContext)

    fun isCollidingWith(other: Collider): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        val dist = sqrt(dx * dx + dy * dy)
        return dist <= r + other.r
    }

    fun isCollidingWith(other: Pair<Double, Double>): Boolean {
        return x + r >= other.first && x - r <= other.second
    }
}

class Ball(override var x: Double, override var y: Double) : Collider() {
    override val r = BALL_RADIUS
    var vx = 0.0
    var vy = 0.0

    override fun draw(gc: GraphicsContext) {
        gc.strokeOval(x - BALL_RADIUS, y - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2)
    }

    fun pegBounce(peg: Peg) {
        // This method is only called if this ball and the peg are colliding.
        val dx = peg.x - x
        val dy = peg.y - y
        var angle = atan2(dy, dx)
        val wiggle = Math.toRadians(WIGGLE_DEG)
        angle += Random.nextDouble(-wiggle, +wiggle)
        val normal = Vector(dx, dy).normalize()
        val velocity = Vector(vx, vy)

        val projectionMag = normal.dot(velocity)
        val newVelocity = velocity - normal * projectionMag * 2 // subtract one to get rid of the component, subtract another to flip

        vx = newVelocity.x * ELASTICITY
        vy = newVelocity.y * ELASTICITY

        // Now, move the ball outside the peg. Safety. THIS IS NEEDED.
        x = peg.x - (BALL_RADIUS + PEG_RADIUS + 0.015) * cos(angle)
        y = peg.y - (BALL_RADIUS + PEG_RADIUS + 0.015) * sin(angle)
    }

    fun bucketBounce(bucket: Pair<Double, Double>) {
        // Method only called if ball is inside bucket.
        vx = -vx * ELASTICITY
        x = if (x - bucket.first <= bucket.second - x) { // Collision on left side
            bucket.first - r - 0.015
        } else {
            bucket.second + r + 0.015
        }
    }

    fun outOfBounds(): Boolean {
        return y >= SCREEN_Y + r
    }

    fun move(dt: Double) {
        x += vx * dt
        y += vy * dt
    }
}

class Peg(private var _x: Double, private var _y: Double) : Collider() {
    override var x: Double
        get() = _x
        set(value) = throw UnsupportedOperationException("Cannot change position of Peg.")
    override var y: Double
        get() = _y
        set(value) = throw UnsupportedOperationException("Cannot change position of Peg.")
    override val r = PEG_RADIUS
    override fun draw(gc: GraphicsContext) {
        gc.fillOval(x - PEG_RADIUS, y - PEG_RADIUS, PEG_RADIUS * 2, PEG_RADIUS * 2)
    }
}

class Vector(val x: Double, val y: Double) {
    fun dot(other: Vector): Double {
        return x * other.x + y * other.y
    }

    fun normalize(): Vector {
        val magnitude = sqrt(x * x + y * y)
        return Vector(x / magnitude, y / magnitude)
    }

    operator fun plus(other: Vector): Vector {
        return Vector(x + other.x, y + other.y)
    }
    operator fun minus(other: Vector): Vector {
        return Vector(x - other.x, y - other.y)
    }
    operator fun times(other: Double): Vector {
        return Vector(x * other, y * other)
    }
    operator fun times(other: Int): Vector {
        return Vector(x * other, y * other)
    }
}


fun main() {
    Application.launch(GaltonBoardApp::class.java)
}