package com.myorderapp.ui.adddish

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddDishImageUploadPolicyTest {
    @Test
    fun `content uri should be queued for upload`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("content://media/picker/1"))
    }

    @Test
    fun `file uri should be queued for upload`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("file:///tmp/dish.jpg"))
    }

    @Test
    fun `remote url should not be queued for upload`() {
        assertFalse(AddDishImageUploadPolicy.shouldQueueUpload("https://example.com/dish.jpg"))
    }

    @Test
    fun `blank url should not be queued for upload`() {
        assertFalse(AddDishImageUploadPolicy.shouldQueueUpload("   "))
    }

    @Test
    fun `uppercase local uri scheme should be queued for upload`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("CONTENT://media/picker/1"))
    }

    @Test
    fun `remote url with surrounding whitespace should not be queued`() {
        assertFalse(AddDishImageUploadPolicy.shouldQueueUpload("  https://example.com/dish.jpg  "))
    }

    @Test
    fun `local uri with surrounding whitespace should be queued`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("  file:///tmp/dish.jpg  "))
    }
}
