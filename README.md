# itechnika-example-pdf-android
Convert PDF to image and image to PDF on Android

***

### Blog  
***Korean***: https://d2j2logs.blogspot.com/2025/03/convert-pdf-to-image-and-image-to-pdf.html  
***English***: https://d2j2logs-en.blogspot.com/2025/03/convert-pdf-to-image-and-image-to-pdf.html  
***Basaha Indonesia***: https://d2j2logs-id.blogspot.com/2025/03/convert-pdf-to-image-and-image-to-pdf.html  

***

### Run Example

<img src="https://github.com/user-attachments/assets/149300b6-6fe2-4d53-8699-e6e3de1ac1d7" width="25%" align="left">

***Touch the Open button***:  
You can select a PDF file.  
  
***Touch the Save button***:  
It captures the screen, creates a PDF, and saves it in the Downloads folder.  

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

### Convert PDF to image(Bitmap)

```kotlin
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
```

### Convert Images(Bitmap List) to PDF(ByteArray)

```kotlin
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
```
