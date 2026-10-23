package ir.shahab.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setPadding(pad, pad, pad, pad)
        root.setOnApplyWindowInsetsListener { v, ins ->
            v.setPadding(pad, pad + topInset(ins), pad, pad + bottomInset(ins))
            ins
        }

        fun label(t: String, size: Float): TextView {
            val tv = TextView(this)
            tv.text = t
            tv.textSize = size
            tv.setPadding(0, pad / 2, 0, pad / 2)
            return tv
        }

        root.addView(label("کیبورد هوشمند فارسی", 24f))
        root.addView(label("۱) کیبورد را فعال کنید.\n۲) آن را به‌عنوان کیبورد فعلی انتخاب کنید.", 16f))

        val b1 = Button(this)
        b1.text = "فعال‌سازی کیبورد"
        b1.setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        root.addView(b1)

        val b2 = Button(this)
        b2.text = "انتخاب کیبورد"
        b2.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        root.addView(b2)

        root.addView(label("تم کیبورد", 18f))
        val group = RadioGroup(this)
        val saved = prefs.getInt("theme", 0)
        THEME_NAMES.forEachIndexed { i, name ->
            val rb = RadioButton(this)
            rb.id = 1000 + i
            rb.text = name
            rb.isChecked = (i == saved)
            group.addView(rb)
        }
        group.setOnCheckedChangeListener { _, id ->
            prefs.edit().putInt("theme", id - 1000).apply()
        }
        root.addView(group)

        val sw = Switch(this)
        sw.text = "لرزش هنگام تایپ"
        sw.isChecked = prefs.getBoolean("vibrate", true)
        sw.setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("vibrate", on).apply() }
        root.addView(sw)

        root.addView(label("برای امتحان اینجا بنویسید:", 16f))
        val et = EditText(this)
        et.hint = "متن آزمایشی"
        root.addView(et)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }
}
