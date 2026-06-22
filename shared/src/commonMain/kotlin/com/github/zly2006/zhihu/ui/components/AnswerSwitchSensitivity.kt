/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui.components

const val ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY = "answerSwitchSensitivity"
const val DEFAULT_ANSWER_SWITCH_SENSITIVITY = 1f
const val MIN_ANSWER_SWITCH_SENSITIVITY = 0.5f
const val MAX_ANSWER_SWITCH_SENSITIVITY = 10f

internal fun normalizedAnswerSwitchSensitivity(value: Float): Float = when {
    value.isNaN() -> DEFAULT_ANSWER_SWITCH_SENSITIVITY
    else -> value.coerceIn(MIN_ANSWER_SWITCH_SENSITIVITY, MAX_ANSWER_SWITCH_SENSITIVITY)
}
