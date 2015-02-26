package com.example.android.STEPNavi;

import java.util.ArrayList;
import java.util.List;

public class Location {
    private int id;
    private int x,y;
    private int zoomX,zoomY;
    private int prezoomX,prezoomY;
    private int scrollX,scrollY;
    private String name;
    private List<String> information;
    private State state;
    private int minimumDistance;
    private Location locationToGoal;

    public Location(int initId, String str, int p1, int p2){
        id = initId;
        name = str;
        x = p1;
        y = p2;
        zoomX = x;
        zoomY = y;
        information = new ArrayList<String>();
        state = new State(0,0);
    }

    /***
     *
     * @param initId : タグID
     * @param str : タグネーム
     * @param p1 : x座標
     * @param p2 : y座標
     * @param s1 : 足元の状況
     * @param s2 : 壁の状況
     */
    public Location(int initId, String str, int p1, int p2, int s1, int s2){
        id = initId;
        name = str;
        x = p1;
        y = p2;
        zoomX=x;
        zoomY=y;
        prezoomX=zoomX;
        prezoomY=zoomY;
        scrollX=0;
        scrollY=0;
        //state = new State(s1, s2);
        information = new ArrayList<String>();
    }

    public void initState(Location[] s){
        state.setTag(s);
    }

    public boolean addInformation(String info){
        return information.add(info);
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }

    public int getZoomX(){
        return zoomX;
    }
    public int getZoomY(){
        return zoomY;
    }

    public int getGroundState(){
        return state.getGroundState();
    }

    public boolean[] getWallState(){
        return state.getWallState();
    }

    public State getState(){
        return state;
    }

    public Location[] getAroundLocation(){
        return state.getTagState();
    }

    public void initNavigation(){
        minimumDistance = 9999999;
    }

    public int getMinimumDistance(){
        return minimumDistance;
    }

    public void setMinimumDistance(int d){
        minimumDistance = d;
    }

    public void setLocationToGoal(Location l){
        locationToGoal = l;
    }

    public Location getLocationToGoal(){
        return locationToGoal;
    }
}
