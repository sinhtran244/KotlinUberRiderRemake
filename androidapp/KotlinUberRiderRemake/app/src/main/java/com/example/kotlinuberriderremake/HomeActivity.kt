package com.example.kotlinuberriderremake

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.kotlinuberriderremake.Common.Common
import com.example.kotlinuberriderremake.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView
    private lateinit var img_avatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarHome.toolbar)

        binding.appBarHome.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        drawerLayout= binding.drawerLayout as DrawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init(){
        navView.setNavigationItemSelectedListener { it ->
            if (it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this@HomeActivity)
                builder.setTitle("Sign out")
                    .setMessage("Do you really want to sign out?")
                    .setNegativeButton("CANCEL") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .setPositiveButton("SIGN OUT") { dialogInterface, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@HomeActivity, SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this@HomeActivity,android.R.color.holo_red_dark))

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this@HomeActivity,R.color.colorAccent))
                }
                builder.show()
            }



            true
        }
        val headerView = navView.getHeaderView( 0)

        val txt_name = headerView.findViewById<TextView>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<TextView>(R.id.txt_phone) as TextView
        val txt_star = headerView.findViewById<TextView>(R.id.txt_star) as TextView
        img_avatar = headerView.findViewById<TextView>(R.id.img_avatar) as ImageView
        txt_name.text = Common.buildWelcomeMessage()
        txt_phone.text = Common.currentRider!!.phoneNumber

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}