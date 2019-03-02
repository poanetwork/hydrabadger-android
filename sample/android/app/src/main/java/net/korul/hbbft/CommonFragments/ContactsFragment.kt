package net.korul.hbbft.CommonFragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daimajia.swipe.util.Attributes
import kotlinx.android.synthetic.main.fragment_contacts.*
import net.korul.hbbft.R
import net.korul.hbbft.adapter.RecyclerViewContactAdapter
import net.korul.hbbft.adapter.util.DividerItemDecoration
import net.korul.hbbft.adapter.util.RecyclerItemClickListener
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.core.Getters.getAllUsers
import java.util.*


class ContactsFragment : Fragment() {

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mDataSet: ArrayList<User>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_add_contact.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(R.id.view, AddToContactsFragment.newInstance(), getString(R.string.tag_contacts))
            transaction.addToBackStack(getString(R.string.tag_contacts))
            transaction.commit()
        }

        initRecicleView()
    }

    fun initRecicleView() {
        // Layout Managers:
        contacts_list.layoutManager = LinearLayoutManager(activity!!)

        // Item Decorator:
        contacts_list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider)))

        mDataSet = ArrayList(getAllUsers().toList())
        mAdapter = RecyclerViewContactAdapter(context!!, mDataSet!!)
        (mAdapter as RecyclerViewContactAdapter).mode = Attributes.Mode.Single
        contacts_list.adapter = mAdapter

        /* Listeners */
        val touch = RecyclerItemClickListener(context!!,
            RecyclerItemClickListener.OnItemClickListener { view, position ->
                val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                transaction.add(
                    R.id.view,
                    ContactInfoFragment.newInstance((mAdapter as RecyclerViewContactAdapter).getItemInPos(position)),
                    getString(R.string.tag_contacts)
                )
                transaction.addToBackStack(getString(R.string.tag_contacts))
                transaction.commit()
            })
        contacts_list.addOnItemTouchListener(touch)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ContactsFragment()
    }
}
