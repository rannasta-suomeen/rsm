package com.rannasta_suomeen.popup_windows


import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.storage.Settings

private val BASE_TAGS = listOf(R.string.cocktail,R.string.shot, R.string.punch, R.string.unmakeble, R.string.unmakebleGrocery, R.string.tagless)

abstract class PopupFilterBase(
    protected val activity: Activity,
    @LayoutRes
    private val layout: Int,
) {
    protected var window: PopupWindow

    protected inline fun <reified T : Any> multiOptionDialog(
        items: List<T>,
        crossinline displayFun: (T) -> String,
        alreadyCheckedItems: List<T>,
        title: String,
        crossinline endfun: (Array<T>) -> Unit
    ): AlertDialog {
        val dialog = AlertDialog.Builder(activity)

        dialog.setTitle(title)
        val booleanArray = items.map { alreadyCheckedItems.contains(it) }.toBooleanArray()
        dialog.setMultiChoiceItems(
            items.map(displayFun).toTypedArray(),
            booleanArray
        ) { _, _, _ -> }
        dialog.setPositiveButton("Ok") { x, y ->
            endfun(items.zip(booleanArray.toTypedArray()).filter { it.second }.map { it.first }
                .toTypedArray())
        }
        dialog.setNegativeButton("Cancel") { _, _ -> }
        return dialog.create()
    }

    init {
        val view = activity.layoutInflater.inflate(layout, null)
        window = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.isFocusable = true
    }

    fun show(parent: View) {
        bind(window.contentView)
        window.showAtLocation(parent, Gravity.TOP, 0, 0)
    }

    /**
     * Binds the layout to logic
     */
    abstract fun bind(view: View)
}

class PopupFilter(
    activity: Activity,
    private val updateFun: () -> Unit
):PopupFilterBase(activity, R.layout.popup_product_filer) {

    class Settings(
        var name: String?,
        var abvMin: Double?,
        var abvMax: Double?,
        var volMin: Int?,
        var volMax: Int?,
        var priceMin: Double?,
        var priceMax: Double?,
        var retailers: List<Retailer>
    ){
        var selectedSubcategory: Array<Subcategory> = Subcategory.values()
    }

    private var settings: Settings = Settings(null,null, null,null,null,null, null, listOf(Retailer.alko, Retailer.superalko))

    override fun bind(view:View) {
        /**
         * Shortens my time to write
         */
        fun<T: View> t(@LayoutRes id: Int): T{
            return view.findViewById<T>(id)
        }
        fun ed(id: Int): EditText{
            return t(id)
        }

        val nameSelect = ed(R.id.editTextTextProductName)

        val abvMin = ed(R.id.editTextAbvMin)
        val abvMax = ed(R.id.editTextAbvMax)

        val volMin = ed(R.id.editTextVolumeMin)
        val volMax = ed(R.id.editTextVolumeMax)

        val priceMin = ed(R.id.editTextPriceMin)
        val priceMax = ed(R.id.editTextPriceMax)


        val buttonRetailer: Button = t(R.id.buttonRetailer)
        val buttonDismiss: Button = t(R.id.buttonProductFilterOk)

        val buttonCategory: Button = view.findViewById(R.id.buttonCategory)
        var categoryDialog = multiOptionDialog(Subcategory.values().toList(),{it.toString()},settings.selectedSubcategory.toList(),"Choose categories"){settings.selectedSubcategory = it}

        val retailerDialog = multiOptionDialog(Retailer.values().toList(),{it.toString()},settings.retailers,"Choose Retailers"){settings.retailers = it.toList()}

        buttonCategory.setOnClickListener {
            categoryDialog.show()
        }
        buttonCategory.setOnLongClickListener {
            if (settings.selectedSubcategory.isNotEmpty()){
                settings.selectedSubcategory = arrayOf()
                Toast.makeText(activity, "Deselected all categories",Toast.LENGTH_SHORT).show()
            } else {
                settings.selectedSubcategory = Subcategory.values()
                Toast.makeText(activity, "Selected all categories",Toast.LENGTH_SHORT).show()
            }
            categoryDialog = multiOptionDialog(Subcategory.values().toList(),{it.toString()},settings.selectedSubcategory.toList(),"Choose categories"){settings.selectedSubcategory = it}
            updateFun
            true
        }
        buttonRetailer.setOnClickListener {
            retailerDialog.show()
        }

        buttonDismiss.setOnClickListener {
            this.settings.name = nameSelect.text.toString()
            this.settings.abvMin = abvMin.text.toString().toDoubleOrNull()
            this.settings.abvMax = abvMax.text.toString().toDoubleOrNull()

            this.settings.volMin = volMin.text.toString().toIntOrNull()
            this.settings.volMax = volMax.text.toString().toIntOrNull()

            this.settings.priceMin = priceMin.text.toString().toDoubleOrNull()
            this.settings.priceMax = priceMax.text.toString().toDoubleOrNull()

            updateFun()
            window.dismiss()
        }
    }

    fun filter(list: List<Product>): List<Product>{
        val mutList = list.toMutableList()

        fun<T> quickRemove(cond: T?, predicate: (Product) -> Boolean){
            if (cond != null){
                mutList.removeAll(predicate)
            }
        }

        quickRemove(settings.name){!it.name.contains(settings.name!!,true)}
        quickRemove(settings.abvMin){it.abv<settings.abvMin!!}
        quickRemove(settings.abvMax){it.abv>settings.abvMax!!}

        quickRemove(settings.priceMin){it.price<settings.priceMin!!}
        quickRemove(settings.priceMax){it.price>settings.priceMax!!}

        quickRemove(settings.volMin){it.volumeCl()<settings.volMin!!}
        quickRemove(settings.volMax){it.volumeCl()>settings.volMax!!}

        return mutList.filter { settings.selectedSubcategory.contains(from(it.subcategory_id)) }.filter { settings.retailers.contains(it.retailer) }
    }
}

class PopupDrinkFilter(activity: Activity, private val updateFun: () -> Unit, private val drinksList: List<DrinkTotal>,private val appSettings: Settings): PopupFilterBase(activity, R.layout.popup_drink_filer){
    private class FilterSettings(
        var searchedName: String,
        var allowedTags: List<String>,
        var abvMin: Double?,
        var abvMax: Double?,
        var volMin: Int?,
        var volMax: Int?,
        var priceMin: Double?,
        var priceMax: Double?,
        var aerMin: Double?,
        var aerMax: Double?,
        var whiteListedIngredients: List<GeneralIngredient>,
    ){
        fun filterTag(mutableList: MutableList<DrinkTotal>, ownedIngredients: List<GeneralIngredient>, allTags: List<String>){
            val disAllowedTags = allTags.toMutableList()
            disAllowedTags.removeAll{allowedTags.contains(it)}
            fun checkTags(drink: DrinkTotal):Boolean{
                for (i in disAllowedTags){
                    if(when (i){
                        // TODO: Make this work without hardcoded strings
                        "Cocktail" -> drink.drink.type == DrinkType.cocktail
                        "Shot" -> drink.drink.type == DrinkType.shot
                        "Punch" -> drink.drink.type == DrinkType.punch
                        "Missing Alcohol" -> drink.missingIngredientsAlcoholic(ownedIngredients) != 0
                        "Missing Groceries" -> drink.missingIngredientsNonAlcoholic(ownedIngredients) != 0
                        "Tagless" -> drink.drink.tag_list.isEmpty()
                        else -> {
                            drink.drink.tag_list.contains(i)
                        }
                    }){
                        return false
                    }
                }
                return true
            }
            mutableList.removeAll {
                !checkTags(it)
            }
        }
    }
    private fun findAllTags(): List<String>{
        val tagMap = mutableSetOf<String>()
        drinksList.forEach {tagMap.addAll(it.drink.tag_list) }
        return tagMap.toList()
    }

    private fun getAllIngredientsFromDrink(): List<GeneralIngredient>{
        val ingredientMap = hashMapOf<Int, Pair<Int,GeneralIngredient>>()
        drinksList.forEach {
            it.ingredients.recipeParts.forEach {
                val i = it.ingredient
                when (val t = ingredientMap[i.id]){
                    null -> ingredientMap[i.id] = Pair(1,i)
                    else -> {
                        val amount = t.first
                        ingredientMap[i.id] = Pair(amount+1, i)
                    }
                }
            }
        }
        return ingredientMap.toList().map {it.second}.sortedBy { -it.first }.map { it.second }
    }

    private fun gsl(list: List<Int>):List<String>{
        return list.map { activity.getString(it)}
    }

    private val tags = gsl(BASE_TAGS) + findAllTags()
    private val ingredients = getAllIngredientsFromDrink()

    private fun createDefault():FilterSettings{
        return FilterSettings(
            "",
            tags,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ingredients
        )
    }
    private val settings = createDefault()
    override fun bind(view: View) {
        fun<T: View> t(
            @LayoutRes
            id: Int): T{
            return view.findViewById<T>(id)
        }
        fun ed(@LayoutRes id: Int): EditText{
            return t(id)
        }
        val nameSelect = ed(R.id.editTextTextProductName)

        val abvMin = ed(R.id.editTextAbvMin)
        val abvMax = ed(R.id.editTextAbvMax)

        val volMin = ed(R.id.editTextVolumeMin)
        val volMax = ed(R.id.editTextVolumeMax)

        val priceMin = ed(R.id.editTextPriceMin)
        val priceMax = ed(R.id.editTextPriceMax)

        val aerMin = ed(R.id.editTextAerMin)
        val aerMax = ed(R.id.editTextAerMax)

        val buttonDismiss: Button = t(R.id.buttonProductFilterOk)
        val buttonTags: Button = t(R.id.buttonTags)
        val buttonIngredients: Button = t(R.id.buttonIngredients)
        var tagDialogue = multiOptionDialog(tags,{it},settings.allowedTags,"Select Criteria"){settings.allowedTags = it.toList()}
        var ingredientDialog = multiOptionDialog(ingredients,{it.name}, settings.whiteListedIngredients,"Select Allowed Ingredients"){settings.whiteListedIngredients = it.toList()}

        fun updateDialogs(){
            tagDialogue = multiOptionDialog(tags,{it},settings.allowedTags,"Select Criteria"){settings.allowedTags = it.toList()}
            ingredientDialog = multiOptionDialog(ingredients,{it.name}, settings.whiteListedIngredients,"Select Allowed Ingredients"){settings.whiteListedIngredients = it.toList()}
        }

        buttonTags.setOnClickListener { tagDialogue.show() }
        buttonTags.setOnLongClickListener {
            if (settings.allowedTags.isNotEmpty()){
                settings.allowedTags = listOf()
                Toast.makeText(activity, "Deselected all tags",Toast.LENGTH_SHORT).show()
            } else {
                settings.allowedTags = tags
                Toast.makeText(activity, "Selected all tags",Toast.LENGTH_SHORT).show()
            }
            updateDialogs()
            updateFun
            true
        }
        buttonIngredients.setOnClickListener { ingredientDialog.show() }
        buttonIngredients.setOnLongClickListener {
            if (settings.whiteListedIngredients.isNotEmpty()){
                settings.whiteListedIngredients = listOf()
                Toast.makeText(activity, "Deselected all ingredients",Toast.LENGTH_SHORT).show()
            } else {
                settings.whiteListedIngredients = ingredients
                Toast.makeText(activity, "Selected all ingredients",Toast.LENGTH_SHORT).show()
            }
            updateDialogs()
            updateFun
            true
        }

        buttonDismiss.setOnClickListener {
            this.settings.searchedName = nameSelect.text.toString()
            this.settings.abvMin = abvMin.text.toString().toDoubleOrNull()
            this.settings.abvMax = abvMax.text.toString().toDoubleOrNull()

            this.settings.volMin = volMin.text.toString().toIntOrNull()
            this.settings.volMax = volMax.text.toString().toIntOrNull()

            this.settings.priceMin = priceMin.text.toString().toDoubleOrNull()
            this.settings.priceMax = priceMax.text.toString().toDoubleOrNull()

            this.settings.aerMin = aerMin.text.toString().toDoubleOrNull()
            this.settings.aerMax = aerMax.text.toString().toDoubleOrNull()

            updateFun()
            window.dismiss()
        }
    }
    fun filter(drinklist: List<DrinkTotal>, ownedIngredients: List<GeneralIngredient>): List<DrinkTotal>{
        val mutList = drinklist.toMutableList()

        fun<T> quickRemove(cond: T?, predicate: (DrinkTotal) -> Boolean){
            if (cond != null){
                mutList.removeAll(predicate)
            }
        }

        quickRemove(settings.searchedName){!it.drink.name.contains(settings.searchedName,true)}
        quickRemove(settings.abvMin){it.drink.abv_average<settings.abvMin!!}
        quickRemove(settings.abvMax){it.drink.abv_average>settings.abvMax!!}

        quickRemove(settings.priceMin){it.drink.price(appSettings)<settings.priceMin!!}
        quickRemove(settings.priceMax){it.drink.price(appSettings)>settings.priceMax!!}

        // TODO parse volume with unit
        quickRemove(settings.volMin){it.drink.total_volume*10<settings.volMin!!}
        quickRemove(settings.volMax){it.drink.total_volume*10>settings.volMax!!}

        quickRemove(settings.volMin){it.drink.pricePerServing(appSettings)<settings.volMin!!}
        quickRemove(settings.volMax){it.drink.pricePerServing(appSettings)>settings.volMax!!}
        settings.filterTag(mutList, ownedIngredients, tags)
        mutList.removeAll{!it.ingredients.recipeParts.map {it.ingredient}.all { settings.whiteListedIngredients.contains(it) }}

        return mutList.toList()
    }
}