class Users {
    var username: String? = null
    var email: String? = null
    var password: String? = null
    var uid: String? = null
    var userType = 0

    constructor()
    constructor(uid: String?, username: String?, email: String?, password: String?, userType: Int) {
        var userType = userType
        this.uid = uid
        this.username = username
        this.email = email
        this.password = password
        userType = userType
    }
}

