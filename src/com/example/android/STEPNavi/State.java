package com.example.android.STEPNavi;

public class State {
    /***
     *  @param ground
     *  	未定
     *
     *
     *
     * @param wall
     * 		[0] : UP_WALL
     * 		[1] : RIGHT_WALL
     * 		[2] : BOTTOM_WALL
     * 		[3] : LEFT_WALL
     */

    private int ground;
    private boolean[] wall;
    private Location[] tag;


    /***
     *
     * @param gr : 地面の状態
     * @param wl : 4ビットで壁の状態を表現。壁なし:0000=0x0, 全部壁:1111=0xf, 上右下左の順番
     */
    public State(int gr, int wl){
        ground = gr;
        wall = new boolean[4];
        for (int i=0; i<4; i++){
            if  ((1 & wl) == 1) wall[i] = true;
            else wall[i] = false;
            wl = wl>>1;
        }
    }

    public void setTag(Location[] s){
        tag = new Location[4];
        tag = s;
    }

    public int getGroundState(){
        return ground;
    }

    public Location[] getTagState(){
        return tag;
    }

    public boolean[] getWallState(){
        return wall;
    }
}
