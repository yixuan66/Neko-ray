package com.neko.welcome

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.neko.splash.SplashActivity
import com.neko.v2ray.ui.BaseActivity
import com.neko.v2ray.R
import java.util.*

class WelcomeActivity : BaseActivity() {
    private lateinit var dialog: AlertDialog.Builder
    private lateinit var f: SharedPreferences
    private lateinit var imageview1: ImageView
    private lateinit var imageview2: ImageView
    private lateinit var imageview3: ImageView
    private lateinit var imageview4: ImageView
    private lateinit var imageview5: ImageView
    private lateinit var imageview6: ImageView
    private lateinit var linear1: LinearLayout
    private lateinit var linear10: LinearLayout
    private lateinit var linear11: LinearLayout
    private lateinit var linear12: LinearLayout
    private lateinit var linear14: LinearLayout
    private lateinit var linear15: LinearLayout
    private lateinit var linear16: LinearLayout
    private lateinit var linear17: LinearLayout
    private lateinit var linear18: LinearLayout
    private lateinit var linear19: LinearLayout
    private lateinit var linear5: LinearLayout
    private lateinit var linear6: LinearLayout
    private lateinit var linear7: LinearLayout
    private lateinit var page1: LinearLayout
    private lateinit var page2: LinearLayout
    private lateinit var page3: LinearLayout
    private lateinit var page_1button: MaterialButton
    private lateinit var page_1textview: TextView
    private lateinit var page_2button: MaterialButton
    private lateinit var page_2textview: TextView
    private lateinit var page_3button: MaterialButton
    private lateinit var pr1: ImageView
    private lateinit var pr2: ImageView
    private lateinit var pr3: ImageView
    private var t: TimerTask? = null
    private val _timer = Timer()
    private var cachePath = ""
    private var finalpath = ""
    private val i = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_activity_welcome) // Replace with actual layout if different
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        linear1 = findViewById(R.id.linear1)
        page1 = findViewById(R.id.page1)
        page2 = findViewById(R.id.page2)
        page3 = findViewById(R.id.page3)
        linear5 = findViewById(R.id.linear5)
        linear6 = findViewById(R.id.linear6)
        linear17 = findViewById(R.id.linear17)
        linear7 = findViewById(R.id.linear7)
        page_1textview = findViewById(R.id.page_1textview)
        pr1 = findViewById(R.id.pr1)
        pr2 = findViewById(R.id.pr2)
        pr3 = findViewById(R.id.pr3)
        page_1button = findViewById(R.id.page_1button)
        linear10 = findViewById(R.id.linear10)
        linear11 = findViewById(R.id.linear11)
        linear18 = findViewById(R.id.linear18)
        linear12 = findViewById(R.id.linear12)
        page_2textview = findViewById(R.id.page_2textview)
        imageview1 = findViewById(R.id.imageview1)
        imageview2 = findViewById(R.id.imageview2)
        imageview3 = findViewById(R.id.imageview3)
        page_2button = findViewById(R.id.page_2button)
        linear14 = findViewById(R.id.linear14)
        linear15 = findViewById(R.id.linear15)
        linear19 = findViewById(R.id.linear19)
        linear16 = findViewById(R.id.linear16)
        imageview4 = findViewById(R.id.imageview4)
        imageview5 = findViewById(R.id.imageview5)
        imageview6 = findViewById(R.id.imageview6)
        page_3button = findViewById(R.id.page_3button)
        dialog = AlertDialog.Builder(this)
        f = getSharedPreferences("f", MODE_PRIVATE)

        page_1textview.setOnClickListener {
            i.setClass(applicationContext, SplashActivity::class.java)
            startActivity(i)
        }

        page_1button.setOnClickListener {
            page1.visibility = View.GONE
            page2.visibility = View.VISIBLE
        }

        page_2textview.setOnClickListener {
            i.setClass(applicationContext, SplashActivity::class.java)
            startActivity(i)
        }

        page_2button.setOnClickListener {
            page2.visibility = View.GONE
            page3.visibility = View.VISIBLE
        }

        page_3button.setOnClickListener {
            i.setClass(applicationContext, SplashActivity::class.java)
            startActivity(i)
        }
    }

    private fun initializeLogic() {
        when (f.getString("f", "")) {
            "null" -> {
                i.setClass(applicationContext, SplashActivity::class.java)
                startActivity(i)
                finish()
            }
            else -> {
                f.edit().putString("f", "true").apply()
                if (f.getString("f", "") == "true") {
                    f.edit().putString("f", "false").apply()
                }
                if (f.getString("f", "") == "false") {
                    f.edit().putString("f", "null").apply()
                }
            }
        }
    }

    @Deprecated("Use modern alternatives like ViewBinding or Adapter's getCheckedItemPositions logic.")
    fun getCheckedItemPositionsToArray(listView: ListView): ArrayList<Double> {
        val result = ArrayList<Double>()
        val checked = listView.checkedItemPositions
        for (i in 0 until checked.size()) {
            if (checked.valueAt(i)) {
                result.add(checked.keyAt(i).toDouble())
            }
        }
        return result
    }

    @Deprecated("Use TypedValue or resources.getDimension directly.")
    fun getDip(value: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        )
    }

    @Deprecated("Use WindowMetrics from WindowManager for newer APIs.")
    fun getDisplayHeightPixels(): Int = resources.displayMetrics.heightPixels

    @Deprecated("Use WindowMetrics from WindowManager for newer APIs.")
    fun getDisplayWidthPixels(): Int = resources.displayMetrics.widthPixels

    @Deprecated("Use view.x or view.translationX.")
    fun getLocationX(view: View): Int {
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        return loc[0]
    }

    @Deprecated("Use view.y or view.translationY.")
    fun getLocationY(view: View): Int {
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        return loc[1]
    }

    @Deprecated("Use kotlin.random.Random instead.")
    fun getRandom(min: Int, max: Int): Int = Random().nextInt((max - min) + 1) + min

    @Deprecated("Use Snackbar or Log instead for modern UX.")
    fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
