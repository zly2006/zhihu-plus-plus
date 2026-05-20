package com.github.zly2006.zhihu.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerDoubleTapActionTest {
    @Test
    fun parsesStoredPreferenceValue() {
        assertEquals(AnswerDoubleTapAction.VoteUp, AnswerDoubleTapAction.fromPreference("voteUp"))
        assertEquals(AnswerDoubleTapAction.OpenComments, AnswerDoubleTapAction.fromPreference("openComments"))
    }

    @Test
    fun defaultsToAskForMissingOrUnknownPreference() {
        assertEquals(AnswerDoubleTapAction.Ask, AnswerDoubleTapAction.fromPreference(null))
        assertEquals(AnswerDoubleTapAction.Ask, AnswerDoubleTapAction.fromPreference("unknown"))
    }
}
