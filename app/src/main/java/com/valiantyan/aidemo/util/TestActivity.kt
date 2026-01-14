package com.valiantyan.aidemo.util

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.valiantyan.aidemo.R

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置一个简单的布局（避免空布局警告）
        setContentView(R.layout.activity_main)
        
        // 在 onCreate 中创建 PermissionHelper（这是必须的，因为 registerForActivityResult 必须在 onCreate 中调用）
        permissionHelper = PermissionHelper(this)
    }
}
