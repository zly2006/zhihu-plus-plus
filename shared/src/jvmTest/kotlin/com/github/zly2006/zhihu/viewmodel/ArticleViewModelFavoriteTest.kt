/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.viewmodel

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.platform.UserMessageSink
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleViewModelFavoriteTest {
    @Test
    fun favoriteRequestCompletesAfterArticleScopeIsCancelled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val requestStarted = CompletableDeferred<Unit>()
        val allowResponse = CompletableDeferred<Unit>()
        val successMessage = CompletableDeferred<String>()
        val messages = mutableListOf<String>()
        val client = HttpClient(
            MockEngine {
                requestStarted.complete(Unit)
                allowResponse.await()
                respond(content = "", status = HttpStatusCode.OK)
            },
        )
        try {
            val viewModel = ArticleViewModel(
                article = Article(type = ArticleType.Answer, id = 1L),
                httpClient = client,
                userMessages = UserMessageSink(
                    showShortMessage = { message ->
                        messages.add(message)
                        successMessage.complete(message)
                    },
                ),
            )

            viewModel.toggleFavorite(
                collectionId = "2",
                remove = false,
                environment = favoriteTestEnvironment(client),
            )
            advanceUntilIdle()
            requestStarted.await()
            viewModel.viewModelScope.cancel()
            allowResponse.complete(Unit)
            successMessage.await()

            assertEquals(listOf("收藏成功"), messages)
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledFavoriteRequestDoesNotReportBusinessFailure() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val messages = mutableListOf<String>()
        val client = HttpClient(
            MockEngine {
                throw CancellationException("Parent job is Cancelling")
            },
        )
        try {
            val viewModel = ArticleViewModel(
                article = Article(type = ArticleType.Answer, id = 1L),
                httpClient = client,
                userMessages = UserMessageSink(messages::add),
            )

            viewModel.toggleFavorite(
                collectionId = "2",
                remove = false,
                environment = favoriteTestEnvironment(client),
            )
            advanceUntilIdle()

            assertTrue(messages.isEmpty())
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    private fun favoriteTestEnvironment(client: HttpClient) = object : ZhihuApiEnvironment {
        override fun httpClient() = client

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }
}
