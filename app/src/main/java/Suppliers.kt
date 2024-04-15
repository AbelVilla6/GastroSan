class Suppliers {
    var id: String? = null
    var name: String? = null
    var logoUrl: String? = null
    var contactName: String? = null
    var contactPhone: String? = null

    constructor()

    constructor(id: String?, name: String?, logoUrl: String?, contactName: String?, contactPhone: String?) {
        this.id = id
        this.name = name
        this.logoUrl = logoUrl
        this.contactName = contactName
        this.contactPhone = contactPhone
    }

    override fun toString(): String {
        return "Provider(id='$id', name='$name', logoUrl='$logoUrl', contactName='$contactName', contactPhone='$contactPhone')"
    }
}