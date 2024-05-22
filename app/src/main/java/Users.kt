class Users {
    var uid: String? = null
    var username: String? = null
    var email: String? = null
    var password: String? = null
    var phone: String? = null
    var address: String? = null
    var profilePic: String? = null
    var suppliers: String? = null // Añadir el campo suppliers

    constructor()

    constructor(uid: String?, username: String?, email: String?, password: String?, phone: String?, address: String?, profilePic: String?, suppliers: String?) {
        this.uid = uid
        this.username = username
        this.email = email
        this.password = password
        this.phone = phone
        this.address = address
        this.profilePic = profilePic
        this.suppliers = suppliers // Inicializar el campo suppliers
    }

    override fun toString(): String {
        return "User(uid='$uid', username='$username', email='$email', password='$password', phone='$phone', address='$address', profilePic='$profilePic', suppliers='$suppliers')"
    }
}
