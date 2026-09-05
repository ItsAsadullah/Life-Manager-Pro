package com.hisabnikash.app.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * ============================================================================
 * LIQUID GLASS SHADER & REFRACTION ENGINE
 * ============================================================================
 * AGSL (Android Graphics Shading Language) RuntimeShader ইমপ্লিমেন্টেশন
 * যা Android 13+ (API 33/Tiramisu) ডিভাইসে ট্রু লিকুইড গ্লাস রিফ্র্যাকশন,
 * ক্রোমাটিক অ্যাবারেশন (রং বিচ্ছুরণ) এবং ডায়নামিক স্পেকুলার হাইলাইট প্রদান করে।
 * 
 * পুরনো ভার্সনের জন্য graceful fallback হিসেবে null প্রদান করে, যার ফলে
 * Canvas Multi-Pass Renderer হাই-ফিডেলিটি গ্লাস লুক রেন্ডার করে।
 */
object LiquidGlassShaderConfig {
    /** রিফ্র্যাকশনের সূক্ষ্মতা (Subtle Refraction Strength) */
    const val REFRACTION_STRENGTH = 6.0f
    
    /** গ্লাসের ট্রান্সলুসেন্ট ব্লু টিন্টের ডিফল্ট অপাসিটি */
    const val BLUE_TINT_ALPHA = 0.35f
    
    /** স্পেকুলার লাইটের উজ্জ্বলতা */
    const val SPECULAR_INTENSITY = 0.80f
}

private const val LIQUID_GLASS_AGSL = """
    uniform shader uContent;
    uniform float2 uCenter;
    uniform float2 uResolution;
    uniform float uVelocity;
    uniform float4 uBlueTint;
    uniform float4 uSpecularColor;

    half4 main(float2 fragCoord) {
        // ১. লিকুইড বাউন্ডারি থেকে দূরত্বের ভেক্টর
        float2 norm = (fragCoord - uCenter) / max(uResolution * 0.5, float2(1.0, 1.0));
        float distSq = dot(norm, norm);
        
        // ২. ৩ডি ডোম সারফেস নরমাল ক্যালকুলেশন (z = sqrt(1 - r^2))
        float z = sqrt(max(0.0, 1.0 - distSq));
        float3 normal = normalize(float3(norm.x, norm.y, z * 1.8));
        
        // ৩. গতিবেগের ওপর ভিত্তি করে আলোর প্রতিসরণ (Refraction Vector)
        float2 refractOffset = normal.xy * 6.0 * (1.0 + abs(uVelocity) * 0.3);
        
        // ৪. অ্যাপল স্টাইল ক্রোমাটিক অ্যাবারেশন (কাঁচের মধ্যে আলোর সূক্ষ্ম বিচ্ছুরণ)
        half4 rSample = uContent.eval(fragCoord + refractOffset * 1.08);
        half4 gSample = uContent.eval(fragCoord + refractOffset);
        half4 bSample = uContent.eval(fragCoord + refractOffset * 0.92);
        half4 refractedBg = half4(rSample.r, gSample.g, bSample.b, (rSample.a + gSample.a + bSample.a) * 0.3333);
        
        // ৫. গতিশীল স্পেকুলার হাইলাইট (Velocity-aware specular reflection)
        float3 lightDir = normalize(float3(-0.35 + uVelocity * 0.15, -0.65, 0.7));
        float3 viewDir = float3(0.0, 0.0, 1.0);
        float3 halfVec = normalize(lightDir + viewDir);
        float NdotH = max(0.0, dot(normal, halfVec));
        float specular = pow(NdotH, 32.0);
        
        // ৬. ফ্রেনেল রিম লাইটিং (প্রান্তের কাঁচের চমক)
        float fresnel = pow(1.0 - max(0.0, normal.z), 2.8);
        
        // ৭. সিগনেচার ব্লু ট্রান্সলুসেন্ট গ্লাসের সাথে মিশ্রণ
        half4 glassColor = mix(refractedBg, half4(uBlueTint), half4(uBlueTint).a);
        half4 finalColor = glassColor + half4(uSpecularColor) * specular * 0.75 + half4(1.0, 1.0, 1.0, 1.0) * fresnel * 0.35;
        
        return finalColor;
    }
"""

/**
 * Android 13+ এর জন্য AGSL রেন্ডার ইফেক্ট তৈরি ও ক্যাশ করে।
 */
@Composable
fun rememberLiquidGlassRenderEffect(
    center: Offset,
    size: Size,
    velocity: Float,
    tintColor: Color = Color(0xFF0A84FF),
    isDark: Boolean = true
): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return null
    }

    return remember(center, size, velocity, tintColor, isDark) {
        try {
            createAgslRenderEffect(center, size, velocity, tintColor, isDark)
        } catch (e: Throwable) {
            // ডিভাইস নির্দিষ্ট কোনো শেডার ইস্যু হলে নিরাপদ ফলব্যাক
            null
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createAgslRenderEffect(
    center: Offset,
    size: Size,
    velocity: Float,
    tintColor: Color,
    isDark: Boolean
): RenderEffect {
    val shader = RuntimeShader(LIQUID_GLASS_AGSL)
    shader.setFloatUniform("uCenter", center.x, center.y)
    shader.setFloatUniform("uResolution", maxOf(size.width, 1f), maxOf(size.height, 1f))
    shader.setFloatUniform("uVelocity", velocity.coerceIn(-2f, 2f))
    
    // খাঁটি ক্রিস্টাল স্বচ্ছ পানির ফোঁটা—পেছনের ব্যাকগ্রাউন্ড ৯০% স্বচ্ছ ও প্রতিসরিত থাকবে
    val tintAlpha = if (isDark) 0.10f else 0.06f
    shader.setColorUniform(
        "uBlueTint",
        android.graphics.Color.argb(
            (tintAlpha * 255).toInt(),
            (tintColor.red * 255).toInt(),
            (tintColor.green * 255).toInt(),
            (tintColor.blue * 255).toInt()
        )
    )

    shader.setColorUniform(
        "uSpecularColor",
        android.graphics.Color.argb(
            (LiquidGlassShaderConfig.SPECULAR_INTENSITY * 255).toInt(),
            255, 255, 255
        )
    )

    val androidEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "uContent")
    return androidEffect.asComposeRenderEffect()
}
