package com.gastrosan.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.gastrosan.R
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

class ProfileActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar

    private lateinit var occupationTxtView: TextView
    private lateinit var nameTxtView: TextView
    private lateinit var workTxtView: TextView
    private lateinit var emailTxtView: TextView
    private lateinit var phoneTxtView: TextView

    private lateinit var emailImageView: ImageView
    private lateinit var phoneImageView: ImageView

    private lateinit var userImageView: CircleImageView
    private val TAG = this::class.java.name.toUpperCase()
    private lateinit var database: FirebaseDatabase
    private lateinit var mDatabase: DatabaseReference
    private lateinit var userMap: MutableMap<String, String>
    private var email: String? = null
    private var userid: String? = null
    private val USERS = "users"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //receive data from login screen
        val intent = intent
        email = intent.getStringExtra("email")

        val rootRef = FirebaseDatabase.getInstance().reference
        val userRef = rootRef.child(USERS)
        userRef.key?.let { Log.v("USERID", it) }

        occupationTxtView = findViewById(R.id.occupation_textview)
        nameTxtView = findViewById(R.id.name_textview)
        workTxtView = findViewById(R.id.workplace_textview)
        emailTxtView = findViewById(R.id.email_textview)
        phoneTxtView = findViewById(R.id.phone_textview)


        userImageView = findViewById(R.id.user_imageview)
        emailImageView = findViewById(R.id.email_imageview)
        phoneImageView = findViewById(R.id.phone_imageview)


        // Read from the database
        userRef.addValueEventListener(object : ValueEventListener {
            var fname: String? = null
            var profession: String? = null
            var workplace: String? = null
            var phone: String? = null
            var facebook: String? = null
            var twitter: String? = null
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (keyId in dataSnapshot.children) {
                    if (keyId.child("email").value == email) {
                        fname = keyId.child("fullName").getValue(String::class.java)
                        profession = keyId.child("profession").getValue(String::class.java)
                        workplace = keyId.child("workplace").getValue(String::class.java)
                        phone = keyId.child("phone").getValue(String::class.java)
                        facebook = keyId.child("facebook").getValue(String::class.java)
                        twitter = keyId.child("twitter").getValue(String::class.java)
                        break
                    }
                }
                nameTxtView.text = fname
                emailTxtView.text = email
                occupationTxtView.text = profession
                workTxtView.text = workplace
                phoneTxtView.text = phone

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }
}

