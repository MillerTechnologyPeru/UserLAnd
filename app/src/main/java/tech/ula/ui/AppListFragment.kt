package tech.ula.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.AdapterView
import android.widget.TextView
import kotlinx.android.synthetic.main.frag_app_list.*
import tech.ula.R
import tech.ula.model.entities.App
import tech.ula.utils.launchAsync
import tech.ula.viewmodel.AppListViewModel
class AppListFragment : Fragment() {

    private lateinit var activityContext: Activity

    private lateinit var appList: List<App>
    private lateinit var appAdapter: AppListAdapter

    private val appListViewModel: AppListViewModel by lazy {
        ViewModelProviders.of(this).get(AppListViewModel::class.java)
    }

    private val appChangeObserver = Observer<List<App>> {
        it?.let {
            appList = it
            appAdapter = AppListAdapter(activityContext, appList)
            list_apps.adapter = appAdapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_create, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        insertApp()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_app_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        appListViewModel.getAllApps().observe(viewLifecycleOwner, appChangeObserver)

        activityContext = activity!!

        registerForContextMenu(list_apps)
        list_apps.onItemClickListener = AdapterView.OnItemClickListener {
            _, _, position, _ ->
            val selectedApp = appList[position]
            println("Clicked on APP: ${selectedApp.name}")
        }

        val swipeLayout = activityContext.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeLayout.setOnRefreshListener(
                SwipeRefreshLayout.OnRefreshListener {
                    println("Refreshing")
                    Handler().postDelayed(Runnable {
                        kotlin.run {
                            swipeLayout.isRefreshing = false
                        }
                    }, 4000)
                }
        )
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        super.onCreateContextMenu(menu, v, menuInfo)
        val app = appList[info.position]
        when {
            app.isPaidApplication ->
                activityContext.menuInflater.inflate(R.menu.context_menu_active_sessions, menu)
            else ->
                activityContext.menuInflater.inflate(R.menu.context_menu_inactive_sessions, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val position = menuInfo.position
        val app = appList[position]

        super.onContextItemSelected(item)
        return true
    }

    private fun insertApp(): Boolean {

        val randomId = (1..20).shuffled().last().toLong()
        val newApp = App(name = "NAME$randomId", category = "CATEGORY$randomId", isPaidApplication = true)

        launchAsync {
            appListViewModel.insertApplication(newApp)
        }

        return true
    }
}
