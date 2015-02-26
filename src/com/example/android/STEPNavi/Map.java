package com.example.android.STEPNavi;

import java.util.*;

import com.example.android.BluetoothChat.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.gesture.Gesture;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomButton;

public class Map {
    private List<Location> location; //Tagを格納するリスト
    private List<ImageView> iv = new ArrayList<ImageView>();
    private String mapName; //マップの名前
    private int locationSize = 0; //Tagの数
    private int activeLocationId; //アクティブなタグid
    private int previousLocationId; //一つ前にアクティブだったタグid
    private boolean loaded = false; //マップ生成フラグ

    private Location now;
    private BluetoothChat BlueC;

    //zoom
    private static final int ORIGINAL_RATE = 40;
    private static final int ZOOM_VELOCITY = 3;
    private static final float ZOOM_MAX = 1.5f;
    private static final float ZOOM_MIN = 0.4f;
    private int RATE = 40;
    private float mScale;
    private float oldScale = 1.0f;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    //Area
    //Path storePath;
    List<Area> area;
    List<Area.AreaView> avList;

    //Scroll
    private int trackedX = 0;
    private int trackedY = 0;
    private int oldTrackedX = 0;
    private int oldTrackedY = 0;

    //Navigation
    Queue<Location> task; //ダイクストラによるルート探索用の待ち行列
    private List<Location> route; //ルートに対応するオブジェクトを格納するリスト
    private boolean demo = false;
    private int itrForDemo;
    private boolean initTimer = false;

    //makeMap
    private Point entry;


    //move
    private int oldX=0;
    private int oldY=0;

    //center
    private int width;
    private int height;
    private int centerX;
    private int centerY;

    //me
    private ImageView me;
    //gif
    private final float gifSize = 0.4f;
    //tag
    private ImageView testTag;
    //Layout
    private AbsoluteLayout absLayout;
    private LinearLayout linLayout;
    //Ic Size
    private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    Handler handler = new Handler();
    Timer timer = new Timer(false);

    public Map(String str, BluetoothChat obj){
        locationSize = 0;
        mapName = str;
        location = new ArrayList<Location>();
        activeLocationId = -1;
        previousLocationId = -1;
        entry = new Point();
        BlueC = obj;
        route = new ArrayList<Location>();
        task = new LinkedList<Location>();

        mScale = 1.0f;
        mScaleGestureDetector = new ScaleGestureDetector(BlueC, mOnScaleListener);
        mGestureDetector = new GestureDetector(BlueC, mOnDoubleTapListener);

        //Store
        area = new ArrayList<Area>();
        avList = new ArrayList<Area.AreaView>();
    }

    public ScaleGestureDetector getmScaleGestureDetector(){
        return mScaleGestureDetector;
    }

    public GestureDetector getmGestureDetector(){
        return mGestureDetector;
    }

    public AbsoluteLayout getAbsLayout(){
        return absLayout;
    }

    private GestureDetector.SimpleOnGestureListener mOnDoubleTapListener
            = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onDoubleTap(MotionEvent event){
            setRATE(ORIGINAL_RATE);
            updateMap();
            return true;
        };
    };

    private ScaleGestureDetector.SimpleOnScaleGestureListener mOnScaleListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if  (demo) {
                Log.d("BL","[Returned for demo]");
                return true;
            }

            mScale *= (detector.getScaleFactor()*0.05f + 0.95f);

            /*****************************汎用化必要アリ？****************************/
            if  (mScale > oldScale){
                setRATE(RATE + ZOOM_VELOCITY);
            }
            else if (mScale < oldScale){
                setRATE(RATE - ZOOM_VELOCITY);
            }
            /*************************************************************************/

            updateMap();

            oldScale = mScale;
            return true;
        };
    };

    private void setRATE(int r){

        float sign = Math.signum((float)(r - RATE)); //拡大(1)or縮小(-1)
        int sub = r - RATE;
        RATE = r;

        if  (RATE > ORIGINAL_RATE * ZOOM_MAX)
        {
            RATE = (int)(ORIGINAL_RATE * ZOOM_MAX);
        }else if  (RATE < ORIGINAL_RATE * ZOOM_MIN)
        {
            RATE = (int)(ORIGINAL_RATE * ZOOM_MIN);
        } else
        {
            trackedX *= (double) RATE / (double) (RATE - (int)sign * Math.abs(sub)); //正規化
            trackedY *= (double) RATE / (double) (RATE - (int)sign * Math.abs(sub));
            for (ImageView i : iv){
                i.setScaleX((float)RATE/(float)ORIGINAL_RATE); //タグサイズを同期
                i.setScaleY((float)RATE/(float)ORIGINAL_RATE);
            }
            me.setScaleX((((float)RATE/(float)ORIGINAL_RATE) - 1.0f)*0.5f + 1.0f); //自分のサイズを同期
            me.setScaleY((((float)RATE/(float)ORIGINAL_RATE) - 1.0f)*0.5f + 1.0f);
            me.setY(me.getY() - (int)sign * 2); //中心座標を調整
        }
    }

    public void setMeObject(ImageView img){
        me = img;
    }

    public void setTestTagObject(ImageView img){
        testTag = img;
    }
    public void setActiveLocation(int id){
        previousLocationId = activeLocationId;
        activeLocationId = id;
    }
    public void setAbsoluteLayout(AbsoluteLayout layout){
        absLayout = layout;
    }
    public void setLinearLayout(LinearLayout layout){
        linLayout = layout;
    }

    public void addArea(String name, Point tp, List<PointF> point){
        area.add(new Area(name, tp, point, BlueC));
    }

    //ロケーションを追加2
    public boolean addLocation(int id, String str, int x, int y){
        locationSize++;
        return location.add(new Location(id, str, x, y));
    }

    //ロケーションを追加3
    public boolean addLocation(int id, String str, int x, int y, int s1, int s2){
        locationSize++;
        return location.add(new Location(id, str, x, y, s1, s2));
    }

    //idよりLocationオブジェクトを取得。
    //存在しない場合はnull
    public Location getLocationById(int id){
        for (Location l : location){
            if  (l.getId() == id) return l;
        }
        return null;
    }

    public boolean isExistByPosition(int p1, int p2){
        for (Location l : location){
            if  (l.getX() == p1 && l.getY() == p2) return true;
        }
        return false;
    }

    public Location getLocationByPosition(int p1, int p2){
        for (Location l : location){
            if  (l.getX() == p1 && l.getY() == p2) return l;
        }
        return null;
    }

    public Location getActiveLocation(){
        if  (activeLocationId == -1) return null;
        return this.getLocationById(activeLocationId);
    }

    public Location getPreviousLocation(){
        if  (previousLocationId == -1) return null;
        return this.getLocationById(previousLocationId);
    }

    public List<Location> getAllLocation(){
        return location;
    }

    public int getLocationSize(){
        return locationSize;
    }

	public List<String> getLocationName(){
		List<String> LocationName = new ArrayList<String>();

		for(Area a : area)
			LocationName.add(a.getName());

		return LocationName;
	}

    private void initState(){
        for (Location l : location){
            Location[] res = new Location[4];
            res[0] = getLocationByPosition(l.getX()    , l.getY() + 1);
            res[1] = getLocationByPosition(l.getX() + 1, l.getY()    );
            res[2] = getLocationByPosition(l.getX()    , l.getY() - 1);
            res[3] = getLocationByPosition(l.getX() - 1, l.getY()    );
            l.initState(res);
        }
    }

    public void makeMap(int id){
        initState();
        loaded = true;
        iv.clear();
        RATE = 40;

        setActiveLocation(id);
        absLayout.removeAllViews();

        now = getLocationById(id);

        int nowX = now.getX(); //DP変換ズミ
        int nowY = now.getY();

        int range = 120 * 8; //動的にすべき？

        for (Location l : getAllLocation()){
            int x = l.getX();//描画座標の設定
            int y = l.getY();
            iv.add(new ImageView(BlueC));
            iv.get(iv.size()-1).setImageResource(R.drawable.tag); //使用画像の設定;

            if  (width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX < 2*width + range
                    && width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX > -range
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY < 2*height + range
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY > -range) {

                absLayout.addView(iv.get(iv.size() - 1), new AbsoluteLayout.LayoutParams(WC, WC,
                        width + ConvertPX((x - nowX)) * RATE - testTag.getWidth() / 2,
                        height + ConvertPX((y - nowY)) * RATE - testTag.getHeight() / 2));
            }
        }

        //エリアの表示
        avList.clear();
        for (Area a : area){
            a.updatePaths(RATE);
            Area.AreaView av = a.createView();
            //av.setBackgroundColor(Color.GRAY);

            absLayout.addView(av, (new AbsoluteLayout.LayoutParams(WC, WC,
                    width + ConvertPX(( - nowX)) * RATE,
                    height + ConvertPX(( - nowY)) * RATE)));
            avList.add(av);
        }

        //gifの表示
        linLayout.setScaleX(gifSize);
        linLayout.setScaleY(gifSize);
        linLayout.setVisibility(View.VISIBLE);
        absLayout.addView(linLayout, (new AbsoluteLayout.LayoutParams(WC,WC
                , calculateImagesPoint(width, height, linLayout.getWidth(), linLayout.getHeight()).x
                , calculateImagesPoint(width, height, linLayout.getWidth(), linLayout.getHeight()).y)));

        //meの表示
        me.bringToFront();
        me.setVisibility(View.VISIBLE);
        absLayout.addView(me, (new AbsoluteLayout.LayoutParams(WC, WC, centerX, centerY)));
        entry.x = now.getX();
        entry.y = now.getY();

        Rect rect = new Rect(); //ステータスバーの高さ取得
        Window window = BlueC.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int h = rect.top;



    }



    public void updateMap(int id){

        double judge = Math.sqrt(50); //規定距離以上動いたら重いので、アニメなし。
        if  (getDistanceFromHere(id) > judge ){
            updateMapNonAnimation(id);
            Log.d("BL","nonAnime");
            return;
        } else {
            Log.d("BL","Anime");
        }

        setActiveLocation(id);
        now = getActiveLocation();

        if  (trackedX != 0 || trackedY != 0) {
            trackedX = 0;
            trackedY = 0;
            oldTrackedX = 0;
            oldTrackedY = 0;
        }

        int X = entry.x - now.getZoomX();
        int Y = entry.y - now.getZoomY();

        int iconSize = iv.get(0).getRight() - iv.get(0).getLeft();
        int range = iconSize * 8;

        for(int i=0;i<getAllLocation().size();i++){

            if  ( width + ConvertPX( (location.get(i).getX()-getActiveLocation().getX())) * RATE < 2 * width + range
                    &&  width + ConvertPX( (location.get(i).getX()-getActiveLocation().getX())) * RATE > - range
                    && height + ConvertPX( (location.get(i).getY()-getActiveLocation().getY())) * RATE < 2 * height + range
                    && height + ConvertPX( (location.get(i).getY()-getActiveLocation().getY())) * RATE > -range) {
                TranslateAnimation trans = new TranslateAnimation(ConvertPX(oldX) * RATE, ConvertPX(X) * RATE, ConvertPX(oldY) * RATE, ConvertPX(Y) * RATE);
                trans.setDuration(500);
                trans.setFillEnabled(true);
                trans.setFillAfter(true);

                iv.get(i).startAnimation(trans);
            }
        }

        /******ペアリング後動作要確認*******/
        for (Area.AreaView a : avList){

            TranslateAnimation trans = new TranslateAnimation(ConvertPX(oldX) * RATE, ConvertPX(X) * RATE, ConvertPX(oldY) * RATE, ConvertPX(Y) * RATE);
            trans.setDuration(500);
            trans.setFillEnabled(true);
            trans.setFillAfter(true);

            a.startAnimation(trans);
        }

        oldX = X;
        oldY = Y;
    }

    public void updateMap(){
        int itr = 0;
        absLayout.removeAllViews();
        now = getActiveLocation();

        int nowX = now.getX(); //DP変換ズミ
        int nowY = now.getY();

        int iconSize = iv.get(0).getRight() - iv.get(0).getLeft();

        for (Location l : getAllLocation()){ //下のifは負荷軽減の為.
            if  (width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX < 2*width + iconSize
                    && width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX > -iconSize
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY < 2*height + iconSize
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY > -iconSize) {
                int x = l.getX();//描画座標の設定
                int y = l.getY();
                ImageView tmp = iv.get(itr);
                tmp.setImageResource(R.drawable.tag); //使用画像の設定
                for (Location l2 : route) {
                    if (l.getId() == l2.getId()) tmp.setImageResource(R.drawable.tag2); //使用画像の設定
                }
                absLayout.addView(tmp, new AbsoluteLayout.LayoutParams(WC, WC,
                        width + ConvertPX((x - nowX)) * RATE - testTag.getWidth() / 2 + trackedX,
                        height + ConvertPX((y - nowY)) * RATE - testTag.getHeight() / 2 + trackedY));
            }
            itr++;
        }

        avList.clear();
        for (Area a : area){
            a.updatePaths(RATE);
            Area.AreaView av = a.createView();
            //av.setBackgroundColor(Color.GRAY);
            absLayout.addView(av, (new AbsoluteLayout.LayoutParams(WC, WC,
                    width + ConvertPX(( - nowX)) * RATE + trackedX,
                    height + ConvertPX(( - nowY)) * RATE + trackedY)));
            avList.add(av);
        }

        absLayout.addView(linLayout, (new AbsoluteLayout.LayoutParams(WC,WC
                , calculateImagesPoint(width + trackedX, height + trackedY, linLayout.getWidth(), linLayout.getHeight()).x
                , calculateImagesPoint(width + trackedX, height + trackedY, linLayout.getWidth(), linLayout.getHeight()).y)));

        //meの表示
        me.bringToFront();
        absLayout.addView(me, (new AbsoluteLayout.LayoutParams(WC, WC, centerX + trackedX, centerY + trackedY)));
        entry.x = nowX;
        entry.y = nowY;
    }

    public void updateMapNonAnimation(int id){
        int itr = 0;
        absLayout.removeAllViews();

        setActiveLocation(id);
        now = getActiveLocation();

        int nowX = now.getX(); //DP変換ズミ
        int nowY = now.getY();

        int iconSize = iv.get(0).getRight() - iv.get(0).getLeft();

        for (Location l : getAllLocation()){ //下のifは負荷軽減の為.
            if  (width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX < 2*width + iconSize
                    && width  + ConvertPX( (l.getX()-nowX)) * RATE + trackedX > -iconSize
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY < 2*height + iconSize
                    && height  + ConvertPX( (l.getY()-nowY)) * RATE + trackedY > -iconSize) {
                int x = l.getX();//描画座標の設定
                int y = l.getY();
                iv.get(itr).setImageResource(R.drawable.tag); //使用画像の設定

                for (Location l2 : route) {
                    if (l.getId() == l2.getId()){
                        iv.get(itr).setImageResource(R.drawable.tag2); //使用画像の設定
                        break;
                    }
                }
                absLayout.addView(iv.get(itr), new AbsoluteLayout.LayoutParams(WC, WC,
                        width + ConvertPX((x - nowX)) * RATE - testTag.getWidth() / 2 + trackedX,
                        height + ConvertPX((y - nowY)) * RATE - testTag.getHeight() / 2 + trackedY));
            }
            itr++;
        }

        avList.clear();
        for (Area a : area){
            a.updatePaths(RATE);
            Area.AreaView av = a.createView();

            //av.setBackgroundColor(Color.GRAY);
            absLayout.addView(av, (new AbsoluteLayout.LayoutParams(WC, WC,
                    width + ConvertPX(( - nowX)) * RATE + trackedX,
                    height + ConvertPX(( - nowY)) * RATE + trackedY)));

            avList.add(av);
        }

        absLayout.addView(linLayout, (new AbsoluteLayout.LayoutParams(WC,WC
                , calculateImagesPoint(width + trackedX, height + trackedY, linLayout.getWidth(), linLayout.getHeight()).x
                , calculateImagesPoint(width + trackedX, height + trackedY, linLayout.getWidth(), linLayout.getHeight()).y)));

        //meの表示
        me.bringToFront();
        absLayout.addView(me, (new AbsoluteLayout.LayoutParams(WC, WC, centerX + trackedX, centerY + trackedY)));
        entry.x = nowX;
        entry.y = nowY;
    }



    public void updateTrackedXY(int x,int y) {
        int subX, subY;

        if(!loaded || demo) return;
        subX = x - oldTrackedX;
        subY = y - oldTrackedY;

        if (Math.abs(subX) < 150 && Math.abs(subY) < 150){
            trackedX += subX;
            trackedY += subY;
            updateMap();
        }
        /**************TEMPORARY*****************
         if  (trackedX < -1000) trackedX = -1000;
         if  (trackedX > 300) trackedX = 300;
         if  (trackedY > 300) trackedY = 300;
         if  (trackedY < -300) trackedY = -300;
         /***************************************/

        oldTrackedX = x;
        oldTrackedY = y;
    }

    public void setImagesPalameter(){

        width = (absLayout.getWidth())/2;
        //- IconSize/2;
        height = (absLayout.getHeight())/2;
        //- IconSize/2;
        centerX = width - me.getWidth()/2 ;
        centerY = height - me.getHeight()/2 - (me.getWidth())/6;
    }

    public Point calculateImagesPoint(int x,int y,float imageSizeX,float imageSizeY){
        Point point = new Point((int)(x - imageSizeX/2),(int)(y - imageSizeY/2));
        return point;
    }

    //dp→px
    private int ConvertPX(int dp){
        return (int) TypedValue.applyDimension(TypedValue. COMPLEX_UNIT_DIP, dp, BlueC.getResources().getDisplayMetrics());
    }

    //px→dp
    private int ConvertDP(int px){
        return (int)(px / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, BlueC.getResources().getDisplayMetrics()));
    }


    public boolean routeNavigation(int end , int start ,boolean flag){
        task.clear(); //処理待ちのクリア
        route.clear(); //ルートのクリア
        for (Location l : getAllLocation() ) l.initNavigation(); //各TAGの最小距離クリア
        if(getLocationById(start) == null || getLocationById(end) == null) return false; //エラー処理
        task.offer(getLocationById(start)); //スタート地点を処理待ちキューに追加
        task.peek().setMinimumDistance(0); //start地点のの距離をクリア

        while (!task.isEmpty()){ //処理待ちのタグが無くなるまで
            Location l1 = task.peek();
            for (Location l : getLocationById(task.peek().getId()).getAroundLocation() ){ //処理待ちのタグに隣接するタグを列挙
                if  (l != null){
                    if  (task.peek().getMinimumDistance() + 1 < l.getMinimumDistance()) { //もしそのタグをつないだ際にこれまでより短ければ
                        l.setLocationToGoal(task.peek());
                        l.setMinimumDistance(task.peek().getMinimumDistance() + 1);
                        task.offer(l);
                    }
                }
            }
            task.poll();
        }

        Location goal = getLocationById(end);
        Location itr = goal;
        while (itr.getId() != getLocationById(start).getId()){
            route.add(itr);
            if  ((itr = itr.getLocationToGoal()) == null) return false;
        }
        route.add(getLocationById(start));

        /*******************************DEMO********************************/

        demo = flag;
        itrForDemo = 0;

        if  (!initTimer) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {//TODO Auto-generated method stub
                    handler.post(new Runnable() {
                        @Override
                        public void run() {// TODO Auto-generated method stub
                            // 描画処理を指示
                            //long currTime = System.currentTimeMillis();
                            //Log.d("d",String.valueOf(currTime));
                            if (demo) {
                                updateMap(route.get(itrForDemo++).getId());
                                if (route.size() == itrForDemo) {
                                    demo = false;
                                }
                            }
                        }
                    });
                }
            }, 2000, 1000); // 初回起動の遅延(2sec)と周期(1sec)指定
        }

        initTimer = true;

        /********************************************************************/

        displayRoute(end, route);
        return true;
    }

    public void displayRoute(int id, List<Location> route){

        /*********: clearする必要ある？？ :*********/
        iv.clear();

        setActiveLocation(id);
        absLayout.removeAllViews();

        now = getLocationById(id);


        int nowX = now.getX(); //DP変換ズミ
        int nowY = now.getY();


        for (Location l : getAllLocation()){
            int x = l.getX();//描画座標の設定
            int y = l.getY();
            iv.add(new ImageView(BlueC));
            iv.get(iv.size()-1).setImageResource(R.drawable.tag); //使用画像の設定

            for(Location l2 : route){
                if(l.getId() == l2.getId()){
                    iv.get(iv.size()-1).setImageResource(R.drawable.tag2); //使用画像の設定
                    break;
                }
            }

            absLayout.addView(iv.get(iv.size()-1), new AbsoluteLayout.LayoutParams(WC, WC,
                    width + ConvertPX( (x-nowX)) * RATE  - testTag.getWidth() / 2 , height + ConvertPX( (y-nowY)) * RATE  - testTag.getHeight() / 2 ));
        }

        avList.clear();
        for (Area a : area){
            a.updatePaths(RATE);
            Area.AreaView av = a.createView();
            //av.setBackgroundColor(Color.GRAY);
            absLayout.addView(av, (new AbsoluteLayout.LayoutParams(WC, WC,
                    width + ConvertPX(( - nowX)) * RATE,
                    height + ConvertPX(( - nowY)) * RATE)));
            avList.add(av);
        }

        //gifの表示
        linLayout.setScaleX(gifSize);
        linLayout.setScaleY(gifSize);
        linLayout.setVisibility(View.VISIBLE);
        absLayout.addView(linLayout, (new AbsoluteLayout.LayoutParams(WC,WC
                , calculateImagesPoint(width, height, linLayout.getWidth(), linLayout.getHeight()).x
                , calculateImagesPoint(width, height, linLayout.getWidth(), linLayout.getHeight()).y)));

        //meの表示
        me.bringToFront();
        me.setVisibility(View.VISIBLE);
        absLayout.addView(me, (new AbsoluteLayout.LayoutParams(WC, WC, centerX, centerY)));
        entry.x = now.getX();
        entry.y = now.getY();
    }

    private double getDistanceFromHere(int id){
        int nowX = getActiveLocation().getX();
        int nowY = getActiveLocation().getY();
        int toX = getLocationById(id).getX();
        int toY = getLocationById(id).getY();

        return Math.sqrt(Math.pow(toX-nowX, 2.0) + Math.pow(toY-nowY, 2.0));
    }

    public boolean isLoaded(){
        return loaded;
    }

    public void trackEnd(){
        oldTrackedX = trackedX;
        oldTrackedY = trackedY;
    }
}


