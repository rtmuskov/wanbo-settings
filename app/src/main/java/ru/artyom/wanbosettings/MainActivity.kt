package ru.artyom.wanbosettings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/**
 * Экран настроек проектора Wanbo T2 Max — замена вкладки «НАСТР.» из лаунчера Zeasn.
 * Каждая плитка открывает системный экран прошивки; если первый вариант недоступен,
 * пробуются запасные.
 */
class MainActivity : Activity() {

    private data class Tile(
        val title: String,
        val icon: Int,
        val targets: List<ComponentName>,
    )

    private fun cn(pkg: String, cls: String) =
        ComponentName(pkg, if (cls.startsWith(".")) pkg + cls else cls)

    private val tvSettings = "com.android.tv.settings"
    private val wanbo = "com.chijimlee.tyylauncher"
    private val wanboMenu = cn(wanbo, ".activity.HomeSetActivity")

    private val rows: List<List<Tile>> by lazy {
        listOf(
            listOf(
                Tile("СЕТЬ", R.drawable.ic_wifi, listOf(
                    cn(tvSettings, ".network.WifiActivity"))),
                Tile("ПРОЕКТОР", R.drawable.ic_projector, listOf(
                    cn(wanbo, ".activity.ProjectModeActivity"), wanboMenu)),
                Tile("ИСТОЧНИК", R.drawable.ic_input, listOf(
                    cn("com.mediatek.wwtv.tvcenter", ".nav.TurnkeyUiMainActivity"))),
                Tile("МЕДИАЦЕНТР", R.drawable.ic_folder, listOf(
                    cn("com.jrm.localmm", ".ui.main.MainActivity"))),
                Tile("ОБНОВЛЕНИЕ", R.drawable.ic_update, listOf(
                    cn(tvSettings, ".update.SystemNetUpdateActivity"),
                    cn(tvSettings, ".update.SystemInformation"),
                    wanboMenu)),
            ),
            listOf(
                Tile("BLUETOOTH", R.drawable.ic_bluetooth, listOf(
                    cn(tvSettings, ".network.BluetoothActivity"),
                    cn(tvSettings, ".accessories.AddAccessoryActivity"))),
                Tile("ПРИ ВКЛЮЧЕНИИ", R.drawable.ic_power, listOf(
                    cn(wanbo, ".activity.BootSourceActivity"), wanboMenu)),
                Tile("ЯЗЫК", R.drawable.ic_language, listOf(
                    cn(tvSettings, ".common.LanguageActivity"))),
                Tile("СИСТЕМА", R.drawable.ic_system, listOf(
                    cn(tvSettings, ".MainActivity"))),
                Tile("ОЧИСТКА", R.drawable.ic_clean, listOf(
                    cn("com.memoryclean.chijimlee", ".activity.MainActivity"),
                    cn("com.memoryclean.chijimlee", ".activity.BootActivity"))),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(56), dp(28), dp(56), dp(28))
        }

        // Верхняя строка: заголовок слева, часы и дата справа
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "НАСТРОЙКИ"
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        })
        header.addView(TextClock(this).apply {
            format24Hour = "EE, d MMM"
            format12Hour = "EE, d MMM"
            setTextColor(0xCCFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), 0, 0, 0)
        })
        root.addView(header)

        // Сетка плиток, по центру по вертикали
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        var first: View? = null
        rows.forEachIndexed { r, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEachIndexed { c, tile ->
                val v = buildTile(tile)
                if (first == null) first = v
                line.addView(v, LinearLayout.LayoutParams(0, dp(190), 1f).apply {
                    if (c > 0) leftMargin = dp(20)
                })
            }
            grid.addView(line, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (r > 0) topMargin = dp(22)
            })
        }
        root.addView(grid, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        first?.post { first?.requestFocus() }
        return root
    }

    private fun buildTile(tile: Tile): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.tile_bg)
            isFocusable = true
            isClickable = true
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        box.addView(ImageView(this).apply {
            setImageResource(tile.icon)
        }, LinearLayout.LayoutParams(dp(64), dp(64)))
        box.addView(TextView(this).apply {
            text = tile.title
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(18), 0, 0)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))
        box.setOnFocusChangeListener { v, hasFocus ->
            val s = if (hasFocus) 1.07f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
        box.setOnClickListener { open(tile) }
        return box
    }

    private fun open(tile: Tile) {
        for (target in tile.targets) {
            try {
                startActivity(Intent().setComponent(target))
                return
            } catch (e: ActivityNotFoundException) {
                // пробуем следующий вариант
            } catch (e: SecurityException) {
                // экран закрыт для сторонних приложений — пробуем следующий
            }
        }
        Toast.makeText(this, "Экран «${tile.title.replace('\n', ' ')}» недоступен",
            Toast.LENGTH_SHORT).show()
    }
}
