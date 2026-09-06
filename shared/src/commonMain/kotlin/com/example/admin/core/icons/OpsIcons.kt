package com.example.admin.core.icons

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Linear paths from the supplied public/icons library; normalized from its variant sheets. */
enum class OpsSymbol { Home, Search, Mission, Messages, Profile, Bell, Settings, ArrowRight, Back, Close, Add, Check, Clock, Location, Shield, Book, People, Chart, Map, Send, Filter, Logout, Warning, Phone, Mail, Heart, Box, Edit, More, Download }

@Composable
fun OpsIcon(symbol: OpsSymbol, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, contentDescription: String? = null) {
    Icon(vectors.getValue(symbol), contentDescription, modifier, tint)
}

private val vectors: Map<OpsSymbol, ImageVector> by lazy {
    mapOf(
        OpsSymbol.Home to ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M25.02 18.8399L19.63 23.0399C18.73 23.7399 18 25.2299 18 26.3599V33.7699C18 36.0899 19.89 37.9899 22.21 37.9899H33.79C36.11 37.9899 38 36.0899 38 33.7799V26.4999C38 25.2899 37.19 23.7399 36.2 23.0499L30.02 18.7199C28.62 17.7399 26.37 17.7899 25.02 18.8399Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28 33.99V30.99").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Search to ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.5f, translationY = -16f)
            addPath(PathParser().parsePathString("M27.5 37C32.7467 37 37 32.7467 37 27.5C37 22.2533 32.7467 18 27.5 18C22.2533 18 18 22.2533 18 27.5C18 32.7467 22.2533 37 27.5 37Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M38 38L36 36").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Mission to ImageVector.Builder("Mission", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M21.1499 18V38").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M21.1499 20H32.3499C35.0499 20 35.6499 21.5 33.7499 23.4L32.5499 24.6C31.7499 25.4 31.7499 26.7 32.5499 27.4L33.7499 28.6C35.6499 30.5 34.9499 32 32.3499 32H21.1499").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Messages to ImageVector.Builder("Messages", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M33.98 26.79V30.79C33.98 31.05 33.97 31.3 33.94 31.54C33.71 34.24 32.12 35.58 29.19 35.58H28.79C28.54 35.58 28.3 35.7 28.15 35.9L26.95 37.5C26.42 38.21 25.56 38.21 25.03 37.5L23.83 35.9C23.7 35.73 23.41 35.58 23.19 35.58H22.79C19.6 35.58 18 34.79 18 30.79V26.79C18 23.86 19.35 22.27 22.04 22.04C22.28 22.01 22.53 22 22.79 22H29.19C32.38 22 33.98 23.6 33.98 26.79Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M37.98 22.79V26.79C37.98 29.73 36.63 31.31 33.94 31.54C33.97 31.3 33.98 31.05 33.98 30.79V26.79C33.98 23.6 32.38 22 29.19 22H22.79C22.53 22 22.28 22.01 22.04 22.04C22.27 19.35 23.86 18 26.79 18H33.19C36.38 18 37.98 19.6 37.98 22.79Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M29.4955 29.25H29.5045").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25.9955 29.25H26.0045").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M22.4955 29.25H22.5045").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Profile to ImageVector.Builder("Profile", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 28C30.7614 28 33 25.7614 33 23C33 20.2386 30.7614 18 28 18C25.2386 18 23 20.2386 23 23C23 25.7614 25.2386 28 28 28Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M36.5901 38C36.5901 34.13 32.7402 31 28.0002 31C23.2602 31 19.4102 34.13 19.4102 38").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Bell to ImageVector.Builder("Bell", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28.0196 18.9102C24.7096 18.9102 22.0196 21.6002 22.0196 24.9102V27.8002C22.0196 28.4102 21.7596 29.3402 21.4496 29.8602L20.2996 31.7702C19.5896 32.9502 20.0796 34.2602 21.3796 34.7002C25.6896 36.1402 30.3396 36.1402 34.6496 34.7002C35.8596 34.3002 36.3896 32.8702 35.7296 31.7702L34.5796 29.8602C34.2796 29.3402 34.0196 28.4102 34.0196 27.8002V24.9102C34.0196 21.6102 31.3196 18.9102 28.0196 18.9102Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M29.8699 19.1999C29.5599 19.1099 29.2399 19.0399 28.9099 18.9999C27.9499 18.8799 27.0299 18.9499 26.1699 19.1999C26.4599 18.4599 27.1799 17.9399 28.0199 17.9399C28.8599 17.9399 29.5799 18.4599 29.8699 19.1999Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M31.0195 35.0601C31.0195 36.7101 29.6695 38.0601 28.0195 38.0601C27.1995 38.0601 26.4395 37.7201 25.8995 37.1801C25.3595 36.6401 25.0195 35.8801 25.0195 35.0601").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Settings to ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 31C29.6569 31 31 29.6569 31 28C31 26.3431 29.6569 25 28 25C26.3431 25 25 26.3431 25 28C25 29.6569 26.3431 31 28 31Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M18 28.8804V27.1204C18 26.0804 18.85 25.2204 19.9 25.2204C21.71 25.2204 22.45 23.9404 21.54 22.3704C21.02 21.4704 21.33 20.3004 22.24 19.7804L23.97 18.7904C24.76 18.3204 25.78 18.6004 26.25 19.3904L26.36 19.5804C27.26 21.1504 28.74 21.1504 29.65 19.5804L29.76 19.3904C30.23 18.6004 31.25 18.3204 32.04 18.7904L33.77 19.7804C34.68 20.3004 34.99 21.4704 34.47 22.3704C33.56 23.9404 34.3 25.2204 36.11 25.2204C37.15 25.2204 38.01 26.0704 38.01 27.1204V28.8804C38.01 29.9204 37.16 30.7804 36.11 30.7804C34.3 30.7804 33.56 32.0604 34.47 33.6304C34.99 34.5404 34.68 35.7004 33.77 36.2204L32.04 37.2104C31.25 37.6804 30.23 37.4004 29.76 36.6104L29.65 36.4204C28.75 34.8504 27.27 34.8504 26.36 36.4204L26.25 36.6104C25.78 37.4004 24.76 37.6804 23.97 37.2104L22.24 36.2204C21.33 35.7004 21.02 34.5304 21.54 33.6304C22.45 32.0604 21.71 30.7804 19.9 30.7804C18.85 30.7804 18 29.9204 18 28.8804Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.ArrowRight to ImageVector.Builder("ArrowRight", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 38C33.5228 38 38 33.5228 38 28C38 22.4772 33.5228 18 28 18C22.4772 18 18 22.4772 18 28C18 33.5228 22.4772 38 28 38Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.5 28H30.5").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28.5 31L31.5 28L28.5 25").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Back to ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 38C33.5228 38 38 33.5228 38 28C38 22.4772 33.5228 18 28 18C22.4772 18 18 22.4772 18 28C18 33.5228 22.4772 38 28 38Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M31.5 28H25.5").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M27.5 25L24.5 28L27.5 31").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Close to ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 38C33.5 38 38 33.5 38 28C38 22.5 33.5 18 28 18C22.5 18 18 22.5 18 28C18 33.5 22.5 38 28 38Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25.1699 30.8299L30.8299 25.1699").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M30.8299 30.8299L25.1699 25.1699").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Add to ImageVector.Builder("Add", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M22 28H34").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28 34V22").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Check to ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 38C33.5 38 38 33.5 38 28C38 22.5 33.5 18 28 18C22.5 18 18 22.5 18 28C18 33.5 22.5 38 28 38Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M23.75 27.9999L26.58 30.8299L32.25 25.1699").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Clock to ImageVector.Builder("Clock", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 25.6602V28.4502L29.4 29.8502").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M21.5 28C21.5 24.41 24.41 21.5 28 21.5C31.59 21.5 34.5 24.41 34.5 28C34.5 30.08 33.52 31.94 32 33.13H31.99C30.89 33.99 29.51 34.5 28 34.5C26.51 34.5 25.14 34 24.04 33.15H24.03C22.49 31.96 21.5 30.1 21.5 28Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.0298 33.1499H24.0398C25.1398 33.9999 26.5098 34.4999 27.9998 34.4999C29.5098 34.4999 30.8898 33.9899 31.9898 33.1299H31.9998L31.4898 35.5999C30.9998 37.4999 29.8998 37.9999 28.5498 37.9999H27.4598C26.1098 37.9999 24.9998 37.4999 24.5198 35.5899L24.0298 33.1499Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.0298 22.85H24.0398C25.1398 22 26.5098 21.5 27.9998 21.5C29.5098 21.5 30.8898 22.01 31.9898 22.87H31.9998L31.4898 20.4C30.9998 18.5 29.8998 18 28.5498 18H27.4598C26.1098 18 24.9998 18.5 24.5198 20.41L24.0298 22.85Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Location to ImageVector.Builder("Location", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 29.4299C29.7231 29.4299 31.12 28.0331 31.12 26.3099C31.12 24.5868 29.7231 23.1899 28 23.1899C26.2769 23.1899 24.88 24.5868 24.88 26.3099C24.88 28.0331 26.2769 29.4299 28 29.4299Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M19.62 24.49C21.59 15.83 34.42 15.84 36.38 24.5C37.53 29.58 34.37 33.88 31.6 36.54C29.59 38.48 26.41 38.48 24.39 36.54C21.63 33.88 18.47 29.57 19.62 24.49Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Shield to ImageVector.Builder("Shield", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M26.4902 18.2296L21.5002 20.1096C20.3502 20.5396 19.4102 21.8996 19.4102 23.1196V30.5496C19.4102 31.7296 20.1902 33.2796 21.1402 33.9896L25.4402 37.1996C26.8502 38.2596 29.1702 38.2596 30.5802 37.1996L34.8802 33.9896C35.8302 33.2796 36.6102 31.7296 36.6102 30.5496V23.1196C36.6102 21.8896 35.6702 20.5296 34.5202 20.0996L29.5302 18.2296C28.6802 17.9196 27.3202 17.9196 26.4902 18.2296Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25.0498 27.8697L26.6598 29.4797L30.9598 25.1797").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Book to ImageVector.Builder("Book", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M38 32.7402V20.6702C38 19.4702 37.02 18.5802 35.83 18.6802H35.77C33.67 18.8602 30.48 19.9302 28.7 21.0502L28.53 21.1602C28.24 21.3402 27.76 21.3402 27.47 21.1602L27.22 21.0102C25.44 19.9002 22.26 18.8402 20.16 18.6702C18.97 18.5702 18 19.4702 18 20.6602V32.7402C18 33.7002 18.78 34.6002 19.74 34.7202L20.03 34.7602C22.2 35.0502 25.55 36.1502 27.47 37.2002L27.51 37.2202C27.78 37.3702 28.21 37.3702 28.47 37.2202C30.39 36.1602 33.75 35.0502 35.93 34.7602L36.26 34.7202C37.22 34.6002 38 33.7002 38 32.7402Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28 21.4902V36.4902").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M23.75 24.4902H21.5").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.5 27.4902H21.5").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.People to ImageVector.Builder("People", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M34.0001 23.16C33.9401 23.15 33.8701 23.15 33.8101 23.16C32.4301 23.11 31.3301 21.98 31.3301 20.58C31.3301 19.15 32.4801 18 33.9101 18C35.3401 18 36.4901 19.16 36.4901 20.58C36.4801 21.98 35.3801 23.11 34.0001 23.16Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M32.9704 30.4402C34.3404 30.6702 35.8504 30.4302 36.9104 29.7202C38.3204 28.7802 38.3204 27.2402 36.9104 26.3002C35.8404 25.5902 34.3104 25.3502 32.9404 25.5902").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M21.9705 23.16C22.0305 23.15 22.1005 23.15 22.1605 23.16C23.5405 23.11 24.6405 21.98 24.6405 20.58C24.6405 19.15 23.4905 18 22.0605 18C20.6305 18 19.4805 19.16 19.4805 20.58C19.4905 21.98 20.5905 23.11 21.9705 23.16Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M23.0004 30.4402C21.6304 30.6702 20.1204 30.4302 19.0604 29.7202C17.6504 28.7802 17.6504 27.2402 19.0604 26.3002C20.1304 25.5902 21.6604 25.3502 23.0304 25.5902").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28.0001 30.6302C27.9401 30.6202 27.8701 30.6202 27.8101 30.6302C26.4301 30.5802 25.3301 29.4502 25.3301 28.0502C25.3301 26.6202 26.4801 25.4702 27.9101 25.4702C29.3401 25.4702 30.4901 26.6302 30.4901 28.0502C30.4801 29.4502 29.3801 30.5902 28.0001 30.6302Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25.0897 33.7804C23.6797 34.7204 23.6797 36.2603 25.0897 37.2003C26.6897 38.2703 29.3097 38.2703 30.9097 37.2003C32.3197 36.2603 32.3197 34.7204 30.9097 33.7804C29.3197 32.7204 26.6897 32.7204 25.0897 33.7804Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Chart to ImageVector.Builder("Chart", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M25 38H31C36 38 38 36 38 31V25C38 20 36 18 31 18H25C20 18 18 20 18 25V31C18 36 20 38 25 38Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M31.5 34.5C32.6 34.5 33.5 33.6 33.5 32.5V23.5C33.5 22.4 32.6 21.5 31.5 21.5C30.4 21.5 29.5 22.4 29.5 23.5V32.5C29.5 33.6 30.39 34.5 31.5 34.5Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.5 34.5C25.6 34.5 26.5 33.6 26.5 32.5V29C26.5 27.9 25.6 27 24.5 27C23.4 27 22.5 27.9 22.5 29V32.5C22.5 33.6 23.39 34.5 24.5 34.5Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Map to ImageVector.Builder("Map", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M18.29 23.78V33.51C18.29 35.41 19.64 36.19 21.28 35.25L23.63 33.91C24.14 33.62 24.99 33.59 25.52 33.86L30.77 36.49C31.3 36.75 32.15 36.73 32.66 36.44L36.99 33.96C37.54 33.64 38 32.86 38 32.22V22.49C38 20.59 36.65 19.81 35.01 20.75L32.66 22.09C32.15 22.38 31.3 22.41 30.77 22.14L25.52 19.52C24.99 19.26 24.14 19.28 23.63 19.57L19.3 22.05C18.74 22.37 18.29 23.15 18.29 23.78Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M24.5601 20V33").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M31.73 22.6201V36.0001").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Send to ImageVector.Builder("Send", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M23.3999 22.3199L31.8899 19.4899C35.6999 18.2199 37.7699 20.2999 36.5099 24.1099L33.6799 32.5999C31.7799 38.3099 28.6599 38.3099 26.7599 32.5999L25.9199 30.0799L23.3999 29.2399C17.6899 27.3399 17.6899 24.2299 23.3999 22.3199Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M26.1101 29.6501L29.6901 26.0601").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Filter to ImageVector.Builder("Filter", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M21.3999 18.1001H34.5999C35.6999 18.1001 36.5999 19.0001 36.5999 20.1001V22.3001C36.5999 23.1001 36.0999 24.1001 35.5999 24.6001L31.2999 28.4001C30.6999 28.9001 30.2999 29.9001 30.2999 30.7001V35.0001C30.2999 35.6001 29.8999 36.4001 29.3999 36.7001L27.9999 37.6001C26.6999 38.4001 24.8999 37.5001 24.8999 35.9001V30.6001C24.8999 29.9001 24.4999 29.0001 24.0999 28.5001L20.2999 24.5001C19.7999 24.0001 19.3999 23.1001 19.3999 22.5001V20.2001C19.3999 19.0001 20.2999 18.1001 21.3999 18.1001Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M26.93 18.1001L22 26.0001").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Logout to ImageVector.Builder("Logout", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M24.9004 23.5602C25.2104 19.9602 27.0604 18.4902 31.1104 18.4902H31.2404C35.7104 18.4902 37.5004 20.2802 37.5004 24.7502V31.2702C37.5004 35.7402 35.7104 37.5302 31.2404 37.5302H31.1104C27.0904 37.5302 25.2404 36.0802 24.9104 32.5402").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M31.0001 28H19.6201").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M21.85 24.6499L18.5 27.9999L21.85 31.3499").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Warning to ImageVector.Builder("Warning", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28 23.75V29").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M37.08 24.58V31.42C37.08 32.54 36.4799 33.58 35.5099 34.15L29.5699 37.58C28.5999 38.14 27.3999 38.14 26.4199 37.58L20.4799 34.15C19.5099 33.59 18.9099 32.55 18.9099 31.42V24.58C18.9099 23.46 19.5099 22.42 20.4799 21.85L26.4199 18.42C27.3899 17.86 28.5899 17.86 29.5699 18.42L35.5099 21.85C36.4799 22.42 37.08 23.45 37.08 24.58Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28 32.2002V32.3002").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Phone to ImageVector.Builder("Phone", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M37.97 34.33C37.97 34.69 37.89 35.06 37.72 35.42C37.55 35.78 37.33 36.12 37.04 36.44C36.55 36.98 36.01 37.37 35.4 37.62C34.8 37.87 34.15 38 33.45 38C32.43 38 31.34 37.76 30.19 37.27C29.04 36.78 27.89 36.12 26.75 35.29C25.6 34.45 24.51 33.52 23.47 32.49C22.44 31.45 21.51 30.36 20.68 29.22C19.86 28.08 19.2 26.94 18.72 25.81C18.24 24.67 18 23.58 18 22.54C18 21.86 18.12 21.21 18.36 20.61C18.6 20 18.98 19.44 19.51 18.94C20.15 18.31 20.85 18 21.59 18C21.87 18 22.15 18.06 22.4 18.18C22.66 18.3 22.89 18.48 23.07 18.74L25.39 22.01C25.57 22.26 25.7 22.49 25.79 22.71C25.88 22.92 25.93 23.13 25.93 23.32C25.93 23.56 25.86 23.8 25.72 24.03C25.59 24.26 25.4 24.5 25.16 24.74L24.4 25.53C24.29 25.64 24.24 25.77 24.24 25.93C24.24 26.01 24.25 26.08 24.27 26.16C24.3 26.24 24.33 26.3 24.35 26.36C24.53 26.69 24.84 27.12 25.28 27.64C25.73 28.16 26.21 28.69 26.73 29.22C27.27 29.75 27.79 30.24 28.32 30.69C28.84 31.13 29.27 31.43 29.61 31.61C29.66 31.63 29.72 31.66 29.79 31.69C29.87 31.72 29.95 31.73 30.04 31.73C30.21 31.73 30.34 31.67 30.45 31.56L31.21 30.81C31.46 30.56 31.7 30.37 31.93 30.25C32.16 30.11 32.39 30.04 32.64 30.04C32.83 30.04 33.03 30.08 33.25 30.17C33.47 30.26 33.7 30.39 33.95 30.56L37.26 32.91C37.52 33.09 37.7 33.3 37.81 33.55C37.91 33.8 37.97 34.05 37.97 34.33Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Mail to ImageVector.Builder("Mail", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M33 36.5H23C20 36.5 18 35 18 31.5V24.5C18 21 20 19.5 23 19.5H33C36 19.5 38 21 38 24.5V31.5C38 35 36 36.5 33 36.5Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M33 25L29.87 27.5C28.84 28.32 27.15 28.32 26.12 27.5L23 25").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Heart to ImageVector.Builder("Heart", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M28.62 36.8096C28.28 36.9296 27.72 36.9296 27.38 36.8096C24.48 35.8196 18 31.6896 18 24.6896C18 21.5996 20.49 19.0996 23.56 19.0996C25.38 19.0996 26.99 19.9796 28 21.3396C29.01 19.9796 30.63 19.0996 32.44 19.0996C35.51 19.0996 38 21.5996 38 24.6896C38 31.6896 31.52 35.8196 28.62 36.8096Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Box to ImageVector.Builder("Box", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M19.1709 23.4399L28.0009 28.5499L36.7709 23.4699").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28.001 37.61V28.54").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25.9306 18.48L20.5906 21.45C19.3806 22.12 18.3906 23.8 18.3906 25.18V30.83C18.3906 32.21 19.3806 33.89 20.5906 34.56L25.9306 37.53C27.0706 38.16 28.9406 38.16 30.0806 37.53L35.4206 34.56C36.6306 33.89 37.6206 32.21 37.6206 30.83V25.18C37.6206 23.8 36.6306 22.12 35.4206 21.45L30.0806 18.48C28.9306 17.84 27.0706 17.84 25.9306 18.48Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M33.0007 29.2401V25.5801L23.5107 20.1001").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Edit to ImageVector.Builder("Edit", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M29.2603 19.6002L21.0503 28.2902C20.7403 28.6202 20.4403 29.2702 20.3803 29.7202L20.0103 32.9602C19.8803 34.1302 20.7203 34.9302 21.8803 34.7302L25.1003 34.1802C25.5503 34.1002 26.1803 33.7702 26.4903 33.4302L34.7003 24.7402C36.1203 23.2402 36.7603 21.5302 34.5503 19.4402C32.3503 17.3702 30.6803 18.1002 29.2603 19.6002Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M27.8896 21.0498C28.3196 23.8098 30.5596 25.9198 33.3396 26.1998").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M19 38H37").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.More to ImageVector.Builder("More", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M21 26C19.9 26 19 26.9 19 28C19 29.1 19.9 30 21 30C22.1 30 23 29.1 23 28C23 26.9 22.1 26 21 26Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M35 26C33.9 26 33 26.9 33 28C33 29.1 33.9 30 35 30C36.1 30 37 29.1 37 28C37 26.9 36.1 26 35 26Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M28 26C26.9 26 26 26.9 26 28C26 29.1 26.9 30 28 30C29.1 30 30 29.1 30 28C30 26.9 29.1 26 28 26Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
        OpsSymbol.Download to ImageVector.Builder("Download", 24.dp, 24.dp, 24f, 24f).apply {
            addGroup(translationX = -16.0f, translationY = -16f)
            addPath(PathParser().parsePathString("M25 27V33L27 31").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M25 33L23 31").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M38 26V31C38 36 36 38 31 38H25C20 38 18 36 18 31V25C18 20 20 18 25 18H30").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(PathParser().parsePathString("M38 26H34C31 26 30 25 30 22V18L38 26Z").toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            clearGroup()
        }.build(),
    )
}
