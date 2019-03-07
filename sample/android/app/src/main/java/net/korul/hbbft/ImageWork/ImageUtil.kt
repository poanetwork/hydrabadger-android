package net.korul.hbbft.ImageWork

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader.TileMode


object ImageUtil {

    fun circleShape(preview_bitmap: Bitmap): Bitmap {

        val circleBitmap = Bitmap.createBitmap(
            preview_bitmap.width,
            preview_bitmap.height, Bitmap.Config.ARGB_8888
        )
        val shader = BitmapShader(
            preview_bitmap, TileMode.CLAMP,
            TileMode.CLAMP
        )
        val paint = Paint()
        paint.shader = shader
        val c = Canvas(circleBitmap)
        c.drawCircle(
            preview_bitmap.width / 2f,
            preview_bitmap.height / 2f,
            Math.min(
                preview_bitmap.width / 2f,
                preview_bitmap.height / 2f
            ), paint
        )
        return circleBitmap

    }

}