package net.korul.hbbft.CommonFragments.tabContacts

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
import net.korul.hbbft.AdapterRecycler.ClickListener
import net.korul.hbbft.AdapterRecycler.RecyclerViewContactAdapter
import net.korul.hbbft.AdapterRecycler.util.DividerItemDecoration
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllLocalUsersDistinct
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CommonFragments.WarningFragment
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.R
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

    override fun onResume() {
        super.onResume()

        action_add_contact.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                AddToContactsFragment.newInstance(), getString(R.string.tag_contacts)
            )
            transaction.addToBackStack(getString(R.string.tag_contacts))
            transaction.commit()
        }

        action_search_contact.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                WarningFragment.newInstance(), getString(R.string.tag_contacts)
            )
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

        mDataSet = ArrayList(getAllLocalUsersDistinct().filter { it.uid != CoreHBBFT.uniqueID1 }.toList())
        mAdapter = RecyclerViewContactAdapter(context!!, mDataSet!!, object : ClickListener {
            override fun onItemClick(view: View, position: Int) {
                val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                transaction.add(
                    R.id.view,
                    ContactInfoFragment.newInstance(
                        (mAdapter as RecyclerViewContactAdapter).getItemInPos(
                            position
                        )
                    ),
                    getString(R.string.tag_contacts)
                )
                transaction.addToBackStack(getString(R.string.tag_contacts))
                transaction.commit()
            }

            override fun onItemButtonClick(view: View, position: Int) {
                AppUtils.showToast(
                    activity!!,
                    "onItemButtonClick $position", true
                )
            }

            override fun onItemLongClick(view: View, position: Int) {
                AppUtils.showToast(
                    activity!!,
                    "LongClick $position", true
                )
            }
        })


        (mAdapter as RecyclerViewContactAdapter).mode = Attributes.Mode.Single
        contacts_list.adapter = mAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance() = ContactsFragment()
    }
}
