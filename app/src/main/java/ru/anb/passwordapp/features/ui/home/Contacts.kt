package ru.anb.passwordapp.features.ui.home

data class Contacts(
    var displayname: String? = null,
    var status: String? = null,
    var country: String? = null, // <- добавлено
    var uid: String? = null
)
