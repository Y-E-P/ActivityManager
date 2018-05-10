package com.sdex.activityrunner.app

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

import com.sdex.activityrunner.R
import com.sdex.activityrunner.preferences.SettingsActivity

class EnableNotExportedActivitiesDialog : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity!!)
      .setTitle("Non-exported activities")
      .setMessage("You can enable displaying non-exported activities in settings")
      .setPositiveButton(R.string.action_settings) { _, _ ->
        SettingsActivity.start(activity!!, SettingsActivity.ADVANCED)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }
}
