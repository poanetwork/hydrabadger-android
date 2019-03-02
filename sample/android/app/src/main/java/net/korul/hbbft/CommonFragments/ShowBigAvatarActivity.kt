package net.korul.hbbft.CommonFragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import net.korul.hbbft.R

class ShowBigAvatarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_contact_info_avatar)

        val pathAvatar = intent.getStringExtra("avatar")
        //TODO image set
//                Bitmap image = BitmapFactory.decodeFile(pathAvatar);
//                contact_icon.setImageBitmap(image);

    }

    companion object {
        fun open(context: Context, avatar: String) {
            val intent = Intent(context, ShowBigAvatarActivity::class.java)
            intent.putExtra("avatar", avatar)
            context.startActivity(intent)
        }
    }

}