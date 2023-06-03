package app.statest.camerax.database

import app.statest.camerax.model.User

interface CameraRepository {
    fun saveUser(user:User):Boolean
    fun getUser(faceUser:String): User?
    fun getAllUser(): ArrayList<User>?
    fun createUserTable()
}