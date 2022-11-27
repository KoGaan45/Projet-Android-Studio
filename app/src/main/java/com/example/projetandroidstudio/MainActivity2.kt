package com.example.projetandroidstudio

import android.Manifest
import android.content.ContentValues.TAG
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


class MainActivity2 : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
    lateinit var mLastLocation: Location
    lateinit var mCurrentLocation: Location
    lateinit var sharedPref: SharedPreferences
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private var startPoint : GeoPoint = GeoPoint(47.845464, 1.939825)
    private var modeJeu : Boolean = true
    lateinit var joueur : Joueur

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                Log.d(TAG,"SESSION = "+extras.getInt("session"))
                mCurrentLocation = extras.get("mCurrentLocation") as Location
                joueur = Joueur(extras.getInt("session"),extras.getLong("signature"),extras.getString("nettoyeur"))
            }
        } else {
            mCurrentLocation = savedInstanceState.getSerializable("mCurrentLocation") as Location
            joueur = Joueur(savedInstanceState.getSerializable("session") as Int,
                savedInstanceState.getSerializable("signature") as Long, savedInstanceState.getSerializable("nettoyeur") as String?
            )
        }
        Log.d(TAG, "Location = $mCurrentLocation")

        sharedPref = getPreferences(android.content.Context.MODE_PRIVATE) ?: return

        this.setUpLocationListener()
        this.checkPermissionForMap()
        mLastLocation = mCurrentLocation
        this.setUpMap()
    }

    private fun setUpMap() {
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main2)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)

        //Test si le  joueur est à la fac alors on centre sur lui sinon on centre 3IA avec erreur
        checkPosition()

        val mapController = map.controller
        mapController.setZoom(19.0)
        mapController.setCenter(startPoint)

        map.isHorizontalMapRepetitionEnabled = false
        map.isVerticalMapRepetitionEnabled = false

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled

        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)
        addMarker(GeoPoint(47.845464, 1.939825),"Batiment 3IA","Dirigez-vous ici pour créer un nettoyeur")
    }

    private fun addMarker(center: GeoPoint?, title : String,snippet : String) {
        val marker = Marker(map)
        marker.position = center
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
        marker.title = title
        marker.snippet = snippet
    }

    private fun checkPermissionForMap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
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
                        }

                        // A ENLEVER
                        mCurrentLocation.latitude = 47.842
                        mCurrentLocation.longitude = 1.939

                        checkPosition()

                        Log.d(TAG,"Joueur session : ${joueur.session} signature : ${joueur.signature} nettoyeur : ${joueur.nettoyeur}")

                        if(modeJeu) {
                            addMarker(GeoPoint(mCurrentLocation),joueur.nettoyeur!!,"Votre position")
                        }

                    }
                }, Looper.myLooper()
            )
        }
    }

    private fun checkPosition(): Boolean {
        Log.d(TAG,"-----> curent longitude: "+mCurrentLocation.longitude.toString()+" & latitude = "+mCurrentLocation.latitude.toString())
        return if((mCurrentLocation.latitude > 47.840 && mCurrentLocation.latitude < 47.847) ||
            (mCurrentLocation.longitude > 1.937 && mCurrentLocation.longitude < 1.941)){
            startPoint = GeoPoint(mCurrentLocation.latitude, mCurrentLocation.longitude)
            val textView : TextView = findViewById(R.id.textView)
            textView.text = resources.getString(R.string.nettoyer_cible)
            textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.Aquamarine))
            modeJeu=true
            true
        } else{
            val textView : TextView = findViewById(R.id.textView)
            textView.text = resources.getString(R.string.erreur_position)
            textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.IndianRed))
            modeJeu=false
            false
        }
    }

    private fun saveLastPosition()
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
        val results = FloatArray(1)

        Location.distanceBetween(lastLocation.latitude, lastLocation.longitude, currentLocation.latitude, currentLocation.longitude, results)

        val kmPerHour = results[0] / 5 * 3600 / 1000

        Log.d(TAG,"-----> distance: " + results[0].toString())
        Log.d(TAG, "-----> km/h: $kmPerHour")

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

    override fun onResume() {
        super.onResume()
        mLastLocation.longitude = sharedPref.getString("last_longitude", "0.0")!!.toDouble()
        mLastLocation.latitude = sharedPref.getString("last_latitude", "0.0")!!.toDouble()
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        saveLastPosition()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveLastPosition()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }
}