package com.example.projetandroidstudio

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    var fusedLocationProviderClient : FusedLocationProviderClient? = null
    var lattitude : String? = null
    var longitude : String? = null
    lateinit var mLastLocation: Location
    lateinit var mCurrentLocation: Location
    lateinit var sharedPref: SharedPreferences
    var joueur : Joueur? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = getPreferences(android.content.Context.MODE_PRIVATE) ?: return
        mLastLocation = Location("DummyProvider")

        mLastLocation.longitude = sharedPref.getString("last_longitude", "0.0")!!.toDouble()
        mLastLocation.latitude = sharedPref.getString("last_latitude", "0.0")!!.toDouble()

        Log.d(TAG,"-----> last_longitude: "+mLastLocation.longitude.toString())

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }

        if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }

        this.setUpLocationListener()
    }

    override fun onResume() {
        super.onResume()

        mLastLocation.longitude = sharedPref.getString("last_longitude", "0.0")!!.toDouble()
        mLastLocation.latitude = sharedPref.getString("last_latitude", "0.0")!!.toDouble()
        //raffaichirMessages()
    }

    override fun onPause() {
        super.onPause()

        saveLastPosition()
    }

    override fun onDestroy() {
        super.onDestroy()

        saveLastPosition()
    }

    fun saveLastPosition()
    {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient!!.lastLocation.addOnSuccessListener {
                mCurrentLocation = it
            }
        }

        with (sharedPref.edit()) {
            putString("last_longitude", mCurrentLocation.longitude.toString())
            putString("last_latitude", mCurrentLocation.latitude.toString())
            apply()
        }
    }

    fun calculVitesseJoueur(lastLocation: Location, currentLocation: Location)
    {
        var results = FloatArray(1)

        Location.distanceBetween(lastLocation.latitude, lastLocation.longitude, currentLocation.latitude, currentLocation.longitude, results)

        var kmPerHour = results[0] / 5 * 3600 / 1000

        Log.d(TAG,"-----> distance: " + results[0].toString())
        Log.d(TAG,"-----> km/h: " + kmPerHour.toString())

        if (kmPerHour > 15.0)
        {
            // TODO Mettre en mode voyage après 1 minutes si supérieur à une vitesse définie
            // Ne pas mettre à jour la dernière position connue du joueur
        }
        else
        {
            mLastLocation = mCurrentLocation
        }
    }

    fun connecter(view : View) {
        var login = findViewById<EditText>(R.id.editText);
        var password = findViewById<EditText>(R.id.editText2);
        Thread {
            val ws = WebServiceConnexion()
            try{
                this.joueur = ws.call(login.text.toString(),password.text.toString())
                creerNettoyeur()
            }
            catch(e : Exception)
            {
                e.printStackTrace();
            }
        }.start()
        Log.d(TAG,"mBouttonConnecter")
    }

    private fun setUpLocationListener() {
        var timer = 0

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

                            calculVitesseJoueur(mLastLocation, mCurrentLocation)

                            timer += 5
                            if (timer % 15 == 0)
                            {
                                //TODO Appel au webservice qui met à jour la position du joueur sur le serveur
                            }
                        }
                    }
                }, Looper.myLooper()
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
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

    fun creerNettoyeur() {
        Thread {
            val ws = WebServiceCreationNettoyeur()
            try{
                Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature + " | longitude = "+mCurrentLocation.longitude.toString() + " | lattitude = "+mCurrentLocation.latitude.toString())
                //joueur!!.nettoyeur = ws.call(joueur!!.session, joueur!!.signature,longitude!!,lattitude!!)
                joueur!!.nettoyeur = ws.call(joueur!!.session, joueur!!.signature,"1.93943","47.845560")
                Log.d(TAG,"-----> "+joueur!!.nettoyeur)
            }
            catch(e : Exception)
            {
                e.printStackTrace();
            }
        }.start()
        Log.d(TAG,"creationNettoyeur")
    }
}