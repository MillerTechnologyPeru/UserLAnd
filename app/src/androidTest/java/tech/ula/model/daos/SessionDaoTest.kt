package tech.ula.model.daos

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import android.database.sqlite.SQLiteConstraintException
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Assert.* // ktlint-disable no-wildcard-imports
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import tech.ula.model.repositories.AppDatabase
import tech.ula.model.entities.Filesystem
import tech.ula.model.entities.Session
import tech.ula.blockingObserve

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: AppDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var observer: Observer<List<Session>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        db.filesystemDao().insertFilesystem(Filesystem(0, name = DEFAULT_FS_NAME))
    }

    @After
    fun closeDb() = db.close()

    // Session Tests
    @Test(expected = SQLiteConstraintException::class)
    fun dbEnforcesUniqueSessionIdConstraint() {
        db.sessionDao().insertSession(DEFAULT_SESSION)
        db.sessionDao().insertSession(DEFAULT_SESSION)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun dbEnforcesUniqueSessionNameConstraint() {
        val session1 = Session(0, name = DEFAULT_NAME, filesystemId = DEFAULT_FS_ID)
        val session2 = Session(100, name = DEFAULT_NAME, filesystemId = DEFAULT_FS_ID)
        db.sessionDao().insertSession(session1)
        db.sessionDao().insertSession(session2)
    }

    @Test
    fun insertSessionAndGetByName() {
        db.sessionDao().insertSession(DEFAULT_SESSION)
        val retrieved = db.sessionDao().getSessionByName(DEFAULT_NAME)
        assertNotNull(retrieved)
        assertEquals(retrieved.name, DEFAULT_NAME)
    }

    @Test
    fun deleteAndFailRetrieval() {
        db.sessionDao().insertSession(DEFAULT_SESSION)
        val id = db.sessionDao().getSessionByName(DEFAULT_NAME).id
        db.sessionDao().deleteSessionById(id)
        assertNull(db.sessionDao().getAllSessions().value)
        assertNull(db.sessionDao().getSessionByName(DEFAULT_NAME))
    }

    @Test
    fun updateSession() {
        val session = Session(DEFAULT_NON_AUTOGENERATED_ID, name = "start", filesystemId = DEFAULT_FS_ID)
        db.sessionDao().insertSession(session)
        assertEquals(session, db.sessionDao().getSessionByName(session.name))

        session.name = "end"
        db.sessionDao().updateSession(session)
        assertEquals(session, db.sessionDao().getSessionByName(session.name))
    }

    @Test
    fun updateFilesystemNamesForAllSessions() {
        val fs = db.filesystemDao().getFilesystemByName(DEFAULT_FS_NAME)

        for (i in 0..9) {
            db.sessionDao().insertSession(Session(DEFAULT_ID, name = "session$i", filesystemId = fs.id))
        }

        fs.name = "end"

        db.filesystemDao().updateFilesystem(fs)
        val sessions = db.sessionDao().getAllSessions().blockingObserve()!!
        for (session in sessions) assertNotEquals(session.filesystemName, fs.name)

        db.sessionDao().updateFilesystemNamesForAllSessions()
        val updatedSessions = db.sessionDao().getAllSessions().blockingObserve()!!
        for (session in updatedSessions) assertEquals(session.filesystemName, fs.name)
    }

    companion object {
        val DEFAULT_ID = 0L
        val DEFAULT_NAME = "test"
        val DEFAULT_FS_ID = 1L
        val DEFAULT_SESSION = Session(DEFAULT_ID, name = DEFAULT_NAME, filesystemId = DEFAULT_FS_ID)

        val DEFAULT_FS_NAME = "start"

        val DEFAULT_NON_AUTOGENERATED_ID = 1L
    }
}