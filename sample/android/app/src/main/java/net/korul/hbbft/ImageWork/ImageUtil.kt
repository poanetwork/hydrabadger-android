package net.korul.hbbft.ImageWork

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader.TileMode
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

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


    @SuppressLint("NewApi")
    fun blurRenderScript(context: Context, smallBitmap: Bitmap, radius: Int): Bitmap {
        var smallBitmap = smallBitmap
        try {
            smallBitmap = RGB565toARGB888(smallBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val bitmap = Bitmap.createBitmap(
            smallBitmap.width, smallBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val renderScript = RenderScript.create(context)

        val blurInput = Allocation.createFromBitmap(renderScript, smallBitmap)
        val blurOutput = Allocation.createFromBitmap(renderScript, bitmap)

        val blur = ScriptIntrinsicBlur.create(
            renderScript,
            Element.U8_4(renderScript)
        )
        blur.setInput(blurInput)
        blur.setRadius(radius.toFloat()) // radius must be 0 < r <= 25
        blur.forEach(blurOutput)

        blurOutput.copyTo(bitmap)
        renderScript.destroy()

        return bitmap

    }

    @Throws(Exception::class)
    private fun RGB565toARGB888(img: Bitmap): Bitmap {
        val numPixels = img.width * img.height
        val pixels = IntArray(numPixels)

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.width, 0, 0, img.width, img.height)

        //Create a Bitmap of the appropriate format.
        val result = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

}