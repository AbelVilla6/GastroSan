package com.gastrosan.activities

import android.app.Activity
import android.app.Instrumentation
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gastrosan.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.manipulation.Ordering
import org.mockito.Mockito.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.After

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    private lateinit var scenario: ActivityScenario<LoginActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(LoginActivity::class.java)
        Intents.init() // Inicializa Intents antes de cada prueba
    }
    @After
    fun tearDown() {
        Intents.release() // Libera Intents después de cada prueba
    }

    /*@Test
    fun testTransitionToRegisterActivity() {
        // Configurar la intención esperada
        val intent = Intent(ApplicationProvider.getApplicationContext(), RegisterActivity::class.java)
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, intent)
        Intents.intending(IntentMatchers.hasComponent(RegisterActivity::class.java.name)).respondWith(result)

        // Ejecutar acción que desencadena la transición de actividad
        onView(withId(R.id.button2)).perform(click()) // Ajusta esto para hacer click en el botón de registro

        // Verificar la intención esperada
        Intents.intended(IntentMatchers.hasComponent(RegisterActivity::class.java.name))
    }*/

    @Test
    fun testCheckCredentialsInvalidEmail() {
        scenario.onActivity { activity ->
            activity.inputEmail.setText("invalidEmail")
            activity.inputPassword.setText("validPassword")

            val result = activity.checkCredentials()
            assertFalse(result)
            assertEquals(activity.inputEmail.error, activity.getString(R.string.por_favor_introduzca_un_correo_electr_nico_v_lido))
        }
    }

    @Test
    fun testCheckCredentialsInvalidPassword() {
        scenario.onActivity { activity ->
            activity.inputEmail.setText("valid@example.com")
            activity.inputPassword.setText("123")

            val result = activity.checkCredentials()
            assertFalse(result)
            assertEquals(activity.inputPassword.error, activity.getString(R.string.la_contrase_a_debe_tener_al_menos_6_caracteres))
        }
    }

    @Test
    fun testHashPassword() {
        scenario.onActivity { activity ->
            val password = "password"
            val hashedPassword = activity.hashPassword(password)
            assertNotNull(hashedPassword)
            assertEquals(hashedPassword.length, 64) // SHA-256 produces a 64-character hex string
        }
    }

    /*@Test
    fun testSuccessfulLogin() {
        scenario.onActivity { activity ->
            val mockAuth = mockk<FirebaseAuth>()
            val mockTask = mockk<Task<AuthResult>>()

            every { mockTask.isSuccessful } returns true
            every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns mockTask

            activity.mAuth = mockAuth

            // Simula la entrada de correo electrónico y contraseña
            onView(withId(R.id.editText1)).perform(typeText("valid@example.com"), closeSoftKeyboard())
            onView(withId(R.id.editText2)).perform(typeText("validPassword"), closeSoftKeyboard())

            // Ejecuta el método de inicio de sesión
            onView(withId(R.id.button1)).perform(click())

            // Verifica que la actividad de menú se inicia
            Intents.init()
            Intents.intended(IntentMatchers.hasComponent(MenuActivity::class.java.name))
            Intents.release()
        }
    }*/


    @Test
    fun testGoogleSignIn() {
        scenario.onActivity { activity ->
            // Simulate a Google Sign-In success
            val mockToken = "mockGoogleToken"
            activity.firebaseAuthWithGoogle(mockToken)

            // Add assertions based on expected behavior
        }
    }

    //@Test
    /*fun testSaveNewUserToFirebase() {
        scenario.onActivity { activity ->
            // Simulate a new user authenticated via Google
            val mockUser = mock(FirebaseUser::class.java)
            `when`(mockUser.uid).thenReturn("mockUid")
            `when`(mockUser.email).thenReturn("mock@example.com")
            `when`(mockUser.displayName).thenReturn("Mock User")
            `when`(mockUser.photoUrl).thenReturn(Uri.parse("http://mockurl.com/photo.jpg"))

            activity.checkAndSaveUser(mockUser)

            // Add assertions to check if the user was saved correctly in Firebase
        }
    }*/

    @Test
    fun testShowHidePassword() {
        scenario.onActivity { activity ->
            // Inicializar EditText y CheckBox
            activity.inputPassword.setText("password")
            activity.checkBox.isChecked = false

            // Simular mostrar contraseña
            activity.checkBox.performClick()
            val showPasswordInputType = activity.inputPassword.inputType
            assertEquals(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, showPasswordInputType)

            // Simular ocultar contraseña
            activity.checkBox.performClick()
            val hidePasswordInputType = activity.inputPassword.inputType
            assertEquals(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, hidePasswordInputType)
        }
    }





}
