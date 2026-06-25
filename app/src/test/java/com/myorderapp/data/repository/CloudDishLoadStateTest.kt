package com.myorderapp.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudDishLoadStateTest {
    @Test
    fun `logged out sessions do not request cloud load and clear loaded pair`() {
        val state = CloudDishLoadState()
        state.markLoaded("user-a", "pair-a")

        assertFalse(state.shouldLoad(isLoggedIn = false, userId = "user-a", pairId = "pair-a"))
        assertTrue(state.shouldLoad(isLoggedIn = true, userId = "user-a", pairId = "pair-a"))
    }

    @Test
    fun `first logged in session requests cloud load`() {
        val state = CloudDishLoadState()

        assertTrue(state.shouldLoad(isLoggedIn = true, userId = "user-a", pairId = "pair-a"))
    }

    @Test
    fun `same loaded pair does not request duplicate load`() {
        val state = CloudDishLoadState()
        state.markLoaded("user-a", "pair-a")

        assertFalse(state.shouldLoad(isLoggedIn = true, userId = "user-a", pairId = "pair-a"))
    }

    @Test
    fun `new pair requests reload`() {
        val state = CloudDishLoadState()
        state.markLoaded("user-a", "pair-a")

        assertTrue(state.shouldLoad(isLoggedIn = true, userId = "user-a", pairId = "pair-b"))
    }

    @Test
    fun `new user on same pair requests reload`() {
        val state = CloudDishLoadState()
        state.markLoaded("user-a", "pair-a")

        assertTrue(state.shouldLoad(isLoggedIn = true, userId = "user-b", pairId = "pair-a"))
    }
}
