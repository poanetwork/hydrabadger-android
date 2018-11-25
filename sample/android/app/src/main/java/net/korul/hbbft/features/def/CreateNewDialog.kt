package net.korul.hbbft.features.def

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_dialog.*
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.DialogsFixtures.Companion.setNewDialog
import net.korul.hbbft.common.data.model.User

class CreateNewDialog: AppCompatActivity()  {

    //TODO I Am
    var mCurUser: User = User(0, CoreHBBFT.uniqueID1, 0.toString(), "", "name", "http://i.imgur.com/pv1tBmT.png", true)
//    var mUser: User = User(0, "", 1.toString(), "", "name", "http://i.imgur.com/Qn9UesZ.png", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_dialog)

        buttonAdd.setOnClickListener {
            setNewDialog(nameRoom.text.toString(), mCurUser)
            super.onBackPressed()
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, CreateNewDialog::class.java))
        }
    }

}