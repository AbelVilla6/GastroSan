class Users {
    var username: String? = null
    var email: String? = null
    var password: String? = null
    var uid: String? = null
    var phone: String? = null
    var address: String? = null


    constructor()
    constructor(uid: String?, username: String?, email: String?, password: String?, phone: String?, address: String?) {
        this.uid = uid
        this.username = username
        this.email = email
        this.password = password
        this.phone = phone
        this.address = address
    }

    constructor(name: String, email: String, password: String, phone: String, address: String){
        this.username = name
        this.email = email
        this.password = password
        this.phone = phone
        this.address = address

    }

    override fun toString(): String {
        return "User(uid='$uid', username='$username', email='$email', password='$password', phone='$phone', address='$address')"
    }

}


