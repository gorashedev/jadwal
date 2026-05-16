package com.jadwal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ===== Material 3 Shapes =====
val JadwalShapes = Shapes(
    // Chips, Badges, Tags الصغيرة
    extraSmall = RoundedCornerShape(6.dp),
    // أزرار صغيرة، TextFields
    small = RoundedCornerShape(10.dp),
    // كاردات، Dialogs
    medium = RoundedCornerShape(16.dp),
    // Bottom Sheets، Panels كبيرة
    large = RoundedCornerShape(24.dp),
    // شاشات كاملة، Drawers
    extraLarge = RoundedCornerShape(32.dp),
)

// ===== Jadwal Radius Constants =====
// استخدمها مباشرة في الكود بدل أرقام ثابتة
object JadwalRadius {
    val xs   = 6.dp    // عناصر صغيرة جداً
    val sm   = 10.dp   // أزرار وحقول نص
    val md   = 16.dp   // كاردات عادية
    val lg   = 24.dp   // كاردات كبيرة، Bottom Bar
    val xl   = 32.dp   // Bottom Sheets
    val xxl  = 40.dp   // Glass Bottom Navigation
    val full = 1000.dp // دائري تماماً (FAB, Avatar)
}

// ===== Spacing System =====
// نظام مسافات موحد للـ padding والـ margin
object JadwalSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 24.dp
    val xxl  = 32.dp
    val xxxl = 48.dp
}

// ===== Elevation =====
object JadwalElevation {
    val none   = 0.dp
    val low    = 2.dp
    val medium = 4.dp
    val high   = 8.dp
}
