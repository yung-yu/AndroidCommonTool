package com.andy.androidcommon

import android.graphics.Color
import android.graphics.Paint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Typeface
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path


class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		pathAnimationView.setPath(getPath("你好"))
		pathAnimationView.start()
	}

	private fun getPath( s:String): Path {
		val textPath = Path()
		val paint = Paint(ANTI_ALIAS_FLAG)
		paint.setColor(Color.RED)
		paint.setTextSize(50F)
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC))
		paint.getTextPath(s, 0, s.length, 0f, 200f, textPath)
		textPath.close()
		return textPath
	}
}
