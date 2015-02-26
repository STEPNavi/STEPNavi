/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.STEPNavi;

import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.example.android.BluetoothChat.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsoluteLayout;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This is the main Activity that displays the current chat session.
 */
@SuppressWarnings("deprecation")
public class BluetoothChat extends Activity implements FragmentLocation.OnFragmentInteractionListener{
    // Debugging
    private static final String TAG  = "BluetoothChat";
    private static final String TAG2 = "test1";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int POINT_SELECT = 4;
	private static final int SEARCH_LOCATION = 5;

    //Intent result codes
    private static final int RESULT_DEMO = 1;
    private static final int RESULT_SEARCH = 2;

    //表示関連
    private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
    private final int IconSize = 150;
    private static final int UPDATE_INTERVAL = 1;
    private int updateFlg = 0;

    // Layout Views
    private BootstrapEditText mOutEditText;
    private TextView tagId;
    private TextView tagName;

    //Map
    private Map testMap;
    private int tagNum = 0;

    private WindowManager wm;
    private Display disp;
    private AbsoluteLayout absoluteLayout;
    private LinearLayout linearLayout;
    private RelativeLayout relativeLayout;

    //2015/01/11

    private int startingpoint = 0; //スタート地点
    private int goalpoint = 0; //ゴール地点
    private WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    private BluetoothChat BC;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    //action bar
    //private EditText startEd;
	private ArrayList<String> Name;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        //camel : appleAndOrange : Method, Field
        //Pascal : AppleAndOrange : Class, Object

        BC = this;
        wm = getWindowManager();
        disp = wm.getDefaultDisplay();
        absoluteLayout = (AbsoluteLayout) findViewById(R.id.AbsoluteLayout);

        //gifセット
        linearLayout = (LinearLayout) findViewById(R.id.LinearLayout);
        GifView GV = new GifView(this);
        GV.setGif(R.drawable.now);
        linearLayout.addView(GV);
        linearLayout.setVisibility(View.INVISIBLE);


        Init();
        testMap.setMeObject( (ImageView)findViewById(R.id.me) );
        testMap.setTestTagObject( (ImageView)findViewById(R.id.TEST_TAG) );

        relativeLayout = (RelativeLayout) findViewById(R.id.RelativeLayout);

        testMap.setAbsoluteLayout(absoluteLayout);
        testMap.setLinearLayout(linearLayout);


        //ButtonとEditText関連(スタート地点指定)
        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ボタンがクリックされた時に呼び出されます
                button.setFocusable(true);
                button.setFocusableInTouchMode(true);
                button.requestFocus();
                BootstrapEditText edit = (BootstrapEditText) findViewById(R.id.editText1);
                String str = edit.getText().toString();
                try {
                    if (str.equals("")) startingpoint = 0;
                    else startingpoint = Integer.parseInt(str);
                    if (testMap.getLocationById(startingpoint) == null) startingpoint = 0;
                    testMap.makeMap(startingpoint);
                } catch (Exception e){
                    startingpoint = 0;
                    if (testMap.getLocationById(startingpoint) == null) startingpoint = 0;
                    testMap.makeMap(startingpoint);
                    Toast.makeText(BC, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        BootstrapEditText et = (BootstrapEditText) findViewById(R.id.editText1);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // EditTextのフォーカスが外れた場合
                if (hasFocus == false) {
                    // 処理を行う
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

		mOutEditText = (BootstrapEditText)findViewById(R.id.editText1);
		mOutEditText.setOnClickListener(SearchEdit);
		mOutEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				String text = mOutEditText.getText().toString();
				ArrayList<String> newName = new ArrayList<String>();

				/*部分一致
				for (String tmp : Name) {
					if (text.indexOf(tmp) != -1) newName.add(tmp);
				}*/

				//前方一致
				for (String tmp : Name)
					if (tmp.startsWith(text)) newName.add(tmp);

				FragmentLocation fragment = new FragmentLocation();

				Bundle bundle = new Bundle();
				if(mOutEditText.getText().length() == 0)
					bundle.putStringArrayList("LocationList", Name);
				else
					bundle.putStringArrayList("LocationList", newName);

				fragment.setArguments(bundle);

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.FragmentContainer, fragment, "LocationList");
				ft.commit();
			}
		});
    }

	private View.OnClickListener SearchEdit = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			FragmentLocation fragment = new FragmentLocation();
			Name = (ArrayList<String>)(testMap.getLocationName());

			Bundle bundle = new Bundle();
			bundle.putStringArrayList("LocationList", Name);
			fragment.setArguments(bundle);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(R.id.FragmentContainer, fragment, "LocationList");
			ft.commit();
		}
	};

    public void Init(){
        //CreateMap @TestData
        testMap = new Map("testMap", this);

        List<PointF> point = new ArrayList<PointF>();
        point.add(new PointF(10f,1f));
        point.add(new PointF(10f,9f));
        point.add(new PointF(20f,9f));
        point.add(new PointF(20f,1f));

        Point tp = new Point(15,5);

        testMap.addArea("西松屋", tp, point);

        List<PointF> point1 = new ArrayList<PointF>();
        point1.add(new PointF(10f,13f));
        point1.add(new PointF(10f,37f));
        point1.add(new PointF(29f,37f));
        point1.add(new PointF(29f,23f));
        point1.add(new PointF(17f,23f));
        point1.add(new PointF(17f,13f));

        Point tp1 = new Point(19,30);

        testMap.addArea("Loft", tp1, point1);

        List<PointF> point2 = new ArrayList<PointF>();
        point2.add(new PointF(23f,9f));
        point2.add(new PointF(24f,10f));
        point2.add(new PointF(30f,4f));
        point2.add(new PointF(42f,16f));
        point2.add(new PointF(43f,15f));
        point2.add(new PointF(30f,2f));

        Point tp2 = new Point(36,9);

        testMap.addArea("へ!?", tp2, point2);

        testMap.addLocation(0, "tag0",1,1);
        testMap.addLocation(1, "tag1",1,2);
        testMap.addLocation(2, "tag2",1,3);
        testMap.addLocation(3, "tag3",1,4);
        testMap.addLocation(4, "tag4",1,5);
        testMap.addLocation(5, "tag5",1,6);
        testMap.addLocation(6, "tag6",1,7);
        testMap.addLocation(7, "tag7",1,77);
        testMap.addLocation(8, "tag8",1,78);
        testMap.addLocation(9, "tag9",1,79);
        testMap.addLocation(10, "tag10",1,80);
        testMap.addLocation(11, "tag11",1,81);
        testMap.addLocation(12, "tag12",1,82);
        testMap.addLocation(13, "tag13",1,83);
        testMap.addLocation(14, "tag14",1,84);
        testMap.addLocation(15, "tag15",1,85);
        testMap.addLocation(16, "tag16",1,86);
        testMap.addLocation(17, "tag17",1,87);
        testMap.addLocation(18, "tag18",1,88);
        testMap.addLocation(19, "tag19",2,1);
        testMap.addLocation(20, "tag20",2,2);
        testMap.addLocation(21, "tag21",2,3);
        testMap.addLocation(22, "tag22",2,4);
        testMap.addLocation(23, "tag23",2,5);
        testMap.addLocation(24, "tag24",2,6);
        testMap.addLocation(25, "tag25",2,7);
        testMap.addLocation(26, "tag26",2,77);
        testMap.addLocation(27, "tag27",2,78);
        testMap.addLocation(28, "tag28",2,79);
        testMap.addLocation(29, "tag29",2,80);
        testMap.addLocation(30, "tag30",2,81);
        testMap.addLocation(31, "tag31",2,82);
        testMap.addLocation(32, "tag32",2,83);
        testMap.addLocation(33, "tag33",2,84);
        testMap.addLocation(34, "tag34",2,85);
        testMap.addLocation(35, "tag35",2,86);
        testMap.addLocation(36, "tag36",2,87);
        testMap.addLocation(37, "tag37",2,88);
        testMap.addLocation(38, "tag38",3,1);
        testMap.addLocation(39, "tag39",3,2);
        testMap.addLocation(40, "tag40",3,3);
        testMap.addLocation(41, "tag41",3,4);
        testMap.addLocation(42, "tag42",3,5);
        testMap.addLocation(43, "tag43",3,6);
        testMap.addLocation(44, "tag44",3,7);
        testMap.addLocation(45, "tag45",3,77);
        testMap.addLocation(46, "tag46",3,78);
        testMap.addLocation(47, "tag47",3,79);
        testMap.addLocation(48, "tag48",3,80);
        testMap.addLocation(49, "tag49",3,81);
        testMap.addLocation(50, "tag50",3,82);
        testMap.addLocation(51, "tag51",3,83);
        testMap.addLocation(52, "tag52",3,84);
        testMap.addLocation(53, "tag53",3,85);
        testMap.addLocation(54, "tag54",3,86);
        testMap.addLocation(55, "tag55",3,87);
        testMap.addLocation(56, "tag56",3,88);
        testMap.addLocation(57, "tag57",4,1);
        testMap.addLocation(58, "tag58",4,2);
        testMap.addLocation(59, "tag59",4,3);
        testMap.addLocation(60, "tag60",4,4);
        testMap.addLocation(61, "tag61",4,5);
        testMap.addLocation(62, "tag62",4,6);
        testMap.addLocation(63, "tag63",4,7);
        testMap.addLocation(64, "tag64",4,77);
        testMap.addLocation(65, "tag65",4,78);
        testMap.addLocation(66, "tag66",4,79);
        testMap.addLocation(67, "tag67",4,80);
        testMap.addLocation(68, "tag68",4,81);
        testMap.addLocation(69, "tag69",4,82);
        testMap.addLocation(70, "tag70",4,83);
        testMap.addLocation(71, "tag71",4,84);
        testMap.addLocation(72, "tag72",4,85);
        testMap.addLocation(73, "tag73",4,86);
        testMap.addLocation(74, "tag74",4,87);
        testMap.addLocation(75, "tag75",4,88);
        testMap.addLocation(76, "tag76",5,1);
        testMap.addLocation(77, "tag77",5,2);
        testMap.addLocation(78, "tag78",5,3);
        testMap.addLocation(79, "tag79",5,4);
        testMap.addLocation(80, "tag80",5,5);
        testMap.addLocation(81, "tag81",5,6);
        testMap.addLocation(82, "tag82",5,7);
        testMap.addLocation(83, "tag83",5,77);
        testMap.addLocation(84, "tag84",5,78);
        testMap.addLocation(85, "tag85",5,79);
        testMap.addLocation(86, "tag86",5,80);
        testMap.addLocation(87, "tag87",5,81);
        testMap.addLocation(88, "tag88",5,82);
        testMap.addLocation(89, "tag89",5,83);
        testMap.addLocation(90, "tag90",5,84);
        testMap.addLocation(91, "tag91",5,85);
        testMap.addLocation(92, "tag92",5,86);
        testMap.addLocation(93, "tag93",5,87);
        testMap.addLocation(94, "tag94",5,88);
        testMap.addLocation(95, "tag95",6,1);
        testMap.addLocation(96, "tag96",6,2);
        testMap.addLocation(97, "tag97",6,3);
        testMap.addLocation(98, "tag98",6,4);
        testMap.addLocation(99, "tag99",6,5);
        testMap.addLocation(100, "tag100",6,6);
        testMap.addLocation(101, "tag101",6,7);
        testMap.addLocation(102, "tag102",6,77);
        testMap.addLocation(103, "tag103",6,78);
        testMap.addLocation(104, "tag104",6,79);
        testMap.addLocation(105, "tag105",6,80);
        testMap.addLocation(106, "tag106",6,81);
        testMap.addLocation(107, "tag107",6,82);
        testMap.addLocation(108, "tag108",6,83);
        testMap.addLocation(109, "tag109",6,84);
        testMap.addLocation(110, "tag110",6,85);
        testMap.addLocation(111, "tag111",6,86);
        testMap.addLocation(112, "tag112",6,87);
        testMap.addLocation(113, "tag113",6,88);
        testMap.addLocation(114, "tag114",7,1);
        testMap.addLocation(115, "tag115",7,2);
        testMap.addLocation(116, "tag116",7,3);
        testMap.addLocation(117, "tag117",7,4);
        testMap.addLocation(118, "tag118",7,5);
        testMap.addLocation(119, "tag119",7,6);
        testMap.addLocation(120, "tag120",7,7);
        testMap.addLocation(121, "tag121",7,8);
        testMap.addLocation(122, "tag122",7,9);
        testMap.addLocation(123, "tag123",7,10);
        testMap.addLocation(124, "tag124",7,11);
        testMap.addLocation(125, "tag125",7,12);
        testMap.addLocation(126, "tag126",7,13);
        testMap.addLocation(127, "tag127",7,14);
        testMap.addLocation(128, "tag128",7,15);
        testMap.addLocation(129, "tag129",7,16);
        testMap.addLocation(130, "tag130",7,17);
        testMap.addLocation(131, "tag131",7,18);
        testMap.addLocation(132, "tag132",7,19);
        testMap.addLocation(133, "tag133",7,20);
        testMap.addLocation(134, "tag134",7,21);
        testMap.addLocation(135, "tag135",7,22);
        testMap.addLocation(136, "tag136",7,23);
        testMap.addLocation(137, "tag137",7,24);
        testMap.addLocation(138, "tag138",7,25);
        testMap.addLocation(139, "tag139",7,26);
        testMap.addLocation(140, "tag140",7,27);
        testMap.addLocation(141, "tag141",7,28);
        testMap.addLocation(142, "tag142",7,29);
        testMap.addLocation(143, "tag143",7,30);
        testMap.addLocation(144, "tag144",7,31);
        testMap.addLocation(145, "tag145",7,32);
        testMap.addLocation(146, "tag146",7,33);
        testMap.addLocation(147, "tag147",7,34);
        testMap.addLocation(148, "tag148",7,35);
        testMap.addLocation(149, "tag149",7,36);
        testMap.addLocation(150, "tag150",7,37);
        testMap.addLocation(151, "tag151",7,38);
        testMap.addLocation(152, "tag152",7,39);
        testMap.addLocation(153, "tag153",7,40);
        testMap.addLocation(154, "tag154",7,41);
        testMap.addLocation(155, "tag155",7,42);
        testMap.addLocation(156, "tag156",7,43);
        testMap.addLocation(157, "tag157",7,44);
        testMap.addLocation(158, "tag158",7,45);
        testMap.addLocation(159, "tag159",7,46);
        testMap.addLocation(160, "tag160",7,47);
        testMap.addLocation(161, "tag161",7,48);
        testMap.addLocation(162, "tag162",7,49);
        testMap.addLocation(163, "tag163",7,50);
        testMap.addLocation(164, "tag164",7,51);
        testMap.addLocation(165, "tag165",7,52);
        testMap.addLocation(166, "tag166",7,53);
        testMap.addLocation(167, "tag167",7,54);
        testMap.addLocation(168, "tag168",7,55);
        testMap.addLocation(169, "tag169",7,56);
        testMap.addLocation(170, "tag170",7,57);
        testMap.addLocation(171, "tag171",7,58);
        testMap.addLocation(172, "tag172",7,59);
        testMap.addLocation(173, "tag173",7,60);
        testMap.addLocation(174, "tag174",7,61);
        testMap.addLocation(175, "tag175",7,62);
        testMap.addLocation(176, "tag176",7,63);
        testMap.addLocation(177, "tag177",7,64);
        testMap.addLocation(178, "tag178",7,65);
        testMap.addLocation(179, "tag179",7,66);
        testMap.addLocation(180, "tag180",7,67);
        testMap.addLocation(181, "tag181",7,68);
        testMap.addLocation(182, "tag182",7,69);
        testMap.addLocation(183, "tag183",7,70);
        testMap.addLocation(184, "tag184",7,71);
        testMap.addLocation(185, "tag185",7,72);
        testMap.addLocation(186, "tag186",7,73);
        testMap.addLocation(187, "tag187",7,74);
        testMap.addLocation(188, "tag188",7,75);
        testMap.addLocation(189, "tag189",7,76);
        testMap.addLocation(190, "tag190",7,77);
        testMap.addLocation(191, "tag191",7,78);
        testMap.addLocation(192, "tag192",7,79);
        testMap.addLocation(193, "tag193",7,80);
        testMap.addLocation(194, "tag194",7,81);
        testMap.addLocation(195, "tag195",7,82);
        testMap.addLocation(196, "tag196",7,83);
        testMap.addLocation(197, "tag197",7,84);
        testMap.addLocation(198, "tag198",7,85);
        testMap.addLocation(199, "tag199",7,86);
        testMap.addLocation(200, "tag200",7,87);
        testMap.addLocation(201, "tag201",7,88);
        testMap.addLocation(202, "tag202",7,89);
        testMap.addLocation(203, "tag203",7,90);
        testMap.addLocation(204, "tag204",7,91);
        testMap.addLocation(205, "tag205",7,92);
        testMap.addLocation(206, "tag206",7,93);
        testMap.addLocation(207, "tag207",7,94);
        testMap.addLocation(208, "tag208",7,95);
        testMap.addLocation(209, "tag209",7,96);
        testMap.addLocation(210, "tag210",7,97);
        testMap.addLocation(211, "tag211",8,1);
        testMap.addLocation(212, "tag212",8,2);
        testMap.addLocation(213, "tag213",8,3);
        testMap.addLocation(214, "tag214",8,4);
        testMap.addLocation(215, "tag215",8,5);
        testMap.addLocation(216, "tag216",8,6);
        testMap.addLocation(217, "tag217",8,7);
        testMap.addLocation(218, "tag218",8,8);
        testMap.addLocation(219, "tag219",8,9);
        testMap.addLocation(220, "tag220",8,10);
        testMap.addLocation(221, "tag221",8,11);
        testMap.addLocation(222, "tag222",8,12);
        testMap.addLocation(223, "tag223",8,13);
        testMap.addLocation(224, "tag224",8,14);
        testMap.addLocation(225, "tag225",8,15);
        testMap.addLocation(226, "tag226",8,16);
        testMap.addLocation(227, "tag227",8,17);
        testMap.addLocation(228, "tag228",8,18);
        testMap.addLocation(229, "tag229",8,19);
        testMap.addLocation(230, "tag230",8,20);
        testMap.addLocation(231, "tag231",8,21);
        testMap.addLocation(232, "tag232",8,22);
        testMap.addLocation(233, "tag233",8,23);
        testMap.addLocation(234, "tag234",8,24);
        testMap.addLocation(235, "tag235",8,25);
        testMap.addLocation(236, "tag236",8,26);
        testMap.addLocation(237, "tag237",8,27);
        testMap.addLocation(238, "tag238",8,28);
        testMap.addLocation(239, "tag239",8,29);
        testMap.addLocation(240, "tag240",8,30);
        testMap.addLocation(241, "tag241",8,31);
        testMap.addLocation(242, "tag242",8,32);
        testMap.addLocation(243, "tag243",8,33);
        testMap.addLocation(244, "tag244",8,34);
        testMap.addLocation(245, "tag245",8,35);
        testMap.addLocation(246, "tag246",8,36);
        testMap.addLocation(247, "tag247",8,37);
        testMap.addLocation(248, "tag248",8,38);
        testMap.addLocation(249, "tag249",8,39);
        testMap.addLocation(250, "tag250",8,40);
        testMap.addLocation(251, "tag251",8,41);
        testMap.addLocation(252, "tag252",8,42);
        testMap.addLocation(253, "tag253",8,43);
        testMap.addLocation(254, "tag254",8,44);
        testMap.addLocation(255, "tag255",8,45);
        testMap.addLocation(256, "tag256",8,46);
        testMap.addLocation(257, "tag257",8,47);
        testMap.addLocation(258, "tag258",8,48);
        testMap.addLocation(259, "tag259",8,49);
        testMap.addLocation(260, "tag260",8,50);
        testMap.addLocation(261, "tag261",8,51);
        testMap.addLocation(262, "tag262",8,52);
        testMap.addLocation(263, "tag263",8,53);
        testMap.addLocation(264, "tag264",8,54);
        testMap.addLocation(265, "tag265",8,55);
        testMap.addLocation(266, "tag266",8,56);
        testMap.addLocation(267, "tag267",8,57);
        testMap.addLocation(268, "tag268",8,58);
        testMap.addLocation(269, "tag269",8,59);
        testMap.addLocation(270, "tag270",8,60);
        testMap.addLocation(271, "tag271",8,61);
        testMap.addLocation(272, "tag272",8,62);
        testMap.addLocation(273, "tag273",8,63);
        testMap.addLocation(274, "tag274",8,64);
        testMap.addLocation(275, "tag275",8,65);
        testMap.addLocation(276, "tag276",8,66);
        testMap.addLocation(277, "tag277",8,67);
        testMap.addLocation(278, "tag278",8,68);
        testMap.addLocation(279, "tag279",8,69);
        testMap.addLocation(280, "tag280",8,70);
        testMap.addLocation(281, "tag281",8,71);
        testMap.addLocation(282, "tag282",8,72);
        testMap.addLocation(283, "tag283",8,73);
        testMap.addLocation(284, "tag284",8,74);
        testMap.addLocation(285, "tag285",8,75);
        testMap.addLocation(286, "tag286",8,76);
        testMap.addLocation(287, "tag287",8,77);
        testMap.addLocation(288, "tag288",8,78);
        testMap.addLocation(289, "tag289",8,79);
        testMap.addLocation(290, "tag290",8,80);
        testMap.addLocation(291, "tag291",8,81);
        testMap.addLocation(292, "tag292",8,82);
        testMap.addLocation(293, "tag293",8,83);
        testMap.addLocation(294, "tag294",8,84);
        testMap.addLocation(295, "tag295",8,85);
        testMap.addLocation(296, "tag296",8,86);
        testMap.addLocation(297, "tag297",8,87);
        testMap.addLocation(298, "tag298",8,88);
        testMap.addLocation(299, "tag299",8,89);
        testMap.addLocation(300, "tag300",8,90);
        testMap.addLocation(301, "tag301",8,91);
        testMap.addLocation(302, "tag302",8,92);
        testMap.addLocation(303, "tag303",8,93);
        testMap.addLocation(304, "tag304",8,94);
        testMap.addLocation(305, "tag305",8,95);
        testMap.addLocation(306, "tag306",8,96);
        testMap.addLocation(307, "tag307",8,97);
        testMap.addLocation(308, "tag308",9,1);
        testMap.addLocation(309, "tag309",9,2);
        testMap.addLocation(310, "tag310",9,3);
        testMap.addLocation(311, "tag311",9,4);
        testMap.addLocation(312, "tag312",9,5);
        testMap.addLocation(313, "tag313",9,6);
        testMap.addLocation(314, "tag314",9,7);
        testMap.addLocation(315, "tag315",9,8);
        testMap.addLocation(316, "tag316",9,9);
        testMap.addLocation(317, "tag317",9,10);
        testMap.addLocation(318, "tag318",9,11);
        testMap.addLocation(319, "tag319",9,12);
        testMap.addLocation(320, "tag320",9,13);
        testMap.addLocation(321, "tag321",9,14);
        testMap.addLocation(322, "tag322",9,15);
        testMap.addLocation(323, "tag323",9,16);
        testMap.addLocation(324, "tag324",9,17);
        testMap.addLocation(325, "tag325",9,18);
        testMap.addLocation(326, "tag326",9,19);
        testMap.addLocation(327, "tag327",9,20);
        testMap.addLocation(328, "tag328",9,21);
        testMap.addLocation(329, "tag329",9,22);
        testMap.addLocation(330, "tag330",9,23);
        testMap.addLocation(331, "tag331",9,24);
        testMap.addLocation(332, "tag332",9,25);
        testMap.addLocation(333, "tag333",9,26);
        testMap.addLocation(334, "tag334",9,27);
        testMap.addLocation(335, "tag335",9,28);
        testMap.addLocation(336, "tag336",9,29);
        testMap.addLocation(337, "tag337",9,30);
        testMap.addLocation(338, "tag338",9,31);
        testMap.addLocation(339, "tag339",9,32);
        testMap.addLocation(340, "tag340",9,33);
        testMap.addLocation(341, "tag341",9,34);
        testMap.addLocation(342, "tag342",9,35);
        testMap.addLocation(343, "tag343",9,36);
        testMap.addLocation(344, "tag344",9,37);
        testMap.addLocation(345, "tag345",9,38);
        testMap.addLocation(346, "tag346",9,39);
        testMap.addLocation(347, "tag347",9,40);
        testMap.addLocation(348, "tag348",9,41);
        testMap.addLocation(349, "tag349",9,42);
        testMap.addLocation(350, "tag350",9,43);
        testMap.addLocation(351, "tag351",9,44);
        testMap.addLocation(352, "tag352",9,45);
        testMap.addLocation(353, "tag353",9,46);
        testMap.addLocation(354, "tag354",9,47);
        testMap.addLocation(355, "tag355",9,48);
        testMap.addLocation(356, "tag356",9,49);
        testMap.addLocation(357, "tag357",9,50);
        testMap.addLocation(358, "tag358",9,51);
        testMap.addLocation(359, "tag359",9,52);
        testMap.addLocation(360, "tag360",9,53);
        testMap.addLocation(361, "tag361",9,54);
        testMap.addLocation(362, "tag362",9,55);
        testMap.addLocation(363, "tag363",9,56);
        testMap.addLocation(364, "tag364",9,57);
        testMap.addLocation(365, "tag365",9,58);
        testMap.addLocation(366, "tag366",9,59);
        testMap.addLocation(367, "tag367",9,60);
        testMap.addLocation(368, "tag368",9,61);
        testMap.addLocation(369, "tag369",9,62);
        testMap.addLocation(370, "tag370",9,63);
        testMap.addLocation(371, "tag371",9,64);
        testMap.addLocation(372, "tag372",9,65);
        testMap.addLocation(373, "tag373",9,66);
        testMap.addLocation(374, "tag374",9,67);
        testMap.addLocation(375, "tag375",9,68);
        testMap.addLocation(376, "tag376",9,69);
        testMap.addLocation(377, "tag377",9,70);
        testMap.addLocation(378, "tag378",9,71);
        testMap.addLocation(379, "tag379",9,72);
        testMap.addLocation(380, "tag380",9,73);
        testMap.addLocation(381, "tag381",9,74);
        testMap.addLocation(382, "tag382",9,75);
        testMap.addLocation(383, "tag383",9,76);
        testMap.addLocation(384, "tag384",9,77);
        testMap.addLocation(385, "tag385",9,78);
        testMap.addLocation(386, "tag386",9,79);
        testMap.addLocation(387, "tag387",9,80);
        testMap.addLocation(388, "tag388",9,81);
        testMap.addLocation(389, "tag389",9,82);
        testMap.addLocation(390, "tag390",9,83);
        testMap.addLocation(391, "tag391",9,84);
        testMap.addLocation(392, "tag392",9,85);
        testMap.addLocation(393, "tag393",9,86);
        testMap.addLocation(394, "tag394",9,87);
        testMap.addLocation(395, "tag395",9,88);
        testMap.addLocation(396, "tag396",9,96);
        testMap.addLocation(397, "tag397",9,97);
        testMap.addLocation(398, "tag398",10,10);
        testMap.addLocation(399, "tag399",10,11);
        testMap.addLocation(400, "tag400",10,12);
        testMap.addLocation(401, "tag401",10,38);
        testMap.addLocation(402, "tag402",10,39);
        testMap.addLocation(403, "tag403",10,40);
        testMap.addLocation(404, "tag404",10,41);
        testMap.addLocation(405, "tag405",10,42);
        testMap.addLocation(406, "tag406",10,43);
        testMap.addLocation(407, "tag407",10,44);
        testMap.addLocation(408, "tag408",10,45);
        testMap.addLocation(409, "tag409",10,77);
        testMap.addLocation(410, "tag410",10,78);
        testMap.addLocation(411, "tag411",10,79);
        testMap.addLocation(412, "tag412",10,80);
        testMap.addLocation(413, "tag413",10,81);
        testMap.addLocation(414, "tag414",10,82);
        testMap.addLocation(415, "tag415",10,83);
        testMap.addLocation(416, "tag416",10,84);
        testMap.addLocation(417, "tag417",10,85);
        testMap.addLocation(418, "tag418",10,86);
        testMap.addLocation(419, "tag419",10,87);
        testMap.addLocation(420, "tag420",10,88);
        testMap.addLocation(421, "tag421",10,96);
        testMap.addLocation(422, "tag422",10,97);
        testMap.addLocation(423, "tag423",11,10);
        testMap.addLocation(424, "tag424",11,11);
        testMap.addLocation(425, "tag425",11,12);
        testMap.addLocation(426, "tag426",11,38);
        testMap.addLocation(427, "tag427",11,39);
        testMap.addLocation(428, "tag428",11,40);
        testMap.addLocation(429, "tag429",11,41);
        testMap.addLocation(430, "tag430",11,42);
        testMap.addLocation(431, "tag431",11,43);
        testMap.addLocation(432, "tag432",11,44);
        testMap.addLocation(433, "tag433",11,45);
        testMap.addLocation(434, "tag434",11,77);
        testMap.addLocation(435, "tag435",11,78);
        testMap.addLocation(436, "tag436",11,79);
        testMap.addLocation(437, "tag437",11,80);
        testMap.addLocation(438, "tag438",11,81);
        testMap.addLocation(439, "tag439",11,82);
        testMap.addLocation(440, "tag440",11,83);
        testMap.addLocation(441, "tag441",11,84);
        testMap.addLocation(442, "tag442",11,85);
        testMap.addLocation(443, "tag443",11,86);
        testMap.addLocation(444, "tag444",11,87);
        testMap.addLocation(445, "tag445",11,88);
        testMap.addLocation(446, "tag446",11,96);
        testMap.addLocation(447, "tag447",11,97);
        testMap.addLocation(448, "tag448",12,10);
        testMap.addLocation(449, "tag449",12,11);
        testMap.addLocation(450, "tag450",12,12);
        testMap.addLocation(451, "tag451",12,38);
        testMap.addLocation(452, "tag452",12,39);
        testMap.addLocation(453, "tag453",12,40);
        testMap.addLocation(454, "tag454",12,41);
        testMap.addLocation(455, "tag455",12,42);
        testMap.addLocation(456, "tag456",12,43);
        testMap.addLocation(457, "tag457",12,44);
        testMap.addLocation(458, "tag458",12,45);
        testMap.addLocation(459, "tag459",12,77);
        testMap.addLocation(460, "tag460",12,78);
        testMap.addLocation(461, "tag461",12,79);
        testMap.addLocation(462, "tag462",12,80);
        testMap.addLocation(463, "tag463",12,81);
        testMap.addLocation(464, "tag464",12,82);
        testMap.addLocation(465, "tag465",12,83);
        testMap.addLocation(466, "tag466",12,84);
        testMap.addLocation(467, "tag467",12,85);
        testMap.addLocation(468, "tag468",12,86);
        testMap.addLocation(469, "tag469",12,87);
        testMap.addLocation(470, "tag470",12,88);
        testMap.addLocation(471, "tag471",12,96);
        testMap.addLocation(472, "tag472",12,97);
        testMap.addLocation(473, "tag473",13,10);
        testMap.addLocation(474, "tag474",13,11);
        testMap.addLocation(475, "tag475",13,12);
        testMap.addLocation(476, "tag476",13,38);
        testMap.addLocation(477, "tag477",13,39);
        testMap.addLocation(478, "tag478",13,40);
        testMap.addLocation(479, "tag479",13,41);
        testMap.addLocation(480, "tag480",13,42);
        testMap.addLocation(481, "tag481",13,43);
        testMap.addLocation(482, "tag482",13,44);
        testMap.addLocation(483, "tag483",13,45);
        testMap.addLocation(484, "tag484",13,77);
        testMap.addLocation(485, "tag485",13,78);
        testMap.addLocation(486, "tag486",13,79);
        testMap.addLocation(487, "tag487",13,80);
        testMap.addLocation(488, "tag488",13,81);
        testMap.addLocation(489, "tag489",13,82);
        testMap.addLocation(490, "tag490",13,83);
        testMap.addLocation(491, "tag491",13,84);
        testMap.addLocation(492, "tag492",13,85);
        testMap.addLocation(493, "tag493",13,86);
        testMap.addLocation(494, "tag494",13,87);
        testMap.addLocation(495, "tag495",13,88);
        testMap.addLocation(496, "tag496",13,96);
        testMap.addLocation(497, "tag497",13,97);
        testMap.addLocation(498, "tag498",14,10);
        testMap.addLocation(499, "tag499",14,11);
        testMap.addLocation(500, "tag500",14,12);
        testMap.addLocation(501, "tag501",14,38);
        testMap.addLocation(502, "tag502",14,39);
        testMap.addLocation(503, "tag503",14,40);
        testMap.addLocation(504, "tag504",14,41);
        testMap.addLocation(505, "tag505",14,42);
        testMap.addLocation(506, "tag506",14,43);
        testMap.addLocation(507, "tag507",14,44);
        testMap.addLocation(508, "tag508",14,45);
        testMap.addLocation(509, "tag509",14,77);
        testMap.addLocation(510, "tag510",14,78);
        testMap.addLocation(511, "tag511",14,79);
        testMap.addLocation(512, "tag512",14,80);
        testMap.addLocation(513, "tag513",14,81);
        testMap.addLocation(514, "tag514",14,82);
        testMap.addLocation(515, "tag515",14,83);
        testMap.addLocation(516, "tag516",14,84);
        testMap.addLocation(517, "tag517",14,85);
        testMap.addLocation(518, "tag518",14,86);
        testMap.addLocation(519, "tag519",14,87);
        testMap.addLocation(520, "tag520",14,88);
        testMap.addLocation(521, "tag521",14,96);
        testMap.addLocation(522, "tag522",14,97);
        testMap.addLocation(523, "tag523",15,10);
        testMap.addLocation(524, "tag524",15,11);
        testMap.addLocation(525, "tag525",15,12);
        testMap.addLocation(526, "tag526",15,38);
        testMap.addLocation(527, "tag527",15,39);
        testMap.addLocation(528, "tag528",15,40);
        testMap.addLocation(529, "tag529",15,41);
        testMap.addLocation(530, "tag530",15,42);
        testMap.addLocation(531, "tag531",15,43);
        testMap.addLocation(532, "tag532",15,44);
        testMap.addLocation(533, "tag533",15,45);
        testMap.addLocation(534, "tag534",15,77);
        testMap.addLocation(535, "tag535",15,78);
        testMap.addLocation(536, "tag536",15,79);
        testMap.addLocation(537, "tag537",15,80);
        testMap.addLocation(538, "tag538",15,81);
        testMap.addLocation(539, "tag539",15,82);
        testMap.addLocation(540, "tag540",15,83);
        testMap.addLocation(541, "tag541",15,84);
        testMap.addLocation(542, "tag542",15,85);
        testMap.addLocation(543, "tag543",15,86);
        testMap.addLocation(544, "tag544",15,87);
        testMap.addLocation(545, "tag545",15,88);
        testMap.addLocation(546, "tag546",15,89);
        testMap.addLocation(547, "tag547",15,90);
        testMap.addLocation(548, "tag548",15,96);
        testMap.addLocation(549, "tag549",15,97);
        testMap.addLocation(550, "tag550",16,10);
        testMap.addLocation(551, "tag551",16,11);
        testMap.addLocation(552, "tag552",16,12);
        testMap.addLocation(553, "tag553",16,38);
        testMap.addLocation(554, "tag554",16,39);
        testMap.addLocation(555, "tag555",16,40);
        testMap.addLocation(556, "tag556",16,41);
        testMap.addLocation(557, "tag557",16,42);
        testMap.addLocation(558, "tag558",16,43);
        testMap.addLocation(559, "tag559",16,44);
        testMap.addLocation(560, "tag560",16,45);
        testMap.addLocation(561, "tag561",16,77);
        testMap.addLocation(562, "tag562",16,78);
        testMap.addLocation(563, "tag563",16,79);
        testMap.addLocation(564, "tag564",16,80);
        testMap.addLocation(565, "tag565",16,81);
        testMap.addLocation(566, "tag566",16,82);
        testMap.addLocation(567, "tag567",16,83);
        testMap.addLocation(568, "tag568",16,84);
        testMap.addLocation(569, "tag569",16,85);
        testMap.addLocation(570, "tag570",16,86);
        testMap.addLocation(571, "tag571",16,87);
        testMap.addLocation(572, "tag572",16,88);
        testMap.addLocation(573, "tag573",16,89);
        testMap.addLocation(574, "tag574",16,90);
        testMap.addLocation(575, "tag575",16,96);
        testMap.addLocation(576, "tag576",16,97);
        testMap.addLocation(577, "tag577",17,10);
        testMap.addLocation(578, "tag578",17,11);
        testMap.addLocation(579, "tag579",17,12);
        testMap.addLocation(580, "tag580",17,38);
        testMap.addLocation(581, "tag581",17,39);
        testMap.addLocation(582, "tag582",17,40);
        testMap.addLocation(583, "tag583",17,41);
        testMap.addLocation(584, "tag584",17,42);
        testMap.addLocation(585, "tag585",17,43);
        testMap.addLocation(586, "tag586",17,44);
        testMap.addLocation(587, "tag587",17,45);
        testMap.addLocation(588, "tag588",17,46);
        testMap.addLocation(589, "tag589",17,47);
        testMap.addLocation(590, "tag590",17,48);
        testMap.addLocation(591, "tag591",17,49);
        testMap.addLocation(592, "tag592",17,50);
        testMap.addLocation(593, "tag593",17,51);
        testMap.addLocation(594, "tag594",17,52);
        testMap.addLocation(595, "tag595",17,53);
        testMap.addLocation(596, "tag596",17,54);
        testMap.addLocation(597, "tag597",17,55);
        testMap.addLocation(598, "tag598",17,56);
        testMap.addLocation(599, "tag599",17,57);
        testMap.addLocation(600, "tag600",17,58);
        testMap.addLocation(601, "tag601",17,59);
        testMap.addLocation(602, "tag602",17,60);
        testMap.addLocation(603, "tag603",17,77);
        testMap.addLocation(604, "tag604",17,78);
        testMap.addLocation(605, "tag605",17,79);
        testMap.addLocation(606, "tag606",17,80);
        testMap.addLocation(607, "tag607",17,81);
        testMap.addLocation(608, "tag608",17,82);
        testMap.addLocation(609, "tag609",17,83);
        testMap.addLocation(610, "tag610",17,84);
        testMap.addLocation(611, "tag611",17,85);
        testMap.addLocation(612, "tag612",17,86);
        testMap.addLocation(613, "tag613",17,87);
        testMap.addLocation(614, "tag614",17,88);
        testMap.addLocation(615, "tag615",17,89);
        testMap.addLocation(616, "tag616",17,90);
        testMap.addLocation(617, "tag617",17,96);
        testMap.addLocation(618, "tag618",17,97);
        testMap.addLocation(619, "tag619",18,10);
        testMap.addLocation(620, "tag620",18,11);
        testMap.addLocation(621, "tag621",18,12);
        testMap.addLocation(622, "tag622",18,13);
        testMap.addLocation(623, "tag623",18,14);
        testMap.addLocation(624, "tag624",18,15);
        testMap.addLocation(625, "tag625",18,16);
        testMap.addLocation(626, "tag626",18,17);
        testMap.addLocation(627, "tag627",18,18);
        testMap.addLocation(628, "tag628",18,19);
        testMap.addLocation(629, "tag629",18,20);
        testMap.addLocation(630, "tag630",18,21);
        testMap.addLocation(631, "tag631",18,22);
        testMap.addLocation(632, "tag632",18,38);
        testMap.addLocation(633, "tag633",18,39);
        testMap.addLocation(634, "tag634",18,40);
        testMap.addLocation(635, "tag635",18,41);
        testMap.addLocation(636, "tag636",18,42);
        testMap.addLocation(637, "tag637",18,43);
        testMap.addLocation(638, "tag638",18,44);
        testMap.addLocation(639, "tag639",18,45);
        testMap.addLocation(640, "tag640",18,46);
        testMap.addLocation(641, "tag641",18,47);
        testMap.addLocation(642, "tag642",18,48);
        testMap.addLocation(643, "tag643",18,49);
        testMap.addLocation(644, "tag644",18,50);
        testMap.addLocation(645, "tag645",18,51);
        testMap.addLocation(646, "tag646",18,52);
        testMap.addLocation(647, "tag647",18,53);
        testMap.addLocation(648, "tag648",18,54);
        testMap.addLocation(649, "tag649",18,55);
        testMap.addLocation(650, "tag650",18,56);
        testMap.addLocation(651, "tag651",18,57);
        testMap.addLocation(652, "tag652",18,58);
        testMap.addLocation(653, "tag653",18,59);
        testMap.addLocation(654, "tag654",18,60);
        testMap.addLocation(655, "tag655",18,77);
        testMap.addLocation(656, "tag656",18,78);
        testMap.addLocation(657, "tag657",18,79);
        testMap.addLocation(658, "tag658",18,80);
        testMap.addLocation(659, "tag659",18,81);
        testMap.addLocation(660, "tag660",18,82);
        testMap.addLocation(661, "tag661",18,83);
        testMap.addLocation(662, "tag662",18,84);
        testMap.addLocation(663, "tag663",18,85);
        testMap.addLocation(664, "tag664",18,86);
        testMap.addLocation(665, "tag665",18,90);
        testMap.addLocation(666, "tag666",18,96);
        testMap.addLocation(667, "tag667",18,97);
        testMap.addLocation(668, "tag668",19,10);
        testMap.addLocation(669, "tag669",19,11);
        testMap.addLocation(670, "tag670",19,12);
        testMap.addLocation(671, "tag671",19,13);
        testMap.addLocation(672, "tag672",19,14);
        testMap.addLocation(673, "tag673",19,15);
        testMap.addLocation(674, "tag674",19,16);
        testMap.addLocation(675, "tag675",19,17);
        testMap.addLocation(676, "tag676",19,18);
        testMap.addLocation(677, "tag677",19,19);
        testMap.addLocation(678, "tag678",19,20);
        testMap.addLocation(679, "tag679",19,21);
        testMap.addLocation(680, "tag680",19,22);
        testMap.addLocation(681, "tag681",19,38);
        testMap.addLocation(682, "tag682",19,39);
        testMap.addLocation(683, "tag683",19,40);
        testMap.addLocation(684, "tag684",19,41);
        testMap.addLocation(685, "tag685",19,42);
        testMap.addLocation(686, "tag686",19,43);
        testMap.addLocation(687, "tag687",19,44);
        testMap.addLocation(688, "tag688",19,45);
        testMap.addLocation(689, "tag689",19,46);
        testMap.addLocation(690, "tag690",19,47);
        testMap.addLocation(691, "tag691",19,48);
        testMap.addLocation(692, "tag692",19,49);
        testMap.addLocation(693, "tag693",19,50);
        testMap.addLocation(694, "tag694",19,51);
        testMap.addLocation(695, "tag695",19,52);
        testMap.addLocation(696, "tag696",19,53);
        testMap.addLocation(697, "tag697",19,54);
        testMap.addLocation(698, "tag698",19,55);
        testMap.addLocation(699, "tag699",19,56);
        testMap.addLocation(700, "tag700",19,57);
        testMap.addLocation(701, "tag701",19,58);
        testMap.addLocation(702, "tag702",19,59);
        testMap.addLocation(703, "tag703",19,60);
        testMap.addLocation(704, "tag704",19,77);
        testMap.addLocation(705, "tag705",19,78);
        testMap.addLocation(706, "tag706",19,79);
        testMap.addLocation(707, "tag707",19,80);
        testMap.addLocation(708, "tag708",19,81);
        testMap.addLocation(709, "tag709",19,82);
        testMap.addLocation(710, "tag710",19,83);
        testMap.addLocation(711, "tag711",19,84);
        testMap.addLocation(712, "tag712",19,85);
        testMap.addLocation(713, "tag713",19,86);
        testMap.addLocation(714, "tag714",19,90);
        testMap.addLocation(715, "tag715",19,96);
        testMap.addLocation(716, "tag716",19,97);
        testMap.addLocation(717, "tag717",20,10);
        testMap.addLocation(718, "tag718",20,11);
        testMap.addLocation(719, "tag719",20,12);
        testMap.addLocation(720, "tag720",20,13);
        testMap.addLocation(721, "tag721",20,14);
        testMap.addLocation(722, "tag722",20,15);
        testMap.addLocation(723, "tag723",20,16);
        testMap.addLocation(724, "tag724",20,17);
        testMap.addLocation(725, "tag725",20,18);
        testMap.addLocation(726, "tag726",20,19);
        testMap.addLocation(727, "tag727",20,20);
        testMap.addLocation(728, "tag728",20,21);
        testMap.addLocation(729, "tag729",20,22);
        testMap.addLocation(730, "tag730",20,38);
        testMap.addLocation(731, "tag731",20,39);
        testMap.addLocation(732, "tag732",20,40);
        testMap.addLocation(733, "tag733",20,41);
        testMap.addLocation(734, "tag734",20,42);
        testMap.addLocation(735, "tag735",20,43);
        testMap.addLocation(736, "tag736",20,44);
        testMap.addLocation(737, "tag737",20,45);
        testMap.addLocation(738, "tag738",20,46);
        testMap.addLocation(739, "tag739",20,47);
        testMap.addLocation(740, "tag740",20,48);
        testMap.addLocation(741, "tag741",20,49);
        testMap.addLocation(742, "tag742",20,50);
        testMap.addLocation(743, "tag743",20,51);
        testMap.addLocation(744, "tag744",20,52);
        testMap.addLocation(745, "tag745",20,53);
        testMap.addLocation(746, "tag746",20,54);
        testMap.addLocation(747, "tag747",20,55);
        testMap.addLocation(748, "tag748",20,56);
        testMap.addLocation(749, "tag749",20,57);
        testMap.addLocation(750, "tag750",20,58);
        testMap.addLocation(751, "tag751",20,59);
        testMap.addLocation(752, "tag752",20,60);
        testMap.addLocation(753, "tag753",20,77);
        testMap.addLocation(754, "tag754",20,78);
        testMap.addLocation(755, "tag755",20,79);
        testMap.addLocation(756, "tag756",20,80);
        testMap.addLocation(757, "tag757",20,81);
        testMap.addLocation(758, "tag758",20,82);
        testMap.addLocation(759, "tag759",20,83);
        testMap.addLocation(760, "tag760",20,84);
        testMap.addLocation(761, "tag761",20,85);
        testMap.addLocation(762, "tag762",20,86);
        testMap.addLocation(763, "tag763",20,90);
        testMap.addLocation(764, "tag764",20,96);
        testMap.addLocation(765, "tag765",20,97);
        testMap.addLocation(766, "tag766",21,20);
        testMap.addLocation(767, "tag767",21,21);
        testMap.addLocation(768, "tag768",21,22);
        testMap.addLocation(769, "tag769",21,38);
        testMap.addLocation(770, "tag770",21,39);
        testMap.addLocation(771, "tag771",21,40);
        testMap.addLocation(772, "tag772",21,41);
        testMap.addLocation(773, "tag773",21,42);
        testMap.addLocation(774, "tag774",21,43);
        testMap.addLocation(775, "tag775",21,44);
        testMap.addLocation(776, "tag776",21,45);
        testMap.addLocation(777, "tag777",21,46);
        testMap.addLocation(778, "tag778",21,47);
        testMap.addLocation(779, "tag779",21,48);
        testMap.addLocation(780, "tag780",21,49);
        testMap.addLocation(781, "tag781",21,50);
        testMap.addLocation(782, "tag782",21,51);
        testMap.addLocation(783, "tag783",21,52);
        testMap.addLocation(784, "tag784",21,53);
        testMap.addLocation(785, "tag785",21,54);
        testMap.addLocation(786, "tag786",21,55);
        testMap.addLocation(787, "tag787",21,56);
        testMap.addLocation(788, "tag788",21,57);
        testMap.addLocation(789, "tag789",21,58);
        testMap.addLocation(790, "tag790",21,59);
        testMap.addLocation(791, "tag791",21,60);
        testMap.addLocation(792, "tag792",21,77);
        testMap.addLocation(793, "tag793",21,78);
        testMap.addLocation(794, "tag794",21,79);
        testMap.addLocation(795, "tag795",21,80);
        testMap.addLocation(796, "tag796",21,81);
        testMap.addLocation(797, "tag797",21,82);
        testMap.addLocation(798, "tag798",21,83);
        testMap.addLocation(799, "tag799",21,84);
        testMap.addLocation(800, "tag800",21,85);
        testMap.addLocation(801, "tag801",21,86);
        testMap.addLocation(802, "tag802",21,90);
        testMap.addLocation(803, "tag803",21,96);
        testMap.addLocation(804, "tag804",21,97);
        testMap.addLocation(805, "tag805",22,20);
        testMap.addLocation(806, "tag806",22,21);
        testMap.addLocation(807, "tag807",22,22);
        testMap.addLocation(808, "tag808",22,38);
        testMap.addLocation(809, "tag809",22,39);
        testMap.addLocation(810, "tag810",22,40);
        testMap.addLocation(811, "tag811",22,41);
        testMap.addLocation(812, "tag812",22,42);
        testMap.addLocation(813, "tag813",22,43);
        testMap.addLocation(814, "tag814",22,44);
        testMap.addLocation(815, "tag815",22,45);
        testMap.addLocation(816, "tag816",22,46);
        testMap.addLocation(817, "tag817",22,47);
        testMap.addLocation(818, "tag818",22,48);
        testMap.addLocation(819, "tag819",22,49);
        testMap.addLocation(820, "tag820",22,50);
        testMap.addLocation(821, "tag821",22,51);
        testMap.addLocation(822, "tag822",22,52);
        testMap.addLocation(823, "tag823",22,53);
        testMap.addLocation(824, "tag824",22,54);
        testMap.addLocation(825, "tag825",22,55);
        testMap.addLocation(826, "tag826",22,56);
        testMap.addLocation(827, "tag827",22,57);
        testMap.addLocation(828, "tag828",22,58);
        testMap.addLocation(829, "tag829",22,59);
        testMap.addLocation(830, "tag830",22,60);
        testMap.addLocation(831, "tag831",22,77);
        testMap.addLocation(832, "tag832",22,78);
        testMap.addLocation(833, "tag833",22,79);
        testMap.addLocation(834, "tag834",22,80);
        testMap.addLocation(835, "tag835",22,81);
        testMap.addLocation(836, "tag836",22,82);
        testMap.addLocation(837, "tag837",22,83);
        testMap.addLocation(838, "tag838",22,84);
        testMap.addLocation(839, "tag839",22,85);
        testMap.addLocation(840, "tag840",22,86);
        testMap.addLocation(841, "tag841",22,90);
        testMap.addLocation(842, "tag842",22,96);
        testMap.addLocation(843, "tag843",22,97);
        testMap.addLocation(844, "tag844",23,20);
        testMap.addLocation(845, "tag845",23,21);
        testMap.addLocation(846, "tag846",23,22);
        testMap.addLocation(847, "tag847",23,38);
        testMap.addLocation(848, "tag848",23,39);
        testMap.addLocation(849, "tag849",23,40);
        testMap.addLocation(850, "tag850",23,41);
        testMap.addLocation(851, "tag851",23,42);
        testMap.addLocation(852, "tag852",23,43);
        testMap.addLocation(853, "tag853",23,44);
        testMap.addLocation(854, "tag854",23,45);
        testMap.addLocation(855, "tag855",23,46);
        testMap.addLocation(856, "tag856",23,47);
        testMap.addLocation(857, "tag857",23,48);
        testMap.addLocation(858, "tag858",23,49);
        testMap.addLocation(859, "tag859",23,50);
        testMap.addLocation(860, "tag860",23,51);
        testMap.addLocation(861, "tag861",23,52);
        testMap.addLocation(862, "tag862",23,53);
        testMap.addLocation(863, "tag863",23,54);
        testMap.addLocation(864, "tag864",23,55);
        testMap.addLocation(865, "tag865",23,56);
        testMap.addLocation(866, "tag866",23,57);
        testMap.addLocation(867, "tag867",23,58);
        testMap.addLocation(868, "tag868",23,59);
        testMap.addLocation(869, "tag869",23,60);
        testMap.addLocation(870, "tag870",23,61);
        testMap.addLocation(871, "tag871",23,62);
        testMap.addLocation(872, "tag872",23,63);
        testMap.addLocation(873, "tag873",23,64);
        testMap.addLocation(874, "tag874",23,65);
        testMap.addLocation(875, "tag875",23,66);
        testMap.addLocation(876, "tag876",23,67);
        testMap.addLocation(877, "tag877",23,68);
        testMap.addLocation(878, "tag878",23,69);
        testMap.addLocation(879, "tag879",23,70);
        testMap.addLocation(880, "tag880",23,71);
        testMap.addLocation(881, "tag881",23,72);
        testMap.addLocation(882, "tag882",23,73);
        testMap.addLocation(883, "tag883",23,74);
        testMap.addLocation(884, "tag884",23,75);
        testMap.addLocation(885, "tag885",23,76);
        testMap.addLocation(886, "tag886",23,77);
        testMap.addLocation(887, "tag887",23,78);
        testMap.addLocation(888, "tag888",23,79);
        testMap.addLocation(889, "tag889",23,80);
        testMap.addLocation(890, "tag890",23,81);
        testMap.addLocation(891, "tag891",23,82);
        testMap.addLocation(892, "tag892",23,83);
        testMap.addLocation(893, "tag893",23,84);
        testMap.addLocation(894, "tag894",23,85);
        testMap.addLocation(895, "tag895",23,86);
        testMap.addLocation(896, "tag896",23,90);
        testMap.addLocation(897, "tag897",23,96);
        testMap.addLocation(898, "tag898",23,97);
        testMap.addLocation(899, "tag899",24,20);
        testMap.addLocation(900, "tag900",24,21);
        testMap.addLocation(901, "tag901",24,22);
        testMap.addLocation(902, "tag902",24,38);
        testMap.addLocation(903, "tag903",24,39);
        testMap.addLocation(904, "tag904",24,40);
        testMap.addLocation(905, "tag905",24,41);
        testMap.addLocation(906, "tag906",24,42);
        testMap.addLocation(907, "tag907",24,43);
        testMap.addLocation(908, "tag908",24,44);
        testMap.addLocation(909, "tag909",24,45);
        testMap.addLocation(910, "tag910",24,46);
        testMap.addLocation(911, "tag911",24,47);
        testMap.addLocation(912, "tag912",24,48);
        testMap.addLocation(913, "tag913",24,49);
        testMap.addLocation(914, "tag914",24,50);
        testMap.addLocation(915, "tag915",24,51);
        testMap.addLocation(916, "tag916",24,52);
        testMap.addLocation(917, "tag917",24,53);
        testMap.addLocation(918, "tag918",24,54);
        testMap.addLocation(919, "tag919",24,55);
        testMap.addLocation(920, "tag920",24,56);
        testMap.addLocation(921, "tag921",24,57);
        testMap.addLocation(922, "tag922",24,58);
        testMap.addLocation(923, "tag923",24,59);
        testMap.addLocation(924, "tag924",24,60);
        testMap.addLocation(925, "tag925",24,61);
        testMap.addLocation(926, "tag926",24,62);
        testMap.addLocation(927, "tag927",24,63);
        testMap.addLocation(928, "tag928",24,64);
        testMap.addLocation(929, "tag929",24,65);
        testMap.addLocation(930, "tag930",24,66);
        testMap.addLocation(931, "tag931",24,67);
        testMap.addLocation(932, "tag932",24,68);
        testMap.addLocation(933, "tag933",24,69);
        testMap.addLocation(934, "tag934",24,70);
        testMap.addLocation(935, "tag935",24,71);
        testMap.addLocation(936, "tag936",24,72);
        testMap.addLocation(937, "tag937",24,73);
        testMap.addLocation(938, "tag938",24,74);
        testMap.addLocation(939, "tag939",24,75);
        testMap.addLocation(940, "tag940",24,76);
        testMap.addLocation(941, "tag941",24,77);
        testMap.addLocation(942, "tag942",24,78);
        testMap.addLocation(943, "tag943",24,79);
        testMap.addLocation(944, "tag944",24,80);
        testMap.addLocation(945, "tag945",24,81);
        testMap.addLocation(946, "tag946",24,82);
        testMap.addLocation(947, "tag947",24,83);
        testMap.addLocation(948, "tag948",24,84);
        testMap.addLocation(949, "tag949",24,85);
        testMap.addLocation(950, "tag950",24,86);
        testMap.addLocation(951, "tag951",24,90);
        testMap.addLocation(952, "tag952",24,96);
        testMap.addLocation(953, "tag953",24,97);
        testMap.addLocation(954, "tag954",25,20);
        testMap.addLocation(955, "tag955",25,21);
        testMap.addLocation(956, "tag956",25,22);
        testMap.addLocation(957, "tag957",25,38);
        testMap.addLocation(958, "tag958",25,39);
        testMap.addLocation(959, "tag959",25,40);
        testMap.addLocation(960, "tag960",25,41);
        testMap.addLocation(961, "tag961",25,42);
        testMap.addLocation(962, "tag962",25,43);
        testMap.addLocation(963, "tag963",25,44);
        testMap.addLocation(964, "tag964",25,45);
        testMap.addLocation(965, "tag965",25,63);
        testMap.addLocation(966, "tag966",25,64);
        testMap.addLocation(967, "tag967",25,65);
        testMap.addLocation(968, "tag968",25,66);
        testMap.addLocation(969, "tag969",25,67);
        testMap.addLocation(970, "tag970",25,68);
        testMap.addLocation(971, "tag971",25,69);
        testMap.addLocation(972, "tag972",25,70);
        testMap.addLocation(973, "tag973",25,71);
        testMap.addLocation(974, "tag974",25,72);
        testMap.addLocation(975, "tag975",25,73);
        testMap.addLocation(976, "tag976",25,74);
        testMap.addLocation(977, "tag977",25,75);
        testMap.addLocation(978, "tag978",25,76);
        testMap.addLocation(979, "tag979",25,77);
        testMap.addLocation(980, "tag980",25,78);
        testMap.addLocation(981, "tag981",25,79);
        testMap.addLocation(982, "tag982",25,80);
        testMap.addLocation(983, "tag983",25,81);
        testMap.addLocation(984, "tag984",25,82);
        testMap.addLocation(985, "tag985",25,83);
        testMap.addLocation(986, "tag986",25,84);
        testMap.addLocation(987, "tag987",25,85);
        testMap.addLocation(988, "tag988",25,86);
        testMap.addLocation(989, "tag989",25,90);
        testMap.addLocation(990, "tag990",25,96);
        testMap.addLocation(991, "tag991",25,97);
        testMap.addLocation(992, "tag992",26,20);
        testMap.addLocation(993, "tag993",26,21);
        testMap.addLocation(994, "tag994",26,22);
        testMap.addLocation(995, "tag995",26,38);
        testMap.addLocation(996, "tag996",26,39);
        testMap.addLocation(997, "tag997",26,40);
        testMap.addLocation(998, "tag998",26,41);
        testMap.addLocation(999, "tag999",26,42);
        testMap.addLocation(1000, "tag1000",26,43);
        testMap.addLocation(1001, "tag1001",26,44);
        testMap.addLocation(1002, "tag1002",26,45);
        testMap.addLocation(1003, "tag1003",26,63);
        testMap.addLocation(1004, "tag1004",26,64);
        testMap.addLocation(1005, "tag1005",26,65);
        testMap.addLocation(1006, "tag1006",26,66);
        testMap.addLocation(1007, "tag1007",26,67);
        testMap.addLocation(1008, "tag1008",26,68);
        testMap.addLocation(1009, "tag1009",26,69);
        testMap.addLocation(1010, "tag1010",26,70);
        testMap.addLocation(1011, "tag1011",26,71);
        testMap.addLocation(1012, "tag1012",26,72);
        testMap.addLocation(1013, "tag1013",26,73);
        testMap.addLocation(1014, "tag1014",26,74);
        testMap.addLocation(1015, "tag1015",26,75);
        testMap.addLocation(1016, "tag1016",26,76);
        testMap.addLocation(1017, "tag1017",26,77);
        testMap.addLocation(1018, "tag1018",26,78);
        testMap.addLocation(1019, "tag1019",26,79);
        testMap.addLocation(1020, "tag1020",26,80);
        testMap.addLocation(1021, "tag1021",26,81);
        testMap.addLocation(1022, "tag1022",26,82);
        testMap.addLocation(1023, "tag1023",26,83);
        testMap.addLocation(1024, "tag1024",26,84);
        testMap.addLocation(1025, "tag1025",26,85);
        testMap.addLocation(1026, "tag1026",26,86);
        testMap.addLocation(1027, "tag1027",26,90);
        testMap.addLocation(1028, "tag1028",26,96);
        testMap.addLocation(1029, "tag1029",26,97);
        testMap.addLocation(1030, "tag1030",27,20);
        testMap.addLocation(1031, "tag1031",27,21);
        testMap.addLocation(1032, "tag1032",27,22);
        testMap.addLocation(1033, "tag1033",27,38);
        testMap.addLocation(1034, "tag1034",27,39);
        testMap.addLocation(1035, "tag1035",27,40);
        testMap.addLocation(1036, "tag1036",27,41);
        testMap.addLocation(1037, "tag1037",27,42);
        testMap.addLocation(1038, "tag1038",27,43);
        testMap.addLocation(1039, "tag1039",27,44);
        testMap.addLocation(1040, "tag1040",27,45);
        testMap.addLocation(1041, "tag1041",27,63);
        testMap.addLocation(1042, "tag1042",27,64);
        testMap.addLocation(1043, "tag1043",27,65);
        testMap.addLocation(1044, "tag1044",27,66);
        testMap.addLocation(1045, "tag1045",27,67);
        testMap.addLocation(1046, "tag1046",27,68);
        testMap.addLocation(1047, "tag1047",27,69);
        testMap.addLocation(1048, "tag1048",27,70);
        testMap.addLocation(1049, "tag1049",27,71);
        testMap.addLocation(1050, "tag1050",27,72);
        testMap.addLocation(1051, "tag1051",27,73);
        testMap.addLocation(1052, "tag1052",27,74);
        testMap.addLocation(1053, "tag1053",27,75);
        testMap.addLocation(1054, "tag1054",27,76);
        testMap.addLocation(1055, "tag1055",27,77);
        testMap.addLocation(1056, "tag1056",27,78);
        testMap.addLocation(1057, "tag1057",27,79);
        testMap.addLocation(1058, "tag1058",27,80);
        testMap.addLocation(1059, "tag1059",27,81);
        testMap.addLocation(1060, "tag1060",27,82);
        testMap.addLocation(1061, "tag1061",27,83);
        testMap.addLocation(1062, "tag1062",27,84);
        testMap.addLocation(1063, "tag1063",27,85);
        testMap.addLocation(1064, "tag1064",27,86);
        testMap.addLocation(1065, "tag1065",27,90);
        testMap.addLocation(1066, "tag1066",27,96);
        testMap.addLocation(1067, "tag1067",27,97);
        testMap.addLocation(1068, "tag1068",28,20);
        testMap.addLocation(1069, "tag1069",28,21);
        testMap.addLocation(1070, "tag1070",28,22);
        testMap.addLocation(1071, "tag1071",28,38);
        testMap.addLocation(1072, "tag1072",28,39);
        testMap.addLocation(1073, "tag1073",28,40);
        testMap.addLocation(1074, "tag1074",28,41);
        testMap.addLocation(1075, "tag1075",28,42);
        testMap.addLocation(1076, "tag1076",28,43);
        testMap.addLocation(1077, "tag1077",28,44);
        testMap.addLocation(1078, "tag1078",28,45);
        testMap.addLocation(1079, "tag1079",28,63);
        testMap.addLocation(1080, "tag1080",28,64);
        testMap.addLocation(1081, "tag1081",28,65);
        testMap.addLocation(1082, "tag1082",28,66);
        testMap.addLocation(1083, "tag1083",28,67);
        testMap.addLocation(1084, "tag1084",28,77);
        testMap.addLocation(1085, "tag1085",28,78);
        testMap.addLocation(1086, "tag1086",28,79);
        testMap.addLocation(1087, "tag1087",28,80);
        testMap.addLocation(1088, "tag1088",28,81);
        testMap.addLocation(1089, "tag1089",28,82);
        testMap.addLocation(1090, "tag1090",28,83);
        testMap.addLocation(1091, "tag1091",28,84);
        testMap.addLocation(1092, "tag1092",28,85);
        testMap.addLocation(1093, "tag1093",28,86);
        testMap.addLocation(1094, "tag1094",28,90);
        testMap.addLocation(1095, "tag1095",28,91);
        testMap.addLocation(1096, "tag1096",28,92);
        testMap.addLocation(1097, "tag1097",28,93);
        testMap.addLocation(1098, "tag1098",28,94);
        testMap.addLocation(1099, "tag1099",28,95);
        testMap.addLocation(1100, "tag1100",28,96);
        testMap.addLocation(1101, "tag1101",28,97);
        testMap.addLocation(1102, "tag1102",29,20);
        testMap.addLocation(1103, "tag1103",29,21);
        testMap.addLocation(1104, "tag1104",29,22);
        testMap.addLocation(1105, "tag1105",29,38);
        testMap.addLocation(1106, "tag1106",29,39);
        testMap.addLocation(1107, "tag1107",29,40);
        testMap.addLocation(1108, "tag1108",29,41);
        testMap.addLocation(1109, "tag1109",29,42);
        testMap.addLocation(1110, "tag1110",29,43);
        testMap.addLocation(1111, "tag1111",29,44);
        testMap.addLocation(1112, "tag1112",29,45);
        testMap.addLocation(1113, "tag1113",29,63);
        testMap.addLocation(1114, "tag1114",29,64);
        testMap.addLocation(1115, "tag1115",29,65);
        testMap.addLocation(1116, "tag1116",29,66);
        testMap.addLocation(1117, "tag1117",29,67);
        testMap.addLocation(1118, "tag1118",29,77);
        testMap.addLocation(1119, "tag1119",29,78);
        testMap.addLocation(1120, "tag1120",29,79);
        testMap.addLocation(1121, "tag1121",29,80);
        testMap.addLocation(1122, "tag1122",29,81);
        testMap.addLocation(1123, "tag1123",29,82);
        testMap.addLocation(1124, "tag1124",29,83);
        testMap.addLocation(1125, "tag1125",29,84);
        testMap.addLocation(1126, "tag1126",29,85);
        testMap.addLocation(1127, "tag1127",29,86);
        testMap.addLocation(1128, "tag1128",29,90);
        testMap.addLocation(1129, "tag1129",29,91);
        testMap.addLocation(1130, "tag1130",29,92);
        testMap.addLocation(1131, "tag1131",29,93);
        testMap.addLocation(1132, "tag1132",29,94);
        testMap.addLocation(1133, "tag1133",29,95);
        testMap.addLocation(1134, "tag1134",29,96);
        testMap.addLocation(1135, "tag1135",29,97);
        testMap.addLocation(1136, "tag1136",30,20);
        testMap.addLocation(1137, "tag1137",30,21);
        testMap.addLocation(1138, "tag1138",30,22);
        testMap.addLocation(1139, "tag1139",30,23);
        testMap.addLocation(1140, "tag1140",30,24);
        testMap.addLocation(1141, "tag1141",30,25);
        testMap.addLocation(1142, "tag1142",30,26);
        testMap.addLocation(1143, "tag1143",30,27);
        testMap.addLocation(1144, "tag1144",30,28);
        testMap.addLocation(1145, "tag1145",30,29);
        testMap.addLocation(1146, "tag1146",30,30);
        testMap.addLocation(1147, "tag1147",30,31);
        testMap.addLocation(1148, "tag1148",30,32);
        testMap.addLocation(1149, "tag1149",30,33);
        testMap.addLocation(1150, "tag1150",30,34);
        testMap.addLocation(1151, "tag1151",30,35);
        testMap.addLocation(1152, "tag1152",30,36);
        testMap.addLocation(1153, "tag1153",30,37);
        testMap.addLocation(1154, "tag1154",30,38);
        testMap.addLocation(1155, "tag1155",30,39);
        testMap.addLocation(1156, "tag1156",30,40);
        testMap.addLocation(1157, "tag1157",30,41);
        testMap.addLocation(1158, "tag1158",30,42);
        testMap.addLocation(1159, "tag1159",30,43);
        testMap.addLocation(1160, "tag1160",30,44);
        testMap.addLocation(1161, "tag1161",30,45);
        testMap.addLocation(1162, "tag1162",30,46);
        testMap.addLocation(1163, "tag1163",30,47);
        testMap.addLocation(1164, "tag1164",30,48);
        testMap.addLocation(1165, "tag1165",30,49);
        testMap.addLocation(1166, "tag1166",30,50);
        testMap.addLocation(1167, "tag1167",30,51);
        testMap.addLocation(1168, "tag1168",30,52);
        testMap.addLocation(1169, "tag1169",30,53);
        testMap.addLocation(1170, "tag1170",30,54);
        testMap.addLocation(1171, "tag1171",30,55);
        testMap.addLocation(1172, "tag1172",30,56);
        testMap.addLocation(1173, "tag1173",30,57);
        testMap.addLocation(1174, "tag1174",30,58);
        testMap.addLocation(1175, "tag1175",30,59);
        testMap.addLocation(1176, "tag1176",30,60);
        testMap.addLocation(1177, "tag1177",30,61);
        testMap.addLocation(1178, "tag1178",30,62);
        testMap.addLocation(1179, "tag1179",30,63);
        testMap.addLocation(1180, "tag1180",30,64);
        testMap.addLocation(1181, "tag1181",30,65);
        testMap.addLocation(1182, "tag1182",30,66);
        testMap.addLocation(1183, "tag1183",30,67);
        testMap.addLocation(1184, "tag1184",30,77);
        testMap.addLocation(1185, "tag1185",30,78);
        testMap.addLocation(1186, "tag1186",30,79);
        testMap.addLocation(1187, "tag1187",30,80);
        testMap.addLocation(1188, "tag1188",30,81);
        testMap.addLocation(1189, "tag1189",30,82);
        testMap.addLocation(1190, "tag1190",30,83);
        testMap.addLocation(1191, "tag1191",30,84);
        testMap.addLocation(1192, "tag1192",30,85);
        testMap.addLocation(1193, "tag1193",30,86);
        testMap.addLocation(1194, "tag1194",30,90);
        testMap.addLocation(1195, "tag1195",30,91);
        testMap.addLocation(1196, "tag1196",30,92);
        testMap.addLocation(1197, "tag1197",30,93);
        testMap.addLocation(1198, "tag1198",30,94);
        testMap.addLocation(1199, "tag1199",30,95);
        testMap.addLocation(1200, "tag1200",30,96);
        testMap.addLocation(1201, "tag1201",30,97);
        testMap.addLocation(1202, "tag1202",31,20);
        testMap.addLocation(1203, "tag1203",31,21);
        testMap.addLocation(1204, "tag1204",31,22);
        testMap.addLocation(1205, "tag1205",31,23);
        testMap.addLocation(1206, "tag1206",31,24);
        testMap.addLocation(1207, "tag1207",31,25);
        testMap.addLocation(1208, "tag1208",31,26);
        testMap.addLocation(1209, "tag1209",31,27);
        testMap.addLocation(1210, "tag1210",31,28);
        testMap.addLocation(1211, "tag1211",31,29);
        testMap.addLocation(1212, "tag1212",31,30);
        testMap.addLocation(1213, "tag1213",31,31);
        testMap.addLocation(1214, "tag1214",31,32);
        testMap.addLocation(1215, "tag1215",31,33);
        testMap.addLocation(1216, "tag1216",31,34);
        testMap.addLocation(1217, "tag1217",31,35);
        testMap.addLocation(1218, "tag1218",31,36);
        testMap.addLocation(1219, "tag1219",31,37);
        testMap.addLocation(1220, "tag1220",31,38);
        testMap.addLocation(1221, "tag1221",31,39);
        testMap.addLocation(1222, "tag1222",31,40);
        testMap.addLocation(1223, "tag1223",31,41);
        testMap.addLocation(1224, "tag1224",31,42);
        testMap.addLocation(1225, "tag1225",31,43);
        testMap.addLocation(1226, "tag1226",31,44);
        testMap.addLocation(1227, "tag1227",31,45);
        testMap.addLocation(1228, "tag1228",31,46);
        testMap.addLocation(1229, "tag1229",31,47);
        testMap.addLocation(1230, "tag1230",31,48);
        testMap.addLocation(1231, "tag1231",31,49);
        testMap.addLocation(1232, "tag1232",31,50);
        testMap.addLocation(1233, "tag1233",31,51);
        testMap.addLocation(1234, "tag1234",31,52);
        testMap.addLocation(1235, "tag1235",31,53);
        testMap.addLocation(1236, "tag1236",31,54);
        testMap.addLocation(1237, "tag1237",31,55);
        testMap.addLocation(1238, "tag1238",31,56);
        testMap.addLocation(1239, "tag1239",31,57);
        testMap.addLocation(1240, "tag1240",31,58);
        testMap.addLocation(1241, "tag1241",31,59);
        testMap.addLocation(1242, "tag1242",31,60);
        testMap.addLocation(1243, "tag1243",31,61);
        testMap.addLocation(1244, "tag1244",31,62);
        testMap.addLocation(1245, "tag1245",31,63);
        testMap.addLocation(1246, "tag1246",31,64);
        testMap.addLocation(1247, "tag1247",31,65);
        testMap.addLocation(1248, "tag1248",31,66);
        testMap.addLocation(1249, "tag1249",31,67);
        testMap.addLocation(1250, "tag1250",31,77);
        testMap.addLocation(1251, "tag1251",31,78);
        testMap.addLocation(1252, "tag1252",31,79);
        testMap.addLocation(1253, "tag1253",31,80);
        testMap.addLocation(1254, "tag1254",31,81);
        testMap.addLocation(1255, "tag1255",31,82);
        testMap.addLocation(1256, "tag1256",31,83);
        testMap.addLocation(1257, "tag1257",31,84);
        testMap.addLocation(1258, "tag1258",31,85);
        testMap.addLocation(1259, "tag1259",31,86);
        testMap.addLocation(1260, "tag1260",31,90);
        testMap.addLocation(1261, "tag1261",31,91);
        testMap.addLocation(1262, "tag1262",31,92);
        testMap.addLocation(1263, "tag1263",31,93);
        testMap.addLocation(1264, "tag1264",31,94);
        testMap.addLocation(1265, "tag1265",31,95);
        testMap.addLocation(1266, "tag1266",31,96);
        testMap.addLocation(1267, "tag1267",31,97);
        testMap.addLocation(1268, "tag1268",32,20);
        testMap.addLocation(1269, "tag1269",32,21);
        testMap.addLocation(1270, "tag1270",32,22);
        testMap.addLocation(1271, "tag1271",32,28);
        testMap.addLocation(1272, "tag1272",32,29);
        testMap.addLocation(1273, "tag1273",32,30);
        testMap.addLocation(1274, "tag1274",32,31);
        testMap.addLocation(1275, "tag1275",32,32);
        testMap.addLocation(1276, "tag1276",32,33);
        testMap.addLocation(1277, "tag1277",32,34);
        testMap.addLocation(1278, "tag1278",32,35);
        testMap.addLocation(1279, "tag1279",32,36);
        testMap.addLocation(1280, "tag1280",32,37);
        testMap.addLocation(1281, "tag1281",32,38);
        testMap.addLocation(1282, "tag1282",32,39);
        testMap.addLocation(1283, "tag1283",32,40);
        testMap.addLocation(1284, "tag1284",32,41);
        testMap.addLocation(1285, "tag1285",32,42);
        testMap.addLocation(1286, "tag1286",32,43);
        testMap.addLocation(1287, "tag1287",32,44);
        testMap.addLocation(1288, "tag1288",32,45);
        testMap.addLocation(1289, "tag1289",32,46);
        testMap.addLocation(1290, "tag1290",32,47);
        testMap.addLocation(1291, "tag1291",32,48);
        testMap.addLocation(1292, "tag1292",32,49);
        testMap.addLocation(1293, "tag1293",32,50);
        testMap.addLocation(1294, "tag1294",32,51);
        testMap.addLocation(1295, "tag1295",32,52);
        testMap.addLocation(1296, "tag1296",32,53);
        testMap.addLocation(1297, "tag1297",32,54);
        testMap.addLocation(1298, "tag1298",32,55);
        testMap.addLocation(1299, "tag1299",32,56);
        testMap.addLocation(1300, "tag1300",32,57);
        testMap.addLocation(1301, "tag1301",32,58);
        testMap.addLocation(1302, "tag1302",32,59);
        testMap.addLocation(1303, "tag1303",32,60);
        testMap.addLocation(1304, "tag1304",32,61);
        testMap.addLocation(1305, "tag1305",32,62);
        testMap.addLocation(1306, "tag1306",32,63);
        testMap.addLocation(1307, "tag1307",32,64);
        testMap.addLocation(1308, "tag1308",32,65);
        testMap.addLocation(1309, "tag1309",32,66);
        testMap.addLocation(1310, "tag1310",32,67);
        testMap.addLocation(1311, "tag1311",32,68);
        testMap.addLocation(1312, "tag1312",32,69);
        testMap.addLocation(1313, "tag1313",32,70);
        testMap.addLocation(1314, "tag1314",32,71);
        testMap.addLocation(1315, "tag1315",32,72);
        testMap.addLocation(1316, "tag1316",32,77);
        testMap.addLocation(1317, "tag1317",32,78);
        testMap.addLocation(1318, "tag1318",32,79);
        testMap.addLocation(1319, "tag1319",32,80);
        testMap.addLocation(1320, "tag1320",32,81);
        testMap.addLocation(1321, "tag1321",32,82);
        testMap.addLocation(1322, "tag1322",32,83);
        testMap.addLocation(1323, "tag1323",32,84);
        testMap.addLocation(1324, "tag1324",32,85);
        testMap.addLocation(1325, "tag1325",32,86);
        testMap.addLocation(1326, "tag1326",32,87);
        testMap.addLocation(1327, "tag1327",32,88);
        testMap.addLocation(1328, "tag1328",32,89);
        testMap.addLocation(1329, "tag1329",32,90);
        testMap.addLocation(1330, "tag1330",32,91);
        testMap.addLocation(1331, "tag1331",32,92);
        testMap.addLocation(1332, "tag1332",32,93);
        testMap.addLocation(1333, "tag1333",32,94);
        testMap.addLocation(1334, "tag1334",32,95);
        testMap.addLocation(1335, "tag1335",32,96);
        testMap.addLocation(1336, "tag1336",32,97);
        testMap.addLocation(1337, "tag1337",33,20);
        testMap.addLocation(1338, "tag1338",33,21);
        testMap.addLocation(1339, "tag1339",33,22);
        testMap.addLocation(1340, "tag1340",33,28);
        testMap.addLocation(1341, "tag1341",33,29);
        testMap.addLocation(1342, "tag1342",33,30);
        testMap.addLocation(1343, "tag1343",33,31);
        testMap.addLocation(1344, "tag1344",33,32);
        testMap.addLocation(1345, "tag1345",33,33);
        testMap.addLocation(1346, "tag1346",33,34);
        testMap.addLocation(1347, "tag1347",33,35);
        testMap.addLocation(1348, "tag1348",33,36);
        testMap.addLocation(1349, "tag1349",33,37);
        testMap.addLocation(1350, "tag1350",33,38);
        testMap.addLocation(1351, "tag1351",33,39);
        testMap.addLocation(1352, "tag1352",33,40);
        testMap.addLocation(1353, "tag1353",33,41);
        testMap.addLocation(1354, "tag1354",33,42);
        testMap.addLocation(1355, "tag1355",33,43);
        testMap.addLocation(1356, "tag1356",33,44);
        testMap.addLocation(1357, "tag1357",33,45);
        testMap.addLocation(1358, "tag1358",33,46);
        testMap.addLocation(1359, "tag1359",33,47);
        testMap.addLocation(1360, "tag1360",33,48);
        testMap.addLocation(1361, "tag1361",33,49);
        testMap.addLocation(1362, "tag1362",33,50);
        testMap.addLocation(1363, "tag1363",33,51);
        testMap.addLocation(1364, "tag1364",33,52);
        testMap.addLocation(1365, "tag1365",33,53);
        testMap.addLocation(1366, "tag1366",33,54);
        testMap.addLocation(1367, "tag1367",33,55);
        testMap.addLocation(1368, "tag1368",33,56);
        testMap.addLocation(1369, "tag1369",33,57);
        testMap.addLocation(1370, "tag1370",33,58);
        testMap.addLocation(1371, "tag1371",33,59);
        testMap.addLocation(1372, "tag1372",33,60);
        testMap.addLocation(1373, "tag1373",33,61);
        testMap.addLocation(1374, "tag1374",33,62);
        testMap.addLocation(1375, "tag1375",33,63);
        testMap.addLocation(1376, "tag1376",33,64);
        testMap.addLocation(1377, "tag1377",33,65);
        testMap.addLocation(1378, "tag1378",33,66);
        testMap.addLocation(1379, "tag1379",33,67);
        testMap.addLocation(1380, "tag1380",33,68);
        testMap.addLocation(1381, "tag1381",33,69);
        testMap.addLocation(1382, "tag1382",33,70);
        testMap.addLocation(1383, "tag1383",33,71);
        testMap.addLocation(1384, "tag1384",33,72);
        testMap.addLocation(1385, "tag1385",33,77);
        testMap.addLocation(1386, "tag1386",33,78);
        testMap.addLocation(1387, "tag1387",33,79);
        testMap.addLocation(1388, "tag1388",33,80);
        testMap.addLocation(1389, "tag1389",33,81);
        testMap.addLocation(1390, "tag1390",33,82);
        testMap.addLocation(1391, "tag1391",33,83);
        testMap.addLocation(1392, "tag1392",33,84);
        testMap.addLocation(1393, "tag1393",33,85);
        testMap.addLocation(1394, "tag1394",33,86);
        testMap.addLocation(1395, "tag1395",33,87);
        testMap.addLocation(1396, "tag1396",33,88);
        testMap.addLocation(1397, "tag1397",33,89);
        testMap.addLocation(1398, "tag1398",33,90);
        testMap.addLocation(1399, "tag1399",33,96);
        testMap.addLocation(1400, "tag1400",33,97);
        testMap.addLocation(1401, "tag1401",34,20);
        testMap.addLocation(1402, "tag1402",34,21);
        testMap.addLocation(1403, "tag1403",34,22);
        testMap.addLocation(1404, "tag1404",34,28);
        testMap.addLocation(1405, "tag1405",34,29);
        testMap.addLocation(1406, "tag1406",34,30);
        testMap.addLocation(1407, "tag1407",34,31);
        testMap.addLocation(1408, "tag1408",34,32);
        testMap.addLocation(1409, "tag1409",34,33);
        testMap.addLocation(1410, "tag1410",34,34);
        testMap.addLocation(1411, "tag1411",34,35);
        testMap.addLocation(1412, "tag1412",34,36);
        testMap.addLocation(1413, "tag1413",34,37);
        testMap.addLocation(1414, "tag1414",34,38);
        testMap.addLocation(1415, "tag1415",34,39);
        testMap.addLocation(1416, "tag1416",34,40);
        testMap.addLocation(1417, "tag1417",34,41);
        testMap.addLocation(1418, "tag1418",34,42);
        testMap.addLocation(1419, "tag1419",34,43);
        testMap.addLocation(1420, "tag1420",34,44);
        testMap.addLocation(1421, "tag1421",34,45);
        testMap.addLocation(1422, "tag1422",34,46);
        testMap.addLocation(1423, "tag1423",34,47);
        testMap.addLocation(1424, "tag1424",34,48);
        testMap.addLocation(1425, "tag1425",34,49);
        testMap.addLocation(1426, "tag1426",34,50);
        testMap.addLocation(1427, "tag1427",34,51);
        testMap.addLocation(1428, "tag1428",34,52);
        testMap.addLocation(1429, "tag1429",34,53);
        testMap.addLocation(1430, "tag1430",34,54);
        testMap.addLocation(1431, "tag1431",34,55);
        testMap.addLocation(1432, "tag1432",34,56);
        testMap.addLocation(1433, "tag1433",34,57);
        testMap.addLocation(1434, "tag1434",34,58);
        testMap.addLocation(1435, "tag1435",34,59);
        testMap.addLocation(1436, "tag1436",34,60);
        testMap.addLocation(1437, "tag1437",34,61);
        testMap.addLocation(1438, "tag1438",34,62);
        testMap.addLocation(1439, "tag1439",34,63);
        testMap.addLocation(1440, "tag1440",34,64);
        testMap.addLocation(1441, "tag1441",34,65);
        testMap.addLocation(1442, "tag1442",34,66);
        testMap.addLocation(1443, "tag1443",34,67);
        testMap.addLocation(1444, "tag1444",34,68);
        testMap.addLocation(1445, "tag1445",34,69);
        testMap.addLocation(1446, "tag1446",34,70);
        testMap.addLocation(1447, "tag1447",34,71);
        testMap.addLocation(1448, "tag1448",34,72);
        testMap.addLocation(1449, "tag1449",34,77);
        testMap.addLocation(1450, "tag1450",34,78);
        testMap.addLocation(1451, "tag1451",34,79);
        testMap.addLocation(1452, "tag1452",34,80);
        testMap.addLocation(1453, "tag1453",34,81);
        testMap.addLocation(1454, "tag1454",34,82);
        testMap.addLocation(1455, "tag1455",34,83);
        testMap.addLocation(1456, "tag1456",34,84);
        testMap.addLocation(1457, "tag1457",34,85);
        testMap.addLocation(1458, "tag1458",34,86);
        testMap.addLocation(1459, "tag1459",34,87);
        testMap.addLocation(1460, "tag1460",34,88);
        testMap.addLocation(1461, "tag1461",34,89);
        testMap.addLocation(1462, "tag1462",34,90);
        testMap.addLocation(1463, "tag1463",34,96);
        testMap.addLocation(1464, "tag1464",34,97);
        testMap.addLocation(1465, "tag1465",35,20);
        testMap.addLocation(1466, "tag1466",35,21);
        testMap.addLocation(1467, "tag1467",35,22);
        testMap.addLocation(1468, "tag1468",35,28);
        testMap.addLocation(1469, "tag1469",35,29);
        testMap.addLocation(1470, "tag1470",35,30);
        testMap.addLocation(1471, "tag1471",35,31);
        testMap.addLocation(1472, "tag1472",35,32);
        testMap.addLocation(1473, "tag1473",35,33);
        testMap.addLocation(1474, "tag1474",35,34);
        testMap.addLocation(1475, "tag1475",35,35);
        testMap.addLocation(1476, "tag1476",35,36);
        testMap.addLocation(1477, "tag1477",35,37);
        testMap.addLocation(1478, "tag1478",35,38);
        testMap.addLocation(1479, "tag1479",35,39);
        testMap.addLocation(1480, "tag1480",35,40);
        testMap.addLocation(1481, "tag1481",35,41);
        testMap.addLocation(1482, "tag1482",35,42);
        testMap.addLocation(1483, "tag1483",35,43);
        testMap.addLocation(1484, "tag1484",35,44);
        testMap.addLocation(1485, "tag1485",35,45);
        testMap.addLocation(1486, "tag1486",35,46);
        testMap.addLocation(1487, "tag1487",35,47);
        testMap.addLocation(1488, "tag1488",35,48);
        testMap.addLocation(1489, "tag1489",35,49);
        testMap.addLocation(1490, "tag1490",35,50);
        testMap.addLocation(1491, "tag1491",35,51);
        testMap.addLocation(1492, "tag1492",35,52);
        testMap.addLocation(1493, "tag1493",35,53);
        testMap.addLocation(1494, "tag1494",35,54);
        testMap.addLocation(1495, "tag1495",35,55);
        testMap.addLocation(1496, "tag1496",35,56);
        testMap.addLocation(1497, "tag1497",35,57);
        testMap.addLocation(1498, "tag1498",35,58);
        testMap.addLocation(1499, "tag1499",35,59);
        testMap.addLocation(1500, "tag1500",35,60);
        testMap.addLocation(1501, "tag1501",35,61);
        testMap.addLocation(1502, "tag1502",35,62);
        testMap.addLocation(1503, "tag1503",35,63);
        testMap.addLocation(1504, "tag1504",35,64);
        testMap.addLocation(1505, "tag1505",35,65);
        testMap.addLocation(1506, "tag1506",35,66);
        testMap.addLocation(1507, "tag1507",35,67);
        testMap.addLocation(1508, "tag1508",35,68);
        testMap.addLocation(1509, "tag1509",35,69);
        testMap.addLocation(1510, "tag1510",35,70);
        testMap.addLocation(1511, "tag1511",35,71);
        testMap.addLocation(1512, "tag1512",35,72);
        testMap.addLocation(1513, "tag1513",35,77);
        testMap.addLocation(1514, "tag1514",35,78);
        testMap.addLocation(1515, "tag1515",35,79);
        testMap.addLocation(1516, "tag1516",35,80);
        testMap.addLocation(1517, "tag1517",35,81);
        testMap.addLocation(1518, "tag1518",35,82);
        testMap.addLocation(1519, "tag1519",35,83);
        testMap.addLocation(1520, "tag1520",35,84);
        testMap.addLocation(1521, "tag1521",35,85);
        testMap.addLocation(1522, "tag1522",35,86);
        testMap.addLocation(1523, "tag1523",35,87);
        testMap.addLocation(1524, "tag1524",35,88);
        testMap.addLocation(1525, "tag1525",35,89);
        testMap.addLocation(1526, "tag1526",35,90);
        testMap.addLocation(1527, "tag1527",35,96);
        testMap.addLocation(1528, "tag1528",35,97);
        testMap.addLocation(1529, "tag1529",36,20);
        testMap.addLocation(1530, "tag1530",36,21);
        testMap.addLocation(1531, "tag1531",36,22);
        testMap.addLocation(1532, "tag1532",36,28);
        testMap.addLocation(1533, "tag1533",36,29);
        testMap.addLocation(1534, "tag1534",36,30);
        testMap.addLocation(1535, "tag1535",36,31);
        testMap.addLocation(1536, "tag1536",36,32);
        testMap.addLocation(1537, "tag1537",36,33);
        testMap.addLocation(1538, "tag1538",36,34);
        testMap.addLocation(1539, "tag1539",36,35);
        testMap.addLocation(1540, "tag1540",36,36);
        testMap.addLocation(1541, "tag1541",36,37);
        testMap.addLocation(1542, "tag1542",36,38);
        testMap.addLocation(1543, "tag1543",36,39);
        testMap.addLocation(1544, "tag1544",36,40);
        testMap.addLocation(1545, "tag1545",36,41);
        testMap.addLocation(1546, "tag1546",36,42);
        testMap.addLocation(1547, "tag1547",36,43);
        testMap.addLocation(1548, "tag1548",36,44);
        testMap.addLocation(1549, "tag1549",36,45);
        testMap.addLocation(1550, "tag1550",36,46);
        testMap.addLocation(1551, "tag1551",36,47);
        testMap.addLocation(1552, "tag1552",36,48);
        testMap.addLocation(1553, "tag1553",36,49);
        testMap.addLocation(1554, "tag1554",36,50);
        testMap.addLocation(1555, "tag1555",36,51);
        testMap.addLocation(1556, "tag1556",36,52);
        testMap.addLocation(1557, "tag1557",36,53);
        testMap.addLocation(1558, "tag1558",36,54);
        testMap.addLocation(1559, "tag1559",36,55);
        testMap.addLocation(1560, "tag1560",36,56);
        testMap.addLocation(1561, "tag1561",36,57);
        testMap.addLocation(1562, "tag1562",36,58);
        testMap.addLocation(1563, "tag1563",36,59);
        testMap.addLocation(1564, "tag1564",36,60);
        testMap.addLocation(1565, "tag1565",36,61);
        testMap.addLocation(1566, "tag1566",36,62);
        testMap.addLocation(1567, "tag1567",36,63);
        testMap.addLocation(1568, "tag1568",36,64);
        testMap.addLocation(1569, "tag1569",36,65);
        testMap.addLocation(1570, "tag1570",36,66);
        testMap.addLocation(1571, "tag1571",36,67);
        testMap.addLocation(1572, "tag1572",36,68);
        testMap.addLocation(1573, "tag1573",36,69);
        testMap.addLocation(1574, "tag1574",36,70);
        testMap.addLocation(1575, "tag1575",36,71);
        testMap.addLocation(1576, "tag1576",36,72);
        testMap.addLocation(1577, "tag1577",36,77);
        testMap.addLocation(1578, "tag1578",36,78);
        testMap.addLocation(1579, "tag1579",36,79);
        testMap.addLocation(1580, "tag1580",36,80);
        testMap.addLocation(1581, "tag1581",36,81);
        testMap.addLocation(1582, "tag1582",36,82);
        testMap.addLocation(1583, "tag1583",36,83);
        testMap.addLocation(1584, "tag1584",36,84);
        testMap.addLocation(1585, "tag1585",36,85);
        testMap.addLocation(1586, "tag1586",36,86);
        testMap.addLocation(1587, "tag1587",36,87);
        testMap.addLocation(1588, "tag1588",36,88);
        testMap.addLocation(1589, "tag1589",36,89);
        testMap.addLocation(1590, "tag1590",36,90);
        testMap.addLocation(1591, "tag1591",36,96);
        testMap.addLocation(1592, "tag1592",36,97);
        testMap.addLocation(1593, "tag1593",37,20);
        testMap.addLocation(1594, "tag1594",37,21);
        testMap.addLocation(1595, "tag1595",37,22);
        testMap.addLocation(1596, "tag1596",37,28);
        testMap.addLocation(1597, "tag1597",37,29);
        testMap.addLocation(1598, "tag1598",37,30);
        testMap.addLocation(1599, "tag1599",37,31);
        testMap.addLocation(1600, "tag1600",37,32);
        testMap.addLocation(1601, "tag1601",37,33);
        testMap.addLocation(1602, "tag1602",37,34);
        testMap.addLocation(1603, "tag1603",37,35);
        testMap.addLocation(1604, "tag1604",37,36);
        testMap.addLocation(1605, "tag1605",37,37);
        testMap.addLocation(1606, "tag1606",37,38);
        testMap.addLocation(1607, "tag1607",37,39);
        testMap.addLocation(1608, "tag1608",37,40);
        testMap.addLocation(1609, "tag1609",37,41);
        testMap.addLocation(1610, "tag1610",37,42);
        testMap.addLocation(1611, "tag1611",37,43);
        testMap.addLocation(1612, "tag1612",37,44);
        testMap.addLocation(1613, "tag1613",37,45);
        testMap.addLocation(1614, "tag1614",37,46);
        testMap.addLocation(1615, "tag1615",37,47);
        testMap.addLocation(1616, "tag1616",37,48);
        testMap.addLocation(1617, "tag1617",37,49);
        testMap.addLocation(1618, "tag1618",37,50);
        testMap.addLocation(1619, "tag1619",37,51);
        testMap.addLocation(1620, "tag1620",37,52);
        testMap.addLocation(1621, "tag1621",37,53);
        testMap.addLocation(1622, "tag1622",37,54);
        testMap.addLocation(1623, "tag1623",37,55);
        testMap.addLocation(1624, "tag1624",37,56);
        testMap.addLocation(1625, "tag1625",37,57);
        testMap.addLocation(1626, "tag1626",37,58);
        testMap.addLocation(1627, "tag1627",37,59);
        testMap.addLocation(1628, "tag1628",37,60);
        testMap.addLocation(1629, "tag1629",37,61);
        testMap.addLocation(1630, "tag1630",37,62);
        testMap.addLocation(1631, "tag1631",37,63);
        testMap.addLocation(1632, "tag1632",37,64);
        testMap.addLocation(1633, "tag1633",37,65);
        testMap.addLocation(1634, "tag1634",37,66);
        testMap.addLocation(1635, "tag1635",37,67);
        testMap.addLocation(1636, "tag1636",37,68);
        testMap.addLocation(1637, "tag1637",37,69);
        testMap.addLocation(1638, "tag1638",37,70);
        testMap.addLocation(1639, "tag1639",37,71);
        testMap.addLocation(1640, "tag1640",37,72);
        testMap.addLocation(1641, "tag1641",37,77);
        testMap.addLocation(1642, "tag1642",37,78);
        testMap.addLocation(1643, "tag1643",37,79);
        testMap.addLocation(1644, "tag1644",37,80);
        testMap.addLocation(1645, "tag1645",37,81);
        testMap.addLocation(1646, "tag1646",37,82);
        testMap.addLocation(1647, "tag1647",37,83);
        testMap.addLocation(1648, "tag1648",37,84);
        testMap.addLocation(1649, "tag1649",37,85);
        testMap.addLocation(1650, "tag1650",37,86);
        testMap.addLocation(1651, "tag1651",37,87);
        testMap.addLocation(1652, "tag1652",37,88);
        testMap.addLocation(1653, "tag1653",37,89);
        testMap.addLocation(1654, "tag1654",37,90);
        testMap.addLocation(1655, "tag1655",37,96);
        testMap.addLocation(1656, "tag1656",37,97);
        testMap.addLocation(1657, "tag1657",38,20);
        testMap.addLocation(1658, "tag1658",38,21);
        testMap.addLocation(1659, "tag1659",38,22);
        testMap.addLocation(1660, "tag1660",38,28);
        testMap.addLocation(1661, "tag1661",38,29);
        testMap.addLocation(1662, "tag1662",38,30);
        testMap.addLocation(1663, "tag1663",38,31);
        testMap.addLocation(1664, "tag1664",38,32);
        testMap.addLocation(1665, "tag1665",38,33);
        testMap.addLocation(1666, "tag1666",38,34);
        testMap.addLocation(1667, "tag1667",38,35);
        testMap.addLocation(1668, "tag1668",38,36);
        testMap.addLocation(1669, "tag1669",38,37);
        testMap.addLocation(1670, "tag1670",38,38);
        testMap.addLocation(1671, "tag1671",38,39);
        testMap.addLocation(1672, "tag1672",38,40);
        testMap.addLocation(1673, "tag1673",38,41);
        testMap.addLocation(1674, "tag1674",38,42);
        testMap.addLocation(1675, "tag1675",38,43);
        testMap.addLocation(1676, "tag1676",38,44);
        testMap.addLocation(1677, "tag1677",38,45);
        testMap.addLocation(1678, "tag1678",38,46);
        testMap.addLocation(1679, "tag1679",38,47);
        testMap.addLocation(1680, "tag1680",38,48);
        testMap.addLocation(1681, "tag1681",38,49);
        testMap.addLocation(1682, "tag1682",38,50);
        testMap.addLocation(1683, "tag1683",38,51);
        testMap.addLocation(1684, "tag1684",38,52);
        testMap.addLocation(1685, "tag1685",38,53);
        testMap.addLocation(1686, "tag1686",38,54);
        testMap.addLocation(1687, "tag1687",38,55);
        testMap.addLocation(1688, "tag1688",38,56);
        testMap.addLocation(1689, "tag1689",38,57);
        testMap.addLocation(1690, "tag1690",38,58);
        testMap.addLocation(1691, "tag1691",38,59);
        testMap.addLocation(1692, "tag1692",38,60);
        testMap.addLocation(1693, "tag1693",38,61);
        testMap.addLocation(1694, "tag1694",38,62);
        testMap.addLocation(1695, "tag1695",38,63);
        testMap.addLocation(1696, "tag1696",38,64);
        testMap.addLocation(1697, "tag1697",38,65);
        testMap.addLocation(1698, "tag1698",38,66);
        testMap.addLocation(1699, "tag1699",38,67);
        testMap.addLocation(1700, "tag1700",38,68);
        testMap.addLocation(1701, "tag1701",38,69);
        testMap.addLocation(1702, "tag1702",38,70);
        testMap.addLocation(1703, "tag1703",38,71);
        testMap.addLocation(1704, "tag1704",38,72);
        testMap.addLocation(1705, "tag1705",38,73);
        testMap.addLocation(1706, "tag1706",38,74);
        testMap.addLocation(1707, "tag1707",38,75);
        testMap.addLocation(1708, "tag1708",38,76);
        testMap.addLocation(1709, "tag1709",38,77);
        testMap.addLocation(1710, "tag1710",38,78);
        testMap.addLocation(1711, "tag1711",38,79);
        testMap.addLocation(1712, "tag1712",38,80);
        testMap.addLocation(1713, "tag1713",38,81);
        testMap.addLocation(1714, "tag1714",38,82);
        testMap.addLocation(1715, "tag1715",38,83);
        testMap.addLocation(1716, "tag1716",38,84);
        testMap.addLocation(1717, "tag1717",38,85);
        testMap.addLocation(1718, "tag1718",38,86);
        testMap.addLocation(1719, "tag1719",38,87);
        testMap.addLocation(1720, "tag1720",38,88);
        testMap.addLocation(1721, "tag1721",38,89);
        testMap.addLocation(1722, "tag1722",38,90);
        testMap.addLocation(1723, "tag1723",38,96);
        testMap.addLocation(1724, "tag1724",38,97);
        testMap.addLocation(1725, "tag1725",39,20);
        testMap.addLocation(1726, "tag1726",39,21);
        testMap.addLocation(1727, "tag1727",39,22);
        testMap.addLocation(1728, "tag1728",39,28);
        testMap.addLocation(1729, "tag1729",39,29);
        testMap.addLocation(1730, "tag1730",39,30);
        testMap.addLocation(1731, "tag1731",39,31);
        testMap.addLocation(1732, "tag1732",39,32);
        testMap.addLocation(1733, "tag1733",39,33);
        testMap.addLocation(1734, "tag1734",39,34);
        testMap.addLocation(1735, "tag1735",39,35);
        testMap.addLocation(1736, "tag1736",39,36);
        testMap.addLocation(1737, "tag1737",39,37);
        testMap.addLocation(1738, "tag1738",39,38);
        testMap.addLocation(1739, "tag1739",39,39);
        testMap.addLocation(1740, "tag1740",39,40);
        testMap.addLocation(1741, "tag1741",39,41);
        testMap.addLocation(1742, "tag1742",39,42);
        testMap.addLocation(1743, "tag1743",39,43);
        testMap.addLocation(1744, "tag1744",39,44);
        testMap.addLocation(1745, "tag1745",39,45);
        testMap.addLocation(1746, "tag1746",39,46);
        testMap.addLocation(1747, "tag1747",39,47);
        testMap.addLocation(1748, "tag1748",39,48);
        testMap.addLocation(1749, "tag1749",39,49);
        testMap.addLocation(1750, "tag1750",39,50);
        testMap.addLocation(1751, "tag1751",39,51);
        testMap.addLocation(1752, "tag1752",39,52);
        testMap.addLocation(1753, "tag1753",39,53);
        testMap.addLocation(1754, "tag1754",39,54);
        testMap.addLocation(1755, "tag1755",39,55);
        testMap.addLocation(1756, "tag1756",39,56);
        testMap.addLocation(1757, "tag1757",39,57);
        testMap.addLocation(1758, "tag1758",39,58);
        testMap.addLocation(1759, "tag1759",39,59);
        testMap.addLocation(1760, "tag1760",39,60);
        testMap.addLocation(1761, "tag1761",39,61);
        testMap.addLocation(1762, "tag1762",39,62);
        testMap.addLocation(1763, "tag1763",39,63);
        testMap.addLocation(1764, "tag1764",39,64);
        testMap.addLocation(1765, "tag1765",39,65);
        testMap.addLocation(1766, "tag1766",39,66);
        testMap.addLocation(1767, "tag1767",39,67);
        testMap.addLocation(1768, "tag1768",39,68);
        testMap.addLocation(1769, "tag1769",39,69);
        testMap.addLocation(1770, "tag1770",39,70);
        testMap.addLocation(1771, "tag1771",39,71);
        testMap.addLocation(1772, "tag1772",39,72);
        testMap.addLocation(1773, "tag1773",39,73);
        testMap.addLocation(1774, "tag1774",39,74);
        testMap.addLocation(1775, "tag1775",39,75);
        testMap.addLocation(1776, "tag1776",39,76);
        testMap.addLocation(1777, "tag1777",39,77);
        testMap.addLocation(1778, "tag1778",39,78);
        testMap.addLocation(1779, "tag1779",39,79);
        testMap.addLocation(1780, "tag1780",39,80);
        testMap.addLocation(1781, "tag1781",39,81);
        testMap.addLocation(1782, "tag1782",39,82);
        testMap.addLocation(1783, "tag1783",39,83);
        testMap.addLocation(1784, "tag1784",39,84);
        testMap.addLocation(1785, "tag1785",39,85);
        testMap.addLocation(1786, "tag1786",39,86);
        testMap.addLocation(1787, "tag1787",39,87);
        testMap.addLocation(1788, "tag1788",39,88);
        testMap.addLocation(1789, "tag1789",39,89);
        testMap.addLocation(1790, "tag1790",39,90);
        testMap.addLocation(1791, "tag1791",39,96);
        testMap.addLocation(1792, "tag1792",39,97);
        testMap.addLocation(1793, "tag1793",40,20);
        testMap.addLocation(1794, "tag1794",40,21);
        testMap.addLocation(1795, "tag1795",40,22);
        testMap.addLocation(1796, "tag1796",40,28);
        testMap.addLocation(1797, "tag1797",40,29);
        testMap.addLocation(1798, "tag1798",40,30);
        testMap.addLocation(1799, "tag1799",40,31);
        testMap.addLocation(1800, "tag1800",40,32);
        testMap.addLocation(1801, "tag1801",40,33);
        testMap.addLocation(1802, "tag1802",40,34);
        testMap.addLocation(1803, "tag1803",40,35);
        testMap.addLocation(1804, "tag1804",40,36);
        testMap.addLocation(1805, "tag1805",40,37);
        testMap.addLocation(1806, "tag1806",40,38);
        testMap.addLocation(1807, "tag1807",40,39);
        testMap.addLocation(1808, "tag1808",40,40);
        testMap.addLocation(1809, "tag1809",40,41);
        testMap.addLocation(1810, "tag1810",40,42);
        testMap.addLocation(1811, "tag1811",40,43);
        testMap.addLocation(1812, "tag1812",40,44);
        testMap.addLocation(1813, "tag1813",40,45);
        testMap.addLocation(1814, "tag1814",40,46);
        testMap.addLocation(1815, "tag1815",40,47);
        testMap.addLocation(1816, "tag1816",40,48);
        testMap.addLocation(1817, "tag1817",40,49);
        testMap.addLocation(1818, "tag1818",40,50);
        testMap.addLocation(1819, "tag1819",40,51);
        testMap.addLocation(1820, "tag1820",40,52);
        testMap.addLocation(1821, "tag1821",40,53);
        testMap.addLocation(1822, "tag1822",40,54);
        testMap.addLocation(1823, "tag1823",40,55);
        testMap.addLocation(1824, "tag1824",40,56);
        testMap.addLocation(1825, "tag1825",40,57);
        testMap.addLocation(1826, "tag1826",40,58);
        testMap.addLocation(1827, "tag1827",40,59);
        testMap.addLocation(1828, "tag1828",40,60);
        testMap.addLocation(1829, "tag1829",40,61);
        testMap.addLocation(1830, "tag1830",40,62);
        testMap.addLocation(1831, "tag1831",40,63);
        testMap.addLocation(1832, "tag1832",40,64);
        testMap.addLocation(1833, "tag1833",40,65);
        testMap.addLocation(1834, "tag1834",40,66);
        testMap.addLocation(1835, "tag1835",40,67);
        testMap.addLocation(1836, "tag1836",40,68);
        testMap.addLocation(1837, "tag1837",40,69);
        testMap.addLocation(1838, "tag1838",40,70);
        testMap.addLocation(1839, "tag1839",40,71);
        testMap.addLocation(1840, "tag1840",40,72);
        testMap.addLocation(1841, "tag1841",40,73);
        testMap.addLocation(1842, "tag1842",40,74);
        testMap.addLocation(1843, "tag1843",40,75);
        testMap.addLocation(1844, "tag1844",40,76);
        testMap.addLocation(1845, "tag1845",40,77);
        testMap.addLocation(1846, "tag1846",40,78);
        testMap.addLocation(1847, "tag1847",40,79);
        testMap.addLocation(1848, "tag1848",40,80);
        testMap.addLocation(1849, "tag1849",40,81);
        testMap.addLocation(1850, "tag1850",40,82);
        testMap.addLocation(1851, "tag1851",40,83);
        testMap.addLocation(1852, "tag1852",40,84);
        testMap.addLocation(1853, "tag1853",40,85);
        testMap.addLocation(1854, "tag1854",40,86);
        testMap.addLocation(1855, "tag1855",40,87);
        testMap.addLocation(1856, "tag1856",40,88);
        testMap.addLocation(1857, "tag1857",40,89);
        testMap.addLocation(1858, "tag1858",40,90);
        testMap.addLocation(1859, "tag1859",40,96);
        testMap.addLocation(1860, "tag1860",40,97);
        testMap.addLocation(1861, "tag1861",41,20);
        testMap.addLocation(1862, "tag1862",41,21);
        testMap.addLocation(1863, "tag1863",41,22);
        testMap.addLocation(1864, "tag1864",41,23);
        testMap.addLocation(1865, "tag1865",41,24);
        testMap.addLocation(1866, "tag1866",41,25);
        testMap.addLocation(1867, "tag1867",41,26);
        testMap.addLocation(1868, "tag1868",41,27);
        testMap.addLocation(1869, "tag1869",41,28);
        testMap.addLocation(1870, "tag1870",41,29);
        testMap.addLocation(1871, "tag1871",41,30);
        testMap.addLocation(1872, "tag1872",41,31);
        testMap.addLocation(1873, "tag1873",41,32);
        testMap.addLocation(1874, "tag1874",41,33);
        testMap.addLocation(1875, "tag1875",41,34);
        testMap.addLocation(1876, "tag1876",41,35);
        testMap.addLocation(1877, "tag1877",41,36);
        testMap.addLocation(1878, "tag1878",41,37);
        testMap.addLocation(1879, "tag1879",41,38);
        testMap.addLocation(1880, "tag1880",41,39);
        testMap.addLocation(1881, "tag1881",41,40);
        testMap.addLocation(1882, "tag1882",41,41);
        testMap.addLocation(1883, "tag1883",41,42);
        testMap.addLocation(1884, "tag1884",41,43);
        testMap.addLocation(1885, "tag1885",41,44);
        testMap.addLocation(1886, "tag1886",41,45);
        testMap.addLocation(1887, "tag1887",41,46);
        testMap.addLocation(1888, "tag1888",41,47);
        testMap.addLocation(1889, "tag1889",41,48);
        testMap.addLocation(1890, "tag1890",41,49);
        testMap.addLocation(1891, "tag1891",41,50);
        testMap.addLocation(1892, "tag1892",41,51);
        testMap.addLocation(1893, "tag1893",41,52);
        testMap.addLocation(1894, "tag1894",41,53);
        testMap.addLocation(1895, "tag1895",41,54);
        testMap.addLocation(1896, "tag1896",41,55);
        testMap.addLocation(1897, "tag1897",41,56);
        testMap.addLocation(1898, "tag1898",41,57);
        testMap.addLocation(1899, "tag1899",41,58);
        testMap.addLocation(1900, "tag1900",41,59);
        testMap.addLocation(1901, "tag1901",41,60);
        testMap.addLocation(1902, "tag1902",41,61);
        testMap.addLocation(1903, "tag1903",41,62);
        testMap.addLocation(1904, "tag1904",41,63);
        testMap.addLocation(1905, "tag1905",41,64);
        testMap.addLocation(1906, "tag1906",41,65);
        testMap.addLocation(1907, "tag1907",41,66);
        testMap.addLocation(1908, "tag1908",41,67);
        testMap.addLocation(1909, "tag1909",41,68);
        testMap.addLocation(1910, "tag1910",41,69);
        testMap.addLocation(1911, "tag1911",41,70);
        testMap.addLocation(1912, "tag1912",41,71);
        testMap.addLocation(1913, "tag1913",41,72);
        testMap.addLocation(1914, "tag1914",41,73);
        testMap.addLocation(1915, "tag1915",41,74);
        testMap.addLocation(1916, "tag1916",41,75);
        testMap.addLocation(1917, "tag1917",41,76);
        testMap.addLocation(1918, "tag1918",41,77);
        testMap.addLocation(1919, "tag1919",41,78);
        testMap.addLocation(1920, "tag1920",41,79);
        testMap.addLocation(1921, "tag1921",41,80);
        testMap.addLocation(1922, "tag1922",41,81);
        testMap.addLocation(1923, "tag1923",41,82);
        testMap.addLocation(1924, "tag1924",41,83);
        testMap.addLocation(1925, "tag1925",41,84);
        testMap.addLocation(1926, "tag1926",41,85);
        testMap.addLocation(1927, "tag1927",41,86);
        testMap.addLocation(1928, "tag1928",41,87);
        testMap.addLocation(1929, "tag1929",41,88);
        testMap.addLocation(1930, "tag1930",41,89);
        testMap.addLocation(1931, "tag1931",41,90);
        testMap.addLocation(1932, "tag1932",41,96);
        testMap.addLocation(1933, "tag1933",41,97);
        testMap.addLocation(1934, "tag1934",42,20);
        testMap.addLocation(1935, "tag1935",42,21);
        testMap.addLocation(1936, "tag1936",42,22);
        testMap.addLocation(1937, "tag1937",42,23);
        testMap.addLocation(1938, "tag1938",42,24);
        testMap.addLocation(1939, "tag1939",42,25);
        testMap.addLocation(1940, "tag1940",42,26);
        testMap.addLocation(1941, "tag1941",42,27);
        testMap.addLocation(1942, "tag1942",42,28);
        testMap.addLocation(1943, "tag1943",42,29);
        testMap.addLocation(1944, "tag1944",42,30);
        testMap.addLocation(1945, "tag1945",42,31);
        testMap.addLocation(1946, "tag1946",42,32);
        testMap.addLocation(1947, "tag1947",42,33);
        testMap.addLocation(1948, "tag1948",42,34);
        testMap.addLocation(1949, "tag1949",42,35);
        testMap.addLocation(1950, "tag1950",42,36);
        testMap.addLocation(1951, "tag1951",42,37);
        testMap.addLocation(1952, "tag1952",42,38);
        testMap.addLocation(1953, "tag1953",42,39);
        testMap.addLocation(1954, "tag1954",42,40);
        testMap.addLocation(1955, "tag1955",42,41);
        testMap.addLocation(1956, "tag1956",42,42);
        testMap.addLocation(1957, "tag1957",42,43);
        testMap.addLocation(1958, "tag1958",42,44);
        testMap.addLocation(1959, "tag1959",42,45);
        testMap.addLocation(1960, "tag1960",42,46);
        testMap.addLocation(1961, "tag1961",42,47);
        testMap.addLocation(1962, "tag1962",42,48);
        testMap.addLocation(1963, "tag1963",42,49);
        testMap.addLocation(1964, "tag1964",42,50);
        testMap.addLocation(1965, "tag1965",42,51);
        testMap.addLocation(1966, "tag1966",42,52);
        testMap.addLocation(1967, "tag1967",42,53);
        testMap.addLocation(1968, "tag1968",42,54);
        testMap.addLocation(1969, "tag1969",42,55);
        testMap.addLocation(1970, "tag1970",42,56);
        testMap.addLocation(1971, "tag1971",42,57);
        testMap.addLocation(1972, "tag1972",42,58);
        testMap.addLocation(1973, "tag1973",42,59);
        testMap.addLocation(1974, "tag1974",42,60);
        testMap.addLocation(1975, "tag1975",42,61);
        testMap.addLocation(1976, "tag1976",42,62);
        testMap.addLocation(1977, "tag1977",42,63);
        testMap.addLocation(1978, "tag1978",42,64);
        testMap.addLocation(1979, "tag1979",42,65);
        testMap.addLocation(1980, "tag1980",42,66);
        testMap.addLocation(1981, "tag1981",42,67);
        testMap.addLocation(1982, "tag1982",42,68);
        testMap.addLocation(1983, "tag1983",42,69);
        testMap.addLocation(1984, "tag1984",42,70);
        testMap.addLocation(1985, "tag1985",42,71);
        testMap.addLocation(1986, "tag1986",42,72);
        testMap.addLocation(1987, "tag1987",42,73);
        testMap.addLocation(1988, "tag1988",42,74);
        testMap.addLocation(1989, "tag1989",42,75);
        testMap.addLocation(1990, "tag1990",42,76);
        testMap.addLocation(1991, "tag1991",42,77);
        testMap.addLocation(1992, "tag1992",42,78);
        testMap.addLocation(1993, "tag1993",42,79);
        testMap.addLocation(1994, "tag1994",42,80);
        testMap.addLocation(1995, "tag1995",42,81);
        testMap.addLocation(1996, "tag1996",42,82);
        testMap.addLocation(1997, "tag1997",42,83);
        testMap.addLocation(1998, "tag1998",42,84);
        testMap.addLocation(1999, "tag1999",42,85);
        testMap.addLocation(2000, "tag2000",42,86);
        testMap.addLocation(2001, "tag2001",42,87);
        testMap.addLocation(2002, "tag2002",42,88);
        testMap.addLocation(2003, "tag2003",42,89);
        testMap.addLocation(2004, "tag2004",42,90);
        testMap.addLocation(2005, "tag2005",42,96);
        testMap.addLocation(2006, "tag2006",42,97);
        testMap.addLocation(2007, "tag2007",43,20);
        testMap.addLocation(2008, "tag2008",43,21);
        testMap.addLocation(2009, "tag2009",43,22);
        testMap.addLocation(2010, "tag2010",43,23);
        testMap.addLocation(2011, "tag2011",43,24);
        testMap.addLocation(2012, "tag2012",43,25);
        testMap.addLocation(2013, "tag2013",43,26);
        testMap.addLocation(2014, "tag2014",43,27);
        testMap.addLocation(2015, "tag2015",43,28);
        testMap.addLocation(2016, "tag2016",43,29);
        testMap.addLocation(2017, "tag2017",43,30);
        testMap.addLocation(2018, "tag2018",43,31);
        testMap.addLocation(2019, "tag2019",43,32);
        testMap.addLocation(2020, "tag2020",43,33);
        testMap.addLocation(2021, "tag2021",43,34);
        testMap.addLocation(2022, "tag2022",43,35);
        testMap.addLocation(2023, "tag2023",43,36);
        testMap.addLocation(2024, "tag2024",43,37);
        testMap.addLocation(2025, "tag2025",43,38);
        testMap.addLocation(2026, "tag2026",43,39);
        testMap.addLocation(2027, "tag2027",43,40);
        testMap.addLocation(2028, "tag2028",43,41);
        testMap.addLocation(2029, "tag2029",43,42);
        testMap.addLocation(2030, "tag2030",43,43);
        testMap.addLocation(2031, "tag2031",43,44);
        testMap.addLocation(2032, "tag2032",43,45);
        testMap.addLocation(2033, "tag2033",43,46);
        testMap.addLocation(2034, "tag2034",43,47);
        testMap.addLocation(2035, "tag2035",43,48);
        testMap.addLocation(2036, "tag2036",43,49);
        testMap.addLocation(2037, "tag2037",43,50);
        testMap.addLocation(2038, "tag2038",43,51);
        testMap.addLocation(2039, "tag2039",43,52);
        testMap.addLocation(2040, "tag2040",43,53);
        testMap.addLocation(2041, "tag2041",43,54);
        testMap.addLocation(2042, "tag2042",43,55);
        testMap.addLocation(2043, "tag2043",43,56);
        testMap.addLocation(2044, "tag2044",43,57);
        testMap.addLocation(2045, "tag2045",43,58);
        testMap.addLocation(2046, "tag2046",43,59);
        testMap.addLocation(2047, "tag2047",43,60);
        testMap.addLocation(2048, "tag2048",43,61);
        testMap.addLocation(2049, "tag2049",43,62);
        testMap.addLocation(2050, "tag2050",43,63);
        testMap.addLocation(2051, "tag2051",43,64);
        testMap.addLocation(2052, "tag2052",43,65);
        testMap.addLocation(2053, "tag2053",43,66);
        testMap.addLocation(2054, "tag2054",43,67);
        testMap.addLocation(2055, "tag2055",43,68);
        testMap.addLocation(2056, "tag2056",43,69);
        testMap.addLocation(2057, "tag2057",43,70);
        testMap.addLocation(2058, "tag2058",43,71);
        testMap.addLocation(2059, "tag2059",43,72);
        testMap.addLocation(2060, "tag2060",43,73);
        testMap.addLocation(2061, "tag2061",43,74);
        testMap.addLocation(2062, "tag2062",43,75);
        testMap.addLocation(2063, "tag2063",43,76);
        testMap.addLocation(2064, "tag2064",43,77);
        testMap.addLocation(2065, "tag2065",43,78);
        testMap.addLocation(2066, "tag2066",43,79);
        testMap.addLocation(2067, "tag2067",43,80);
        testMap.addLocation(2068, "tag2068",43,81);
        testMap.addLocation(2069, "tag2069",43,82);
        testMap.addLocation(2070, "tag2070",43,83);
        testMap.addLocation(2071, "tag2071",43,84);
        testMap.addLocation(2072, "tag2072",43,85);
        testMap.addLocation(2073, "tag2073",43,86);
        testMap.addLocation(2074, "tag2074",43,87);
        testMap.addLocation(2075, "tag2075",43,88);
        testMap.addLocation(2076, "tag2076",43,89);
        testMap.addLocation(2077, "tag2077",43,90);
        testMap.addLocation(2078, "tag2078",43,96);
        testMap.addLocation(2079, "tag2079",43,97);
        testMap.addLocation(2080, "tag2080",44,20);
        testMap.addLocation(2081, "tag2081",44,21);
        testMap.addLocation(2082, "tag2082",44,22);
        testMap.addLocation(2083, "tag2083",44,38);
        testMap.addLocation(2084, "tag2084",44,39);
        testMap.addLocation(2085, "tag2085",44,40);
        testMap.addLocation(2086, "tag2086",44,41);
        testMap.addLocation(2087, "tag2087",44,42);
        testMap.addLocation(2088, "tag2088",44,43);
        testMap.addLocation(2089, "tag2089",44,44);
        testMap.addLocation(2090, "tag2090",44,45);
        testMap.addLocation(2091, "tag2091",44,46);
        testMap.addLocation(2092, "tag2092",44,47);
        testMap.addLocation(2093, "tag2093",44,48);
        testMap.addLocation(2094, "tag2094",44,49);
        testMap.addLocation(2095, "tag2095",44,50);
        testMap.addLocation(2096, "tag2096",44,51);
        testMap.addLocation(2097, "tag2097",44,52);
        testMap.addLocation(2098, "tag2098",44,53);
        testMap.addLocation(2099, "tag2099",44,54);
        testMap.addLocation(2100, "tag2100",44,55);
        testMap.addLocation(2101, "tag2101",44,56);
        testMap.addLocation(2102, "tag2102",44,57);
        testMap.addLocation(2103, "tag2103",44,58);
        testMap.addLocation(2104, "tag2104",44,59);
        testMap.addLocation(2105, "tag2105",44,60);
        testMap.addLocation(2106, "tag2106",44,61);
        testMap.addLocation(2107, "tag2107",44,62);
        testMap.addLocation(2108, "tag2108",44,63);
        testMap.addLocation(2109, "tag2109",44,64);
        testMap.addLocation(2110, "tag2110",44,65);
        testMap.addLocation(2111, "tag2111",44,66);
        testMap.addLocation(2112, "tag2112",44,67);
        testMap.addLocation(2113, "tag2113",44,68);
        testMap.addLocation(2114, "tag2114",44,69);
        testMap.addLocation(2115, "tag2115",44,70);
        testMap.addLocation(2116, "tag2116",44,71);
        testMap.addLocation(2117, "tag2117",44,72);
        testMap.addLocation(2118, "tag2118",44,80);
        testMap.addLocation(2119, "tag2119",44,81);
        testMap.addLocation(2120, "tag2120",44,82);
        testMap.addLocation(2121, "tag2121",44,83);
        testMap.addLocation(2122, "tag2122",44,84);
        testMap.addLocation(2123, "tag2123",44,85);
        testMap.addLocation(2124, "tag2124",44,86);
        testMap.addLocation(2125, "tag2125",44,87);
        testMap.addLocation(2126, "tag2126",44,88);
        testMap.addLocation(2127, "tag2127",44,89);
        testMap.addLocation(2128, "tag2128",44,90);
        testMap.addLocation(2129, "tag2129",44,96);
        testMap.addLocation(2130, "tag2130",44,97);
        testMap.addLocation(2131, "tag2131",45,20);
        testMap.addLocation(2132, "tag2132",45,21);
        testMap.addLocation(2133, "tag2133",45,22);
        testMap.addLocation(2134, "tag2134",45,38);
        testMap.addLocation(2135, "tag2135",45,39);
        testMap.addLocation(2136, "tag2136",45,40);
        testMap.addLocation(2137, "tag2137",45,41);
        testMap.addLocation(2138, "tag2138",45,42);
        testMap.addLocation(2139, "tag2139",45,43);
        testMap.addLocation(2140, "tag2140",45,44);
        testMap.addLocation(2141, "tag2141",45,45);
        testMap.addLocation(2142, "tag2142",45,46);
        testMap.addLocation(2143, "tag2143",45,47);
        testMap.addLocation(2144, "tag2144",45,48);
        testMap.addLocation(2145, "tag2145",45,49);
        testMap.addLocation(2146, "tag2146",45,50);
        testMap.addLocation(2147, "tag2147",45,51);
        testMap.addLocation(2148, "tag2148",45,52);
        testMap.addLocation(2149, "tag2149",45,53);
        testMap.addLocation(2150, "tag2150",45,54);
        testMap.addLocation(2151, "tag2151",45,55);
        testMap.addLocation(2152, "tag2152",45,56);
        testMap.addLocation(2153, "tag2153",45,57);
        testMap.addLocation(2154, "tag2154",45,58);
        testMap.addLocation(2155, "tag2155",45,59);
        testMap.addLocation(2156, "tag2156",45,60);
        testMap.addLocation(2157, "tag2157",45,61);
        testMap.addLocation(2158, "tag2158",45,62);
        testMap.addLocation(2159, "tag2159",45,63);
        testMap.addLocation(2160, "tag2160",45,64);
        testMap.addLocation(2161, "tag2161",45,65);
        testMap.addLocation(2162, "tag2162",45,66);
        testMap.addLocation(2163, "tag2163",45,67);
        testMap.addLocation(2164, "tag2164",45,68);
        testMap.addLocation(2165, "tag2165",45,69);
        testMap.addLocation(2166, "tag2166",45,70);
        testMap.addLocation(2167, "tag2167",45,71);
        testMap.addLocation(2168, "tag2168",45,72);
        testMap.addLocation(2169, "tag2169",45,80);
        testMap.addLocation(2170, "tag2170",45,81);
        testMap.addLocation(2171, "tag2171",45,82);
        testMap.addLocation(2172, "tag2172",45,83);
        testMap.addLocation(2173, "tag2173",45,84);
        testMap.addLocation(2174, "tag2174",45,85);
        testMap.addLocation(2175, "tag2175",45,86);
        testMap.addLocation(2176, "tag2176",45,87);
        testMap.addLocation(2177, "tag2177",45,88);
        testMap.addLocation(2178, "tag2178",45,89);
        testMap.addLocation(2179, "tag2179",45,90);
        testMap.addLocation(2180, "tag2180",45,96);
        testMap.addLocation(2181, "tag2181",45,97);
        testMap.addLocation(2182, "tag2182",46,20);
        testMap.addLocation(2183, "tag2183",46,21);
        testMap.addLocation(2184, "tag2184",46,22);
        testMap.addLocation(2185, "tag2185",46,38);
        testMap.addLocation(2186, "tag2186",46,39);
        testMap.addLocation(2187, "tag2187",46,40);
        testMap.addLocation(2188, "tag2188",46,41);
        testMap.addLocation(2189, "tag2189",46,42);
        testMap.addLocation(2190, "tag2190",46,43);
        testMap.addLocation(2191, "tag2191",46,44);
        testMap.addLocation(2192, "tag2192",46,45);
        testMap.addLocation(2193, "tag2193",46,46);
        testMap.addLocation(2194, "tag2194",46,47);
        testMap.addLocation(2195, "tag2195",46,48);
        testMap.addLocation(2196, "tag2196",46,49);
        testMap.addLocation(2197, "tag2197",46,50);
        testMap.addLocation(2198, "tag2198",46,51);
        testMap.addLocation(2199, "tag2199",46,52);
        testMap.addLocation(2200, "tag2200",46,53);
        testMap.addLocation(2201, "tag2201",46,54);
        testMap.addLocation(2202, "tag2202",46,55);
        testMap.addLocation(2203, "tag2203",46,56);
        testMap.addLocation(2204, "tag2204",46,57);
        testMap.addLocation(2205, "tag2205",46,58);
        testMap.addLocation(2206, "tag2206",46,59);
        testMap.addLocation(2207, "tag2207",46,60);
        testMap.addLocation(2208, "tag2208",46,61);
        testMap.addLocation(2209, "tag2209",46,62);
        testMap.addLocation(2210, "tag2210",46,63);
        testMap.addLocation(2211, "tag2211",46,64);
        testMap.addLocation(2212, "tag2212",46,65);
        testMap.addLocation(2213, "tag2213",46,66);
        testMap.addLocation(2214, "tag2214",46,67);
        testMap.addLocation(2215, "tag2215",46,68);
        testMap.addLocation(2216, "tag2216",46,69);
        testMap.addLocation(2217, "tag2217",46,70);
        testMap.addLocation(2218, "tag2218",46,71);
        testMap.addLocation(2219, "tag2219",46,72);
        testMap.addLocation(2220, "tag2220",46,80);
        testMap.addLocation(2221, "tag2221",46,81);
        testMap.addLocation(2222, "tag2222",46,82);
        testMap.addLocation(2223, "tag2223",46,83);
        testMap.addLocation(2224, "tag2224",46,84);
        testMap.addLocation(2225, "tag2225",46,85);
        testMap.addLocation(2226, "tag2226",46,86);
        testMap.addLocation(2227, "tag2227",46,87);
        testMap.addLocation(2228, "tag2228",46,88);
        testMap.addLocation(2229, "tag2229",46,89);
        testMap.addLocation(2230, "tag2230",46,90);
        testMap.addLocation(2231, "tag2231",46,91);
        testMap.addLocation(2232, "tag2232",46,92);
        testMap.addLocation(2233, "tag2233",46,93);
        testMap.addLocation(2234, "tag2234",46,94);
        testMap.addLocation(2235, "tag2235",46,95);
        testMap.addLocation(2236, "tag2236",46,96);
        testMap.addLocation(2237, "tag2237",46,97);
        testMap.addLocation(2238, "tag2238",47,20);
        testMap.addLocation(2239, "tag2239",47,21);
        testMap.addLocation(2240, "tag2240",47,22);
        testMap.addLocation(2241, "tag2241",47,38);
        testMap.addLocation(2242, "tag2242",47,39);
        testMap.addLocation(2243, "tag2243",47,40);
        testMap.addLocation(2244, "tag2244",47,41);
        testMap.addLocation(2245, "tag2245",47,42);
        testMap.addLocation(2246, "tag2246",47,43);
        testMap.addLocation(2247, "tag2247",47,44);
        testMap.addLocation(2248, "tag2248",47,45);
        testMap.addLocation(2249, "tag2249",47,46);
        testMap.addLocation(2250, "tag2250",47,47);
        testMap.addLocation(2251, "tag2251",47,48);
        testMap.addLocation(2252, "tag2252",47,49);
        testMap.addLocation(2253, "tag2253",47,50);
        testMap.addLocation(2254, "tag2254",47,51);
        testMap.addLocation(2255, "tag2255",47,52);
        testMap.addLocation(2256, "tag2256",47,53);
        testMap.addLocation(2257, "tag2257",47,54);
        testMap.addLocation(2258, "tag2258",47,55);
        testMap.addLocation(2259, "tag2259",47,56);
        testMap.addLocation(2260, "tag2260",47,57);
        testMap.addLocation(2261, "tag2261",47,58);
        testMap.addLocation(2262, "tag2262",47,59);
        testMap.addLocation(2263, "tag2263",47,60);
        testMap.addLocation(2264, "tag2264",47,61);
        testMap.addLocation(2265, "tag2265",47,62);
        testMap.addLocation(2266, "tag2266",47,63);
        testMap.addLocation(2267, "tag2267",47,64);
        testMap.addLocation(2268, "tag2268",47,65);
        testMap.addLocation(2269, "tag2269",47,66);
        testMap.addLocation(2270, "tag2270",47,67);
        testMap.addLocation(2271, "tag2271",47,68);
        testMap.addLocation(2272, "tag2272",47,69);
        testMap.addLocation(2273, "tag2273",47,70);
        testMap.addLocation(2274, "tag2274",47,71);
        testMap.addLocation(2275, "tag2275",47,72);
        testMap.addLocation(2276, "tag2276",47,80);
        testMap.addLocation(2277, "tag2277",47,81);
        testMap.addLocation(2278, "tag2278",47,82);
        testMap.addLocation(2279, "tag2279",47,83);
        testMap.addLocation(2280, "tag2280",47,84);
        testMap.addLocation(2281, "tag2281",47,85);
        testMap.addLocation(2282, "tag2282",47,86);
        testMap.addLocation(2283, "tag2283",47,87);
        testMap.addLocation(2284, "tag2284",47,88);
        testMap.addLocation(2285, "tag2285",47,89);
        testMap.addLocation(2286, "tag2286",47,90);
        testMap.addLocation(2287, "tag2287",47,91);
        testMap.addLocation(2288, "tag2288",47,92);
        testMap.addLocation(2289, "tag2289",47,93);
        testMap.addLocation(2290, "tag2290",47,94);
        testMap.addLocation(2291, "tag2291",47,95);
        testMap.addLocation(2292, "tag2292",47,96);
        testMap.addLocation(2293, "tag2293",47,97);
        testMap.addLocation(2294, "tag2294",48,38);
        testMap.addLocation(2295, "tag2295",48,39);
        testMap.addLocation(2296, "tag2296",48,40);
        testMap.addLocation(2297, "tag2297",48,41);
        testMap.addLocation(2298, "tag2298",48,42);
        testMap.addLocation(2299, "tag2299",48,43);
        testMap.addLocation(2300, "tag2300",48,44);
        testMap.addLocation(2301, "tag2301",48,45);
        testMap.addLocation(2302, "tag2302",48,46);
        testMap.addLocation(2303, "tag2303",48,47);
        testMap.addLocation(2304, "tag2304",48,48);
        testMap.addLocation(2305, "tag2305",48,49);
        testMap.addLocation(2306, "tag2306",48,50);
        testMap.addLocation(2307, "tag2307",48,51);
        testMap.addLocation(2308, "tag2308",48,52);
        testMap.addLocation(2309, "tag2309",48,53);
        testMap.addLocation(2310, "tag2310",48,54);
        testMap.addLocation(2311, "tag2311",48,55);
        testMap.addLocation(2312, "tag2312",48,56);
        testMap.addLocation(2313, "tag2313",48,57);
        testMap.addLocation(2314, "tag2314",48,58);
        testMap.addLocation(2315, "tag2315",48,59);
        testMap.addLocation(2316, "tag2316",48,60);
        testMap.addLocation(2317, "tag2317",48,61);
        testMap.addLocation(2318, "tag2318",48,62);
        testMap.addLocation(2319, "tag2319",48,63);
        testMap.addLocation(2320, "tag2320",48,64);
        testMap.addLocation(2321, "tag2321",48,65);
        testMap.addLocation(2322, "tag2322",48,66);
        testMap.addLocation(2323, "tag2323",48,67);
        testMap.addLocation(2324, "tag2324",48,68);
        testMap.addLocation(2325, "tag2325",48,69);
        testMap.addLocation(2326, "tag2326",48,70);
        testMap.addLocation(2327, "tag2327",48,71);
        testMap.addLocation(2328, "tag2328",48,72);
        testMap.addLocation(2329, "tag2329",48,91);
        testMap.addLocation(2330, "tag2330",48,92);
        testMap.addLocation(2331, "tag2331",48,96);
        testMap.addLocation(2332, "tag2332",48,97);
        testMap.addLocation(2333, "tag2333",49,38);
        testMap.addLocation(2334, "tag2334",49,39);
        testMap.addLocation(2335, "tag2335",49,40);
        testMap.addLocation(2336, "tag2336",49,41);
        testMap.addLocation(2337, "tag2337",49,42);
        testMap.addLocation(2338, "tag2338",49,43);
        testMap.addLocation(2339, "tag2339",49,44);
        testMap.addLocation(2340, "tag2340",49,45);
        testMap.addLocation(2341, "tag2341",49,46);
        testMap.addLocation(2342, "tag2342",49,47);
        testMap.addLocation(2343, "tag2343",49,48);
        testMap.addLocation(2344, "tag2344",49,49);
        testMap.addLocation(2345, "tag2345",49,50);
        testMap.addLocation(2346, "tag2346",49,51);
        testMap.addLocation(2347, "tag2347",49,52);
        testMap.addLocation(2348, "tag2348",49,53);
        testMap.addLocation(2349, "tag2349",49,54);
        testMap.addLocation(2350, "tag2350",49,55);
        testMap.addLocation(2351, "tag2351",49,56);
        testMap.addLocation(2352, "tag2352",49,57);
        testMap.addLocation(2353, "tag2353",49,58);
        testMap.addLocation(2354, "tag2354",49,59);
        testMap.addLocation(2355, "tag2355",49,60);
        testMap.addLocation(2356, "tag2356",49,61);
        testMap.addLocation(2357, "tag2357",49,62);
        testMap.addLocation(2358, "tag2358",49,63);
        testMap.addLocation(2359, "tag2359",49,64);
        testMap.addLocation(2360, "tag2360",49,65);
        testMap.addLocation(2361, "tag2361",49,66);
        testMap.addLocation(2362, "tag2362",49,67);
        testMap.addLocation(2363, "tag2363",49,68);
        testMap.addLocation(2364, "tag2364",49,69);
        testMap.addLocation(2365, "tag2365",49,70);
        testMap.addLocation(2366, "tag2366",49,71);
        testMap.addLocation(2367, "tag2367",49,72);
        testMap.addLocation(2368, "tag2368",49,91);
        testMap.addLocation(2369, "tag2369",49,92);
        testMap.addLocation(2370, "tag2370",49,96);
        testMap.addLocation(2371, "tag2371",49,97);
        testMap.addLocation(2372, "tag2372",50,38);
        testMap.addLocation(2373, "tag2373",50,39);
        testMap.addLocation(2374, "tag2374",50,40);
        testMap.addLocation(2375, "tag2375",50,41);
        testMap.addLocation(2376, "tag2376",50,42);
        testMap.addLocation(2377, "tag2377",50,43);
        testMap.addLocation(2378, "tag2378",50,44);
        testMap.addLocation(2379, "tag2379",50,45);
        testMap.addLocation(2380, "tag2380",50,46);
        testMap.addLocation(2381, "tag2381",50,47);
        testMap.addLocation(2382, "tag2382",50,48);
        testMap.addLocation(2383, "tag2383",50,49);
        testMap.addLocation(2384, "tag2384",50,50);
        testMap.addLocation(2385, "tag2385",50,51);
        testMap.addLocation(2386, "tag2386",50,52);
        testMap.addLocation(2387, "tag2387",50,53);
        testMap.addLocation(2388, "tag2388",50,54);
        testMap.addLocation(2389, "tag2389",50,55);
        testMap.addLocation(2390, "tag2390",50,56);
        testMap.addLocation(2391, "tag2391",50,57);
        testMap.addLocation(2392, "tag2392",50,58);
        testMap.addLocation(2393, "tag2393",50,59);
        testMap.addLocation(2394, "tag2394",50,60);
        testMap.addLocation(2395, "tag2395",50,61);
        testMap.addLocation(2396, "tag2396",50,62);
        testMap.addLocation(2397, "tag2397",50,63);
        testMap.addLocation(2398, "tag2398",50,64);
        testMap.addLocation(2399, "tag2399",50,65);
        testMap.addLocation(2400, "tag2400",50,66);
        testMap.addLocation(2401, "tag2401",50,67);
        testMap.addLocation(2402, "tag2402",50,68);
        testMap.addLocation(2403, "tag2403",50,69);
        testMap.addLocation(2404, "tag2404",50,70);
        testMap.addLocation(2405, "tag2405",50,71);
        testMap.addLocation(2406, "tag2406",50,72);
        testMap.addLocation(2407, "tag2407",50,91);
        testMap.addLocation(2408, "tag2408",50,92);
        testMap.addLocation(2409, "tag2409",50,96);
        testMap.addLocation(2410, "tag2410",50,97);
        testMap.addLocation(2411, "tag2411",51,38);
        testMap.addLocation(2412, "tag2412",51,39);
        testMap.addLocation(2413, "tag2413",51,40);
        testMap.addLocation(2414, "tag2414",51,41);
        testMap.addLocation(2415, "tag2415",51,42);
        testMap.addLocation(2416, "tag2416",51,43);
        testMap.addLocation(2417, "tag2417",51,44);
        testMap.addLocation(2418, "tag2418",51,45);
        testMap.addLocation(2419, "tag2419",51,46);
        testMap.addLocation(2420, "tag2420",51,47);
        testMap.addLocation(2421, "tag2421",51,48);
        testMap.addLocation(2422, "tag2422",51,49);
        testMap.addLocation(2423, "tag2423",51,50);
        testMap.addLocation(2424, "tag2424",51,51);
        testMap.addLocation(2425, "tag2425",51,52);
        testMap.addLocation(2426, "tag2426",51,53);
        testMap.addLocation(2427, "tag2427",51,54);
        testMap.addLocation(2428, "tag2428",51,55);
        testMap.addLocation(2429, "tag2429",51,56);
        testMap.addLocation(2430, "tag2430",51,57);
        testMap.addLocation(2431, "tag2431",51,58);
        testMap.addLocation(2432, "tag2432",51,59);
        testMap.addLocation(2433, "tag2433",51,60);
        testMap.addLocation(2434, "tag2434",51,61);
        testMap.addLocation(2435, "tag2435",51,62);
        testMap.addLocation(2436, "tag2436",51,63);
        testMap.addLocation(2437, "tag2437",51,64);
        testMap.addLocation(2438, "tag2438",51,65);
        testMap.addLocation(2439, "tag2439",51,66);
        testMap.addLocation(2440, "tag2440",51,67);
        testMap.addLocation(2441, "tag2441",51,68);
        testMap.addLocation(2442, "tag2442",51,69);
        testMap.addLocation(2443, "tag2443",51,70);
        testMap.addLocation(2444, "tag2444",51,71);
        testMap.addLocation(2445, "tag2445",51,72);
        testMap.addLocation(2446, "tag2446",51,91);
        testMap.addLocation(2447, "tag2447",51,92);
        testMap.addLocation(2448, "tag2448",51,96);
        testMap.addLocation(2449, "tag2449",51,97);
        testMap.addLocation(2450, "tag2450",52,38);
        testMap.addLocation(2451, "tag2451",52,39);
        testMap.addLocation(2452, "tag2452",52,40);
        testMap.addLocation(2453, "tag2453",52,41);
        testMap.addLocation(2454, "tag2454",52,42);
        testMap.addLocation(2455, "tag2455",52,43);
        testMap.addLocation(2456, "tag2456",52,44);
        testMap.addLocation(2457, "tag2457",52,45);
        testMap.addLocation(2458, "tag2458",52,46);
        testMap.addLocation(2459, "tag2459",52,47);
        testMap.addLocation(2460, "tag2460",52,48);
        testMap.addLocation(2461, "tag2461",52,49);
        testMap.addLocation(2462, "tag2462",52,50);
        testMap.addLocation(2463, "tag2463",52,51);
        testMap.addLocation(2464, "tag2464",52,52);
        testMap.addLocation(2465, "tag2465",52,53);
        testMap.addLocation(2466, "tag2466",52,54);
        testMap.addLocation(2467, "tag2467",52,55);
        testMap.addLocation(2468, "tag2468",52,56);
        testMap.addLocation(2469, "tag2469",52,57);
        testMap.addLocation(2470, "tag2470",52,58);
        testMap.addLocation(2471, "tag2471",52,59);
        testMap.addLocation(2472, "tag2472",52,60);
        testMap.addLocation(2473, "tag2473",52,61);
        testMap.addLocation(2474, "tag2474",52,62);
        testMap.addLocation(2475, "tag2475",52,63);
        testMap.addLocation(2476, "tag2476",52,64);
        testMap.addLocation(2477, "tag2477",52,65);
        testMap.addLocation(2478, "tag2478",52,66);
        testMap.addLocation(2479, "tag2479",52,67);
        testMap.addLocation(2480, "tag2480",52,68);
        testMap.addLocation(2481, "tag2481",52,69);
        testMap.addLocation(2482, "tag2482",52,70);
        testMap.addLocation(2483, "tag2483",52,71);
        testMap.addLocation(2484, "tag2484",52,72);
        testMap.addLocation(2485, "tag2485",52,91);
        testMap.addLocation(2486, "tag2486",52,92);
        testMap.addLocation(2487, "tag2487",52,96);
        testMap.addLocation(2488, "tag2488",52,97);
        testMap.addLocation(2489, "tag2489",53,91);
        testMap.addLocation(2490, "tag2490",53,92);
        testMap.addLocation(2491, "tag2491",53,96);
        testMap.addLocation(2492, "tag2492",53,97);
        testMap.addLocation(2493, "tag2493",54,91);
        testMap.addLocation(2494, "tag2494",54,92);
        testMap.addLocation(2495, "tag2495",54,96);
        testMap.addLocation(2496, "tag2496",54,97);
        testMap.addLocation(2497, "tag2497",55,91);
        testMap.addLocation(2498, "tag2498",55,92);
        testMap.addLocation(2499, "tag2499",55,96);
        testMap.addLocation(2500, "tag2500",55,97);
        testMap.addLocation(2501, "tag2501",56,91);
        testMap.addLocation(2502, "tag2502",56,92);
        testMap.addLocation(2503, "tag2503",56,93);
        testMap.addLocation(2504, "tag2504",56,94);
        testMap.addLocation(2505, "tag2505",56,95);
        testMap.addLocation(2506, "tag2506",56,96);
        testMap.addLocation(2507, "tag2507",56,97);
        testMap.addLocation(2508, "tag2508",57,91);
        testMap.addLocation(2509, "tag2509",57,92);
        testMap.addLocation(2510, "tag2510",57,93);
        testMap.addLocation(2511, "tag2511",57,94);
        testMap.addLocation(2512, "tag2512",57,95);
        testMap.addLocation(2513, "tag2513",57,96);
        testMap.addLocation(2514, "tag2514",57,97);
        testMap.addLocation(2515, "tag2515",58,91);
        testMap.addLocation(2516, "tag2516",58,92);
        testMap.addLocation(2517, "tag2517",59,91);
        testMap.addLocation(2518, "tag2518",59,92);
        testMap.addLocation(2519, "tag2519",60,91);
        testMap.addLocation(2520, "tag2520",60,92);
        testMap.addLocation(2521, "tag2521",61,91);
        testMap.addLocation(2522, "tag2522",61,92);
        testMap.addLocation(2523, "tag2523",62,91);
        testMap.addLocation(2524, "tag2524",62,92);
        testMap.addLocation(2525, "tag2525",63,91);
        testMap.addLocation(2526, "tag2526",63,92);
        testMap.addLocation(2527, "tag2527",64,91);
        testMap.addLocation(2528, "tag2528",64,92);
        testMap.addLocation(2529, "tag2529",64,93);
        testMap.addLocation(2530, "tag2530",64,94);
        testMap.addLocation(2531, "tag2531",64,95);
        testMap.addLocation(2532, "tag2532",64,96);
        testMap.addLocation(2533, "tag2533",64,97);
        testMap.addLocation(2534, "tag2534",64,98);
        testMap.addLocation(2535, "tag2535",64,99);
        testMap.addLocation(2536, "tag2536",64,100);
        testMap.addLocation(2537, "tag2537",65,91);
        testMap.addLocation(2538, "tag2538",65,92);
        testMap.addLocation(2539, "tag2539",65,93);
        testMap.addLocation(2540, "tag2540",65,94);
        testMap.addLocation(2541, "tag2541",65,95);
        testMap.addLocation(2542, "tag2542",65,96);
        testMap.addLocation(2543, "tag2543",65,97);
        testMap.addLocation(2544, "tag2544",65,98);
        testMap.addLocation(2545, "tag2545",65,99);
        testMap.addLocation(2546, "tag2546",65,100);
        testMap.addLocation(2547, "tag2547",66,98);
        testMap.addLocation(2548, "tag2548",66,99);
        testMap.addLocation(2549, "tag2549",66,100);
        testMap.addLocation(2550, "tag2550",67,98);
        testMap.addLocation(2551, "tag2551",67,99);
        testMap.addLocation(2552, "tag2552",67,100);
        testMap.addLocation(2553, "tag2553",68,98);
        testMap.addLocation(2554, "tag2554",68,99);
        testMap.addLocation(2555, "tag2555",68,100);
        testMap.addLocation(2556, "tag2556",69,98);
        testMap.addLocation(2557, "tag2557",69,99);
        testMap.addLocation(2558, "tag2558",69,100);
        testMap.addLocation(2559, "tag2559",70,98);
        testMap.addLocation(2560, "tag2560",70,99);
        testMap.addLocation(2561, "tag2561",70,100);


        for (Location l : testMap.getAllLocation()){
            //wall
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // フォーカス変更後の処理
        super.onWindowFocusChanged(hasFocus);
        testMap.setImagesPalameter();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if  (event.getPointerCount() == 2){
            testMap.getmScaleGestureDetector().onTouchEvent(event);
        }

        testMap.getmGestureDetector().onTouchEvent(event);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                Log.d("ScrTouch",event.getX() + " " +event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                updateFlg = ++updateFlg % UPDATE_INTERVAL;
                if  (updateFlg == 0) testMap.updateTrackedXY((int)event.getX(), (int)event.getY());
                break;

            case MotionEvent.ACTION_UP:
                //離した時
                testMap.trackEnd();
                break;
        }

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /*
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    */

    // The action listener for the EditText widget, to listen for the return key
    /*
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    if(D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };
     */

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                //case MESSAGE_WRITE:

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    //受け取ったメッセージがaddコマンドならロケーションとして追加
                    //addLocation(readMessage);

                    //数字なら対象のロケーションをget。
                    Location tmp = getLocation(readMessage);

                    if  (tmp != null && testMap.isLoaded()){
                        //tagId.setText(String.valueOf(tmp.getId()));
                        //tagName.setText(tmp.getName());
                        testMap.updateMap(tmp.getId());
                        //testMap.makeMap(tmp.getId());
                    }


                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    //strが数字ならidが一致するLocationをreturn
    private Location getLocation(String str){
        if (!checkStringNumber(str)) return null;
        int id = Integer.parseInt(str);
        return testMap.getLocationById(id);
    }

    //引数が数字か否かを判定
    private boolean checkStringNumber(String number) {
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case POINT_SELECT:
                boolean flag = false;
                int start = 0, goal = 0;
                if(resultCode == RESULT_CANCELED) break;
                Bundle extras = data.getExtras();

                if (extras != null) { //エラー処理もろもろ
                    if ((extras.getString("RETURN_STARTING_POINT") == null) || !checkStringNumber(extras.getString("RETURN_STARTING_POINT"))) {
                        String str = getString(R.string.no_results) + extras.getString("RETURN_STARTING_POINT");
                        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                    if ((extras.getString("RETURN_GOAL_POINT") == null) || !checkStringNumber(extras.getString("RETURN_GOAL_POINT"))) {
                        Toast.makeText(this, getString(R.string.no_results) + extras.getString("RETURN_GOAL_POINT"), Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                    if (flag) break;
                    start = Integer.parseInt(extras.getString("RETURN_STARTING_POINT")); //start及びgoalが正規のタグidならセット
                    goal  = Integer.parseInt(extras.getString("RETURN_GOAL_POINT"));
                    if (testMap.getLocationById(start) == null) {
                        Toast.makeText(this, start + " " + getString(R.string.no_exist), Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                    if  (testMap.getLocationById(goal) == null) {
                        Toast.makeText(this, goal + " " + getString(R.string.no_exist), Toast.LENGTH_LONG).show();
                        flag = true;
                    }
                    if (flag) break;
                }

                switch (resultCode) {
                    case RESULT_DEMO: //デモの場合

                        testMap.makeMap(start);
                        if (!testMap.routeNavigation(start, goal, true)) {
                            Toast.makeText(this, getString(R.string.route_not_found), Toast.LENGTH_LONG).show();
                        }
                        break;

                    case RESULT_SEARCH: //検索の場合
                        testMap.makeMap(start);
                        if (!testMap.routeNavigation(start, goal, false)) {
                            Toast.makeText(this, getString(R.string.route_not_found), Toast.LENGTH_LONG).show();
                        }
                        break;
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            case R.id.Point_Select:
                // 検索
                serverIntent = new Intent(this, PointSelect.class);
                startActivityForResult(serverIntent, POINT_SELECT);
                return true;
        }
        return false;
    }

	@Override
	public void onFragmentInteraction(String id) {

	}
}
