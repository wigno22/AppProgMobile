package com.example.progettoprogrammazionemobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

//dichiaro classi dei miei 3 fragment
class profilefrg: Fragment(R.layout.fragment_profile)
class accountfrg: Fragment(R.layout.fragment_account)
class operationfrg: Fragment(R.layout.fragment_operation)
class MainActivity : AppCompatActivity() {


    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

        //da xml recupero il container dei miei frgament
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        //aggiungo un controller per potermi spostare da un fragment all'altro
        val navController = navHostFragment.navController
        //imposto la navigazione tramite una bottomnavigation alla quale è collegato il bottom menu
        //la navigazione è gestita con le action del navigation_menu
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)

        //inizio servizio periodico una volta eseguito l'accesso
        if (user != null) {
            val UID = user.uid
            startWorkerForUser(this, UID)
        }


        //gestione delle notifiche
        //Require the permission POST_NOTIFICATIONS
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
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
            Log.d("FCMExample", token)
            //Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

    }

        //per ogni utente faccio partire un servizio periodico univoco
        fun startWorkerForUser(context: Context, UID: String) {

            //creo file di tipo persistente con chiave valore l'uid dell'utente, da passare al worker
            val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("UID", UID)
                apply()
            }


            val workManager = WorkManager.getInstance(this)
            //workManager.cancelAllWork()
            //val myWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<MyWorker>().build()

            // Creo il PeriodicWorkRequest di 30 giorni
            val myWorkRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 30, TimeUnit.DAYS).build()


            //metto in coda il WorkRequest in modo unico
            workManager.enqueueUniquePeriodicWork(
                "Mywork_$UID",
                ExistingPeriodicWorkPolicy.KEEP, // Mantiene il lavoro esistente e non crea un nuovo lavoro
                myWorkRequest
            )


            // Osserva lo stato del lavoro
            workManager.getWorkInfoByIdLiveData(myWorkRequest.id)
                .observe(this, Observer { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        // Lavoro terminato con successo
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            Log.d("MainActivity", "Work completed successfully")
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            Log.d("MainActivity", "Work failed")
                        }
                    }
                })
        }

}

