package com.valiantyan.music801.util

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.valiantyan.music801.R

/**
 * 测试专用的 Activity
 * 
 * 用于集成测试，在 onCreate 中创建 PermissionHelper 并暴露给测试。
 * 
 * 注意：此 Activity 仅用于测试目的，不会在正常应用流程中使用。
 */
class TestActivity : ComponentActivity() {
    /**
     * PermissionHelper 实例，供测试使用
     */
    lateinit var permissionHelper: PermissionHelper
        private set

    /**
     * 创建测试环境所需依赖
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionHelper = PermissionHelper(this)
    }
}
