package com.example.android.STEPNavi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.TypedValue;
import android.view.View;

import java.util.List;

/**
 * Created by kazuki on 2015/02/21.
 */
public class Area {

    private String areaName;
    private Path path;
    private AreaView view;
    private BluetoothChat STEPNavi;
    private List<PointF> point;
    private Point textPoint;
    private Point textPointForView;
    private static final int vSIZE = 5000;
    private static final int STROKE_WIDTH = 2;



    public Area(String name, Point p1, List<PointF> p2, BluetoothChat context){
        areaName = name;
        textPoint = p1;
        point = p2;
        STEPNavi = context;
        path = new Path();
    }

    public void updatePaths(int rate){
        path.reset();
        path.moveTo(ConvertPX(point.get(0).x) * rate, ConvertPX(point.get(0).y) * rate);
        for (int i=1; i<point.size(); i++) path.lineTo(ConvertPX(point.get(i).x) * rate, ConvertPX(point.get(i).y) * rate);
        path.lineTo(ConvertPX(point.get(0).x) * rate,ConvertPX( point.get(0).y) * rate);
        textPointForView = new Point(ConvertPX(textPoint.x) * rate, ConvertPX(textPoint.y) * rate);
    }

    public AreaView createView(){
        return (new AreaView(STEPNavi));
    }

    class AreaView extends View {
        public AreaView (BluetoothChat context){
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas){
            Paint paint = new Paint();
            Paint frame = new Paint();
            Paint textPaint = new Paint( Paint.ANTI_ALIAS_FLAG);

            paint.setAntiAlias(true);
            frame.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            frame.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            frame.setStyle(Paint.Style.STROKE);
            frame.setStrokeWidth(STROKE_WIDTH);

            textPaint.setTextSize(35);
            textPaint.setColor( Color.RED);

            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            // 文字列の幅を取得
            float textWidth = textPaint.measureText(areaName);
            // 中心にしたいX座標から文字列の幅の半分を引く
            float baseX = textPointForView.x - textWidth / 2;
            // 中心にしたいY座標からAscentとDescentの半分を引く
            float baseY = textPointForView.y - (fontMetrics.ascent + fontMetrics.descent) / 2;

            canvas.drawPath(path, paint);
            canvas.drawPath(path, frame);
            canvas.drawText( areaName, baseX, baseY, textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            // Viewの描画サイズを指定する
            setMeasuredDimension(vSIZE, vSIZE);
        }
    }


    //dp→px
    private int ConvertPX(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, STEPNavi.getResources().getDisplayMetrics());
    }

    //dp→px
    private int ConvertPX(float dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, STEPNavi.getResources().getDisplayMetrics());
    }

	public String getName(){
		return areaName;
	}
}

