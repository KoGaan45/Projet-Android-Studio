package com.example.projetandroidstudio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        //raffaichirMessages()
    }

    override fun onPause() {
        super.onPause()
    }

    fun connecter(view : View) {
        var login = findViewById<EditText>(R.id.editText);
        var password = findViewById<EditText>(R.id.editText2);
        Thread {
            val ws = WebServiceConnexion()
            try{
                val joueur: Joueur? = ws.call(login.text.toString(),password.text.toString())
                Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature)
            }
            catch(e : Exception)
            {
                e.printStackTrace();
            }
        }.start()
        Log.d(TAG,"mBouttonConnecter")
    }

    private fun setUpLocationListener() {

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val builder = LocationRequest.Builder(1000)
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
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        for (location in locationResult.locations) {
                            var lattitude = location.latitude.toString()
                            var longitude = location.longitude.toString()
                        }
                    }
                }, Looper.myLooper()
            )
        }

    }

    /*fun creerNettoyeur(joueur : Joueur) {
        Thread {
            val ws = WebServiceCreationNettoyeur()
            try{

                val joueur: Joueur? = ws.call(login.text.toString(),password.text.toString())
                Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature)
            }
            catch(e : Exception)
            {
                e.printStackTrace();
            }
        }.start()
        Log.d(TAG,"creationNettoyeur")
    }*/

    /*private fun raffraichirMission() {
        Thread {
            val ws = WebServiceLastMSG()
            val aAjouter: ArrayList<Message> = ws.call()
            try {
                runOnUiThread {
                    messagesFragment.deleteMessages()
                    for (m in aAjouter) {
                        messagesFragment.addMessage(
                            m.getId(),
                            m.getVraieDate(),
                            m.getTitre(),
                            m.getContenu()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }*/


}