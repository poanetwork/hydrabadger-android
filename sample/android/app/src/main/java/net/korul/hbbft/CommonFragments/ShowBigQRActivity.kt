package net.korul.hbbft.CommonFragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_contact_info_qr.*
import net.korul.hbbft.R

class ShowBigQRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_contact_info_qr)

        val qr = intent.getStringExtra("qr")
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qr, BarcodeFormat.QR_CODE, 400, 400)
            qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        fun open(context: Context, qr: String) {
            val intent = Intent(context, ShowBigQRActivity::class.java)
            intent.putExtra("qr", qr)
            context.startActivity(intent)
        }
    }

}