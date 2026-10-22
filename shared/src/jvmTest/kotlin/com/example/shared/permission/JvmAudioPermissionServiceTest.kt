package com.example.shared.permission

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmAudioPermissionServiceTest {

    @Test
    fun testJvmAudioPermissionServiceDetection() {
        val service = JvmAudioPermissionService()
        assertNotNull(service.status.value)

        var callbackCalled = false
        service.requestPermission {
            callbackCalled = true
        }
        assertTrue(callbackCalled)
    }
}
