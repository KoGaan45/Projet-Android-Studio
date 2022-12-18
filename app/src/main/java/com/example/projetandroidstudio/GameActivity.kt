package com.example.projetandroidstudio

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import org.w3c.dom.NodeList
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class GameActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
    private lateinit var mLastLocation: Location
    //private lateinit var mCurrentLocation: Location
    private lateinit var sharedPref: SharedPreferences
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private var startPoint : GeoPoint = GeoPoint(47.845464, 1.939825)
    private var modeJeu : Boolean = true
    private var modeVoyage : Boolean = false
    private var miseEnModeVoyage : Boolean = false
    private lateinit var joueur : Joueur
    private lateinit var boutonVoyage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                Log.d(TAG,"SESSION = "+extras.getInt("session"))
                //mCurrentLocation = extras.get("mCurrentLocation") as Location
                joueur = Joueur(
                    extras.getInt("session"),
                    extras.getLong("signature"),
                    extras.getString("nettoyeur"),
                    extras.getString("value"),
                    extras.get("currentLocation") as Location,
                    extras.getString("statut")
                )
            }
        } else {
            //mCurrentLocation = savedInstanceState.getSerializable("mCurrentLocation") as Location
            joueur = Joueur(
                savedInstanceState.getSerializable("session") as Int,
                savedInstanceState.getSerializable("signature") as Long,
                savedInstanceState.getSerializable("nettoyeur") as String?,
                savedInstanceState.getSerializable("value") as String?,
                savedInstanceState.getSerializable("currentLocation") as Location,
                savedInstanceState.getSerializable("statut") as String?
            )
        }
        Log.d(TAG, "Location = ${joueur.loc}")

        sharedPref = getPreferences(android.content.Context.MODE_PRIVATE) ?: return

        this.setUpLocationListener()
        this.checkPermissionForMap()
        mLastLocation = joueur.loc!!
        this.setUpMap()

        boutonVoyage = findViewById<Button>(R.id.VoyageButton)

        when (joueur.statut) {
            "DEAD" -> boutonVoyage.text = "Créer nettoyeur"
            "UP" -> boutonVoyage.text = "Mode Voyage"
            else -> {
                boutonVoyage.text = "Remise en jeu"
                modeVoyage = true
            }
        }


        boutonVoyage.setOnClickListener {
            if (joueur.statut == "DEAD") this.creerNettoyeur()

            if (joueur.statut == "VOY") this.remiseEnJeu()

            if (joueur.statut == "UP") this.miseEnModeVoyage()
        }
    }

    private fun setUpMap() {
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.jeu)

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

    private fun addMarker(center: GeoPoint?, title : String, snippet : String) {
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
        var time = 0;

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
                            joueur.loc = location
                        }

                        // A ENLEVER
                        joueur.loc!!.latitude = 47.845095
                        joueur.loc!!.longitude = 1.937282

                        if (!miseEnModeVoyage && !modeVoyage)
                        {
                            checkPosition()
                        }

                        Log.d(TAG,"Joueur session : ${joueur.session} signature : ${joueur.signature} nettoyeur : ${joueur.nettoyeur}")

                        if (time % 15 == 0 && !modeVoyage && !miseEnModeVoyage)
                        {
                            deplaceJoueur()
                        }

                        if(modeJeu) {
                            addMarker(GeoPoint(joueur.loc),joueur.nettoyeur!!,"Votre position")
                        }

                        time += 5;
                    }
                }, Looper.myLooper()
            )
        }
    }

    private fun checkPosition(): Boolean {
        //Log.d(TAG,"-----> curent longitude: "+mCurrentLocation.longitude.toString()+" & latitude = "+mCurrentLocation.latitude.toString())
        return if((47.840 < joueur.loc!!.latitude && joueur.loc!!.latitude < 47.847) ||
            (1.937 < joueur.loc!!.longitude && joueur.loc!!.longitude < 1.941)){
            startPoint = GeoPoint(joueur.loc!!.latitude, joueur.loc!!.longitude)
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
                joueur.loc = it
            }
        }

        with (sharedPref.edit()) {
            putString("last_longitude", joueur.loc!!.longitude.toString())
            putString("last_latitude", joueur.loc!!.latitude.toString())
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
            mLastLocation = joueur.loc!!
        }
    }

    private fun deplaceJoueur()
    {
        Thread {
            val ws = WebServiceDeplace()
            val nl: NodeList = ws.call(joueur.session, joueur.signature, joueur.loc!!)!!

            val listeCiblesXML: NodeList = nl.item(0).childNodes
            val listeEnnemisXML: NodeList = nl.item(1).childNodes

            var i = 0
            var cibles: ArrayList<Cible> = ArrayList()
            var ennemis: ArrayList<Ennemi> = ArrayList()

            while (i < listeCiblesXML.length)
            {
                val id = listeCiblesXML.item(i).childNodes.item(0).textContent.toInt()
                val value = listeCiblesXML.item(i).childNodes.item(1).textContent.toInt()

                val loc = Location("")
                loc.longitude = listeCiblesXML.item(i).childNodes.item(2).textContent.toDouble()
                loc.latitude = listeCiblesXML.item(i).childNodes.item(3).textContent.toDouble()

                val cible = Cible(id, value, loc)
                cibles.add(cible)

                ++i
            }

            i = 0
            while (i < listeEnnemisXML.length)
            {
                val id = listeEnnemisXML.item(i).childNodes.item(0).textContent.toInt()
                val value = listeEnnemisXML.item(i).childNodes.item(1).textContent.toInt()

                val loc = Location("")
                loc.longitude = listeEnnemisXML.item(i).childNodes.item(2).textContent.toDouble()
                loc.latitude = listeEnnemisXML.item(i).childNodes.item(3).textContent.toDouble()

                val lifespan = listeEnnemisXML.item(i).childNodes.item(4).textContent.toInt()

                val ennemi = Ennemi(id, value, loc, lifespan)
                ennemis.add(ennemi)

                ++i
            }

            try {
                runOnUiThread {
                    for (c in cibles)
                    {
                        addMarker(GeoPoint(c.loc),"Cible n°${c.id}","Valeur: ${c.value}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun miseEnModeVoyage()
    {
        val textView : TextView = findViewById(R.id.textView)

        var timeRemaining = 60
        val timer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining -= 1
                textView.text = "$timeRemaining secondes restantes avant mode voyage!"
            }

            override fun onFinish() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        }

        val alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle("Voulez-vous vous déconnecter?")

        // Si l'utilisateur veut entrer en mode voyage exécuter l'action suivante
        alertDialogBuilder.setPositiveButton(android.R.string.yes) { dialog, which ->
            //Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT).show()

            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            
            //Timer("Passage en mode Voyage", false).schedule(delay = 60000) {
                Thread {
                    val ws = WebServiceModeVoyage()
                    modeVoyage = ws.call(joueur.session, joueur.signature)!!
                    timer.start()

                    try {
                        runOnUiThread {
                            miseEnModeVoyage = false

                            if (modeVoyage) boutonVoyage.text = "Remise en jeu"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            //}
        }

        alertDialogBuilder.setNegativeButton(android.R.string.cancel) { _, _->
        }

        alertDialogBuilder.show()
    }

    private fun creerNettoyeur() {
        Thread {
            val ws = WebServiceCreationNettoyeur()
            try{
                Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature + " | longitude = "+joueur.loc!!.longitude.toString() + " | lattitude = "+joueur.loc!!.latitude.toString())
                //joueur!!.nettoyeur = ws.call(joueur!!.session, joueur!!.signature,longitude!!,lattitude!!)
                joueur!!.nettoyeur = ws.call(joueur!!.session, joueur!!.signature,"1.93943","47.845560")
                Log.d(TAG,"-----> "+joueur!!.nettoyeur)
            }
            catch(e : Exception)
            {
                e.printStackTrace()
            }
        }.start()
        Log.d(TAG,"creationNettoyeur")
    }

    private fun remiseEnJeu() {
        Thread {
            val ws = WebServiceRemiseEnJeu()
            val enJeu = ws.call(joueur.session, joueur.signature, joueur.loc!!)
            try{
                runOnUiThread {
                    if (enJeu)
                    {
                        modeVoyage = false
                        boutonVoyage.text = "Mode Voyage"
                    }
                }
            }
            catch(e : Exception)
            {
                e.printStackTrace()
            }
        }.start()
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

    fun logout(view: View)
    {
        var alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle("Voulez-vous vous déconnecter?")

        alertDialogBuilder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(applicationContext, "Déconnexion", Toast.LENGTH_SHORT).show()
            finish()
        }

        alertDialogBuilder.setNegativeButton(android.R.string.cancel) { dialog, which ->
            Toast.makeText(applicationContext, android.R.string.cancel, Toast.LENGTH_SHORT).show()
        }

        alertDialogBuilder.show()
    }

    fun goToChat(view: View)
    {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("session", joueur!!.session)
        intent.putExtra("signature", joueur!!.signature)
        intent.putExtra("nettoyeur", joueur!!.nettoyeur)
        startActivity(intent)
    }

    fun goToStats(view: View)
    {
        //val intent = Intent(this, StatsEquipes::class.java)
        //startActivity(intent)
    }
}