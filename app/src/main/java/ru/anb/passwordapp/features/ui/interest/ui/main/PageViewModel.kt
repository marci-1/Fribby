package ru.anb.passwordapp.features.ui.interest.ui.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.ViewModel
import ru.anb.passwordapp.features.ui.interest.model.Interest
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class PageViewModel : ViewModel() {

    private val mMap = MutableLiveData<HashMap<String, Any>>()

    private val mInterestList: LiveData<ArrayList<Interest>> = mMap.map { inputMap ->
        val interestsList = ArrayList<Interest>()
        val context = inputMap["context"] as? Context
        val input = inputMap["type"] as? String

        if (context != null && input != null) {
            try {
                val am = context.assets
                val br1: BufferedReader = BufferedReader(InputStreamReader(am.open("$input.txt")))
                val br2: BufferedReader = BufferedReader(InputStreamReader(am.open("${input}_images.txt")))

                var hobby: String?
                while (br1.readLine().also { hobby = it } != null) {
                    val hobbyImage = br2.readLine()
                    val currentInterest = Interest(hobbyImage ?: "", hobby ?: "", input)
                    interestsList.add(currentInterest)
                }
                br1.close()
                br2.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Log.e("PageViewModel", "Context or input type is null")
        }

        interestsList
    }

    fun getmInterestList(): LiveData<ArrayList<Interest>> = mInterestList

    fun setmMap(hashMap: HashMap<String, Any>) {
        mMap.value = hashMap
    }

    fun getmMap(): MutableLiveData<HashMap<String, Any>> = mMap
}