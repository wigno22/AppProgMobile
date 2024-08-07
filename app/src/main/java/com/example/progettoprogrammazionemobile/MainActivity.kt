package com.example.progettoprogrammazionemobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

// Dichiaro classi dei miei 3 fragment
class profilefrg : Fragment(R.layout.fragment_profile)
class accountfrg : Fragment(R.layout.fragment_account)
class operationfrg : Fragment(R.layout.fragment_operation)
class investmentfrg : Fragment(R.layout.fragment_investment)
class aiintegrationfrg : Fragment(R.layout.fragment_aiintergration)
class chataifrg : Fragment(R.layout.fragment_chatai)

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

        // Da xml recupero il container dei miei fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        // Aggiungo un controller per potermi spostare da un fragment all'altro
        val navController = navHostFragment.navController
        // Imposto la navigazione tramite una bottom navigation alla quale è collegato il bottom menu
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)

        // Set up the action bar to use the navigation controller for the up button
        NavigationUI.setupActionBarWithNavController(this, navController)

        // Inizio servizio periodico una volta eseguito l'accesso
        if (user != null) {
            val UID = user.uid
            startWorkerForUser(this, UID)
        }

        // Gestione delle notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCMExample", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            // Log and toast
            Log.e("FCMExample", token)
            // Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })
    }

    // Gonfia il menu della toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    // Gestisci il click del bottone della toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        return when (item.itemId) {
            R.id.navigation_chatAi -> {
                navController.navigate(R.id.navigation_chatAi)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Per ogni utente faccio partire un servizio periodico univoco
    fun startWorkerForUser(context: Context, UID: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("UID", UID)
            apply()
        }

        val workManager = WorkManager.getInstance(this)
        val myWorkRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 30, TimeUnit.DAYS).build()

        // Metto in coda il WorkRequest in modo unico
        workManager.enqueueUniquePeriodicWork(
            "Mywork_$UID",
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene il lavoro esistente e non crea un nuovo lavoro
            myWorkRequest
        )

        // Osserva lo stato del lavoro
        workManager.getWorkInfoByIdLiveData(myWorkRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Log.d("MainActivity", "Work completed successfully")
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        Log.d("MainActivity", "Work failed")
                    }
                }
            })
    }
}
