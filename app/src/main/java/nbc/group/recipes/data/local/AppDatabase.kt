package nbc.group.recipes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import nbc.group.recipes.data.local.dao.ContentBanDao
import nbc.group.recipes.data.local.dao.SpecialtyDao
import nbc.group.recipes.data.local.dao.RecipeDao
import nbc.group.recipes.data.local.dao.UserBanDao
import nbc.group.recipes.data.model.entity.ContentBanEntity
import nbc.group.recipes.data.model.entity.SpecialtyEntity
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.data.model.entity.UserBanEntity

@Database(
    entities = [
        SpecialtyEntity::class,
        RecipeEntity::class,
        ContentBanEntity::class,
        UserBanEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun specialtyDao(): SpecialtyDao
    abstract fun recipeDao(): RecipeDao
    abstract fun contentBanDao(): ContentBanDao
    abstract fun userBanDao(): UserBanDao

}
