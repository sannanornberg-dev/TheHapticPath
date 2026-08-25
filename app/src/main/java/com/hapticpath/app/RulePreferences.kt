package com.hapticpath.app

import android.content.Context
import android.content.SharedPreferences

class RulePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var isBiffRuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIFF_RULE, true)
        set(value) = prefs.edit().putBoolean(KEY_BIFF_RULE, value).apply()

    companion object {
        private const val PREFS_NAME = "haptic_path_rule_prefs"
        private const val KEY_BIFF_RULE = "key_biff_rule_enabled"
    }
}