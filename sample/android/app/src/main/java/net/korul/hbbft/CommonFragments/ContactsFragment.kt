package net.korul.hbbft.CommonFragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.daimajia.swipe.util.Attributes
import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.swipe_contact_options.*
import net.korul.hbbft.R
import net.korul.hbbft.adapter.RecyclerViewAdapter
import net.korul.hbbft.adapter.util.DividerItemDecoration
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.core.Getters.getAllUsers
import java.util.*
import net.korul.hbbft.adapter.util.RecyclerItemClickListener


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

        // Layout Managers:
        contacts_list.layoutManager = LinearLayoutManager(activity!!)

        // Item Decorator:
        contacts_list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider)))

        mDataSet = ArrayList(getAllUsers().toList())
        mAdapter = RecyclerViewAdapter(context!!, mDataSet!!)
        (mAdapter as RecyclerViewAdapter).mode = Attributes.Mode.Single
        contacts_list.adapter = mAdapter

        /* Listeners */
        val touch = RecyclerItemClickListener(context!!,
            RecyclerItemClickListener.OnItemClickListener { view, position ->
                Toast.makeText(
                    view.context,
                    "onItemSelected: $position",
                    Toast.LENGTH_SHORT
                ).show()
            })
        contacts_list.addOnItemTouchListener(touch)
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ContactsFragment()
    }
}
