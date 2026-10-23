package ir.shahab.keyboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView

private val FA_ROWS = listOf(
    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ"),
    listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
    listOf("ظ", "ط", "ز", "ر", "ذ", "د", "ئ", "و", "پ", "ژ")
)
private val FA_SHIFT_ROWS = listOf(
    listOf("\u064B", "\u064C", "\u064D", "\u064E", "\u064F", "\u0650", "\u0651", "\u0652", "ء", "أ", "إ", "ؤ"),
    listOf("آ", "ة", "ي", "ك", "«", "»", "؛", "٪", "×", "÷", "ـ"),
    listOf("؟", "،", "٫", "٬", "+", "-", "=", "/", "\\", "_")
)
private val EN_ROWS = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)
private val SYM_ROWS = listOf(
    listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
    listOf("-", "_", "=", "+", "[", "]", "{", "}", "/", "\\"),
    listOf("،", "؛", "؟", "«", "»", ":", ";", "'", "\"", "~")
)
private val EMOJIS = listOf(
    "😀", "😂", "🤣", "😍", "🥰", "😘", "😎", "🤔",
    "😢", "😭", "😡", "😅", "😁", "😉", "😊", "🥺",
    "👍", "👎", "🙏", "👏", "💪", "👌", "🤝", "✌️",
    "❤️", "💔", "🔥", "🎉", "🌹", "⭐", "✅", "❌"
)
private const val KEY_HEIGHT_DP = 46

class SmartKeyboardService : InputMethodService() {

    private enum class Mode { FA, EN, SYM, EMOJI }

    private var root: LinearLayout? = null
    private var mode = Mode.FA
    private var lastLang = Mode.FA
    private var shift = false
    private var caps = false
    private var vibrate = true
    private var theme: KbTheme = THEMES[0]
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateInputView(): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val pad = dp(4)
        r.setPadding(pad, pad, pad, pad)
        r.setOnApplyWindowInsetsListener { v, ins ->
            v.setPadding(pad, pad, pad, pad + bottomInset(ins))
            ins
        }
        root = r
        render()
        return r
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        mode = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) Mode.SYM else lastLang
        shift = false
        caps = false
        render()
    }

    // ---------- building the UI ----------

    private fun render() {
        val r = root ?: return
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        theme = THEMES[prefs.getInt("theme", 0).coerceIn(0, THEMES.size - 1)]
        vibrate = prefs.getBoolean("vibrate", true)
        r.setBackgroundColor(theme.bg)
        r.removeAllViews()
        if (mode == Mode.EMOJI) buildEmoji(r) else buildLetters(r)
    }

    private fun buildLetters(r: LinearLayout) {
        val fa = lastLang == Mode.FA
        val digits = if (fa) "۱۲۳۴۵۶۷۸۹۰" else "1234567890"
        r.addView(row(digits.map { charKey(it.toString(), 1f, 16f) }))

        val rows: List<List<String>> = when (mode) {
            Mode.SYM -> SYM_ROWS
            Mode.EN -> EN_ROWS.map { line -> line.map { if (shift) it.uppercase() else it } }
            else -> if (shift) FA_SHIFT_ROWS else FA_ROWS
        }
        rows.forEachIndexed { i, keys ->
            val views = ArrayList<View>()
            if (i == 2 && mode != Mode.SYM) views.add(shiftKey())
            keys.forEach { views.add(charKey(it, 1f, 20f)) }
            if (i == 2) views.add(deleteKey())
            r.addView(row(views))
        }

        val b = ArrayList<View>()
        val symLabel = if (mode == Mode.SYM) (if (fa) "ابج" else "ABC") else "!#1"
        b.add(specialKey(symLabel, 1.3f, 16f) {
            mode = if (mode == Mode.SYM) lastLang else Mode.SYM
            render()
        })
        b.add(specialKey("🌐", 1f, 18f, onLong = { showPicker() }) {
            lastLang = if (lastLang == Mode.FA) Mode.EN else Mode.FA
            mode = lastLang
            shift = false
            caps = false
            render()
        })
        b.add(specialKey("😀", 1f, 18f) { mode = Mode.EMOJI; render() })
        b.add(makeKey(if (fa) "فارسی" else "English", 3.6f, theme.key, theme.text, 14f) { commit(" ") })
        b.add(specialKey("نیم‌فاصله", 1.3f, 10f) { commit("\u200C") })
        b.add(specialKey(if (fa) "،" else ",", 0.8f, 20f) { commit(if (fa) "،" else ",") })
        b.add(specialKey(".", 0.8f, 20f) { commit(".") })
        b.add(makeKey("↵", 1.3f, theme.accent, theme.text, 20f) { enter() })
        r.addView(row(b))
    }

    private fun buildEmoji(r: LinearLayout) {
        EMOJIS.chunked(8).forEach { chunk ->
            r.addView(row(chunk.map { e -> makeKey(e, 1f, theme.key, theme.text, 22f) { commit(e) } }))
        }
        val b = ArrayList<View>()
        b.add(specialKey(if (lastLang == Mode.FA) "ابج" else "ABC", 1.5f, 16f) { mode = lastLang; render() })
        b.add(makeKey(" ", 5f, theme.key, theme.text, 14f) { commit(" ") })
        b.add(deleteKey())
        r.addView(row(b))
    }

    private fun row(views: List<View>): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        l.layoutDirection = View.LAYOUT_DIRECTION_LTR
        l.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        views.forEach { l.addView(it) }
        return l
    }

    private fun charKey(text: String, weight: Float, size: Float): View {
        val isMark = text.isNotEmpty() && Character.getType(text[0]) == Character.NON_SPACING_MARK.toInt()
        val shown = if (isMark) "\u25CC$text" else text
        return makeKey(shown, weight, theme.key, theme.text, size) {
            commit(text)
            if (shift && !caps && mode != Mode.SYM) {
                shift = false
                render()
            }
        }
    }

    private fun specialKey(label: String, weight: Float, size: Float, onLong: (() -> Unit)? = null, onClick: () -> Unit): View =
        makeKey(label, weight, theme.special, theme.text, size, false, onLong, onClick)

    private fun shiftKey(): View {
        val bg = if (shift) theme.accent else theme.special
        return makeKey(if (caps) "⇪" else "⇧", 1.4f, bg, theme.text, 20f, false, {
            caps = !caps
            shift = caps
            render()
        }) {
            caps = false
            shift = !shift
            render()
        }
    }

    private fun deleteKey(): View =
        makeKey("⌫", 1.4f, theme.special, theme.text, 20f, true, null) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) }

    private fun makeKey(
        label: String, weight: Float, bg: Int, fg: Int, size: Float,
        repeat: Boolean = false, onLong: (() -> Unit)? = null, onClick: () -> Unit
    ): View {
        val tv = TextView(this)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(fg)
        tv.textSize = size
        tv.setSingleLine(true)
        tv.background = rounded(bg)
        val lp = LinearLayout.LayoutParams(0, dp(KEY_HEIGHT_DP), weight)
        lp.setMargins(dp(2), dp(3), dp(2), dp(3))
        tv.layoutParams = lp

        val repeater = object : Runnable {
            override fun run() {
                onClick()
                handler.postDelayed(this, 45)
            }
        }
        tv.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = rounded(theme.accent)
                    if (vibrate) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (repeat) {
                        onClick()
                        handler.postDelayed(repeater, 380)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = rounded(bg)
                    if (repeat) handler.removeCallbacks(repeater)
                }
            }
            repeat
        }
        if (!repeat) {
            tv.setOnClickListener { onClick() }
            if (onLong != null) tv.setOnLongClickListener { onLong(); true }
        }
        return tv
    }

    // ---------- actions ----------

    private fun commit(t: String) {
        currentInputConnection?.commitText(t, 1)
    }

    private fun enter() {
        val ei = currentInputEditorInfo
        val opts = ei?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        val noAction = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (ei == null || noAction || action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            currentInputConnection?.performEditorAction(action)
        }
    }

    private fun showPicker() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun rounded(color: Int): GradientDrawable {
        val d = GradientDrawable()
        d.cornerRadius = dp(8).toFloat()
        d.setColor(color)
        return d
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
