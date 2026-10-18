package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PropertyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Property", appName)
  }

  @Test
  fun `property repository loads sample listings and toggles save`() {
    val repo = PropertyRepository()
    val properties = repo.properties.value
    assertTrue("Properties should not be empty", properties.isNotEmpty())

    val firstId = properties.first().id
    val initialSaved = properties.first().isSaved
    repo.toggleSave(firstId)
    val updated = repo.properties.value.first { it.id == firstId }
    assertEquals(!initialSaved, updated.isSaved)
  }
}
