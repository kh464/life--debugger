package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.AppCategoryRuleEntity

@Dao
interface AppCategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppCategoryRuleEntity)

    @Query("SELECT * FROM app_category_rules WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AppCategoryRuleEntity?
}
