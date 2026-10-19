package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PropertyDao
import com.example.data.local.toSavedEntity
import com.example.data.repository.PropertyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PropertyDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = AppDatabase.createInMemory(context)
        dao = database.propertyDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Property", appName)
    }

    @Test
    fun `propertyDao inserts queries and deletes saved property listings`() = runBlocking {
        val sample = PropertyRepository.createSampleProperties().first()
        val entity = sample.toSavedEntity()

        dao.insertProperty(entity)
        val loaded = dao.getPropertyById(sample.id)
        assertNotNull(loaded)
        assertEquals(sample.title, loaded?.title)
        assertEquals(sample.price, loaded?.price)
        assertEquals(sample.amenities, loaded?.amenities)
        assertEquals(sample.propertyType, loaded?.propertyType)

        val allSaved = dao.getAllSavedProperties().first()
        assertEquals(1, allSaved.size)

        dao.deletePropertyById(sample.id)
        val afterDelete = dao.getPropertyById(sample.id)
        assertNull(afterDelete)
    }

    @Test
    fun `property repository persists saved listings via PropertyDao`() = runBlocking {
        val repo = PropertyRepository(propertyDao = dao, seedInitialSaved = false)
        val targetProperty = repo.properties.value.first { !it.isSaved }

        repo.saveProperty(targetProperty)
        val savedInDb = dao.getSavedPropertyById(targetProperty.id)
        assertNotNull("Saved property should exist in Room DB", savedInDb)
        assertEquals(targetProperty.title, savedInDb?.title)

        val savedFlowItems = repo.savedPropertiesFlow.first()
        assertTrue(savedFlowItems.any { it.id == targetProperty.id })

        repo.removeSavedProperty(targetProperty.id)
        assertNull("Removed property should be deleted from Room DB", dao.getSavedPropertyById(targetProperty.id))
    }
}
