package com.example.projetandroidstudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*


@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private var joueur : Joueur? = null
    lateinit var mCurrentLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        this.setUpLocationListener()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun connecter(view : View) {
        val login = findViewById<EditText>(R.id.MainMenuLogin)
        val password = findViewById<EditText>(R.id.MainMenuPassword)
        Thread {
            val ws = WebServiceConnexion()
            try{

                this.joueur = ws.call(login.text.toString(),password.text.toString())

                if(joueur == null) {
                    this.runOnUiThread(Runnable {
                        AlertDialog.Builder(this)
                            .setMessage(R.string.dialog_connexion_invalide)
                            .setPositiveButton(
                                R.string.retour
                            ) { _, _ ->
                                onResume()
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    })
                    Log.d(TAG,"Connexion échouée")
                }
                else {
                    val wsStats = WebServiceStatsNettoyeur() // Tentative de récupération
                    joueur = wsStats.call(joueur!!.session, joueur!!.signature)
                    joueur!!.loc = mCurrentLocation

                    Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature + " | longitude = "+joueur!!.loc!!.longitude.toString() + " | lattitude = "+joueur!!.loc!!.latitude.toString()
                    + " | nom = " + joueur!!.nettoyeur + " | value = " + joueur!!.value + " | statut = " + joueur!!.statut)


                    // Si tous les tests sont passés démarrer la seconde activité contenant le jeu
                    val intent = Intent(this,GameActivity::class.java)
                    intent.putExtra("currentLocation", joueur!!.loc)
                    intent.putExtra("session", joueur!!.session)
                    intent.putExtra("signature", joueur!!.signature)
                    intent.putExtra("nettoyeur", joueur!!.nettoyeur)
                    intent.putExtra("value", joueur!!.value)
                    intent.putExtra("statut", joueur!!.statut)
                    startActivity(intent)
                }
            }
            catch(e : Exception)
            {
                e.printStackTrace()
            }
        }.start()
    }

    private fun setUpLocationListener() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val builder = LocationRequest.Builder(5000)
        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        val locationRequest = builder.build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        for (location in locationResult.locations) {
                            mCurrentLocation = location
                            //Log.d(TAG, mCurrentLocation.longitude.toString())
                        }
                    }
                }, Looper.myLooper()
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) ===
                    PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}