package com.rannasta_suomeen.storage

import android.content.Context
import com.rannasta_suomeen.data_classes.RandomizerItem

const val RANDOMIZER_FILE = "random_drinks"
class Randomizer(context: Context): GenericLocalStorage<RandomizerItem>(context, RANDOMIZER_FILE,Array<RandomizerItem>::class.java)