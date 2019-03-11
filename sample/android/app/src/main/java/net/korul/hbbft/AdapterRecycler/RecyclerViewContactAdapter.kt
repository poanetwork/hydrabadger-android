package net.korul.hbbft.AdapterRecycler


import android.content.Context
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.daimajia.swipe.SimpleSwipeListener
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.core.Getters.setInvisUserByUid
import net.korul.hbbft.R
import java.util.*


interface ClickListener {
    fun onItemClick(view: View, position: Int)
    fun onItemButtonClick(view: View, position: Int)
    fun onItemLongClick(view: View, position: Int)
}

class RecyclerViewContactAdapter(
    private val mContext: Context,
    private val mDataset: ArrayList<User>,
    private val mClickListener: ClickListener
) :
    RecyclerSwipeAdapter<RecyclerViewContactAdapter.SimpleViewHolder>() {

    class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var swipeLayout: SwipeLayout = itemView.findViewById<View>(R.id.swipe) as SwipeLayout
        internal var contact_search_icon: ImageView = itemView.findViewById<View>(R.id.contact_search_icon) as ImageView
        internal var contact_search_name: TextView = itemView.findViewById<View>(R.id.contact_search_name) as TextView
        internal var contact_search_status: TextView =
            itemView.findViewById<View>(R.id.contact_search_status) as TextView
        internal var contact_search_uid: TextView = itemView.findViewById<View>(R.id.contact_search_uid) as TextView
        internal var contact_remove: LinearLayout = itemView.findViewById<View>(R.id.contact_remove) as LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.swipe_contact_options, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: SimpleViewHolder, position: Int) {
        val item = mDataset[position]
        viewHolder.swipeLayout.showMode = SwipeLayout.ShowMode.LayDown

        viewHolder.swipeLayout.addSwipeListener(object : SimpleSwipeListener() {
            override fun onOpen(layout: SwipeLayout?) {
            }
        })
        viewHolder.swipeLayout.surfaceView.setOnClickListener { v ->
            mClickListener.onItemClick(v, position)
        }
        viewHolder.swipeLayout.surfaceView.setOnLongClickListener {
            mClickListener.onItemLongClick(it, position)
            return@setOnLongClickListener true
        }
        viewHolder.contact_remove.setOnClickListener {
            mClickListener.onItemButtonClick(it, position)
            try {
                setInvisUserByUid(mDataset[position])
                mItemManger.removeShownLayouts(viewHolder.swipeLayout)
                mDataset.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, mDataset.size)
                mItemManger.closeAllItems()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        if (item.avatar != "") {
            val image = BitmapFactory.decodeFile(item.avatar)
            viewHolder.contact_search_icon.setImageBitmap(image)
        } else {
            viewHolder.contact_search_icon.setImageResource(R.drawable.ic_contact)
        }

        viewHolder.contact_search_name.text = item.nick
        viewHolder.contact_search_status.text = item.name
        viewHolder.contact_search_uid.text = item.uid

        mItemManger.bindView(viewHolder.itemView, position)
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    fun getItemInPos(position: Int): User {
        return mDataset[position]
    }

    override fun getSwipeLayoutResourceId(position: Int): Int {
        return R.id.swipe
    }
}