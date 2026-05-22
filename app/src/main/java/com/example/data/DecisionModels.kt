package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class DecisionCriteria(
    val name: String,
    val optionAScore: Int,
    val optionBScore: Int,
    val rationale: String
)

@JsonClass(generateAdapter = true)
data class DecisionDetailResponse(
    val title: String,
    val optionA: String,
    val optionB: String,
    val criteria: List<DecisionCriteria>,
    val optionA_pros: List<String>,
    val optionA_cons: List<String>,
    val optionB_pros: List<String>,
    val optionB_cons: List<String>,
    val verdict: String,
    val winningOption: String
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val optionAName: String,
    val optionBName: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Serialized JSON fields
    val criteriaJson: String,
    val optionAProsJson: String,
    val optionAConsJson: String,
    val optionBProsJson: String,
    val optionBConsJson: String,
    
    val verdict: String,
    val winningOption: String
) {
    // Companion to handle conversions utilizing Moshi
    companion object {
        private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        private val criteriaListAdapter = moshi.adapter<List<DecisionCriteria>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, DecisionCriteria::class.java)
        )
        private val listStringAdapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )

        fun fromResponse(response: DecisionDetailResponse): DecisionEntity {
            return DecisionEntity(
                title = response.title,
                optionAName = response.optionA,
                optionBName = response.optionB,
                criteriaJson = criteriaListAdapter.toJson(response.criteria),
                optionAProsJson = listStringAdapter.toJson(response.optionA_pros),
                optionAConsJson = listStringAdapter.toJson(response.optionA_cons),
                optionBProsJson = listStringAdapter.toJson(response.optionB_pros),
                optionBConsJson = listStringAdapter.toJson(response.optionB_cons),
                verdict = response.verdict,
                winningOption = response.winningOption
            )
        }
    }

    fun getCriteriaList(): List<DecisionCriteria> {
        val adapter = moshi.adapter<List<DecisionCriteria>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, DecisionCriteria::class.java)
        )
        return adapter.fromJson(criteriaJson) ?: emptyList()
    }

    fun getOptionAPros(): List<String> {
        val adapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )
        return adapter.fromJson(optionAProsJson) ?: emptyList()
    }

    fun getOptionACons(): List<String> {
        val adapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )
        return adapter.fromJson(optionAConsJson) ?: emptyList()
    }

    fun getOptionBPros(): List<String> {
        val adapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )
        return adapter.fromJson(optionBProsJson) ?: emptyList()
    }

    fun getOptionBCons(): List<String> {
        val adapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )
        return adapter.fromJson(optionBConsJson) ?: emptyList()
    }

    fun optionATotalScore(): Int {
        return getCriteriaList().sumOf { it.optionAScore }
    }

    fun optionBTotalScore(): Int {
        return getCriteriaList().sumOf { it.optionBScore }
    }
}
