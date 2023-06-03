package app.statest.camerax.database

import android.database.Cursor
import app.statest.camerax.SQLHelper
import app.statest.camerax.model.User

class CameraDataSource(private val sqlHelper: SQLHelper):CameraRepository {
    override fun saveUser(user: User): Boolean {
        try {
            user.apply {
                val insertSQL =
                    ("INSERT INTO USER ( USER_ID, USER_NAME, FACE_USER) " +
                            " VALUES ( '${userId}',"
                            + "'${userName}',"
                            + "'${userFace}')")
                if (insertSQL != "") {
                    val result10: String = sqlHelper.Insert(insertSQL)
                    if (result10 == "true") {
                        return true
                    }
                }
            }
        }catch (e:Exception){
            return false
        }
        return false
    }

    override fun getUser(faceUser:String): User? {
        var mCursor: Cursor? = null
        try {
            val selectSQL =
                "SELECT USER_ID, USER_NAME, FACE_USER FROM USER WHERE USER_FACE = $faceUser "
            mCursor = sqlHelper.Select(selectSQL, null)
            if (mCursor != null && mCursor.count > 0 && mCursor.moveToFirst()) {
                    val user = User()
                    user.userId =    mCursor.getString(0)
                     user.userName = mCursor.getString(1)
                    user.userFace =  mCursor.getString(2)
                    return  user
            }
        } catch (e: Exception) {
            return null
        } finally {
            mCursor?.close()

        }
        return null
    }

    override fun getAllUser(): ArrayList<User>? {
        var mCursor: Cursor? = null
        val users = ArrayList<User>()
        try {
            val selectSQL =
                "SELECT USER_ID, USER_NAME, FACE_USER FROM USER ORDER BY CAST(USER_ID AS INTEGER)"
            mCursor = sqlHelper.Select(selectSQL, null)
            if (mCursor != null && mCursor.count > 0 && mCursor.moveToFirst()) {
                do {
                    val user = User()
                    user.userId =    mCursor.getString(0)
                    user.userName = mCursor.getString(1)
                    user.userFace =  mCursor.getString(2)
                    users.add(user)
                }while (mCursor.moveToNext())
                return  users
            }
        } catch (e: Exception) {
            return null
        } finally {
            mCursor?.close()

        }
        return null
    }
    override fun createUserTable() {
        sqlHelper.Update(
            "CREATE TABLE IF NOT EXISTS  USER (ID TEXT, " +
                    " USER_ID TEXT, " +
                    " USER_NAME TEXT, " +
                    " FACE_USER TEXT, " +
                    " NOTES TEXT )"
        )
    }
}