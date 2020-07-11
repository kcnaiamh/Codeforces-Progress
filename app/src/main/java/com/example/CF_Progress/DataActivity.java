package com.example.CF_Progress;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 2 mothods in DataActivity class
 * - onCreate
 * - getStatus
 */
public class DataActivity extends AppCompatActivity {

    private static final String TAG = "DataActivityFF";
    private static final String BASE_URL = "https://codeforces.com/api/";
    private static final int COUNT = 100000; // Maximum number of submissions fetch from codeforces
    private static final int FROM = 1; // 1-based index of the first submission

    private Retrofit retrofit;
    private String handle;
    private TextView handleName;
    private ApiInterfaceGetStatus apiInterfaceGS;
    private ApiInterfaceGetUserInfo apiInterfaceGUI;

    private int x = 0;
    private int y;

    private ScatterChart scatterChart;
    private ArrayList<ArrayList<Entry>> entries = new ArrayList<>();
    private ArrayList<IScatterDataSet> scatterDataSets = new ArrayList<>();
    private ScatterData scatterData;

    private LinearLayout LLProgressBar, LLData;

    private ImageView avatar;
    private TextView fullName, rating, countryName, organizationName, rank,
                     contribution, maxRank, friendOfCount, email, registered,
                     scoreView;

    long currentValue = 0;
    boolean firsttime = true;
    long prevSubmissionTime = 0;
    long diff = 0; // difference of days between consecutive submissions
    double penalty = 0;
    long userScore;

    private ProgressBar score;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        LLProgressBar = findViewById(R.id.LLProgressBarId);
        LLData = findViewById(R.id.LLDataId);

        // for passing data from one layout to another
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            handle = bundle.getString("tag");
        }
        handleName = findViewById(R.id.handleId);
        handleName.setText(handle);

        scatterChart = findViewById(R.id.scatterChart);
        avatar = findViewById(R.id.avatarId);
        fullName = findViewById(R.id.fullNameId);
        rating = findViewById(R.id.ratingId);
        countryName = findViewById(R.id.countryId);
        organizationName = findViewById(R.id.organizationId);
        rank = findViewById(R.id.rankId);
        contribution = findViewById(R.id.contributionId);
        maxRank = findViewById(R.id.maxRankId);
        friendOfCount = findViewById(R.id.friendOfCountId);
        email = findViewById(R.id.emailId);
        registered = findViewById(R.id.registeredId);
        score = findViewById(R.id.scoreId);
        scoreView = findViewById(R.id.scoreViewId);

        retrofitFun();
        init();
        new Task().execute();
    }

    class Task extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            LLProgressBar.setVisibility(View.VISIBLE);
            LLData.setVisibility(View.GONE);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... strings) {

            Call<UserStatus> call = apiInterfaceGS.getUserStatus(handle, FROM, COUNT);
            call.enqueue(new Callback<UserStatus>() {

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onResponse(Call<UserStatus> call, Response<UserStatus> response) {

                    List<Result> results = response.body().getResults();

                    // to get oldest to newest sumbission
                    Collections.reverse(results);
                    HashSet<String> hs = new HashSet<>();

                    for (Result result : results) {
                        if (result.getVerdict().equals("OK")) {
                            y = result.getProblem().getRating(); // rating
                            String problemName = result.getProblem().getName();

                            if (y >= 800 && y <= 3500 && !hs.contains(problemName)) {
                                entries.get(y / 100 - 8).add(new Entry(x, y));
                                hs.add(problemName);
                                x++;

                                if (firsttime) {
                                    prevSubmissionTime = result.getCreationTimeSeconds();
                                    firsttime = false;
                                } else {
                                    diff = (result.getCreationTimeSeconds() - prevSubmissionTime)/86400;
                                    prevSubmissionTime = result.getCreationTimeSeconds();
                                }

                                if (y < 1200) penalty = 0.20;
                                else if (y < 1400) penalty = 0.17;
                                else if (y < 1600) penalty = 0.14;
                                else if (y < 1900) penalty = 0.11;
                                else if (y < 2100) penalty = 0.08;
                                else if (y < 2300) penalty = 0.05;
                                else if (y < 2400) penalty = 0.04;
                                else if (y < 2600) penalty = 0.03;
                                else if (y < 3000) penalty = 0.02;
                                else if (y <= 3500) penalty = 0.01;

                                currentValue += (y - y*penalty*diff);
                            }
                        }
                    }
                    userScore = currentValue/35/(x-1);
                    scoreView.setText(String.valueOf(userScore) + "/100");

                    int[] ratingArray = getResources().getIntArray(R.array.cf_lvl);

                    for (int i = 0; i < 28; i++) {
                        if (entries.get(i).size() > 0) {
                            ScatterDataSet sds = new ScatterDataSet(entries.get(i), "");
                            sds.setColor(ratingArray[i]);
                            sds.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
                            sds.setScatterShapeSize(11f);
                            scatterDataSets.add(sds);
                        }
                    }

                    scatterData = new ScatterData(scatterDataSets);
                    scatterChart.setData(scatterData);

                    // set scrollable and scalable
                    scatterChart.setDragEnabled(true);
                    scatterChart.setScaleEnabled(true);

                    scatterChart.animateXY(0, 5000, Easing.EaseOutBounce, Easing.EaseOutBounce);
                    scatterChart.invalidate();

                    scatterChart.getDescription().setEnabled(false);
                    scatterChart.getLegend().setEnabled(false);

                    // axis properties
                    scatterChart.getAxisRight().setEnabled(false);
                    XAxis xAxis = scatterChart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    xAxis.setGranularity(1f);
                    xAxis.setGranularityEnabled(true);
                    xAxis.setDrawGridLines(true);
                    xAxis.setAxisMaximum(x + 10);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    YAxis yAxis = scatterChart.getAxisLeft();
                    yAxis.setDrawGridLines(true);
                    yAxis.setSpaceTop(1);
                    yAxis.setAxisMinimum(750f);
                }

                @Override
                public void onFailure(Call<UserStatus> call, Throwable t) {
                    Log.d(TAG, "onFailure: " + t.getMessage());
                }
            });

            Call<UserInfo> callUI = apiInterfaceGUI.getUserInfo(handle);
            callUI.enqueue(new Callback<UserInfo>() {

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                    List<ResultOfUserInfo> results = response.body().getResultOfUserInfo();

                    ResultOfUserInfo result = results.get(0);

                    String mImageAvatar = "https:" + result.getAvatar();
                    String mFullName = "Name: " + result.getFirstName() + " " + result.getLastName();
                    String mRating = result.getRating() + "/" + result.getMaxRating();
                    String mCountryName = "Country: " + result.getCountry();
                    String mOrganizationName = "Organization: " + result.getOrganization();
                    String mRank = "Rank: " + result.getRank();
                    String mContribution = "Contribution: " + result.getContribution();
                    String mMaxRank = result.getMaxRank();
                    String mFriendOfCount = "Friend of: " + result.getFriendOfCount();
                    String mEmail = "Email: " + result.getEmail();
                    Date d = new Date(result.getRegistrationTimeSeconds() * 1000L);
                    String date = new SimpleDateFormat("dd MMM, yyyy").format(d);
                    String mRegistered = "Registered: " + date;

                    Picasso.get().load(mImageAvatar).into(avatar);

                    if (!mFullName.equals("Name: null null")) {
                        fullName.setText(mFullName);
                    } else {
                        fullName.setVisibility(View.GONE);
                    }

                    if (!mCountryName.equals("Country: null")) {
                        countryName.setText(mCountryName);
                    } else {
                        countryName.setVisibility(View.GONE);
                    }

                    if (!mOrganizationName.equals("Organization: ") && !mOrganizationName.equals("Organization: null")) {
                        organizationName.setText(mOrganizationName);
                    } else {
                        organizationName.setVisibility(View.GONE);
                    }

                    if (!mRank.equals("Rank: null")) {
                        rank.setText(mRank);
                    } else {
                        rank.setVisibility(View.GONE);
                    }

                    if (!mEmail.equals("Email: null")) {
                        email.setText(mEmail);
                    } else {
                        email.setVisibility(View.GONE);
                    }

                    rating.setText(mRating);
                    contribution.setText(mContribution);
                    maxRank.setText(mMaxRank);
                    friendOfCount.setText(mFriendOfCount);
                    registered.setText(mRegistered);

                    for (int i = 0; i < 2; i++) {

                        int ratingNumber = 0;
                        if (i == 0) {
                            ratingNumber = result.getRating(); // current rating
                        } else if (i == 1) {
                            ratingNumber = result.getMaxRating(); // max rating
                        }

                        if (ratingNumber == 0) {
                            // color is black
                        } else if (ratingNumber < 1200) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_800, getResources().newTheme()));
                            } else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_800, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 1400) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_1300, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_1300, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 1600) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_1500, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_1500, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 1900) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_1700, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_1700, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 2100) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_2000, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_2000, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 2300) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_2200, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_2200, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 2400) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_2300, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_2300, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 2600) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_2500, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_2500, getResources().newTheme()));
                            }
                        } else if (ratingNumber < 3000) {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_2800, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_2800, getResources().newTheme()));
                            }
                        } else {
                            if (i == 0) {
                                handleName.setTextColor(getResources().getColor(R.color.lvl_3000, getResources().newTheme()));
                            }
                            else if (i == 1) {
                                maxRank.setTextColor(getResources().getColor(R.color.lvl_3000, getResources().newTheme()));
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<UserInfo> call, Throwable t) {

                }
            });

            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            LLProgressBar.setVisibility(View.GONE);
            LLData.setVisibility(View.VISIBLE);

            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {


                    for (int i = 0; i <= userScore; i++) {
                        try {
                            Thread.sleep(40);
                            score.setProgress(i);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();

            super.onPostExecute(aBoolean);
        }
    }

    private void retrofitFun() {
        // API client
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterfaceGS = retrofit.create(ApiInterfaceGetStatus.class);
        apiInterfaceGUI = retrofit.create(ApiInterfaceGetUserInfo.class);
    }

    private void init() {
        for (int i = 0; i < 28; i++) {
            entries.add(new ArrayList());
        }
    }

}