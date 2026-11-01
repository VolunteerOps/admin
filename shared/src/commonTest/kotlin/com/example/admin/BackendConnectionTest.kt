package com.example.admin
import com.example.admin.core.network.BackendConnection
import kotlin.test.*

class BackendConnectionTest {
    @Test fun acceptsSecureDeploymentAndLocalOrigins() {
        try {
            BackendConnection.configure("https://api.example.org/")
            assertEquals("https://api.example.org/sentinel", BackendConnection.sentinelUrl)
            BackendConnection.configure("http://127.0.0.1:3000")
            assertEquals("http://127.0.0.1:3000", BackendConnection.apiOrigin)
        } finally { BackendConnection.configure(BackendConnection.DEFAULT_API_ORIGIN) }
    }
    @Test fun rejectsCredentialsPathsAndUnencryptedRemoteEndpoints() {
        for (origin in listOf("http://api.example.org", "https://user:secret@api.example.org", "https://api.example.org/path", "https://api.example.org?key=secret", "https://api.example.org#token", "javascript:alert(1)", "https://api.example.org:99999")) {
            assertFailsWith<IllegalArgumentException> { BackendConnection.configure(origin) }
        }
        assertEquals(BackendConnection.DEFAULT_API_ORIGIN, BackendConnection.apiOrigin)
    }
}
