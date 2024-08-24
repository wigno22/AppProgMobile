package com.example.progettoprogrammazionemobile

import InvestmentWorker
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.firebase.ui.auth.AuthUI.TAG
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

// Dichiaro classi dei miei 3 fragment
class profilefrg : Fragment(R.layout.fragment_profile)
class accountfrg : Fragment(R.layout.fragment_account)
class operationfrg : Fragment(R.layout.fragment_operation)
class investmentfrg : Fragment(R.layout.fragment_investment)
class aiintegrationfrg : Fragment(R.layout.fragment_stock)
class chataifrg : Fragment(R.layout.fragment_chatai)

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

        // Definisci un TAG personalizzato
        val TAG = "InvestmentWorker"

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
            val db = Firebase.firestore

            // Recupera il documento "Account" e il campo "periodoInvestimento"
            db.collection(UID).document("Account")
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Recupera il valore di "periodoInvestimento"
                        val durationInMonths = document.getLong("periodoInvestimento")?.toInt() ?: 0

                        if (durationInMonths > 0) {
                            // Avvia il Worker con il periodo di investimento recuperato
                            startInvestmentWorker(this, UID, durationInMonths)
                        } else {
                            Log.e(TAG, "Il campo 'periodoInvestimento' non è valido.")
                        }
                    } else {
                        Log.e(TAG, "Documento 'Account' non trovato.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Errore nel recupero del documento 'Account': ", exception)
                }
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

    private fun startInvestmentWorker(mainActivity: MainActivity, uid: String, durationInMonths: Int) {
        val sharedPreferences = mainActivity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UID", uid)
        editor.apply()

        val delayDurationInDays = durationInMonths * 30L

        val inputData = Data.Builder()
            .putString("UID", uid)
            .putInt("durationInMonths", durationInMonths)
            .build()

        // Nome unico per il worker
        val uniqueWorkName = "InvestmentWorker_$uid"

        // Cancella il vecchio worker se esiste
        WorkManager.getInstance(mainActivity).cancelUniqueWork(uniqueWorkName)

        // Crea un nuovo OneTimeWorkRequest con un nome unico
        val workRequest = OneTimeWorkRequestBuilder<InvestmentWorker>()
            .setInitialDelay(delayDurationInDays, TimeUnit.DAYS) // Imposta il ritardo in giorni
            .setInputData(inputData)
            .build()

        // Avvia il worker con un nome unico
        WorkManager.getInstance(mainActivity)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
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

    // Metodo per mostrare o nascondere la Bottom Navigation Bar
    fun setBottomNavigationVisibility(isVisible: Boolean) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        if (isVisible) {
            bottomNavigationView.visibility = View.VISIBLE
        } else {
            bottomNavigationView.visibility = View.GONE
        }
    }
}
