package com.lilcode.aop.p3.c05.tinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.DIS_LIKE
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.LIKE
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.LIKED_BY
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.MATCH
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.NAME
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.USERS
import com.lilcode.aop.p3.c05.tinder.DBKey.Companion.USER_ID
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
class LikeActivity : AppCompatActivity(), CardStackListener {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference
    private val adapter = CardItemAdapter()
    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }
    private val cardItems = mutableListOf<CardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child(USERS)

        val currentUserDB = userDB.child(getCurrentLoginedUserId())

        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(NAME).value == null) {

                    showNameInputPopup()
                    return
                }

                // ?????? ?????? ??????;
                getUnSelectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        initCardStacView()
        initSignOutButton()
        initMatchedListButton()
    }

    private fun initSignOutButton(){
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initMatchedListButton(){
        val matchedListButton = findViewById<Button>(R.id.matchListButton)
        matchedListButton.setOnClickListener {

            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
    }


    private fun initCardStacView() {

        val cardStackView = findViewById<CardStackView>(R.id.cardStackView)
        cardStackView.layoutManager = manager // ????????? ?????????;
        cardStackView.adapter = adapter //CardStackAdapter()
    }

    private fun getUnSelectedUsers() {

        userDB.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(USER_ID).value != getCurrentLoginedUserId() // ?????? ????????? ?????? ?????????;
                    && snapshot.child(LIKED_BY).child(LIKE).hasChild(getCurrentLoginedUserId()).not() // ?????? ?????? likedBy??? Like??? ?????? ??????;
                    && snapshot.child(LIKED_BY).child(DIS_LIKE).hasChild(getCurrentLoginedUserId()).not()) // ?????? ?????? likeBy??? disLike??? ?????? ??????;
                        // ??? ?????? ????????? ?????? ????????? ?????? ??????
                {
                    val userId = snapshot.child(USER_ID).value.toString()
                    var name = getString(R.string.undecided) // ?????? ????????? ????????? ????????? ?????? ??? ??????.

                    // ????????? ????????? ???????????? ???????????????;
                    if (snapshot.child(NAME).value != null){
                        name = snapshot.child(NAME).value.toString()
                    }

                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // ???????????? ???????????? ??????????????? ??? ????????????;
                cardItems.find {
                    it.userId == snapshot.key
                }?.let {
                    it.name = snapshot.child(NAME).value.toString()
                }

                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun showNameInputPopup() {

        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.write_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                if (editText.text.isEmpty()) { // ??? ???????????? ?????? ????????? (?????? ?????????)
                    showNameInputPopup()
                } else {
                    // ?????? ???????????? ?????? db??? ??????;
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false) // ?????? ??????;
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentLoginedUserId()

        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)

    }



    private fun getCurrentLoginedUserId(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, getString(R.string.not_logined), Toast.LENGTH_SHORT)
                .show()
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }

    private fun like(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst() // ??? ???????????? ????????? ?????? ??????;

        // ???????????? likedBy??? ??????;
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(LIKE)
            .child(getCurrentLoginedUserId())
            .setValue(true)

        // ????????? ??? ????????? ????????????.
        saveMatchIfOtherUserLikedMe(card.userId)


        Toast.makeText(this, "${card.name}?????? Like ???????????????.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun disLike(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst() // ??? ???????????? ????????? ?????? ??????;

        // ???????????? likedBy??? ??????;
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(DIS_LIKE)
            .child(getCurrentLoginedUserId())
            .setValue(true)

        Toast.makeText(this, "${card.name}?????? disLike ???????????????.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun saveMatchIfOtherUserLikedMe(otherUserId: String){
        val otherUserDB = userDB.child(getCurrentLoginedUserId()).child(LIKED_BY)
            .child(LIKE).child(otherUserId) // ???????????? ?????? ????????? ??? ??? ????????? ?????????.

        // ?????? ?????? ?????? ????????? ?????????.
        otherUserDB.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true){ // ???????????? ?????? ???????????? ??????.
                    userDB.child(getCurrentLoginedUserId())
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(otherUserId)
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(getCurrentLoginedUserId())
                        .setValue(true)

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    // <CardStackListener>
    override fun onCardDragging(direction: Direction?, ratio: Float) {

    }

    override fun onCardSwiped(direction: Direction?) {
        when(direction){
            Direction.Right ->{
                like()
            }
            Direction.Left ->{
                disLike()
            }
            else -> Unit
        }
    }

    override fun onCardRewound() {
    }

    override fun onCardCanceled() {
    }

    override fun onCardAppeared(view: View?, position: Int) {
    }

    override fun onCardDisappeared(view: View?, position: Int) {
    }
    // </CardStackListener>

}