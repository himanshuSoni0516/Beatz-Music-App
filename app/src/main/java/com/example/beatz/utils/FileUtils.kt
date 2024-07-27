package com.example.beatz.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.beatz.R
import showToast

import java.io.*
import java.text.DecimalFormat
import java.util.*

private val TAG = "FileUtils"

// configured android:authorities in AndroidManifest (https://developer.android.com/reference/android/support/v4/content/FileProvider)
private const val AUTHORITY =  ".fileprovider"
private const val HIDDEN_PREFIX = "."

private const val DOCUMENTS_DIR = "documents"

/**
 * Checks the file write permission for the opration perform
 */
@Throws(Exception::class)
private fun Context.isFilePermissionGiven(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
    } else
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            throw Exception()
        }
}

/**
 * This function will create file at particular given location
 */
fun Context.createFile(filePath: String): File? {
    return if (isFilePermissionGiven()) {
        return File(filePath).apply {
            createNewFile()
        }
    } else {
        null
    }
}

/**
 * This function will check weather the file is exists or not
 *
 * @param filePath File path for check file exist or not
 */
fun Context.isFileExists(filePath: String): Boolean {
    return if (isFilePermissionGiven()) {
        File(filePath).exists()
    } else {
        false
    }
}

/**
 * This function will delete file at given file path
 * @param filePath File path
 */
fun Context.deleteFile(filePath: String): Boolean {
    return if (isFilePermissionGiven()) {
        File(filePath).delete()
    } else {
        false
    }
}

/**
 * This function will create file in default cache directory
 * @param fileName Name of the file
 */
fun Context.createFileInCache(fileName: String): File {
    val file = File(cacheDir, fileName)
    return if (file.createNewFile()) file else throw NullPointerException()
}

/**
 * This function will create file in cache directory from Bitmap
 * @param bitmap Bitmap which needs to convert in file
 * @param fileName Name of the file
 */
fun Context.createFileInCacheFromBitmap(bitmap: Bitmap, fileName: String): File {
    val file = File(cacheDir, fileName)
    file.createNewFile()

    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream)

    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

    val fileOutputStream = FileOutputStream(file)
    fileOutputStream.apply {
        write(byteArray)
        flush()
        close()
    }
    return file
}

/**
 * This function will create file from Bitmap at given file path
 * @param bitmap Bitmap which needs to convert in file
 * @param filePath Path of the file
 */
fun Context.createFileFromBitmap(bitmap: Bitmap, filePath: String): File {
    return if (isFilePermissionGiven()) {
        val file = File(filePath)
        file.createNewFile()

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)

        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.apply {
            write(byteArray)
            flush()
            close()
        }
        file
    } else {
        throw IOException()
    }
}

/**
 * This function will create bitmap from file
 */
fun File.createBitmapFromFile(): Bitmap {
    if (!exists()) createNewFile()

    val bmOption = BitmapFactory.Options()
    return BitmapFactory.decodeFile(absolutePath, bmOption)
}

/**
 * This function will create file in internal storage
 *
 * @param fileName Name of the file which need to create
 */
fun Context.createFileInInternalStorage(fileName: String): File {
    return File(filesDir, fileName).apply {
        createNewFile()
    }
}

/**
 * This function will create file in external storage
 *
 * @param fileName Name of the file which need to create
 */
fun Context.createFileInExternalStorage(fileName: String): File {
    return File(getExternalFilesDir(null)!!.absolutePath, fileName).apply {
        createNewFile()
    }
}

/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.<br></br>
 * <br></br>
 * Callers should check whether the path is local before assuming it
 * represents a local file.
 *
 * @param uri     The Uri to query.
 * @see .isLocal
 * @see .getFile
 */
fun Context.getPath(uri: Uri): String? {
    // DocumentProvider
    if (DocumentsContract.isDocumentUri(this, uri)) {
        // LocalStorageProvider
        if (uri.isLocalStorageDocument()) {
            // The path is the id
            return DocumentsContract.getDocumentId(uri)
        } else if (uri.isExternalStorageDocument()) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split((":").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]
            var filePath: String? = ""
            if ("primary".equals(type)) {
                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    //getExternalMediaDirs() added in API 21
                    val external = arrayOf(externalMediaDirs as File)
                    if (external.size > 1) {
                        filePath = external[1].absolutePath
                        filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1];
                    }
                } else {
                    filePath = "/storage/" + type + "/" + split[1];
                }
                return filePath;
            }
        } else if (uri.isDownloadsDocument()) {
            val id = DocumentsContract.getDocumentId(uri)
            val fileName = getFileName(uri)
            val cacheDir = getDocumentCacheDir()
            val file = generateFileName(cacheDir, fileName)
            if (id != null && id.startsWith("raw:")) {
                return id.substring(4)
            } else if (id != null && id.startsWith("msf:")) {
                try {
                    contentResolver.openInputStream(uri).use { inputStream ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(10 * 1024) // or other buffer size
                            var read: Int
                            while (inputStream?.read(buffer).also { read = it ?: 0 } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                            return file?.absolutePath
                        }
                    }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
            val contentUriPrefixesToTry =
                arrayOf("content://downloads/public_downloads", "content://downloads/my_downloads")

            for (contentUriPrefix in contentUriPrefixesToTry) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse(contentUriPrefix),
                    id.toLong()
                )
                try {
                    val path = getDataColumn(contentUri, null, null)
                    if (path != null) {
                        return path
                    }
                } catch (ignored: Exception) {
                }
            }

            // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
            var destinationPath: String? = null
            if (file != null) {
                destinationPath = file.absolutePath
                uri.saveFileFromUri(this, destinationPath)
            }
            return destinationPath
        } else if (uri.isMediaDocument()) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split((":").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            return getDataColumn(contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
        // Return the remote address
        return if (uri.isGooglePhotosUri()) {
            uri.lastPathSegment
        } else getDataColumn(uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    return null
}

fun Uri.saveFileFromUri(context: Context, destinationPath: String) {
    var `is`: InputStream? = null
    var bos: BufferedOutputStream? = null
    try {
        `is` = context.contentResolver.openInputStream(this)
        bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
        val buf = ByteArray(1024)
        `is`!!.read(buf)
        do {
            bos.write(buf)
        } while (`is`.read(buf) != -1)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            `is`?.close()
            bos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

/**
 * Gets the extension of a file name, like ".png" or ".jpg".
 *
 * @return Extension including the dot("."); "" if there is no extension;
 * null if uri was null.
 */
fun File.getFileExtension(): String {
    return if (path.lastIndexOf(".") >= 0) return path.substring(path.lastIndexOf(".")) else ""
}

/**
 * Gets the extension of a file name, like ".png" or ".jpg".
 * @param path Path of the file
 * @return Extension including the dot("."); "" if there is no extension;
 * null if uri was null.
 */
fun getFileExtension(path: String): String {
    return if (path.lastIndexOf(".") >= 0) return path.substring(path.lastIndexOf(".")) else ""
}

/**
 * @return Whether the URI is a local one.
 */
fun File.isLocal(): Boolean {
    return !(path.startsWith("http:") || path.startsWith("https:"))
}

/**
 * @return Whether the URI is a local one.
 */
fun String.isLocal(): Boolean {
    return !(this.startsWith("http:") || this.startsWith("https:"))
}

/**
 * to get file uri as per OS version check for pre Marshmallow uri also
 *
 * @param file    file which Uri wants
 * @return file Uri
 */
fun Context.getUri(file: File): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
    } else {
        Uri.fromFile(file)
    }
}

/**
 * Returns the path only (without file name).
 */
fun File.getPathWithoutFileName(): String {
    return if (this.isDirectory) absolutePath else {
        val fileName = this.name
        val filePath = this.absolutePath

        var pathWithOutName = filePath.substring(0, filePath.length - fileName.length)

        if (pathWithOutName.endsWith("/")) pathWithOutName =
            pathWithOutName.substring(0, pathWithOutName.length - 1)

        pathWithOutName
    }
}

/**
 * @return The MIME type for the given file.
 */
fun File.getMimeType(): String {
    val extension = this.getFileExtension()
    return if (extension.isNotEmpty()) {
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.substring(1)).toString()
    } else {
        "application/octet-stream"
    }
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is local.
 */
fun Uri.isLocalStorageDocument(): Boolean {
    return AUTHORITY == authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
fun Uri.isExternalStorageDocument(): Boolean {
    return authority == "com.android.externalstorage.documents"
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
fun Uri.isDownloadsDocument(): Boolean {
    return "com.android.providers.downloads.documents" == authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
fun Uri.isMediaDocument(): Boolean {
    return "com.android.providers.media.documents" == authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Photos.
 */
fun Uri.isGooglePhotosUri(): Boolean {
    return "com.google.android.apps.photos.content" == authority
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param uri           The Uri to query.
 * @param selection     (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
fun Context.getDataColumn(uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
    val column = MediaStore.Files.FileColumns.DATA
    val projection = arrayOf(column)
    contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(column))
            }
        }
    return null
}

/**
 * Get the file size in a human-readable string.
 */
fun File.getReadableFileSize(addSuffix: Boolean = true): Double {
    val bytesInKilobytes = 1024
//    val dec = DecimalFormat("###.#")
    val kilobytes = " KB"
    val megabytes = " MB"
    val gigabytes = " GB"
    var fileSize = 0f
    var suffix = kilobytes

    when {
        length() > bytesInKilobytes -> {
            fileSize = (length() / bytesInKilobytes).toFloat()
            when {
                fileSize > bytesInKilobytes -> {
                    fileSize /= bytesInKilobytes
                    when {
                        fileSize > bytesInKilobytes -> {
                            fileSize /= bytesInKilobytes
                            suffix = gigabytes
                        }
                        else -> suffix = megabytes
                    }
                }
            }
        }
    }
    return if (addSuffix) {
//        dec.format(fileSize.toDouble()) + suffix
        fileSize.toDouble()
    } else {
//        dec.format(fileSize.toDouble())
        fileSize.toDouble()
    }
}

/**
 * Creates View intent for given file
 *
 * @return The intent for viewing file
 */
fun Context.getViewIntent(file: File): Intent {
    //Uri uri = Uri.fromFile(file);
    val uri = getUri(file)
    val intent = Intent(Intent.ACTION_VIEW)
    val url = toString()
    when {
        url.contains(".doc") || url.contains(".docx") -> // Word document
            intent.setDataAndType(uri, "application/msword")
        url.contains(".pdf") -> // PDF file
            intent.setDataAndType(uri, "application/pdf")
        url.contains(".ppt") || url.contains(".pptx") -> // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint")
        url.contains(".xls") || url.contains(".xlsx") -> // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel")
        url.contains(".zip") || url.contains(".rar") -> // WAV audio file
            intent.setDataAndType(uri, "application/x-wav")
        url.contains(".rtf") -> // RTF file
            intent.setDataAndType(uri, "application/rtf")
        url.contains(".wav") || url.contains(".mp3") -> // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav")
        url.contains(".gif") -> // GIF file
            intent.setDataAndType(uri, "image/gif")
        url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png") -> // JPG file
            intent.setDataAndType(uri, "image/jpeg")
        url.contains(".txt") -> // Text file
            intent.setDataAndType(uri, "text/plain")
        url.contains(".3gp") || url.contains(".mpg") || url.contains(".mpeg") || url.contains(
            ".mpe"
        ) || url.contains(".mp4") || url.contains(".avi") -> // Video files
            intent.setDataAndType(uri, "video/*")
        else -> intent.setDataAndType(uri, "*/*")
    }

    return intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

fun Context.getDownloadDir(): File? {
    return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
}

fun Context.getDocumentCacheDir(): File {
    val dir = File(cacheDir, DOCUMENTS_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }

    return dir
}

fun generateFileName(directory: File, name: String?): File? {
    if (name.isNullOrEmpty()) {
        return null
    }
    var file = File(directory, name)
    when {
        file.exists() -> {
            var fileName = name
            var extension = ""
            val dotIndex = name.lastIndexOf('.')
            when {
                dotIndex > 0 -> {
                    fileName = name.substring(0, dotIndex)
                    extension = name.substring(dotIndex)
                }
            }

            var index = 0
            while (file.exists()) {
                index++
                file = File(directory, "$fileName($index)$extension")
            }
        }
    }
    try {
        when {
            !file.createNewFile() -> return null
        }
    } catch (e: IOException) {
        return null
    }
    return file
}

fun String.readBytesFromFile(): ByteArray? {
    var fileInputStream: FileInputStream? = null
    var bytesArray: ByteArray? = null

    try {
        val file = File(this)
        bytesArray = ByteArray(file.length().toInt())

        //read file into bytes[]
        fileInputStream = FileInputStream(file)
        fileInputStream.read(bytesArray)

    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (fileInputStream != null) {
            try {
                fileInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return bytesArray
}

@Throws(IOException::class)
fun Context.createTempImageFile(fileName: String, extension: String = ".jpg"): File {
    // Create an image file name
    val storageDir = File(cacheDir, DOCUMENTS_DIR)
    return File.createTempFile(fileName, extension, storageDir)
}

fun Context.getFileName(uri: Uri): String? {
    val mimeType = contentResolver.getType(uri)
    var filename: String? = null

    when (mimeType) {
        null -> {
            val path = getPath(uri)
            filename = when (path) {
                null -> toString().getFileName()
                else -> {
                    val file = File(path)
                    file.name
                }
            }
        }
        else -> {
            val returnCursor = contentResolver.query(uri, null, null, null, null)
            when {
                returnCursor != null -> {
                    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    returnCursor.moveToFirst()
                    filename = returnCursor.getString(nameIndex)
                    returnCursor.close()
                }
            }
        }
    }
    return filename
}


fun Context.getFile(uri: Uri): File? {
    val path = getPath(uri)
    return if (path!!.isLocal()) File(path) else null
}

/**
 * Get File Name
 *
 * @return String of File name
 */
fun String.getFileName(): String {
    return if (isLocal()) {
        val index = lastIndexOf('/')
        substring(index + 1)
    } else {
        ""
    }
}

/**
 * Get file size in Byte.
 *
 * @return Int
 */
fun File.getFileSize(): Long {
    return if (isLocal()) {
        length()
    } else {
        0L
    }
}

fun File.validateFileSize(): Boolean {
    return length() < (5 * 1024 * 1024)
}

fun Context.directoryExists(uri: Uri): Boolean {
    val directoryPath = this.uriToFilePath(uri)
    return directoryPath?.let {
        val directory = File(it)
        directory.exists() && directory.isDirectory
    } ?: false
}

private fun Context.uriToFilePath(uri: Uri): String? {
    val cursor = this.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val columnIndex = it.getColumnIndexOrThrow("_data")
        if (it.moveToFirst()) {
            return it.getString(columnIndex)
        }
    }
    return null
}



// size in bytes
@Throws(java.lang.Exception::class)
fun Context.getFileSize(uri: Uri): Long {
    val cursor: Cursor? = this.contentResolver.query(uri, null, null, null, null)
    var size: Long = -1
    if (cursor != null && cursor.moveToFirst()) {
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        size = cursor.getLong(sizeIndex)
        cursor.close()
    }
    return size
}

fun Context.validateFileSize(
    uri: Uri? = null,
    filePath: String? = null,
    fileSize: Int = 5
): Boolean {
    try {
        var fileSize: Double? = null
        // Get the file size using the URI
        fileSize = if (filePath != null) {
            val file = File(filePath)
            file.getReadableFileSize(false)
        } else
            this.getFileSize(uri!!).toDouble()

        // Define the maximum allowed file size (in bytes)
        val maxFileSize = (fileSize * 1024 * 1024).toDouble() // 5 MB, for example

        // Check if the file size is within the allowed range
        return if (fileSize!! <= maxFileSize) {
            // File size is within the allowed range, proceed with the media
            Log.d("MainActivity", "File size is acceptable: $fileSize bytes")
            true
        } else {
            this.showToast(messageResId = R.string.upload_jpeg_png_pdf_max_size_5_mb)
            // File size exceeds the allowed range, handle the situation
            false
        }
    } catch (e: java.lang.Exception) {
        return false
        // Handle any exceptions that may occur
        Log.e("MainActivity", "Error getting file size: ", e)
    }
}

fun compressImage(context: Context, uri: Uri): File? {
    val contentResolver: ContentResolver = context.contentResolver
    val file: File = File(getRealPathFromURI(context, uri))
    val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

    try {
        val inputStream: InputStream = FileInputStream(file)
        val outputStream: OutputStream = FileOutputStream(compressedFile)

        // Compress the image
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } > 0) {
            outputStream.write(buffer, 0, bytesRead)
        }

        inputStream.close()
        outputStream.close()

        return compressedFile
    } catch (e: IOException) {
        Log.e("CompressImage", "Error compressing image: ${e.message}")
    }
    return null
}

// Function to get the real path from URI
private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
    var filePath: String? = null
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(contentUri, projection, null, null, null)
    cursor?.let {
        it.moveToFirst()
        val columnIndex: Int = it.getColumnIndex(projection[0])
        filePath = it.getString(columnIndex)
        it.close()
    }
    return filePath
}
