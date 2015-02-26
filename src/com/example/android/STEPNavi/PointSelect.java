package com.example.android.STEPNavi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.BluetoothChat.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by crow on 2015/02/14.
 */
public class PointSelect extends Activity implements TextWatcher {
    //Intent resule codes
    private final int RESULT_DEMO = 1;
    private final int RESULT_SEARCH = 2;
    //リスト
    //private List<Location> location = new ArrayList<Location>();
    //エディットテキスト
    private EditText startEd;
    private EditText goalEd;
    //ボタン
    private Button searchBtn;
    private Button demoBtn;
    private Button cancelBtn;

    private LinearLayout mainlinear;

    //private ListView listView;
    //private static List<String> dataList = new ArrayList<String>();
    //private static ArrayAdapter<String> adapter;

    //エディットテキスト最大文字数
    private int editTextLengthMax = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //画面初期化
        setContentView(R.layout.pointselect);
        //キャンセルセット
        setResult(Activity.RESULT_CANCELED);
        //引数受け取り
        //location = (ArrayList<Location>)getIntent().getSerializableExtra("MAP_DATA");

        //エディットテキスト
        startEd = (EditText) findViewById(R.id.StartPtEd);
        startEd.addTextChangedListener(this);
        goalEd = (EditText) findViewById(R.id.GoalPtEd);
        goalEd.addTextChangedListener(this);

        //ボタン
        searchBtn = (Button) findViewById(R.id.SearchBtn);
        demoBtn = (Button) findViewById(R.id.DemoBtn);
        cancelBtn = (Button) findViewById(R.id.CancelBtn);

        mainlinear = (LinearLayout) findViewById(R.id.mainLinear);

        //デモボタンが押されたとき
        demoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Searchボタンがクリックされた時に呼び出されます
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mainlinear.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                Intent intent = new Intent();
                intent.putExtra("RETURN_STARTING_POINT", startEd.getText().toString());
                intent.putExtra("RETURN_GOAL_POINT", goalEd.getText().toString());
                setResult(RESULT_DEMO,intent);
                demoBtn.requestFocus();
                finish();
            }
        });

        //サーチボタンが押されたとき
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Searchボタンがクリックされた時に呼び出されます
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mainlinear.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                Intent intent = new Intent();
                intent.putExtra("RETURN_STARTING_POINT", startEd.getText().toString());
                intent.putExtra("RETURN_GOAL_POINT", goalEd.getText().toString());
                setResult(RESULT_SEARCH,intent);
                searchBtn.requestFocus();
                finish();
            }
        });

        //キャンセルボタンが押されたとき
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cancelボタンがクリックされた時に呼び出されます
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        //リスト
        /*listView = (ListView) findViewById(R.id.startplistView);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        adapter.clear();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                Intent intent = new Intent();
                intent.putExtra("RETURN_STARTING_POINT", item);
                setResult(RESULT_OK,intent);
                finish();
            }
        });*/
    }
    public void afterTextChanged(Editable s) {
        //Log.d("d", "after");
        //Toast.makeText(this, "after", Toast.LENGTH_SHORT).show();
        //adapter.add(s.toString());
        //adapter.clear();
        /*for(Location l : location){
            if( l.getName() == s.toString()){
                adapter.add(s.toString());
            }
        }*/
    }

    public void beforeTextChanged(CharSequence s, int start,int count, int after) {
        //Log.d("d", "before");
        //Toast.makeText(this, "before", Toast.LENGTH_SHORT).show();
    }

    public void onTextChanged(CharSequence s, int start,int before, int count) {
        //Log.d("d", "changed!!!!!!!!!!!!!!");
        //Toast.makeText(this, "changed!!!!!!!!!!!!!!", Toast.LENGTH_SHORT).show();
        if(startEd.getText().toString().length() > editTextLengthMax) {
            String startText = startEd.getText().toString();
            startEd.setText(startText.substring(0, startText.length() + (editTextLengthMax - s.toString().length())));
            startEd.setSelection(startText.length() - 1);
        }else if(goalEd.getText().toString().length() > editTextLengthMax){
            String goalText = goalEd.getText().toString();
            goalEd.setText(goalText.substring(0, goalText.length() + ( editTextLengthMax - s.toString().length() )));
            goalEd.setSelection(goalText.length()-1);
        }
    }
}
