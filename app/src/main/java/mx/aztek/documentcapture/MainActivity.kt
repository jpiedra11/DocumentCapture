package mx.aztek.documentcapture

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import mx.aztek.documentcapture.GlobalVariables.imagePath
import mx.aztek.documentcapture.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                val bitmap = BitmapFactory.decodeFile(imagePath)

                val matrix = Matrix()
                matrix.postRotate(90F)

                val rotatedBitmap: Bitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )

                var offsetX = rotatedBitmap.width / 6
                var offsetY = rotatedBitmap.height / 5

                val croppedBitmap: Bitmap = Bitmap.createBitmap(
                    rotatedBitmap,
                    offsetX,
                    offsetY,
                    rotatedBitmap.width - 2 * offsetX,
                    rotatedBitmap.height - 2 * offsetY
                )

                val imageView = findViewById<ImageView>(R.id.resultImageView)
                imageView.setImageBitmap(croppedBitmap)

                val encodedString = encodeImage(croppedBitmap)

                Log.i("BASE64", encodedString.toString())
            }
        }

    fun startDocumentCapture(view: View) {

        val intentScan = Intent(this, DocumentCaptureActivity::class.java)
        resultLauncher.launch(intentScan)
    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }
}
