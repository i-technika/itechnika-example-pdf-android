package com.itechnika.example.pdf

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Insets
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowMetrics
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.itechnika.example.pdf.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    inner class ViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var ivPage: ImageView = itemView.findViewById(R.id.iv_page)

        fun onBind(item: Bitmap) {
            ivPage.setImageBitmap(item)
        }
    }

    inner class ViewPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.cell_pdf, parent, false)
            return ViewPagerHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ViewPagerHolder).onBind(items[position])
        }
    }

    private lateinit var binding: ActivityMainBinding
    private var items = ArrayList<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        @SuppressLint("NotifyDataSetChanged")
        val pdfActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    contentResolver.openFileDescriptor(
                        uri,
                        "r",
                        null
                    )?.use { parcelFileDescriptor ->
                        items.clear()
                        items.addAll(pdf2BitmapArray(parcelFileDescriptor, getScreenWidth()))
                        binding.vp2Pdf.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        binding.btnLoad.setOnClickListener {
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("application/pdf")
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            intent.setAction(Intent.ACTION_GET_CONTENT)
            pdfActivityResultLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            bitmapArray2Pdf(items)?.let { bytes ->
                val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                val filename = "pdf-${sdf.format(Date())}.pdf"
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, filename)
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(file).use { os ->
                    os.write(bytes)
                    os.flush()
                }
                Toast.makeText(
                    this,
                    "$filename has been created in the Downloads folder.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.vp2Pdf.offscreenPageLimit = 1
        binding.vp2Pdf.adapter = ViewPagerAdapter()
    }

    private fun pdf2BitmapArray(fileDescriptor: ParcelFileDescriptor, width: Int = 0) : ArrayList<Bitmap> {
        val array = ArrayList<Bitmap>()
        PdfRenderer(fileDescriptor).use { renderer ->
            for (i: Int in 0..<renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    var bmWidth = page.width
                    var bmHeight = page.height
                    if (width > 0) {
                        bmWidth = width
                        bmHeight = width * page.height  / page.width
                    }
                    val bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    array.add(bitmap)
                }
            }
        }
        return array
    }

    private fun bitmapArray2Pdf(array: ArrayList<Bitmap>) : ByteArray? {
        var bytes: ByteArray?
        val document = PdfDocument()
        val paint = Paint()
        var count = 1
        array.forEach{ bitmap ->
            val pageInfo = PageInfo.Builder(bitmap.width, bitmap.height, count).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            document.finishPage(page)
            count++
        }
        ByteArrayOutputStream().use { os ->
            document.writeTo(os)
            bytes = os.toByteArray()
        }
        document.close()
        return bytes
    }

    private fun getScreenWidth(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }
    }

}
