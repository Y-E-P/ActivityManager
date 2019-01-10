package com.sdex.activityrunner.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sdex.activityrunner.R
import com.sdex.activityrunner.db.cache.ApplicationModel
import com.sdex.activityrunner.extensions.addDivider
import com.sdex.activityrunner.extensions.enableBackButton
import com.sdex.activityrunner.preferences.AppPreferences
import com.sdex.activityrunner.premium.GetPremiumDialog
import com.sdex.activityrunner.ui.SnackbarContainerActivity
import com.sdex.commons.BaseActivity
import com.sdex.commons.analytics.AnalyticsManager
import com.sdex.commons.util.UIUtils
import kotlinx.android.synthetic.main.activity_activities_list.*

class ActivitiesListActivity : BaseActivity(), SnackbarContainerActivity {

  private val appPreferences: AppPreferences by lazy { AppPreferences(this) }
  private val viewModel: ActivitiesListViewModel by lazy {
    ViewModelProviders.of(this).get(ActivitiesListViewModel::class.java)
  }

  private var isShowNotExported: Boolean = false
  private var appPackageName: String? = null
  private var searchText: String? = null

  override fun getLayout(): Int {
    return R.layout.activity_activities_list
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val item = intent.getSerializableExtra(ARG_APPLICATION) as ApplicationModel?
    if (item == null) {
      finish()
      return
    }
    appPreferences.incrementAppOpenCount()
    appPackageName = item.packageName
    title = item.name
    enableBackButton()
    list.addDivider(this)
    val adapter = ActivitiesListAdapter(this)
    list.adapter = adapter

    searchText = savedInstanceState?.getString(STATE_SEARCH_TEXT)

    viewModel.getItems(appPackageName!!).observe(this, Observer {
      adapter.submitList(it)
      val size = it!!.size
      setSubtitle(resources.getQuantityString(R.plurals.activities_count, size, size))
      if (size == 0 && searchText == null) {
        empty.visibility = VISIBLE
      } else {
        empty.visibility = GONE
      }
    })

    isShowNotExported = appPreferences.showNotExported

    turnOnAdvanced.setOnClickListener {
      appPreferences.showNotExported = true
      viewModel.reloadItems(appPackageName!!)
    }

    if (!appPreferences.showNotExported
      && !appPreferences.isNotExportedDialogShown
      && appPreferences.appOpenCount > SHOW_NOT_EXPORTED_DIALOG_THRESHOLD) {
      appPreferences.isNotExportedDialogShown = true
      val dialog = EnableNotExportedActivitiesDialog()
      dialog.show(supportFragmentManager, EnableNotExportedActivitiesDialog.TAG)
    }

    if (!appPreferences.isPremiumDialogShown
      && !appPreferences.isProVersion
      && appPreferences.appOpenCount > SHOW_PREMIUM_DIALOG_THRESHOLD) {
      appPreferences.isPremiumDialogShown = true
      val dialog = GetPremiumDialog.newInstance(R.string.pro_version_unlock)
      dialog.show(supportFragmentManager, GetPremiumDialog.TAG)
    }
  }

  override fun onStart() {
    super.onStart()
    if (appPreferences.showNotExported != isShowNotExported) {
      viewModel.reloadItems(appPackageName!!)
    }
  }

  public override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_SEARCH_TEXT, searchText)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activities_list, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val searchView = searchItem.actionView as SearchView
    val hint = getString(R.string.action_search_activity_hint)
    searchView.queryHint = hint

    if (!TextUtils.isEmpty(searchText)) {
      searchView.post { searchView.setQuery(searchText, false) }
      searchItem.expandActionView()
      UIUtils.setMenuItemsVisibility(menu, searchItem, false)
    }

    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        return false
      }

      override fun onQueryTextChange(newText: String): Boolean {
        filter(newText)
        return false
      }
    })
    searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        UIUtils.setMenuItemsVisibility(menu, item, false)
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        searchText = null
        UIUtils.setMenuItemsVisibility(menu, true)
        invalidateOptionsMenu()
        return true
      }
    })
    return super.onCreateOptionsMenu(menu)
  }

  override fun getView(): View {
    return container
  }

  override fun getActivity(): Activity {
    return this
  }

  private fun filter(text: String) {
    this.searchText = text
    viewModel.filterItems(appPackageName!!, searchText)
  }

  companion object {

    const val ARG_APPLICATION = "arg_application"
    private const val STATE_SEARCH_TEXT = "state_search_text"
    const val SHOW_PREMIUM_DIALOG_THRESHOLD = 30
    const val SHOW_NOT_EXPORTED_DIALOG_THRESHOLD = 5

    fun start(context: Context, item: ApplicationModel) {
      AnalyticsManager.logApplicationOpen(item)
      val starter = Intent(context, ActivitiesListActivity::class.java)
      starter.putExtra(ARG_APPLICATION, item)
      context.startActivity(starter)
    }
  }
}
