package eizaburo.kotlin.mynumbercardreader

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import jp.co.osstech.libjeid.JeidReader
import jp.co.osstech.libjeid.`in`.INTextAttributes
import jp.co.osstech.libjeid.`in`.INTextFiles
import jp.co.osstech.libjeid.`in`.INTextMyNumber
import jp.co.osstech.libjeid.`in`.INVisualEntries
import jp.co.osstech.libjeid.`in`.INVisualFiles
import jp.co.osstech.libjeid.`in`.INVisualMyNumber

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import jp.co.osstech.libjeid.INVisualAP
import jp.co.osstech.libjeid.InvalidPinException
import android.nfc.tech.IsoDep

import android.nfc.tech.NfcB

import android.content.IntentFilter.MalformedMimeTypeException
import android.view.View
import java.lang.RuntimeException
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import android.widget.ImageView


class MainActivity : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private var txt_name:TextView?=null
    private var txt_address:TextView?=null
    private var txt_birthday:TextView?=null
    private var txt_sex:TextView?=null
    private var editPin:EditText?=null
    private var imgPhoto:ImageView?=null
    private var imgAddr:ImageView?=null
    private var imgName:ImageView?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity","Created")

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // 受け取るIntentを指定
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        // 反応するタグの種類を指定
        techLists = arrayOf(
            arrayOf(android.nfc.tech.NfcB::class.java.name),
            arrayOf(android.nfc.tech.NfcA::class.java.name),
            arrayOf(android.nfc.tech.NfcF::class.java.name),
            arrayOf(android.nfc.tech.NfcV::class.java.name),
            arrayOf(android.nfc.tech.NfcBarcode::class.java.name),
            arrayOf(android.nfc.tech.NdefFormatable::class.java.name),
            arrayOf(android.nfc.tech.Ndef::class.java.name),
            arrayOf(android.nfc.tech.IsoDep::class.java.name))

        mNfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)

        txt_name = findViewById(R.id.txt_name) as TextView
        txt_address = findViewById(R.id.txt_address) as TextView
        txt_birthday = findViewById(R.id.txt_birthday) as TextView
        txt_sex = findViewById(R.id.txt_sex) as TextView
        editPin = findViewById(R.id.edit_pin) as EditText
        imgPhoto=findViewById(R.id.img_photo) as ImageView
        imgName=findViewById(R.id.img_name) as ImageView
        imgAddr=findViewById(R.id.img_addr) as ImageView
    }

    override fun onResume() {
        super.onResume()
        // NFCタグの検出を有効化
        mNfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)

    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // タグのIDを取得
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        println("TAG Discovered")
        try {
            val pin: String = editPin?.getText().toString()
            val reader = JeidReader(tag)
            val ap = reader.selectINTextAP()
            // Text files
            try {
                ap.verifyPin(pin)
            } catch (e: InvalidPinException) {
                if (e.isBlocked) {
                    Log.d("Read", "PINがブロックされています。")
                } else {
                    Log.d("Read", "PINが間違っています。残り回数: " + e.counter)
                }
                return
            }
            val files = ap.readFiles()
            // Filesオブジェクトから券面の表面を取得
            val attrs = files.attributes
            val visualAp= reader.selectINVisualAP()
            try{
                visualAp.verifyPin(pin)
            }catch (e:InvalidPinException){
                if (e.isBlocked) {
                    Log.d("Read", "PINがブロックされています。")
                } else {
                    Log.d("Read", "PINが間違っています。残り回数: " + e.counter)
                }
                return
            }
            // Visual Files
            val visualFiles=visualAp.readFiles()
            val visualEntries=visualFiles.entries
            
            val argb=visualEntries.photoBitmapARGB
            val addrImg = BitmapFactory.decodeByteArray(visualEntries.addr, 0, visualEntries.addr.size)
            val nameImg = BitmapFactory.decodeByteArray(visualEntries.name, 0, visualEntries.name.size)
            val bitmapphoto: Bitmap = Bitmap.createBitmap(
                argb.data,
                argb.width,
                argb.height,
                Bitmap.Config.ARGB_8888
            )
            runOnUiThread {
                // Stuff that updates the UI
                txt_name?.text="氏名　 : " + attrs.name
                txt_address?.text="住所  : " + attrs.addr
                txt_birthday?.text="生年月日: " + attrs.birth
                txt_sex?.text="性別  : " + attrs.sexString
                imgPhoto?.setImageBitmap(bitmapphoto)
                imgAddr?.setImageBitmap(addrImg)
                imgName?.setImageBitmap(nameImg)
            }
        }catch(e:IOException){
            println(e.message)
            Log.e("Read","読み取りエラー")
        }
    }


    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }
}