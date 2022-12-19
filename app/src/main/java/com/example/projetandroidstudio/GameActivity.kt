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
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.w3c.dom.NodeList
import java.util.*


class GameActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
    private lateinit var mLastLocation: Location
    private lateinit var sharedPref: SharedPreferences
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private var startPoint : GeoPoint = GeoPoint(47.845464, 1.939825)
    private var modeJeu : Boolean = true
    private var modeVoyage : Boolean = false
    private var modeNettoyage : Boolean = false
    private lateinit var joueur : Joueur

    private lateinit var boutonVoyage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                Log.d(TAG,"SESSION = "+extras.getInt("session"))
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

        // Check l'état du joueur à la création pour changer l'affichage du bouton
        when (joueur.statut) {
            "DEAD" -> boutonVoyage.text = "Créer nettoyeur"
            "UP" -> boutonVoyage.text = "Mode Voyage"
            "NET" -> {
                modeNettoyage = true
                boutonVoyage.text = "Mode Voyage"
            }
            "PACK" -> {
                modeVoyage = true
                boutonVoyage.text = "Remise en jeu"
            }
            else -> {
                boutonVoyage.text = "Remise en jeu"
                modeVoyage = true
            }
        }

        // Check l'état du joueur pour changer la fonction appelé
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
        addMarker(GeoPoint(47.845464, 1.939825),"Batiment 3IA","Dirigez-vous ici pour créer un nettoyeur", "3IA", "3IA")
    }

    private fun addMarker(center: GeoPoint?, title : String, snippet : String, type: String, idMarker: String) {
        for (i in 0 until map.overlays.size) {
            val overlay: Overlay = map.overlays[i]
            if (overlay is Marker && (overlay as Marker).id == idMarker) {
                map.overlays.remove(overlay)
                break
            }
        }

        map.invalidate()

        val marker = Marker(map)
        marker.position = center
        marker.id = idMarker
        marker.title = title
        marker.snippet = snippet

        when (type) {
            "CIBLE" -> marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.marker_cible, null)!!
            "ME" -> marker.icon = ResourcesCompat.getDrawable(resources, org.osmdroid.library.R.drawable.person, null)!!
            "ENNEMI" -> marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.marker_ennemi, null)!!
        }

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
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
                        //joueur.loc!!.latitude = 47.845095
                        //joueur.loc!!.longitude = 1.937282

                        if (!modeVoyage && joueur.statut != "DEAD")
                        {
                            checkPosition()
                            calculVitesseJoueur(time)
                        }
                        else mLastLocation = joueur.loc!!

                        Log.d(TAG,"Joueur session : ${joueur.session} signature : ${joueur.signature} nettoyeur : ${joueur.nettoyeur}")

                        if(modeJeu) addMarker(GeoPoint(joueur.loc), joueur.nettoyeur!!,"Votre position", "ME", "ME")

                        time += 5;
                    }
                }, Looper.myLooper()
            )
        }
    }

    private fun checkPosition(): Boolean {
        return if((47.840 < joueur.loc!!.latitude && joueur.loc!!.latitude < 47.847) || (1.937 < joueur.loc!!.longitude && joueur.loc!!.longitude < 1.941)){
            startPoint = GeoPoint(joueur.loc!!.latitude, joueur.loc!!.longitude)

            val textView : TextView = findViewById(R.id.textView)
            textView.text = resources.getString(R.string.nettoyer_cible)
            textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.Aquamarine))
            modeJeu = true

            true
        } else{
            val textView : TextView = findViewById(R.id.textView)
            textView.text = resources.getString(R.string.erreur_position)
            textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.IndianRed))

            modeJeu = false

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

    fun calculVitesseJoueur(time: Int)
    {
        val results = FloatArray(1)

        Location.distanceBetween(mLastLocation.latitude, mLastLocation.longitude, joueur.loc!!.latitude, joueur.loc!!.longitude, results)

        val kmPerHour = results[0] / 5 * 3600 / 1000

        Log.d(TAG,"-----> distance: " + results[0].toString())
        Log.d(TAG, "-----> km/h: $kmPerHour")

        // en fonction de la vitesse du joueur on le met en mode voyage ou on le déplace
        if (kmPerHour > 15.0)
        {
            Toast.makeText(applicationContext, "Vous allez trop vite! Passage en mode voyage", Toast.LENGTH_SHORT).show()
            this.miseEnModeVoyageForce()
        } else if (kmPerHour <= 15.0 && time % 15 == 0) this.deplaceJoueur()

        mLastLocation = joueur.loc!!
    }

    private fun deplaceJoueur()
    {
        Thread {
            val ws = WebServiceDeplace()
            val nl: NodeList = ws.call(joueur.session, joueur.signature, joueur.loc!!) ?: return@Thread

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
                        addMarker(GeoPoint(c.loc),"Cible n°${c.id}","Valeur: ${c.value}", "CIBLE", "${c.id}${c.value}")
                    }

                    for (e in ennemis)
                    {
                        addMarker(GeoPoint(e.loc),"Ennemi n°${e.id}","Valeur: ${e.value}, il y a ${e.lifespan}s", "ENNEMI", "${e.id}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun miseEnModeVoyage()
    {
        Log.d("VOYAGE", joueur.statut!!)

        if (joueur.statut != "UP") {
            Toast.makeText(applicationContext, "Vous ne pouvez pas encore faire cela", Toast.LENGTH_LONG).show()
            return
        }

        val textView : TextView = findViewById(R.id.textView)

        var timeRemaining = 60
        val timer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining -= 1
                textView.text = "$timeRemaining secondes restantes avant mode voyage!"
            }

            override fun onFinish() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                getStatutJoueur()
                checkPosition()
            }
        }

        val alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle("Voulez-vous vous passer en mode voyage?")

        // Si l'utilisateur veut entrer en mode voyage exécuter l'action suivante
        alertDialogBuilder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(applicationContext, "Toutes les actions sont bloqués durant le passage en mode voyage", Toast.LENGTH_LONG).show()

            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            Thread {
                val ws = WebServiceModeVoyage()
                modeVoyage = ws.call(joueur.session, joueur.signature)!!
                timer.start()

                getStatutJoueur()

                try {
                    runOnUiThread {
                        if (modeVoyage) boutonVoyage.text = "Remise en jeu"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        alertDialogBuilder.setNegativeButton(android.R.string.cancel) { _, _->
        }

        alertDialogBuilder.show()
    }

    private fun miseEnModeVoyageForce()
    {
        if (joueur.statut != "UP") return

        val textView : TextView = findViewById(R.id.textView)

        var timeRemaining = 60
        val timer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining -= 1
                textView.text = "$timeRemaining secondes restantes avant mode voyage!"
            }

            override fun onFinish() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                getStatutJoueur()
                checkPosition()
            }
        }

        Toast.makeText(applicationContext, "Toutes les actions sont bloqués durant le passage en mode voyage", Toast.LENGTH_LONG).show()

        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        Thread {
            val ws = WebServiceModeVoyage()
            modeVoyage = ws.call(joueur.session, joueur.signature)!!
            timer.start()

            getStatutJoueur()

            try {
                runOnUiThread {
                    if (modeVoyage) boutonVoyage.text = "Remise en jeu"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getStatutJoueur() {
        Thread {
            val wsStats = WebServiceStatsNettoyeur() // Tentative de récupération
            val j = wsStats.call(joueur!!.session, joueur!!.signature)!!
            joueur.statut = j.statut
        }.start()
    }

    private fun creerNettoyeur() {
        if (joueur.statut != "DEAD")
        {
            Toast.makeText(applicationContext, "Votre nettoyeur n'est pas mort", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val ws = WebServiceCreationNettoyeur()
            val nettoyeur = ws.call(joueur.session, joueur.signature, joueur.loc!!) ?: return@Thread

            try{
                if (nettoyeur!!.startsWith("KO"))
                {
                    val textView : TextView = findViewById(R.id.textView)

                    if (nettoyeur == "KO-not in 3IA")
                        textView.text = "Vous n'êtes pas en 3IA, création du nettoyeur impossible!"
                    else
                        textView.text = "Le délai de 15 minutes n'est pas terminé!"

                    textView.setTextColor(ContextCompat.getColor(applicationContext,R.color.IndianRed))
                }
                else
                {
                    joueur.nettoyeur = nettoyeur
                    modeVoyage = true
                    getStatutJoueur()
                    boutonVoyage.text = "Remise en jeu"

                    Log.d(TAG,"Session = "+joueur!!.session + " | Signature = "+joueur!!.signature + " | longitude = "+joueur.loc!!.longitude.toString() + " | lattitude = "+joueur.loc!!.latitude.toString())
                    Log.d(TAG,"-----> "+joueur!!.nettoyeur)
                }
            }
            catch(e : Exception)
            {
                e.printStackTrace()
            }
        }.start()
        Log.d(TAG,"creationNettoyeur")
    }

    private fun remiseEnJeu() {
        Log.d("REMISE", joueur.statut!!)

        if (joueur.statut != "VOY") {
            Toast.makeText(applicationContext, "Vous ne pouvez pas encore faire cela", Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            val ws = WebServiceRemiseEnJeu()
            val enJeu = ws.call(joueur.session, joueur.signature, joueur.loc!!)

            try{
                getStatutJoueur()

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