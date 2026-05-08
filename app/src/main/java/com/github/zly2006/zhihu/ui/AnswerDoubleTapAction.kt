/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import androidx.annotation.StringRes
import com.github.zly2006.zhihu.R

const val ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY = "answerDoubleTapAction"

enum class AnswerDoubleTapAction(
    val preferenceValue: String,
    @StringRes val labelResId: Int,
) {
    None(
        preferenceValue = "none",
        labelResId = R.string.double_tap_none,
    ),
    Ask(
        preferenceValue = "ask",
        labelResId = R.string.double_tap_ask,
    ),
    VoteUp(
        preferenceValue = "voteUp",
        labelResId = R.string.double_tap_vote_up,
    ),
    OpenComments(
        preferenceValue = "openComments",
        labelResId = R.string.double_tap_open_comments,
    ),
    ;

    companion object {
        fun fromPreference(value: String?): AnswerDoubleTapAction = entries.firstOrNull {
            it.preferenceValue == value
        } ?: Ask
    }
}
