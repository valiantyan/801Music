package com.valiantyan.aidemo.ui.scan

import androidx.appcompat.app.AppCompatActivity

/**
 * 测试专用的 Activity，用于承载 Fragment 进行 Robolectric 测试
 *
 * 这个 Activity 位于 debug source set，不会影响生产构建。
 * 用于替代 FragmentScenario 的 EmptyFragmentActivity，解决 Robolectric 兼容性问题。
 */
class TestFragmentActivity : AppCompatActivity()
