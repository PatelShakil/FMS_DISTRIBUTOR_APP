package com.shakilpatel.boloindia.distributor.ui.qr.shapes

import android.graphics.Path
import androidx.annotation.FloatRange
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape

// Heart Shape
data class Heart(
    @FloatRange(from = 0.0) val width: Float = 1f,
    override val size: Int = 7
) : QrVectorFrameShape {
    override fun Path.shape(size: Float, neighbors: Neighbors) {
        val width = (size / this@Heart.size.coerceAtLeast(1)) * width.coerceAtLeast(0f)

        moveTo(size / 2f, size / 5f)
        cubicTo(
            5 * size / 14, 0f,
            0f, size / 15,
            size / 28, 2 * size / 5
        )
        cubicTo(
            size / 14, 2 * size / 3,
            3 * size / 7, 5 * size / 6,
            size / 2, size
        )
        cubicTo(
            4 * size / 7, 5 * size / 6,
            13 * size / 14, 2 * size / 3,
            27 * size / 28, 2 * size / 5
        )
        cubicTo(
            size, size / 15,
            9 * size / 14, 0f,
            size / 2, size / 5
        )
    }
}

// Hexagon Shape
data class Hexagon(
    @FloatRange(from = 0.0) val width: Float = 1f,
    override val size: Int = 7
) : QrVectorFrameShape {
    override fun Path.shape(size: Float, neighbors: Neighbors) {
        val width = (size / this@Hexagon.size.coerceAtLeast(1)) * width.coerceAtLeast(0f)
        val halfSize = size / 2f

        moveTo(width, halfSize)
        lineTo(halfSize, width)
        lineTo(size - width, width)
        lineTo(size - width, halfSize)
        lineTo(size - width, size - width)
        lineTo(halfSize, size - width)
        lineTo(width, size - width)
        lineTo(width, halfSize)
    }
}

// Octagon Shape
data class Octagon(
    @FloatRange(from = 0.0) val width: Float = 1f,
    override val size: Int = 7
) : QrVectorFrameShape {
    override fun Path.shape(size: Float, neighbors: Neighbors) {
        val width = (size / this@Octagon.size.coerceAtLeast(1)) * width.coerceAtLeast(0f)
        val innerWidth = size - width * 2f

        moveTo(width, 0f)
        lineTo(innerWidth, 0f)
        lineTo(size, width)
        lineTo(size, innerWidth)
        lineTo(size - width, size)
        lineTo(width, size)
        lineTo(0f, innerWidth)
        lineTo(0f, width)
        lineTo(width, 0f)
    }
}

// Triangle Shape
data class Triangle(
    @FloatRange(from = 0.0) val width: Float = 1f,
    override val size: Int = 7
) : QrVectorFrameShape {
    override fun Path.shape(size: Float, neighbors: Neighbors) {
        val width = (size / this@Triangle.size.coerceAtLeast(1)) * width.coerceAtLeast(0f)
        moveTo(size / 2f, 0f)
        lineTo(size - width, size)
        lineTo(0f, size)
        lineTo(size / 2f, 0f)
    }
}
