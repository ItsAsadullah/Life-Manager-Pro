package com.hisabnikash.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * ============================================================================
 * REAL-LIFE WATER DROPLET (APPLE LIQUID GLASS) ENGINE
 * ============================================================================
 * রেফারেন্স ইমেজ ("Say hello to Liquid Glass") অনুযায়ী বাস্তব জীবনের
 * পানির ফোঁটার (Water Droplet / Crystal Lens) পদার্থবিজ্ঞান ও জ্যামিতিক ক্যালকুলেশন।
 * 
 * এটি ব্যাকগ্রাউন্ড অ্যাডাপ্টিভ—কোনো কৃত্রিম অস্বচ্ছ নীল পেইন্ট নয়।
 * খাঁটি ক্রিস্টাল স্বচ্ছ পানির ফোঁটা যা পেছনের ব্যাকগ্রাউন্ড ও আইকনকে
 * প্রতিসরিত (Refract) ও বিবর্ধিত (Magnify) করে।
 */
object WaterDropletConfig {
    /** সর্বোচ্চ তরল প্রসারণ (Max Droplet Elongation) */
    const val MAX_STRETCH_RATIO = 1.50f

    /** তরল নেক বা পৃষ্ঠটানের খাঁজ (Capillary Neck Dip) */
    const val CAPILLARY_NECK_DIP = 0.28f

    /** লক্ষ্যস্থলে পানির ফোঁটার স্প্ল্যাশ বা ফোলা ভাব (Bulge Amount) */
    const val DROPLET_BULGE_RATIO = 0.08f

    /** পানির ফোঁটার দ্রুত প্রবাহকাল (Duration in ms) - দ্রুত ও প্রাকৃতিক */
    const val DROPLET_FLOW_DURATION_MS = 320

    /** স্প্রিং বাউন্স ড্যাম্পিং (পানির ফোঁটার মার্জিত সেটলিং) */
    const val DROPLET_BOUNCE_DAMPING = 0.58f

    /** স্প্রিং বাউন্স স্টিফনেস */
    const val DROPLET_BOUNCE_STIFFNESS = 560f
}

/**
 * পানির ফোঁটার ফ্রেমভিত্তিক অবস্থান
 */
data class WaterDropletState(
    val headX: Float,
    val tailX: Float,
    val visualCenterX: Float,
    val isFlowing: Boolean,
    val stretchRatio: Float,
    val progress: Float
)

/**
 * পানির ফোঁটার প্রবাহ ক্যালকুলেটর
 */
object WaterDropletCalculator {

    fun computeState(
        p: Float,
        fromX: Float,
        toX: Float,
        tabWidth: Float,
        bounceProgress: Float = 0f
    ): WaterDropletState {
        val totalDelta = toX - fromX
        val dist = abs(totalDelta)
        val dir = if (totalDelta >= 0f) 1f else -1f

        // সর্বোচ্চ প্রসারণ সীমা (অতিরিক্ত বড় হওয়া রোধ করে)
        val maxSeparation = min(dist * 0.70f, tabWidth * WaterDropletConfig.MAX_STRETCH_RATIO)

        // পানির ফোঁটার অগ্রভাগ দ্রুত লক্ষ্যস্থলে পৌঁছায়, পিছনের অংশ পৃষ্ঠটানে টেনে আনে
        val (pHead, pTail) = if (p <= 0.52f) {
            val normP = (p / 0.52f).coerceIn(0f, 1f)
            val head = (1f - (1f - normP).pow(2.0f)) * 0.88f
            val tail = normP.pow(2.2f) * 0.20f
            head to tail
        } else {
            val t = ((p - 0.52f) / 0.48f).coerceIn(0f, 1f)
            val head = 0.88f + t.pow(1.1f) * 0.12f
            val tail = 0.20f + t.pow(0.70f) * 0.80f
            head to tail
        }

        val rawLeadDistance = min(dist * pHead, dist * pTail + maxSeparation)
        val rawTrailDistance = dist * pTail

        // টার্গেটে পৌঁছার পর নরম পানির ফোঁটার ওভারশুট
        val overshoot = if (bounceProgress > 0f) {
            dir * (dist * 0.045f).coerceAtMost(tabWidth * 0.14f) * bounceProgress * cos(bounceProgress * 3.1416f)
        } else 0f

        val headX = fromX + dir * rawLeadDistance + overshoot
        val tailX = fromX + dir * rawTrailDistance + overshoot

        val visualCenter = (headX + tailX) / 2f
        val separation = abs(headX - tailX)
        val stretchRatio = (separation / tabWidth).coerceAtLeast(0f)

        return WaterDropletState(
            headX = headX,
            tailX = tailX,
            visualCenterX = visualCenter,
            isFlowing = separation > 2f,
            stretchRatio = stretchRatio,
            progress = p
        )
    }
}

/**
 * রেফারেন্স ইমেজের হুবহু পানির ফোঁটা পাথ জেনারেটর
 */
class WaterDropletPathGenerator {

    /**
     * রেফারেন্স ইমেজ ৩ ("Say hello to Liquid Glass")-এর মতো
     * একটি ক্যাপসুল ও একটি বৃত্তাকার ফোঁটার মাঝের মসৃণ তরল ব্রিজ তৈরি করে।
     */
    fun buildDropletPath(
        state: WaterDropletState,
        tabWidth: Float,
        tabHeight: Float,
        cornerRadius: Float,
        bounceProgress: Float = 0f,
        outPath: Path = Path()
    ): Path {
        outPath.reset()

        val xL = min(state.headX, state.tailX)
        val xR = max(state.headX, state.tailX)
        val span = xR - xL
        val p = state.progress
        val yMid = tabHeight / 2f

        // ১. শান্ত অবস্থা: খাঁটি স্বচ্ছ পানির ফোঁটা লেন্স (Crystal Water Droplet Capsule)
        if (span < 1.5f) {
            val squashFactor = bounceProgress * 0.10f
            val squashX = 1.0f + squashFactor * cos(bounceProgress * 6.283f)
            val squashY = 1.0f - squashFactor * 0.60f * cos(bounceProgress * 6.283f)

            val w = tabWidth * squashX
            val h = tabHeight * squashY
            val r = cornerRadius * min(squashX, squashY)

            outPath.addRoundRect(
                RoundRect(
                    left = xL - w / 2f,
                    top = yMid - h / 2f,
                    right = xL + w / 2f,
                    bottom = yMid + h / 2f,
                    cornerRadius = CornerRadius(r, r)
                )
            )
            return outPath
        }

        // ২. চলমান অবস্থা: রেফারেন্স ইমেজের মতো পানির ফোঁটার জৈব রূপান্তর
        val (hL, hR) = if (p < 0.50f) {
            val normP = p / 0.50f
            val leftH = tabHeight * 0.98f
            val rightH = tabHeight * (0.86f + 0.14f * normP)
            leftH to rightH
        } else {
            val normP = (p - 0.50f) / 0.50f
            val leftH = tabHeight * (0.98f - 0.18f * normP)
            val rightH = tabHeight * (1.0f + WaterDropletConfig.DROPLET_BULGE_RATIO * sin(normP * 3.1416f))
            leftH to rightH
        }

        val halfWL = (tabWidth / 2f) * 0.95f
        val halfWR = (tabWidth / 2f) * (if (p > 0.5f) 1.03f else 0.95f)
        val halfHL = hL / 2f
        val halfHR = hR / 2f

        val leftOuterX = xL - halfWL
        val rightOuterX = xR + halfWR

        // পানির ফোঁটার পৃষ্ঠটান খাঁজ (Surface Tension Neck)
        val stretchNorm = (span / (tabWidth * 1.5f)).coerceIn(0f, 1f)
        val currentDip = (tabHeight / 2f) * WaterDropletConfig.CAPILLARY_NECK_DIP * stretchNorm.pow(0.80f)

        val xMid = (xL + xR) / 2f
        val minHalfH = min(halfHL, halfHR)
        val waistTopY = yMid - minHalfH + currentDip
        val waistBottomY = yMid + minHalfH - currentDip

        val rL = cornerRadius * (hL / tabHeight)
        val rR = cornerRadius * (hR / tabHeight)

        // উপরের বক্ররেখা
        outPath.moveTo(xL, yMid - halfHL)
        val tensionTop = (xMid - xL) * 0.50f
        outPath.cubicTo(
            x1 = xL + tensionTop, y1 = yMid - halfHL,
            x2 = xMid - tensionTop, y2 = waistTopY,
            x3 = xMid, y3 = waistTopY
        )
        outPath.cubicTo(
            x1 = xMid + tensionTop, y1 = waistTopY,
            x2 = xR - tensionTop, y2 = yMid - halfHR,
            x3 = xR, y3 = yMid - halfHR
        )

        // ডান ফোঁটার বৃত্তাকার ক্যাপ
        outPath.arcTo(
            rect = Rect(rightOuterX - 2f * rR, yMid - halfHR, rightOuterX, yMid - halfHR + 2f * rR),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        outPath.lineTo(rightOuterX, yMid + halfHR - rR)
        outPath.arcTo(
            rect = Rect(rightOuterX - 2f * rR, yMid + halfHR - 2f * rR, rightOuterX, yMid + halfHR),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // নিচের বক্ররেখা
        outPath.lineTo(xR, yMid + halfHR)
        outPath.cubicTo(
            x1 = xR - tensionTop, y1 = yMid + halfHR,
            x2 = xMid + tensionTop, y2 = waistBottomY,
            x3 = xMid, y3 = waistBottomY
        )
        outPath.cubicTo(
            x1 = xMid - tensionTop, y1 = waistBottomY,
            x2 = xL + tensionTop, y2 = yMid + halfHL,
            x3 = xL, y3 = yMid + halfHL
        )

        // বাম ফোঁটার বৃত্তাকার ক্যাপ
        outPath.lineTo(leftOuterX + rL, yMid + halfHL)
        outPath.arcTo(
            rect = Rect(leftOuterX, yMid + halfHL - 2f * rL, leftOuterX + 2f * rL, yMid + halfHL),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        outPath.lineTo(leftOuterX, yMid - halfHL + rL)
        outPath.arcTo(
            rect = Rect(leftOuterX, yMid - halfHL, leftOuterX + 2f * rL, yMid - halfHL + 2f * rL),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        outPath.close()

        return outPath
    }
}
